package ru.magicdev.mdtanos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import ru.magicdev.mdtanos.command.CommandHandler;
import ru.magicdev.mdtanos.command.TabHandler;
import ru.magicdev.mdtanos.listener.AltarListener;
import ru.magicdev.mdtanos.listener.BossListener;
import ru.magicdev.mdtanos.listener.MineManager;
import ru.magicdev.mdtanos.listener.PlayerDeathListener;
import ru.magicdev.mdtanos.manager.BossManager;
import ru.magicdev.mdtanos.manager.StoneManager;

public class MDTanos extends JavaPlugin {
    private StoneManager stoneManager;
    private BossManager bossManager;
    private FileConfiguration messagesConfig;
    private final Map<UUID, Set<String>> collectedStones = new HashMap();

    public void onEnable() {
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
        }

        this.saveDefaultConfig();
        this.saveResource("boss.yml", false);
        this.saveResource("mine.yml", false);
        this.saveResource("messages.yml", false);
        this.saveResource("altar.yml", false);
        this.loadMessagesConfig();
        this.setupHologram();
        this.stoneManager = new StoneManager(this);
        this.bossManager = new BossManager(this);
        this.bossManager.startAutoSpawnTask();
        this.getServer().getPluginManager().registerEvents(new MineManager(this), this);
        this.getServer().getPluginManager().registerEvents(new AltarGUI(this), this);
        this.getServer().getPluginManager().registerEvents(new AltarListener(this), this);
        this.getServer().getPluginManager().registerEvents(new BossListener(this, this.bossManager), this);
        this.getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        this.getCommand("mtanos").setExecutor(new CommandHandler(this));
        this.getCommand("mtanos").setTabCompleter(new TabHandler(this));
        MDUtils.log("MDTanos успешно запущен!");
    }

    public void setupHologram() {
        if (this.getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                try {
                    YamlConfiguration cfg = MDUtils.loadCfg(this, "altar.yml");
                    if (cfg == null) {
                        return;
                    }

                    Location loc = new Location(Bukkit.getWorld(cfg.getString("altar-location.world", "world")), cfg.getDouble("altar-location.x") + 0.5D, cfg.getDouble("altar-location.y") + 2.0D, cfg.getDouble("altar-location.z") + 0.5D);
                    Class dhApi = Class.forName("eu.decentsoftware.holograms.api.DHAPI");

                    try {
                        dhApi.getMethod("removeHologram", String.class).invoke((Object)null, "thanos_altar");
                    } catch (Exception var5) {
                    }

                    dhApi.getMethod("createHologram", String.class, Location.class, List.class).invoke((Object)null, "thanos_altar", loc, cfg.getStringList("hologram.lines"));
                } catch (Exception var6) {
                    MDUtils.log("Ошибка голограммы: " + var6.getMessage());
                }

            }, 20L);
        }
    }

    public void openAltarGUI(Player p) {
        YamlConfiguration cfg = MDUtils.loadCfg(this, "altar.yml");
        if (cfg != null) {
            int rows = cfg.getInt("gui.rows", 3);
            String title = MDUtils.color(cfg.getString("gui.title", "Алтарь"));
            Inventory inv = Bukkit.createInventory((InventoryHolder)null, rows * 9, title);
            List slotList;
            if (cfg.contains("gui.decoration")) {
                ConfigurationSection decoSection = cfg.getConfigurationSection("gui.decoration");
                Iterator var7 = decoSection.getKeys(false).iterator();

                while(var7.hasNext()) {
                    String key = (String)var7.next();
                    String matName = cfg.getString("gui.decoration." + key + ".material", "GRAY_STAINED_GLASS_PANE");
                    Material mat = Material.getMaterial(matName);
                    String name = cfg.getString("gui.decoration." + key + ".display-name", " ");
                    List<String> lore = cfg.getStringList("gui.decoration." + key + ".lore");
                    slotList = cfg.getList("gui.decoration." + key + ".slots", new ArrayList());
                    ItemStack item;
                    if (mat == Material.PLAYER_HEAD) {
                        item = this.getSkull(cfg.getString("gui.decoration." + key + ".skull-value", ""));
                    } else {
                        item = new ItemStack(mat != null ? mat : Material.GRAY_STAINED_GLASS_PANE);
                    }

                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(MDUtils.color(name));
                    List<String> coloredLore = new ArrayList();
                    Iterator var17 = lore.iterator();

                    while(var17.hasNext()) {
                        String line = (String)var17.next();
                        coloredLore.add(MDUtils.color(line));
                    }

                    meta.setLore(coloredLore);
                    item.setItemMeta(meta);
                    var17 = this.parseSlots(slotList).iterator();

                    while(var17.hasNext()) {
                        int slot = (Integer)var17.next();
                        if (slot >= 0 && slot < inv.getSize()) {
                            inv.setItem(slot, item);
                        }
                    }
                }
            }

            Set<String> collected = (Set)this.collectedStones.getOrDefault(p.getUniqueId(), new HashSet());
            ConfigurationSection itemsSection = cfg.getConfigurationSection("gui.items");
            List<String> loreTemplate = cfg.getStringList("gui.status-template");
            if (loreTemplate.isEmpty()) {
                loreTemplate = Arrays.asList("&7Сдано: &e%collected%&7/&e%required%");
            }

            if (itemsSection != null) {
                Iterator var25 = itemsSection.getKeys(false).iterator();

                while(var25.hasNext()) {
                    String stoneType = (String)var25.next();
                    int required = cfg.getInt("gui.items." + stoneType + ".required", 1);
                    int current = collected.contains(stoneType.toLowerCase()) ? 1 : 0;
                    slotList = cfg.getList("gui.items." + stoneType + ".slots", Collections.singletonList(cfg.getInt("gui.items." + stoneType + ".slot", 0)));
                    String displayName = cfg.getString("gui.items." + stoneType + ".display-name", "Камень");
                    String skullValue = cfg.getString("gui.items." + stoneType + ".skull-value", "");
                    ItemStack item = this.getSkull(skullValue);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(MDUtils.color(displayName));
                    List<String> lore = new ArrayList();
                    Iterator var19 = loreTemplate.iterator();

                    while(var19.hasNext()) {
                        String line = (String)var19.next();
                        String formattedLine = line.replace("%collected%", String.valueOf(current)).replace("%required%", String.valueOf(required));
                        lore.add(MDUtils.color(formattedLine));
                    }

                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    var19 = this.parseSlots(slotList).iterator();

                    while(var19.hasNext()) {
                        int slot = (Integer)var19.next();
                        if (slot >= 0 && slot < inv.getSize()) {
                            inv.setItem(slot, item);
                        }
                    }
                }
            }

            p.openInventory(inv);
        }
    }

    private List<Integer> parseSlots(List<?> slotData) {
        List<Integer> slots = new ArrayList();
        if (slotData == null) {
            return slots;
        } else {
            Iterator var3 = slotData.iterator();

            while(true) {
                while(var3.hasNext()) {
                    Object obj = var3.next();
                    String s = obj.toString();
                    if (s.contains("-")) {
                        String[] parts = s.split("-");
                        int start = Integer.parseInt(parts[0]);
                        int end = Integer.parseInt(parts[1]);

                        for(int i = start; i <= end; ++i) {
                            slots.add(i);
                        }
                    } else {
                        slots.add(Integer.parseInt(s));
                    }
                }

                return slots;
            }
        }
    }

    public ItemStack getSkull(String base64) {
        if (base64 != null && !base64.isEmpty()) {
            String var10000 = String.valueOf(UUID.randomUUID());
            String nbt = "{SkullOwner:{Id:\"" + var10000 + "\",Properties:{textures:[{Value:\"" + base64 + "\"}]}}}";

            try {
                return Bukkit.getUnsafe().modifyItemStack(new ItemStack(Material.PLAYER_HEAD), nbt);
            } catch (Exception var4) {
                return new ItemStack(Material.PLAYER_HEAD);
            }
        } else {
            return new ItemStack(Material.PLAYER_HEAD);
        }
    }

    private void loadMessagesConfig() {
        this.messagesConfig = MDUtils.loadCfg(this, "messages.yml");
    }

    public FileConfiguration getMessagesConfig() {
        return this.messagesConfig;
    }

    public StoneManager getStoneManager() {
        return this.stoneManager;
    }

    public BossManager getBossManager() {
        return this.bossManager;
    }

    public Map<UUID, Set<String>> getPlayerStones() {
        return this.collectedStones;
    }
}