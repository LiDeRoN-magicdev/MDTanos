package ru.magicdev.mdtanos.command;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.magicdev.mdtanos.MDTanos;

public class CommandHandler implements CommandExecutor {
    private final MDTanos plugin;

    public CommandHandler(MDTanos p) {
        this.plugin = p;
    }

    public boolean onCommand(CommandSender s, Command cmd, String l, String[] args) {
        if (!s.hasPermission("mtanos.admin")) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "У вас нет прав!");
            return true;
        } else if (args.length == 0) {
            s.sendMessage(String.valueOf(ChatColor.RED) + "Использование: /mtanos <give | giveall | mine | boss | reload | addloot>");
            return true;
        } else {
            String var5 = args[0].toLowerCase();
            byte var6 = -1;
            switch(var5.hashCode()) {
                case -1147861303:
                    if (var5.equals("addloot")) {
                        var6 = 1;
                    }
                    break;
                case -934641255:
                    if (var5.equals("reload")) {
                        var6 = 0;
                    }
                    break;
                case 3029869:
                    if (var5.equals("boss")) {
                        var6 = 4;
                    }
                    break;
                case 3173137:
                    if (var5.equals("give")) {
                        var6 = 2;
                    }
                    break;
                case 41740528:
                    if (var5.equals("giveall")) {
                        var6 = 3;
                    }
            }

            String var10001;
            switch(var6) {
                case 0:
                    this.plugin.reloadConfig();
                    s.sendMessage(String.valueOf(ChatColor.GREEN) + "MDTanos успешно перезагружен!");
                    return true;
                case 1:
                    if (!(s instanceof Player)) {
                        s.sendMessage(String.valueOf(ChatColor.RED) + "Команду могут использовать только игроки!");
                        return true;
                    } else if (args.length < 3) {
                        s.sendMessage(String.valueOf(ChatColor.RED) + "Использование: /mtanos addloot <bossId> <шанс(0.0-1.0)>");
                        return true;
                    } else {
                        Player p = (Player)s;
                        ItemStack item = p.getInventory().getItemInMainHand();
                        if (item != null && item.getType() != Material.AIR) {
                            try {
                                double chance = Double.parseDouble(args[2]);
                                this.plugin.getBossManager().addLoot(args[1], item, chance);
                                var10001 = String.valueOf(ChatColor.GREEN);
                                p.sendMessage(var10001 + "Предмет добавлен в лут босса " + args[1] + " (Шанс: " + chance + ")");
                            } catch (NumberFormatException var18) {
                                p.sendMessage(String.valueOf(ChatColor.RED) + "Неверный формат шанса! Используйте число от 0.0 до 1.0");
                            }

                            return true;
                        }

                        p.sendMessage(String.valueOf(ChatColor.RED) + "Возьмите предмет в руку!");
                        return true;
                    }
                case 2:
                    if (args.length < 3) {
                        s.sendMessage(String.valueOf(ChatColor.RED) + "Использование: /mtanos give <ник> <тип_камня>");
                        return true;
                    } else {
                        Player target = this.plugin.getServer().getPlayer(args[1]);
                        if (target == null) {
                            var10001 = String.valueOf(ChatColor.RED);
                            s.sendMessage(var10001 + "Игрок " + args[1] + " не найден!");
                            return true;
                        }

                        this.giveStoneSafe(target, args[2]);
                        return true;
                    }
                case 3:
                    if (args.length < 2) {
                        s.sendMessage(String.valueOf(ChatColor.RED) + "Использование: /mtanos giveall <ник>");
                        return true;
                    } else {
                        Player allTarget = this.plugin.getServer().getPlayer(args[1]);
                        if (allTarget == null) {
                            var10001 = String.valueOf(ChatColor.RED);
                            s.sendMessage(var10001 + "Игрок " + args[1] + " не найден!");
                            return true;
                        } else {
                            File altarFile = new File(this.plugin.getDataFolder(), "altar.yml");
                            FileConfiguration cfg = YamlConfiguration.loadConfiguration(altarFile);
                            ConfigurationSection stonesSection = cfg.getConfigurationSection("stones");
                            if (stonesSection != null) {
                                Iterator var20 = stonesSection.getKeys(false).iterator();

                                while(var20.hasNext()) {
                                    String stoneId = (String)var20.next();
                                    this.giveStoneSafe(allTarget, stoneId);
                                }

                                var10001 = String.valueOf(ChatColor.GREEN);
                                s.sendMessage(var10001 + "Все доступные камни выданы игроку " + allTarget.getName());
                            } else {
                                s.sendMessage(String.valueOf(ChatColor.RED) + "Камни не найдены в altar.yml!");
                            }

                            return true;
                        }
                    }
                case 4:
                    if (args.length >= 3 && args[1].equalsIgnoreCase("start")) {
                        String bossId = args[2];
                        File bossFile = new File(this.plugin.getDataFolder(), "boss.yml");
                        YamlConfiguration bossCfg = YamlConfiguration.loadConfiguration(bossFile);
                        if (!bossCfg.contains("bosses." + bossId)) {
                            var10001 = String.valueOf(ChatColor.RED);
                            s.sendMessage(var10001 + "Босс '" + bossId + "' не найден в boss.yml!");
                            return true;
                        }

                        if (this.plugin.getBossManager().isBossActive()) {
                            s.sendMessage(String.valueOf(ChatColor.RED) + "Босс уже активен! Сначала убейте текущего.");
                            return true;
                        }

                        List<String> enabledWorlds = this.plugin.getConfig().getStringList("settings.enabled-worlds");
                        if (s instanceof Player && !enabledWorlds.isEmpty() && !enabledWorlds.contains(((Player)s).getWorld().getName())) {
                            s.sendMessage(String.valueOf(ChatColor.RED) + "В этом мире спавн боссов запрещен!");
                            return true;
                        }

                        this.plugin.getBossManager().spawnBoss(bossId);
                        var10001 = String.valueOf(ChatColor.GREEN);
                        s.sendMessage(var10001 + "Босс " + bossId + " заспавнен!");
                    } else {
                        s.sendMessage(String.valueOf(ChatColor.RED) + "Использование: /mtanos boss start <bossId>");
                    }

                    return true;
                default:
                    s.sendMessage(String.valueOf(ChatColor.RED) + "Неизвестная команда. Используйте: /mtanos <give | giveall | mine | boss | reload | addloot>");
                    return true;
            }
        }
    }

    private void giveStoneSafe(Player target, String stoneId) {
        File altarFile = new File(this.plugin.getDataFolder(), "altar.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(altarFile);
        String path = "stones." + stoneId;
        if (cfg.contains(path)) {
            String skullValue = cfg.getString(path + ".skull-value", "");
            String displayName = cfg.getString(path + ".display-name", "Камень");
            String var10000 = String.valueOf(UUID.randomUUID());
            String nbt = "{SkullOwner:{Id:\"" + var10000 + "\",Properties:{textures:[{Value:\"" + skullValue + "\"}]}}}";

            try {
                ItemStack baseHead = new ItemStack(Material.PLAYER_HEAD);
                ItemStack finalHead = Bukkit.getUnsafe().modifyItemStack(baseHead, nbt);
                ItemMeta meta = finalHead.getItemMeta();
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
                NamespacedKey key = this.plugin.getStoneManager().getStoneKey();
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, stoneId.toLowerCase());
                finalHead.setItemMeta(meta);
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    target.getInventory().addItem(new ItemStack[]{finalHead});
                    target.updateInventory();
                });
            } catch (Exception var13) {
                this.plugin.getLogger().warning("Ошибка при выдаче: " + var13.getMessage());
            }

        }
    }
}