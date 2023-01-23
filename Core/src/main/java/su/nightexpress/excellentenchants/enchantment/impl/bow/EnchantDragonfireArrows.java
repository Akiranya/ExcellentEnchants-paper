package su.nightexpress.excellentenchants.enchantment.impl.bow;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nexmedia.engine.utils.NumberUtil;
import su.nightexpress.excellentenchants.ExcellentEnchants;
import su.nightexpress.excellentenchants.Placeholders;
import su.nightexpress.excellentenchants.api.enchantment.ExcellentEnchant;
import su.nightexpress.excellentenchants.api.enchantment.meta.Arrowed;
import su.nightexpress.excellentenchants.api.enchantment.meta.Chanced;
import su.nightexpress.excellentenchants.api.enchantment.type.BowEnchant;
import su.nightexpress.excellentenchants.api.enchantment.util.EnchantPriority;
import su.nightexpress.excellentenchants.enchantment.config.EnchantScaler;
import su.nightexpress.excellentenchants.enchantment.impl.meta.ArrowImplementation;
import su.nightexpress.excellentenchants.enchantment.impl.meta.ChanceImplementation;

import java.util.function.UnaryOperator;

public class EnchantDragonfireArrows extends ExcellentEnchant implements Chanced, Arrowed, BowEnchant {

    public static final String ID = "dragonfire_arrows";

    public static final String PLACEHOLDER_FIRE_RADIUS = "%enchantment_fire_radius%";
    public static final String PLACEHOLDER_FIRE_DURATION = "%enchantment_fire_duration%";

    private EnchantScaler fireDuration;
    private EnchantScaler fireRadius;

    private ArrowImplementation arrowImplementation;
    private ChanceImplementation chanceImplementation;

    public EnchantDragonfireArrows(@NotNull ExcellentEnchants plugin) {
        super(plugin, ID, EnchantPriority.MEDIUM);
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        this.arrowImplementation = ArrowImplementation.create(this);
        this.chanceImplementation = ChanceImplementation.create(this);
        this.fireDuration = EnchantScaler.read(this, "Settings.Fire.Duration", "100 * " + Placeholders.ENCHANTMENT_LEVEL,
            "Sets the dragonfire cloud effect duration (in ticks). 20 ticks = 1 second.");
        this.fireRadius = EnchantScaler.read(this, "Settings.Fire.Radius", "2.0 + " + Placeholders.ENCHANTMENT_LEVEL,
            "Sets the dragonfire cloud effect radius.");
    }

    @Override
    @NotNull
    public UnaryOperator<String> replacePlaceholders(int level) {
        return str -> str
            .transform(super.replacePlaceholders(level))
            .replace(PLACEHOLDER_FIRE_DURATION, NumberUtil.format(this.getFireDuration(level) / 20D))
            .replace(PLACEHOLDER_FIRE_RADIUS, NumberUtil.format(this.getFireRadius(level)))
            ;
    }

    @NotNull
    @Override
    public ArrowImplementation getArrowImplementation() {
        return arrowImplementation;
    }

    @NotNull
    @Override
    public ChanceImplementation getChanceImplementation() {
        return chanceImplementation;
    }

    @NotNull
    @Override
    public EnchantmentTarget getItemTarget() {
        return EnchantmentTarget.BOW;
    }

    public int getFireDuration(int level) {
        return (int) this.fireDuration.getValue(level);
    }

    public double getFireRadius(int level) {
        return this.fireRadius.getValue(level);
    }

    @Override
    public boolean onShoot(@NotNull EntityShootBowEvent e, @NotNull LivingEntity shooter, @NotNull ItemStack bow, int level) {
        if (!this.isAvailableToUse(shooter)) return false;

        return this.checkTriggerChance(level);
    }

    @Override
    public boolean onHit(@NotNull ProjectileHitEvent e, @NotNull Projectile projectile, @NotNull ItemStack bow, int level) {
        if (!this.isOurProjectile(projectile)) return false;
        if (e.getHitEntity() != null) return false;

        this.createCloud(projectile.getShooter(), projectile.getLocation(), level);
        return true;
    }

    @Override
    public boolean onDamage(@NotNull EntityDamageByEntityEvent e, @NotNull Projectile projectile, @NotNull LivingEntity shooter, @NotNull LivingEntity victim, @NotNull ItemStack weapon, int level) {
        if (!this.isOurProjectile(projectile)) return false;

        this.createCloud(shooter, victim.getLocation(), level);
        return true;
    }

    private void createCloud(@Nullable ProjectileSource shooter, @NotNull Location location, int level) {
        World world = location.getWorld();
        if (world == null) return;

        AreaEffectCloud cloud = world.spawn(location, AreaEffectCloud.class);
        cloud.clearCustomEffects();
        cloud.setSource(shooter);
        cloud.setParticle(Particle.DRAGON_BREATH);
        cloud.setRadius((float) this.getFireRadius(level));
        cloud.setDuration(this.getFireDuration(level));
        cloud.setRadiusPerTick((7.0F - cloud.getRadius()) / (float) cloud.getDuration());
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.HARM, 1, 1), true);
    }
}