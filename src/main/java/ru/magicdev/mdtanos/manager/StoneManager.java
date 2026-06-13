package ru.magicdev.mdtanos.manager;

import java.util.Collections;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.magicdev.mdtanos.MDTanos;
import ru.magicdev.mdtanos.MDUtils;

public class StoneManager {
    private final MDTanos plugin;
    private final NamespacedKey STONE_KEY;

    public StoneManager(MDTanos plugin) {
        this.plugin = plugin;
        this.STONE_KEY = new NamespacedKey(plugin, "stone_type");
    }

    public ItemStack getSoulStone() {
        return this.getStoneForMine("soul");
    }

    public ItemStack getStoneForMine(String stoneId) {
        YamlConfiguration cfg = MDUtils.loadCfg(this.plugin, "altar.yml");
        if (cfg != null && cfg.contains("stones." + stoneId)) {
            String path = "stones." + stoneId;
            String displayName = cfg.getString(path + ".display-name", "&bКамень");
            String skullValue = cfg.getString(path + ".skull-value", "");
            boolean glow = cfg.getBoolean(path + ".glow", false);
            ItemStack item = this.plugin.getSkull(skullValue);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(MDUtils.color(displayName));
            if (glow) {
                meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
                meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            }

            meta.getPersistentDataContainer().set(this.STONE_KEY, PersistentDataType.STRING, stoneId);
            item.setItemMeta(meta);
            return item;
        } else {
            return null;
        }
    }

    public Set<String> getAllStoneTypes() {
        YamlConfiguration cfg = MDUtils.loadCfg(this.plugin, "altar.yml");
        return cfg != null && cfg.contains("stones") ? cfg.getConfigurationSection("stones").getKeys(false) : Collections.emptySet();
    }

    public ItemStack getStone(String type) {
        return this.getStoneForMine(type);
    }

    public NamespacedKey getStoneKey() {
        return this.STONE_KEY;
    }

    public boolean isStone(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(this.STONE_KEY, PersistentDataType.STRING);
    }

    public String getStoneType(ItemStack item) {
        return !this.isStone(item) ? null : (String)item.getItemMeta().getPersistentDataContainer().get(this.STONE_KEY, PersistentDataType.STRING);
    }
}