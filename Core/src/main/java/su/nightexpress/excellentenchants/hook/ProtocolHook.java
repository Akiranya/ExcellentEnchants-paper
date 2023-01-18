package su.nightexpress.excellentenchants.hook;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import su.nexmedia.engine.utils.ComponentUtil;
import su.nightexpress.excellentenchants.ExcellentEnchantsAPI;
import su.nightexpress.excellentenchants.api.enchantment.ExcellentEnchant;
import su.nightexpress.excellentenchants.config.Config;
import su.nightexpress.excellentenchants.manager.EnchantManager;

import java.util.*;

public class ProtocolHook {

    private static final Style FALLBACK_STYLE = Style.empty()
        .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
        .colorIfAbsent(NamedTextColor.GRAY);

    private static boolean isRegistered = false;

    public static void setup() {
        if (isRegistered) return;

        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        manager.addPacketListener(new PacketAdapter(ExcellentEnchantsAPI.PLUGIN, PacketType.Play.Server.SET_SLOT) {
            @Override public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();

                // Packet is buggy with creative mode, we just don't handle it
                if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;

                ItemStack item = packet.getItemModifier().read(0);
                packet.getItemModifier().write(0, update(item));
            }
        });

        manager.addPacketListener(new PacketAdapter(ExcellentEnchantsAPI.PLUGIN, PacketType.Play.Server.WINDOW_ITEMS) {
            @Override public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();

                // Packet is buggy with creative mode, we just don't handle it
                if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;

                List<ItemStack> items = packet.getItemListModifier().readSafely(0);
                items.replaceAll(ProtocolHook::update);
                packet.getItemListModifier().write(0, items);
            }
        });

        isRegistered = true;
    }

    private static @Nullable ItemStack update(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return item;

        if (!item.hasItemMeta()) return item; // if this is simple item
        // final ItemStack copy = item.clone(); // TODO avoid redundant copy?
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        final Map<ExcellentEnchant, Integer> enchants = EnchantManager.getExcellentEnchantments(item);
        if (enchants.isEmpty()) return item; // if no enchants on this item
        final List<Component> lore = Objects.requireNonNullElse(meta.lore(), new ArrayList<>());

        // Add verbose enchantment description lore
        if (Config.ENCHANTMENTS_DESCRIPTION_ENABLED) {
            enchants.forEach((enchant, level) -> {
                List<Component> desc = new ArrayList<>(ComponentUtil.asComponent(Config.formatDescription(enchant.getDescription(level))));
                desc.replaceAll(component -> component.applyFallbackStyle(FALLBACK_STYLE));
                lore.addAll(0, desc);
            });
        }

        // Add basic enchantment lore (mimic vanilla one)
        enchants.forEach((enchant, level) -> lore.add(0, enchant.displayName(level).applyFallbackStyle(FALLBACK_STYLE)));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
