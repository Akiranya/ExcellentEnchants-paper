package su.nightexpress.excellentenchants.enchantment.impl.bow;

import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.ExcellentEnchants;
import su.nightexpress.excellentenchants.api.enchantment.meta.Arrowed;
import su.nightexpress.excellentenchants.api.enchantment.meta.Chanced;
import su.nightexpress.excellentenchants.api.enchantment.template.PotionEnchant;
import su.nightexpress.excellentenchants.api.enchantment.type.BowEnchant;
import su.nightexpress.excellentenchants.api.enchantment.util.EnchantPriority;
import su.nightexpress.excellentenchants.enchantment.impl.meta.ArrowImplementation;
import su.nightexpress.excellentenchants.enchantment.impl.meta.ChanceImplementation;

public class EnchantHover extends PotionEnchant implements Chanced, Arrowed, BowEnchant {

    public static final String ID = "hover";

    private ArrowImplementation arrowImplementation;
    private ChanceImplementation chanceImplementation;

    public EnchantHover(@NotNull ExcellentEnchants plugin) {
        super(plugin, ID, EnchantPriority.MEDIUM, PotionEffectType.LEVITATION, false);
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        this.arrowImplementation = ArrowImplementation.create(this);
        this.chanceImplementation = ChanceImplementation.create(this);
    }

    @Override
    public @NotNull ArrowImplementation getArrowImplementation() {
        return this.arrowImplementation;
    }

    @Override
    public @NotNull ChanceImplementation getChanceImplementation() {
        return this.chanceImplementation;
    }

    @Override
    public @NotNull EnchantmentTarget getItemTarget() {
        return EnchantmentTarget.BOW;
    }

    @Override
    public boolean onShoot(@NotNull EntityShootBowEvent e, @NotNull LivingEntity shooter, @NotNull ItemStack bow, int level) {
        if (!this.isAvailableToUse(shooter)) return false;
        if (!(e.getProjectile() instanceof Arrow arrow)) return false;
        if (!this.checkTriggerChance(level)) return false;

        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        return arrow.addCustomEffect(this.createEffect(level), true);
    }

    @Override
    public boolean onHit(@NotNull ProjectileHitEvent e, @NotNull Projectile projectile, @NotNull ItemStack bow, int level) {
        return this.isOurProjectile(projectile);
    }

    @Override
    public boolean onDamage(@NotNull EntityDamageByEntityEvent e, @NotNull Projectile projectile, @NotNull LivingEntity shooter, @NotNull LivingEntity victim, @NotNull ItemStack weapon, int level) {
        return this.isOurProjectile(projectile);
    }
}
