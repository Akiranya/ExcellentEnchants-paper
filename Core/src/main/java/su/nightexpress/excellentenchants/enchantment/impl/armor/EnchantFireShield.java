package su.nightexpress.excellentenchants.enchantment.impl.armor;

import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nexmedia.engine.utils.NumberUtil;
import su.nightexpress.excellentenchants.ExcellentEnchants;
import su.nightexpress.excellentenchants.Placeholders;
import su.nightexpress.excellentenchants.api.enchantment.ExcellentEnchant;
import su.nightexpress.excellentenchants.api.enchantment.meta.Chanced;
import su.nightexpress.excellentenchants.api.enchantment.type.CombatEnchant;
import su.nightexpress.excellentenchants.api.enchantment.util.EnchantPriority;
import su.nightexpress.excellentenchants.enchantment.config.EnchantScaler;
import su.nightexpress.excellentenchants.enchantment.impl.meta.ChanceImplementation;

import java.util.function.UnaryOperator;

public class EnchantFireShield extends ExcellentEnchant implements Chanced, CombatEnchant {

    public static final String ID = "fire_shield";
    public static final String PLACEHOLDER_FIRE_DURATION = "%enchantment_fire_duration%";

    private EnchantScaler fireDuration;
    private ChanceImplementation chanceImplementation;

    public EnchantFireShield(@NotNull ExcellentEnchants plugin) {
        super(plugin, ID, EnchantPriority.MEDIUM);
    }

    @Override
    @NotNull
    public UnaryOperator<String> replacePlaceholders(int level) {
        return str -> str
            .transform(super.replacePlaceholders(level))
            .replace(PLACEHOLDER_FIRE_DURATION, NumberUtil.format(this.getFireDuration(level)))
            ;
    }

    @Override
    public void loadConfig() {
        super.loadConfig();

        this.chanceImplementation = ChanceImplementation.create(this);
        this.fireDuration = EnchantScaler.read(this, "Settings.Fire.Duration", "2.5 * " + Placeholders.ENCHANTMENT_LEVEL,
            "Sets the fire duration (in seconds).",
            "If entity's current fire ticks amount is less than this value, it will be set to this value.",
            "If entity's current fire ticks amount is greater than this value, it won't be changed.");
    }

    @NotNull
    @Override
    public ChanceImplementation getChanceImplementation() {
        return chanceImplementation;
    }

    @NotNull
    @Override
    public EnchantmentTarget getItemTarget() {
        return EnchantmentTarget.ARMOR;
    }

    public double getFireDuration(int level) {
        return this.fireDuration.getValue(level);
    }

    @Override
    public boolean onAttack(@NotNull EntityDamageByEntityEvent e, @NotNull LivingEntity damager, @NotNull LivingEntity victim, @NotNull ItemStack weapon, int level) {
        return false;
    }

    @Override
    public boolean onProtect(@NotNull EntityDamageByEntityEvent e,
        @NotNull LivingEntity damager, @NotNull LivingEntity victim,
        @NotNull ItemStack weapon, int level) {
        if (!this.isAvailableToUse(victim)) return false;
        if (!this.checkTriggerChance(level)) return false;

        int ticksToSet = (int) (this.getFireDuration(level) * 20);
        int ticksHas = damager.getFireTicks();
        if (ticksHas >= ticksToSet) return false;

        damager.setFireTicks(ticksToSet);
        return true;
    }
}
