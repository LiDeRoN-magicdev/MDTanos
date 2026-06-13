package ru.magicdev.mdtanos.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import ru.magicdev.mdtanos.MDTanos;

public class BossProtectionListener implements Listener {
    private final MDTanos plugin;

    public BossProtectionListener(MDTanos plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityRemove(EntityDeathEvent event) {
        if (event.getEntity().hasMetadata("IsMDTanosBoss")) {
        }

    }
}