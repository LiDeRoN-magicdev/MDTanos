package ru.magicdev.mdtanos.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPC.Metadata;
import net.citizensnpcs.trait.LookClose;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import ru.magicdev.mdtanos.MDTanos;
import ru.magicdev.mdtanos.MDUtils;

public class BossManager {
    private final MDTanos plugin;
    private BossBar bossBar;
    private LivingEntity activeBoss;
    private String activeBossId;
    private final Random random = new Random();

    public BossManager(MDTanos plugin) {
        this.plugin = plugin;
    }

    public boolean isBossActive() {
        return this.activeBoss != null && this.activeBoss.isValid() && !this.activeBoss.isDead();
    }

    public void startAutoSpawnTask() {
        YamlConfiguration cfg = MDUtils.loadCfg(this.plugin, "boss.yml");
        if (cfg != null && cfg.contains("bosses")) {
            Iterator var2 = cfg.getConfigurationSection("bosses").getKeys(false).iterator();

            while(var2.hasNext()) {
                final String bossId = (String)var2.next();
                boolean enabled = cfg.getBoolean("bosses." + bossId + ".auto-spawn.enabled", false);
                int intervalMinutes = cfg.getInt("bosses." + bossId + ".auto-spawn.interval-minutes", 60);
                if (enabled) {
                    (new BukkitRunnable() {
                        public void run() {
                            if (!BossManager.this.isBossActive()) {
                                BossManager.this.spawnBoss(bossId);
                            }

                        }
                    }).runTaskTimer(this.plugin, (long)intervalMinutes * 1200L, (long)intervalMinutes * 1200L);
                    MDUtils.log("Авто-спавн включен для босса: " + bossId + " (каждые " + intervalMinutes + " мин)");
                }
            }

        }
    }

    public String getActiveBossId() {
        return this.activeBossId;
    }

    private void markAsBossEntity(LivingEntity entity) {
        entity.setMetadata("IsMDTanosBoss", new FixedMetadataValue(this.plugin, true));
        entity.setPersistent(true);
        entity.addScoreboardTag("MDTanosBoss");
    }

    public void addLoot(String bossId, ItemStack item, double chance) {
        File f = new File(this.plugin.getDataFolder(), "boss.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        List<String> lootList = cfg.getStringList("bosses." + bossId + ".loot");
        String var10001 = this.serializeItem(item);
        lootList.add(var10001 + ":" + chance);
        cfg.set("bosses." + bossId + ".loot", lootList);

        try {
            cfg.save(f);
        } catch (Exception var9) {
            var9.printStackTrace();
        }

    }

    public void dropLoot(Location loc) {
        if (this.activeBossId != null) {
            YamlConfiguration cfg = MDUtils.loadCfg(this.plugin, "boss.yml");
            if (cfg != null) {
                List<String> lootList = cfg.getStringList("bosses." + this.activeBossId + ".loot");
                Iterator var4 = lootList.iterator();

                while(var4.hasNext()) {
                    String line = (String)var4.next();
                    String[] parts = line.split(":");

                    try {
                        if (parts[0].length() > 50) {
                            ItemStack item = this.deserializeItem(parts[0]);
                            double chance = Double.parseDouble(parts[parts.length - 1]);
                            if (Math.random() <= chance) {
                                loc.getWorld().dropItemNaturally(loc, item);
                            }
                        } else {
                            Material mat = Material.valueOf(parts[0].toUpperCase());
                            int amount = Integer.parseInt(parts[1]);
                            double chance = Double.parseDouble(parts[2]);
                            if (Math.random() <= chance) {
                                loc.getWorld().dropItemNaturally(loc, new ItemStack(mat, amount));
                            }
                        }
                    } catch (Exception var11) {
                        this.plugin.getLogger().warning("Ошибка лута для " + this.activeBossId + ": " + line);
                    }
                }

            }
        }
    }

    private String serializeItem(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception var4) {
            var4.printStackTrace();
            return null;
        }
    }

