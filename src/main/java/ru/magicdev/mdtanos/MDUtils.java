package ru.magicdev.mdtanos;

import java.io.File;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

public class MDUtils {
    public static YamlConfiguration loadCfg(MDTanos plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.getLogger().warning("Файл " + fileName + " не найден!");
            return null;
        } else {
            return YamlConfiguration.loadConfiguration(file);
        }
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static void log(String msg) {
        System.out.println("[MDTanos-Pro] " + msg);
    }
}