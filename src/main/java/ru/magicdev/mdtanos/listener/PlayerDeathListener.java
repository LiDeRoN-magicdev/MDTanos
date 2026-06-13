package ru.magicdev.mdtanos.listener;

import java.util.Random;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import ru.magicdev.mdtanos.MDTanos;
import ru.magicdev.mdtanos.MDUtils;

public class PlayerDeathListener implements Listener {
    private final MDTanos plugin;
    private final Random random = new Random();

    public PlayerDeathListener(MDTanos plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (this.plugin.getConfig().getBoolean("soul-stone-drop.enabled", true)) {
            double chance = this.plugin.getConfig().getDouble("soul-stone-drop.chance", 0.1D);
            if (this.random.nextDouble() <= chance) {
                Player victim = event.getEntity();
                ItemStack stone = this.plugin.getStoneManager().getSoulStone();
                victim.getWorld().dropItemNaturally(victim.getLocation(), stone);
                victim.sendMessage(MDUtils.color("&bС вас выпал Камень Души!"));
            }

        }
    }
}