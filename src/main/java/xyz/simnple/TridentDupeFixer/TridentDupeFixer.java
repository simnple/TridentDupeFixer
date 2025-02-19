package xyz.simnple.TridentDupeFixer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.Hand;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.ProtocolLibrary;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class TridentDupeFixer extends JavaPlugin implements Listener {
    private final Set<UUID> readyThrow = new HashSet<>();
    private final Set<UUID> digCancel = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(new PacketAdapter(
                this,
                ListenerPriority.NORMAL,
                PacketType.Play.Client.BLOCK_DIG,
                PacketType.Play.Client.USE_ITEM,
                PacketType.Play.Client.HELD_ITEM_SLOT,
                PacketType.Play.Client.WINDOW_CLICK
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                PacketType type = event.getPacketType();

                if (type == PacketType.Play.Client.USE_ITEM) {
                    Hand handType = event.getPacket().getHands().read(0);
                    ItemStack item;

                    if (handType == Hand.MAIN_HAND) {
                        item = player.getInventory().getItemInMainHand();
                    } else {
                        item = player.getInventory().getItemInOffHand();
                    }

                    if (item.getType() == Material.TRIDENT) {
                        readyThrow.add(player.getUniqueId());
                        digCancel.remove(player.getUniqueId());
                    }
                }

                if (readyThrow.contains(player.getUniqueId())) {
                    if (type == PacketType.Play.Client.WINDOW_CLICK) {
                        event.setCancelled(true);
                    } else if (type == PacketType.Play.Client.HELD_ITEM_SLOT || type == PacketType.Play.Client.BLOCK_DIG) {
                        readyThrow.remove(player.getUniqueId());

                        if (type == PacketType.Play.Client.BLOCK_DIG) {
                            digCancel.add(player.getUniqueId());
                        }
                    }
                }
            }
        });
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Trident) {
            if (event.getEntity().getShooter() instanceof Player player) {
                if (!digCancel.contains(player.getUniqueId())) {
                    event.setCancelled(true);
                }
                digCancel.remove(player.getUniqueId());
                readyThrow.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        digCancel.remove(event.getPlayer().getUniqueId());
        readyThrow.remove(event.getPlayer().getUniqueId());
    }
}
