package ru.magicdev.mdtanos.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import ru.magicdev.mdtanos.MDTanos;
import ru.magicdev.mdtanos.MDUtils;

public class AltarListener implements Listener {
    private final MDTanos plugin;

    public AltarListener(MDTanos plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAltarClick(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = e.getClickedBlock();
            if (block != null) {
                YamlConfiguration altarCfg = MDUtils.loadCfg(this.plugin, "altar.yml");
                if (altarCfg != null) {
                    Location loc = block.getLocation();
                    int x = altarCfg.getInt("altar-location.x");
                    int y = altarCfg.getInt("altar-location.y");
                    int z = altarCfg.getInt("altar-location.z");
                    if (loc.getBlockX() == x && loc.getBlockY() == y && loc.getBlockZ() == z) {
                        e.setCancelled(true);
                        this.plugin.openAltarGUI(e.getPlayer());
                    }

                }
            }
        }
    }

    public void triggerBossSummon() {
        YamlConfiguration altarCfg = MDUtils.loadCfg(this.plugin, "altar.yml");
        if (altarCfg != null) {
            String bossId = altarCfg.getString("altar-settings.summon-boss-id", "thanos");
            boolean allowMultiple = this.plugin.getConfig().getBoolean("settings.allow-multiple-bosses", false);
            if (!allowMultiple && this.plugin.getBossManager().isBossActive()) {
                Bukkit.broadcastMessage(MDUtils.color("&cБосс уже на арене!"));
            } else {
                this.plugin.getBossManager().spawnBoss(bossId);
            }
        }
    }
}