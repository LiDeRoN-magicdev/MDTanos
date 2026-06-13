package ru.magicdev.mdtanos.listener;

import java.util.Random;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import ru.magicdev.mdtanos.MDTanos;
import ru.magicdev.mdtanos.MDUtils;

public class MineManager implements Listener {
    private final MDTanos plugin;
    private final Random random = new Random();

    public MineManager(MDTanos plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.isCancelled()) {
            YamlConfiguration cfg = MDUtils.loadCfg(this.plugin, "mine.yml");
            if (cfg != null) {
                Block block = event.getBlock();
                String materialName = block.getType().name();
                String path = "blocks." + materialName;
                if (cfg.contains(path)) {
                    double chance = cfg.getDouble(path + ".chance");
                    if (this.random.nextDouble() <= chance) {
                        String stoneId = cfg.getString(path + ".stone-id");
                        ItemStack stone = this.plugin.getStoneManager().getStoneForMine(stoneId);
                        if (stone != null) {
                            event.setDropItems(false);
                            block.getWorld().dropItemNaturally(block.getLocation().add(0.5D, 0.0D, 0.5D), stone);
                            String rawMessage = cfg.getString(path + ".found-message", "&aВы нашли редкий камень!");
                            event.getPlayer().sendMessage(MDUtils.color(rawMessage));
                        }
                    }

                }
            }
        }
    }
}