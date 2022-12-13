package com.phoenixra.atum.core.gui;

import com.phoenixra.atum.core.old.PhoenixException;
import com.phoenixra.atum.core.gui.api.PhoenixFrame;
import com.phoenixra.atum.core.gui.api.PhoenixFrameComponent;
import com.phoenixra.atum.core.gui.baseframes.WarningFrame;
import com.phoenixra.atum.core.gui.events.FrameOpenEvent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class GuiDrawer {
    private final Plugin plugin;
    private final PhoenixGuiController guiController;
    private final ConcurrentHashMap<UUID, PhoenixFrame> OPENING = new ConcurrentHashMap<>();
    @Getter @Setter @Accessors(chain = true)
    private boolean debug;

    public GuiDrawer(Plugin plugin, PhoenixGuiController guiController) {
        this.plugin = plugin;
        this.guiController = guiController;
    }


    public void open(@NotNull PhoenixFrame frame) {
        UUID uuid = frame.getViewer().getUniqueId();
        if (frame.equals(OPENING.get(uuid))) return;

        OPENING.put(uuid, frame);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Player viewer = frame.getViewer();
            try {
                Inventory inventory = prepareInventory(frame);
                if (!frame.equals(OPENING.get(uuid))) return;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    FrameOpenEvent event = new FrameOpenEvent(viewer, frame);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) return;

                    viewer.openInventory(inventory);
                    guiController.register(frame);
                    OPENING.remove(uuid);
                });
            }catch (PhoenixException ex){
                ex.printStackTrace();
                if(!(frame instanceof WarningFrame)) {
                    open(new WarningFrame(this, null, viewer, ex.getMessageToPlayer()));
                }else viewer.closeInventory();
            }catch (Exception e){
                e.printStackTrace();
                if(!(frame instanceof WarningFrame)) {
                    open(new WarningFrame(this, null, viewer, "&8&oUnhandled error,\n&8&o please contact with server administration"));
                }else viewer.closeInventory();
            }
        });
    }
    public void update(@NotNull PhoenixFrame frame) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Player viewer = frame.getViewer();
            try {

                setComponents(viewer.getOpenInventory().getTopInventory(), frame);

            }catch (PhoenixException ex){
                ex.printStackTrace();
                if(!(frame instanceof WarningFrame)) {
                    open(new WarningFrame(this, null, viewer, ex.getMessageToPlayer()));
                }else viewer.closeInventory();
            }catch (Exception e){
                e.printStackTrace();
                if(!(frame instanceof WarningFrame)) {
                    open(new WarningFrame(this, null, viewer, "&8&oUnhandled error,\n&8&o please contact with server administration"));
                }else viewer.closeInventory();
            }
        });
    }
    @NotNull
    private Inventory prepareInventory(@NotNull PhoenixFrame frame) throws PhoenixException {
        Inventory inventory = Bukkit.createInventory(frame.getViewer(), frame.getSize(), frame.getTitle());
        long start = System.currentTimeMillis();
        setComponents(inventory, frame);

        if (debug) {
            plugin.getLogger().log(Level.INFO,
                    String.format("It took %s millisecond(s) to load the frame %s for %s",
                            System.currentTimeMillis() - start, frame.getTitle(), frame.getViewer().getName()));
        }
        return inventory;
    }

    private void setComponents(@NotNull Inventory inventory, @NotNull PhoenixFrame frame) throws PhoenixException {
        frame.clear();
        frame.createComponents();
        inventory.clear();

        Set<PhoenixFrameComponent> components = frame.getComponents();
        if (components.isEmpty()) {
            plugin.getLogger().warning(String.format("Frame %s has no components", frame.getTitle()));
            return;
        }
        for (PhoenixFrameComponent c : frame.getComponents()) {

            if(c.getInventoryType() == InventoryType.PLAYER) {
                if (c.getSlot() >= frame.getViewer().getInventory().getSize()) continue;
                checkLorePermission(frame, c);
                frame.getViewer().getInventory().setItem(c.getSlot(), c.getItem());
            }else {
                if (c.getSlot() >= frame.getSize()) continue;
                checkLorePermission(frame, c);
                inventory.setItem(c.getSlot(), c.getItem());
            }

        }
    }
    private static void checkLorePermission(@NotNull PhoenixFrame frame, @NotNull PhoenixFrameComponent component) {
        ItemMeta itemMeta = component.getItemMeta();
        if (itemMeta == null || itemMeta.getLore() == null || component.getLorePermission() == null) return;
        List<String> lore = itemMeta.getLore();
        if (!frame.getViewer().hasPermission(component.getLorePermission())) {
            lore.clear();
            lore.add("&4You don't have the required permission");
            itemMeta.setLore(lore);
            component.setItemMeta(itemMeta);
        }

    }


}