    private ItemStack deserializeItem(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack)dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception var5) {
            return new ItemStack(Material.AIR);
        }
    }

    public void spawnBoss(String bossId) {
        boolean allowMultiple = this.plugin.getConfig().getBoolean("settings.allow-multiple-bosses", false);
        if (allowMultiple || !this.isBossActive()) {
            this.activeBossId = bossId;
            YamlConfiguration cfg = MDUtils.loadCfg(this.plugin, "boss.yml");
            if (cfg != null) {
                String path = "bosses." + bossId;
                World world = Bukkit.getWorld(cfg.getString(path + ".world", "world"));
                Location loc = new Location(world, cfg.getDouble(path + ".x", 100.0D), cfg.getDouble(path + ".y", 64.0D), cfg.getDouble(path + ".z", 100.0D));
                EntityType type = EntityType.valueOf(cfg.getString(path + ".entity-type", "IRON_GOLEM"));
                String name = MDUtils.color(cfg.getString(path + ".display-name", "Boss"));
                final NPC npc = CitizensAPI.getNPCRegistry().createNPC(type, "MDTanosBoss");
                npc.spawn(loc);
                npc.setProtected(false);
                npc.setName(name);
                npc.data().set(Metadata.NAMEPLATE_VISIBLE, true);
                npc.addTrait(LookClose.class);
                ((LookClose)npc.getTrait(LookClose.class)).lookClose(true);
                this.activeBoss = (LivingEntity)npc.getEntity();
                this.activeBoss.setMetadata("MDTanos_NPC_ID", new FixedMetadataValue(this.plugin, npc.getId()));
                this.markAsBossEntity(this.activeBoss);
                this.activeBoss.setRemoveWhenFarAway(false);
                this.activeBoss.setCollidable(true);
                this.activeBoss.setCustomName(name);
                this.activeBoss.setCustomNameVisible(true);
                double hp = cfg.getDouble(path + ".health", 100.0D);
                this.activeBoss.setMaxHealth(hp > 0.0D ? hp : 100.0D);
                this.activeBoss.setHealth(this.activeBoss.getMaxHealth());
                if (this.activeBoss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                    this.activeBoss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(cfg.getDouble(path + ".damage", 5.0D));
                }

                long timeout = this.plugin.getConfig().getLong("timers.boss-despawn-timeout", 600L) * 20L;
                (new BukkitRunnable() {
                    public void run() {
                        if (BossManager.this.isBossActive()) {
                            BossManager.this.despawnBoss();
                            Bukkit.broadcastMessage(MDUtils.color("&cБосс слишком долго был на арене и исчез."));
                        }

                    }
                }).runTaskLater(this.plugin, timeout);
                (new BukkitRunnable() {
                    public void run() {
                        if (BossManager.this.isBossActive() && npc.isSpawned()) {
                            Player nearest = null;
                            double dist = 400.0D;
                            Iterator var4 = BossManager.this.activeBoss.getWorld().getPlayers().iterator();

                            while(var4.hasNext()) {
                                Player p = (Player)var4.next();
                                double d = p.getLocation().distanceSquared(BossManager.this.activeBoss.getLocation());
                                if (d < dist) {
                                    dist = d;
                                    nearest = p;
                                }
                            }

                            if (nearest != null) {
                                npc.getNavigator().setTarget(nearest, true);
                            }

                        } else {
                            this.cancel();
                        }
                    }
                }).runTaskTimer(this.plugin, 20L, 20L);
                String spawnMsg = cfg.getString(path + ".messages.spawn", "&cБосс " + name + " появился!");
                Bukkit.broadcastMessage(MDUtils.color(spawnMsg));
                Iterator var15 = Bukkit.getOnlinePlayers().iterator();

                while(var15.hasNext()) {
                    Player p = (Player)var15.next();
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F);
                }

                this.bossBar = Bukkit.createBossBar(name, BarColor.valueOf(cfg.getString(path + ".bossbar-color", "RED")), BarStyle.SOLID, new BarFlag[0]);
                this.startBossBarTask();
                this.startMinionTask(bossId, cfg, path);
                this.startEffectTask(bossId, cfg, path);
            }
        }
    }

    public void despawnBoss() {
        if (this.activeBoss != null && this.activeBoss.hasMetadata("MDTanos_NPC_ID")) {
            int id = ((MetadataValue)this.activeBoss.getMetadata("MDTanos_NPC_ID").get(0)).asInt();
            NPC npc = CitizensAPI.getNPCRegistry().getById(id);
            if (npc != null) {
                npc.destroy();
            }

            this.activeBoss = null;
        }

        List<NPC> minionsToRemove = new ArrayList();
        Iterator var5 = CitizensAPI.getNPCRegistry().iterator();

        NPC npc;
        while(var5.hasNext()) {
            npc = (NPC)var5.next();
            if (npc.data().has("MDTanosMinion")) {
                minionsToRemove.add(npc);
            }
        }

        var5 = minionsToRemove.iterator();

        while(var5.hasNext()) {
            npc = (NPC)var5.next();
            npc.destroy();
        }

        if (this.bossBar != null) {
            this.bossBar.removeAll();
        }

        this.activeBossId = null;
    }

    private void startBossBarTask() {
        (new BukkitRunnable() {
            public void run() {
                if (!BossManager.this.isBossActive()) {
                    if (BossManager.this.bossBar != null) {
                        BossManager.this.bossBar.removeAll();
                    }

                    this.cancel();
                } else {
                    BossManager.this.bossBar.setProgress(Math.max(0.0D, Math.min(1.0D, BossManager.this.activeBoss.getHealth() / BossManager.this.activeBoss.getMaxHealth())));
                    Iterator var1 = BossManager.this.activeBoss.getWorld().getPlayers().iterator();

                    while(var1.hasNext()) {
                        Player p = (Player)var1.next();
                        if (p.getLocation().distanceSquared(BossManager.this.activeBoss.getLocation()) <= 400.0D) {
                            if (!BossManager.this.bossBar.getPlayers().contains(p)) {
                                BossManager.this.bossBar.addPlayer(p);
                            }
                        } else {
                            BossManager.this.bossBar.removePlayer(p);
                        }
                    }

                }
            }
        }).runTaskTimer(this.plugin, 10L, 10L);
    }

    private void startMinionTask(String bossId, final YamlConfiguration cfg, final String path) {
        final int baseInterval = cfg.getInt(path + ".minions.spawn-interval-seconds", 30);
        final double threshold = cfg.getDouble(path + ".minions.low-hp-threshold", 0.3D);
        (new BukkitRunnable() {
            int ticks = 0;

            public void run() {
                if (!BossManager.this.isBossActive()) {
                    this.cancel();
                } else {
                    if (this.ticks++ >= (BossManager.this.activeBoss.getHealth() / BossManager.this.activeBoss.getMaxHealth() <= threshold ? baseInterval / 2 : baseInterval)) {
                        BossManager.this.spawnMinions(BossManager.this.activeBoss.getLocation(), cfg, path);
                        this.ticks = 0;
                    }

                }
            }
        }).runTaskTimer(this.plugin, 20L, 20L);
    }

    private void spawnMinions(Location loc, YamlConfiguration cfg, String path) {
        int amount = cfg.getInt(path + ".minions.amount", 0);
        if (amount > 0) {
            List<String> typeNames = cfg.getStringList(path + ".minions.types");
            if (typeNames.isEmpty()) {
                typeNames = Collections.singletonList("ZOMBIE");
            }

            for(int i = 0; i < amount; ++i) {
                EntityType mType = EntityType.valueOf(((String)typeNames.get(this.random.nextInt(typeNames.size()))).toUpperCase());
                NPC minionNpc = CitizensAPI.getNPCRegistry().createNPC(mType, "");
                minionNpc.data().set(Metadata.NAMEPLATE_VISIBLE, false);
                minionNpc.setProtected(false);
                minionNpc.data().set("MDTanosMinion", true);
                minionNpc.spawn(loc.clone().add(this.random.nextDouble() * 4.0D - 2.0D, 0.0D, this.random.nextDouble() * 4.0D - 2.0D));
                LivingEntity entity = (LivingEntity)minionNpc.getEntity();
                entity.setMetadata("isMinion", new FixedMetadataValue(this.plugin, true));
                double mHp = cfg.getDouble(path + ".minions.health", 20.0D);
                entity.setMaxHealth(mHp);
                entity.setHealth(mHp);
                Player target = null;
                double dist = 1000.0D;
                Iterator var15 = entity.getWorld().getPlayers().iterator();

                while(var15.hasNext()) {
                    Player p = (Player)var15.next();
                    double d = p.getLocation().distanceSquared(entity.getLocation());
                    if (d < dist) {
                        dist = d;
                        target = p;
                    }
                }

                if (target != null) {
                    minionNpc.getNavigator().setTarget(target, true);
                    if (entity instanceof Creature) {
                        ((Creature)entity).setTarget(target);
                    }
                }
            }

        }
    }

    private void startEffectTask(String bossId, YamlConfiguration cfg, String path) {
        final List<String> effects = cfg.getStringList(path + ".negative-effects");
        if (!effects.isEmpty()) {
            final int interval = cfg.getInt(path + ".effect-interval-seconds", 10);
            (new BukkitRunnable() {
                public void run() {
                    if (!BossManager.this.isBossActive()) {
                        this.cancel();
                    } else {
                        Iterator var1 = BossManager.this.activeBoss.getWorld().getPlayers().iterator();

                        while(true) {
                            Player p;
                            do {
                                if (!var1.hasNext()) {
                                    return;
                                }

                                p = (Player)var1.next();
                            } while(p.getLocation().distanceSquared(BossManager.this.activeBoss.getLocation()) > 400.0D);

                            Iterator var3 = effects.iterator();

                            while(var3.hasNext()) {
                                String eff = (String)var3.next();

                                try {
                                    String[] parts = eff.split(":");
                                    PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                                    if (type != null) {
                                        p.addPotionEffect(new PotionEffect(type, (interval + 2) * 20, Integer.parseInt(parts[2])), true);
                                    }
                                } catch (Exception var7) {
                                }
                            }
                        }
                    }
                }
            }).runTaskTimer(this.plugin, 100L, (long)interval * 20L);
        }
    }

    public BossBar getBossBar() {
        return this.bossBar;
    }

    public LivingEntity getActiveBoss() {
        return this.activeBoss;
    }

    public void setActiveBoss(LivingEntity boss) {
        this.activeBoss = boss;
    }
}