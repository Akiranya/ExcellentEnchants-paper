package su.nightexpress.excellentenchants.enchantment.impl.weapon;

import org.bukkit.Particle;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nexmedia.engine.api.config.JOption;
import su.nexmedia.engine.utils.EffectUtil;
import su.nexmedia.engine.utils.NumberUtil;
import su.nightexpress.excellentenchants.ExcellentEnchants;
import su.nightexpress.excellentenchants.Placeholders;
import su.nightexpress.excellentenchants.api.enchantment.ExcellentEnchant;
import su.nightexpress.excellentenchants.api.enchantment.type.CombatEnchant;
import su.nightexpress.excellentenchants.api.enchantment.util.EnchantPriority;
import su.nightexpress.excellentenchants.enchantment.config.EnchantScaler;

import java.util.Set;
import java.util.function.UnaryOperator;

public class EnchantBaneOfNetherspawn extends ExcellentEnchant implements CombatEnchant {

    public static final String ID = "bane_of_netherspawn";

    private static final String PLACEHOLDER_DAMAGE = "%enchantment_damage%";
    private static final Set<EntityType> ENTITY_TYPES = Set.of(
        EntityType.BLAZE, EntityType.MAGMA_CUBE,
        EntityType.WITHER_SKELETON, EntityType.GHAST, EntityType.WITHER,
        EntityType.PIGLIN, EntityType.PIGLIN_BRUTE,
        EntityType.ZOGLIN, EntityType.HOGLIN,
        EntityType.STRIDER, EntityType.ZOMBIFIED_PIGLIN
    );

    private boolean damageModifier;
    private EnchantScaler damageFormula;

    public EnchantBaneOfNetherspawn(@NotNull ExcellentEnchants plugin) {
        super(plugin, ID, EnchantPriority.MEDIUM);
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        this.damageModifier = JOption.create("Settings.Damage.As_Modifier", false,
            "When 'true' multiplies the damage. When 'false' sums plain values.").read(this.cfg);
        this.damageFormula = EnchantScaler.read(this, "Settings.Damage.Amount", "0.5 * " + Placeholders.ENCHANTMENT_LEVEL,
            "Amount of additional damage.");
    }

    public double getDamageModifier(int level) {
        return this.damageFormula.getValue(level);
    }

    @Override
    public @NotNull UnaryOperator<String> replacePlaceholders(int level) {
        return str -> str
            .transform(super.replacePlaceholders(level))
            .replace(PLACEHOLDER_DAMAGE, NumberUtil.format(this.getDamageModifier(level)));
    }

    @Override
    public @NotNull EnchantmentTarget getItemTarget() {
        return EnchantmentTarget.WEAPON;
    }

    @Override
    public boolean onAttack(@NotNull EntityDamageByEntityEvent e, @NotNull LivingEntity damager, @NotNull LivingEntity victim, @NotNull ItemStack weapon, int level) {
        if (!ENTITY_TYPES.contains(victim.getType())) return false;
        if (!this.isAvailableToUse(damager)) return false;

        double damageEvent = e.getDamage();
        double damageAdd = this.getDamageModifier(level);
        e.setDamage(this.damageModifier ? damageEvent * damageAdd : damageEvent + damageAdd);
        if (this.hasVisualEffects()) {
            EffectUtil.playEffect(victim.getEyeLocation(), Particle.SMOKE_NORMAL, "", 0.25, 0.25, 0.25, 0.1f, 30);
        }
        return true;
    }

    @Override
    public boolean onProtect(@NotNull EntityDamageByEntityEvent e, @NotNull LivingEntity damager, @NotNull LivingEntity victim, @NotNull ItemStack weapon, int level) {
        return false;
    }
}
