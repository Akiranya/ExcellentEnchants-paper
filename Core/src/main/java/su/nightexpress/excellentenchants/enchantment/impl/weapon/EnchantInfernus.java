package su.nightexpress.excellentenchants.enchantment.impl.weapon;

import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nexmedia.engine.utils.NumberUtil;
import su.nightexpress.excellentenchants.ExcellentEnchants;
import su.nightexpress.excellentenchants.Placeholders;
import su.nightexpress.excellentenchants.api.enchantment.ExcellentEnchant;
import su.nightexpress.excellentenchants.api.enchantment.util.EnchantPriority;
import su.nightexpress.excellentenchants.enchantment.EnchantManager;
import su.nightexpress.excellentenchants.enchantment.config.EnchantScaler;

import java.util.function.UnaryOperator;

public class EnchantInfernus extends ExcellentEnchant {

    public static final String ID = "infernus";
    public static final String PLACEHOLDER_FIRE_DURATION = "%enchantment_fire_duration%";

    private EnchantScaler fireTicks;

    public EnchantInfernus(@NotNull ExcellentEnchants plugin) {
        super(plugin, ID, EnchantPriority.MEDIUM);
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        this.fireTicks = EnchantScaler.read(this, "Settings.Fire_Ticks", "60 + " + Placeholders.ENCHANTMENT_LEVEL + " * 20",
            "Sets for how long (in ticks) entity will be ignited on hit. 20 ticks = 1 second.");
    }

    public int getFireTicks(int level) {
        return (int) this.fireTicks.getValue(level);
    }

    @Override
    @NotNull
    public UnaryOperator<String> replacePlaceholders(int level) {
        return str -> str
            .transform(super.replacePlaceholders(level))
            .replace(PLACEHOLDER_FIRE_DURATION, NumberUtil.format((double) this.getFireTicks(level) / 20D));
    }

    @Override
    @NotNull
    public EnchantmentTarget getItemTarget() {
        return EnchantmentTarget.TRIDENT;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInfernusTridentLaunch(ProjectileLaunchEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof Trident trident))
            return;
        if (!(trident.getShooter() instanceof LivingEntity shooter))
            return;
        if (!this.isAvailableToUse(shooter))
            return;

        ItemStack item = trident.getItem();

        int level = EnchantManager.getEnchantmentLevel(item, this);
        if (level <= 0)
            return;

        trident.setFireTicks(Integer.MAX_VALUE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInfernusDamageApply(EntityDamageByEntityEvent e) {
        Entity entity = e.getDamager();
        if (!(entity instanceof Trident trident))
            return;

        ItemStack item = trident.getItem();

        int level = EnchantManager.getEnchantmentLevel(item, this);
        if (level <= 0 || trident.getFireTicks() <= 0)
            return;

        int ticks = this.getFireTicks(level);
        e.getEntity().setFireTicks(ticks);
    }
}
