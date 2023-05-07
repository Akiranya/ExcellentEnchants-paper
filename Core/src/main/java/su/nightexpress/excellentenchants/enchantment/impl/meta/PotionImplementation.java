package su.nightexpress.excellentenchants.enchantment.impl.meta;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import su.nexmedia.engine.utils.Scaler;
import su.nightexpress.excellentenchants.Placeholders;
import su.nightexpress.excellentenchants.api.enchantment.ExcellentEnchant;
import su.nightexpress.excellentenchants.api.enchantment.meta.Potioned;
import su.nightexpress.excellentenchants.config.Config;
import su.nightexpress.excellentenchants.enchantment.config.EnchantScaler;

public final class PotionImplementation implements Potioned {

    public static final String PLACEHOLDER_POTION_LEVEL = "%enchantment_potion_level%";
    public static final String PLACEHOLDER_POTION_DURATION = "%enchantment_potion_duration%";
    public static final String PLACEHOLDER_POTION_TYPE = "%enchantment_potion_type%";

    //private final ExcellentEnchant enchant;
    private final PotionEffectType effectType;
    private final Scaler duration;
    private final Scaler amplifier;
    private final boolean isPermanent;

    private PotionImplementation(@NotNull ExcellentEnchant enchant, @NotNull PotionEffectType effectType, boolean isPermanent) {
        //this.enchant = enchant;
        this.effectType = effectType;
        this.duration = EnchantScaler.read(enchant, "Settings.Potion_Effect.Duration", "5.0 * " + Placeholders.ENCHANTMENT_LEVEL,
            "Potion effect duration (in seconds). This setting is useless for 'permanent' effects.");
        this.amplifier = EnchantScaler.read(enchant, "Settings.Potion_Effect.Level", Placeholders.ENCHANTMENT_LEVEL,
            "Potion effect level.");
        this.isPermanent = isPermanent;
    }

    @Override
    public @NotNull Potioned getPotionImplementation() {
        return this;
    }

    public static PotionImplementation create(@NotNull ExcellentEnchant enchant, @NotNull PotionEffectType type, boolean isPermanent) {
        return new PotionImplementation(enchant, type, isPermanent);
    }

    @Override
    public boolean isPermanent() {
        return this.isPermanent;
    }

    public @NotNull PotionEffectType getEffectType() {
        return this.effectType;
    }

    public int getEffectDuration(int level) {
        if (this.isPermanent()) {
            int duration = Config.TASKS_PASSIVE_POTION_EFFECTS_APPLY_INTERVAL.get().intValue() + 30;
            if (this.getEffectType().getName().equalsIgnoreCase(PotionEffectType.NIGHT_VISION.getName())) {
                duration += 30 * 20;
            }
            return duration;
        }
        return (int) (this.duration.getValue(level) * 20);
    }

    public int getEffectAmplifier(int level) {
        return (int) this.amplifier.getValue(level);
    }

    public @NotNull PotionEffect createEffect(int level) {
        int duration = this.getEffectDuration(level);
        int amplifier = Math.max(0, this.getEffectAmplifier(level) - 1);

        return new PotionEffect(this.getEffectType(), duration, amplifier, false, false);
    }

    public boolean addEffect(@NotNull LivingEntity target, int level) {
        target.addPotionEffect(this.createEffect(level));
        return true;
    }
}
