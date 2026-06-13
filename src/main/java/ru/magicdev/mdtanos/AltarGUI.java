/*
 * MDTanos - Custom Plugin for MagicDev
 * Developed by MagicDev
 * Module: Altar GUI Handler
 * Optimization: Integrated MDUtils for cleaner configuration and messaging
 */
package ru.magicdev.mdtanos;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AltarGUI implements Listener {
    private final MDTanos plugin;
    private final Random random = new Random();

    public AltarGUI(MDTanos plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // Замена на MDUtils.loadCfg
        YamlConfiguration altarCfg = MDUtils.loadCfg(plugin, "altar.yml");
        if (altarCfg == null) return;

        // Замена на MDUtils.color
        String title = MDUtils.color(altarCfg.getString("gui.title", "Алтарь"));
        if (!e.getView().getTitle().equals(title)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        boolean allowMultiple = plugin.getConfig().getBoolean("settings.allow-multiple-bosses", false);
        if (!allowMultiple && plugin.getBossManager().isBossActive()) {
            p.sendMessage("§cВы не можете сдавать камни, пока босс активен!");
            p.closeInventory();
            return;
        }

        int clickedSlot = e.getSlot();

        if (altarCfg.contains("gui.decoration")) {
            ConfigurationSection decoSection = altarCfg.getConfigurationSection("gui.decoration");
            for (String key : decoSection.getKeys(false)) {
                List<?> slotList = altarCfg.getList("gui.decoration." + key + ".slots", new ArrayList<>());
                if (parseSlots(slotList).contains(clickedSlot)) {
                    String cmd = altarCfg.getString("gui.decoration." + key + ".command");
                    if (cmd != null && !cmd.isEmpty()) {
                        p.performCommand(cmd);
                        p.closeInventory();
                        return;
                    }
                    break;
                }
            }
        }

        String stoneName = null;
        if (altarCfg.contains("gui.items")) {
            for (String key : altarCfg.getConfigurationSection("gui.items").getKeys(false)) {
                List<Integer> slots = parseSlots(altarCfg.getList("gui.items." + key + ".slots",
                        Collections.singletonList(altarCfg.getInt("gui.items." + key + ".slot"))));
                if (slots.contains(clickedSlot)) {
                    stoneName = key.toLowerCase();
                    break;
                }
            }
        }

        if (stoneName == null) return;

        UUID uuid = p.getUniqueId();
        Set<String> collected = plugin.getPlayerStones().computeIfAbsent(uuid, k -> new HashSet<>());

        if (collected.contains(stoneName)) {
            p.sendMessage("§cЭтот камень уже сдан!");
            return;
        }

        ItemStack found = null;
        for (ItemStack invItem : p.getInventory().getContents()) {
            if (invItem != null && plugin.getStoneManager().isStone(invItem)) {
                String type = plugin.getStoneManager().getStoneType(invItem);
                if (type != null && type.equalsIgnoreCase(stoneName)) {
                    found = invItem;
                    break;
                }
            }
        }

        if (found != null) {
            found.setAmount(found.getAmount() - 1);
            collected.add(stoneName);
            p.sendMessage("§aВы сдали камень: " + stoneName);

            plugin.openAltarGUI(p);

            ConfigurationSection itemsSection = altarCfg.getConfigurationSection("gui.items");
            boolean allCollected = true;
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    if (!collected.contains(key.toLowerCase())) {
                        allCollected = false;
                        break;
                    }
                }
            }

            if (allCollected) {
                p.sendMessage("§6Все камни собраны! Босс заспавнен!");

                plugin.getBossManager().spawnBoss("thanos");

                List<Map<?, ?>> rewardList = altarCfg.getMapList("rewards");
                if (!rewardList.isEmpty()) {
                    int totalChance = 0;
                    for (Map<?, ?> reward : rewardList) {
                        Object chanceObj = reward.get("chance");
                        totalChance += (chanceObj instanceof Number) ? ((Number) chanceObj).intValue() : 100;
                    }

                    int rand = random.nextInt(totalChance);
                    int currentChance = 0;

                    for (Map<?, ?> reward : rewardList) {
                        Object chanceObj = reward.get("chance");
                        currentChance += (chanceObj instanceof Number) ? ((Number) chanceObj).intValue() : 100;
                        if (rand < currentChance) {
                            String cmd = (String) reward.get("command");
                            String finalCommand = cmd.replace("%player%", p.getName());
                            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), finalCommand);
                            break;
                        }
                    }
                }

                plugin.getPlayerStones().remove(uuid);
                p.closeInventory();
            }
        } else {
            p.sendMessage("§cУ вас нет этого камня в инвентаре!");
        }
    }

    private List<Integer> parseSlots(List<?> slotData) {
        List<Integer> slots = new ArrayList<>();
        if (slotData == null) return slots;
        for (Object obj : slotData) {
            String s = obj.toString();
            if (s.contains("-")) {
                String[] parts = s.split("-");
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                for (int i = start; i <= end; i++) slots.add(i);
            } else {
                slots.add(Integer.parseInt(s));
            }
        }
        return slots;
    }
}