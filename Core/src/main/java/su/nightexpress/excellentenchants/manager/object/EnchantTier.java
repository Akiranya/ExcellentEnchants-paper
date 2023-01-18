package su.nightexpress.excellentenchants.manager.object;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentenchants.api.enchantment.ExcellentEnchant;
import su.nightexpress.excellentenchants.manager.type.ObtainType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnchantTier {

    private final String id;
    private final int priority;
    private final String name; // Stored in MiniMessage string representation
    private final TextColor color;
    private final Map<ObtainType, Double> chance;

    private final Set<ExcellentEnchant> enchants;

    public EnchantTier(@NotNull String id, int priority, @NotNull String name, @NotNull TextColor color, @NotNull Map<ObtainType, Double> chance) {
        this.id = id.toLowerCase();
        this.priority = priority;
        this.name = name;
        this.color = color;
        this.chance = chance;
        this.enchants = new HashSet<>();
    }

    @NotNull
    public String getId() {
        return this.id;
    }

    public int getPriority() {
        return priority;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public TextColor getColor() {
        return this.color;
    }

    @NotNull
    public Map<ObtainType, Double> getChance() {
        return this.chance;
    }

    public double getChance(@NotNull ObtainType obtainType) {
        return this.getChance().getOrDefault(obtainType, 0D);
    }

    @NotNull
    public Set<ExcellentEnchant> getEnchants() {
        return this.enchants;
    }

    @NotNull
    public Set<ExcellentEnchant> getEnchants(@NotNull ObtainType obtainType) {
        return this.getEnchants(obtainType, null);
    }

    @NotNull
    public Set<ExcellentEnchant> getEnchants(@NotNull ObtainType obtainType, @Nullable ItemStack item) {
        Set<ExcellentEnchant> set = this.getEnchants()
            .stream()
            .filter(enchant -> enchant.getObtainChance(obtainType) > 0)
            .filter(enchant -> item == null || enchant.canEnchantItem(item))
            .collect(Collectors.toCollection(HashSet::new));
        set.removeIf(enchant -> obtainType == ObtainType.ENCHANTING && enchant.isTreasure());
        return set;
    }
}
