package ru.magicdev.mdtanos.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.magicdev.mdtanos.MDTanos;

public class TabHandler implements TabCompleter {
    private final MDTanos plugin;

    public TabHandler(MDTanos plugin) {
        this.plugin = plugin;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("give", "giveall", "mine", "boss", "addloot", "reload");
        } else {
            if (args.length == 2) {
                String var5 = args[0].toLowerCase();
                byte var6 = -1;
                switch(var5.hashCode()) {
                    case -1147861303:
                        if (var5.equals("addloot")) {
                            var6 = 3;
                        }
                        break;
                    case 3029869:
                        if (var5.equals("boss")) {
                            var6 = 2;
                        }
                        break;
                    case 3173137:
                        if (var5.equals("give")) {
                            var6 = 0;
                        }
                        break;
                    case 41740528:
                        if (var5.equals("giveall")) {
                            var6 = 1;
                        }
                }

                switch(var6) {
                    case 0:
                    case 1:
                        return null;
                    case 2:
                        return Collections.singletonList("start");
                    case 3:
                        return this.getBossList();
                }
            }

            if (args.length == 3) {
                if (args[0].equalsIgnoreCase("give")) {
                    return Arrays.asList("power", "mind", "reality", "space", "soul", "time", "ego");
                }

                if (args[0].equalsIgnoreCase("addloot")) {
                    return Collections.singletonList("0.1");
                }

                if (args[0].equalsIgnoreCase("boss") && args[1].equalsIgnoreCase("start")) {
                    return this.getBossList();
                }
            }

            return Collections.emptyList();
        }
    }

    private List<String> getBossList() {
        File f = new File(this.plugin.getDataFolder(), "boss.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection section = cfg.getConfigurationSection("bosses");
        return (List)(section != null ? new ArrayList(section.getKeys(false)) : Collections.emptyList());
    }
}