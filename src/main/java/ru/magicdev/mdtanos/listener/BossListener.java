package ru.magicdev.mdtanos.listener;

import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import ru.magicdev.mdtanos.MDTanos;
import ru.magicdev.mdtanos.MDUtils;
import ru.magicdev.mdtanos.manager.BossManager;

public class BossListener implements Listener {
    private final BossManager bossManager;
    private final MDTanos plugin;

    public BossListener(MDTanos plugin, BossManager bossManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity().hasMetadata("IsMDTanosBoss") && event.isCancelled()) {
            event.setCancelled(false);
            MDUtils.log("Обнаружен блок спавна босса! Принудительно разрешаю спавн.");
        }

    }

    @EventHandler
    public void onBossTarget(EntityTargetEvent event) {
        if (event.getEntity() == this.bossManager.getActiveBoss()) {
            if (!(event.getTarget() instanceof Player)) {
                Player nearest = null;
                double dist = 25.0D;
                Iterator var5 = event.getEntity().getWorld().getPlayers().iterator();

                while(var5.hasNext()) {
                    Player p = (Player)var5.next();
                    double d = p.getLocation().distanceSquared(event.getEntity().getLocation());
                    if (d < dist * dist) {
                        dist = Math.sqrt(d);
                        nearest = p;
                    }
                }

                if (nearest != null) {
                    event.setTarget(nearest);
                }
            }

            if (event.getTarget() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity)event.getTarget();
                if (target.hasMetadata("isMinion")) {
                    event.setCancelled(true);
                }
            }
        }

    }

    @EventHandler
    public void onBossDamage(EntityDamageEvent event) {
        if (event.getEntity() == this.bossManager.getActiveBoss()) {
            LivingEntity boss = this.bossManager.getActiveBoss();
            double health = Math.max(0.0D, boss.getHealth() - event.getFinalDamage());
            double max = boss.getMaxHealth();
            if (this.bossManager.getBossBar() != null) {
                this.bossManager.getBossBar().setProgress(Math.max(0.0D, Math.min(1.0D, health / max)));
            }
        }

    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        if (event.getEntity() == this.bossManager.getActiveBoss()) {
            String bossId = this.bossManager.getActiveBossId();
            Player killer = event.getEntity().getKiller();
            if (killer != null) {
                List<String> rewardCommands = this.plugin.getConfig().getStringList("settings.reward-commands");
                Iterator var5 = rewardCommands.iterator();

                while(var5.hasNext()) {
                    String cmd = (String)var5.next();
                    String formattedCmd = cmd.replace("%player%", killer.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
                }
            }

            this.bossManager.dropLoot(event.getEntity().getLocation());
            if (bossId != null) {
                YamlConfiguration bossCfg = MDUtils.loadCfg(this.plugin, "boss.yml");
                if (bossCfg != null) {
                    String deathMsg = bossCfg.getString("bosses." + bossId + ".messages.death", "&aБосс повержен!");
                    Bukkit.broadcastMessage(MDUtils.color(deathMsg));
                }
            }

            this.bossManager.despawnBoss();
        }

    }
}