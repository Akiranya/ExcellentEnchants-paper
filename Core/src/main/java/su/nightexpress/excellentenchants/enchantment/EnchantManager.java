package su.nightexpress.excellentenchants.enchantment;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nexmedia.engine.api.manager.AbstractManager;
import su.nexmedia.engine.utils.EntityUtil;
import su.nexmedia.engine.utils.ItemUtil;
import su.nexmedia.engine.utils.PDCUtil;
import su.nexmedia.engine.utils.random.Rnd;
import su.nightexpress.excellentenchants.ExcellentEnchants;
import su.nightexpress.excellentenchants.ExcellentEnchantsAPI;
import su.nightexpress.excellentenchants.api.enchantment.ExcellentEnchant;
import su.nightexpress.excellentenchants.api.enchantment.IEnchantment;
import su.nightexpress.excellentenchants.api.enchantment.meta.Potioned;
import su.nightexpress.excellentenchants.api.enchantment.type.PassiveEnchant;
import su.nightexpress.excellentenchants.config.Config;
import su.nightexpress.excellentenchants.config.ObtainSettings;
import su.nightexpress.excellentenchants.enchantment.listener.EnchantAnvilListener;
import su.nightexpress.excellentenchants.enchantment.listener.EnchantGenericListener;
import su.nightexpress.excellentenchants.enchantment.listener.EnchantHandlerListener;
import su.nightexpress.excellentenchants.enchantment.menu.EnchantmentsListMenu;
import su.nightexpress.excellentenchants.enchantment.task.ArrowTrailsTask;
import su.nightexpress.excellentenchants.enchantment.task.PotionEffectsTask;
import su.nightexpress.excellentenchants.enchantment.type.ObtainType;
import su.nightexpress.excellentenchants.tier.Tier;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class EnchantManager extends AbstractManager<ExcellentEnchants> {

    private EnchantmentsListMenu enchantmentsListMenu;

    private ArrowTrailsTask arrowTrailsTask;
    private PotionEffectsTask potionEffectsTask;

    public EnchantManager(@NotNull ExcellentEnchants plugin) {
        super(plugin);
    }

    @Override
    protected void onLoad() {
        EnchantRegister.setup();

        this.enchantmentsListMenu = new EnchantmentsListMenu(this.plugin);
        this.addListener(new EnchantHandlerListener(this));
        this.addListener(new EnchantGenericListener(this));
        this.addListener(new EnchantAnvilListener(this.plugin));

        this.arrowTrailsTask = new ArrowTrailsTask(this.plugin);
        this.arrowTrailsTask.start();

        this.potionEffectsTask = new PotionEffectsTask(this.plugin);
        this.potionEffectsTask.start();
    }

    @Override
    protected void onShutdown() {
        if (this.enchantmentsListMenu != null) {
            this.enchantmentsListMenu.clear();
            this.enchantmentsListMenu = null;
        }
        if (this.arrowTrailsTask != null) {
            this.arrowTrailsTask.stop();
            this.arrowTrailsTask = null;
        }
        if (this.potionEffectsTask != null) {
            this.potionEffectsTask.stop();
            this.potionEffectsTask = null;
        }
        EnchantRegister.shutdown();
    }

    @NotNull
    public EnchantmentsListMenu getEnchantsListGUI() {
        return enchantmentsListMenu;
    }

    public static boolean isEnchantable(@NotNull ItemStack item) {
        if (item.getType().isAir()) return false;

        return item.getType() == Material.ENCHANTED_BOOK || Stream.of(EnchantmentTarget.values()).anyMatch(target -> target.includes(item));
    }

    public static @NotNull Map<Enchantment, Integer> getEnchantsToPopulate(@NotNull ItemStack item, @NotNull ObtainType obtainType) {
        return getEnchantsToPopulate(item, obtainType, new HashMap<>(), (enchant) -> enchant.generateLevel(obtainType));
    }

    public static @NotNull Map<Enchantment, Integer> getEnchantsToPopulate(
        @NotNull ItemStack item,
        @NotNull ObtainType obtainType,
        @NotNull Map<Enchantment, Integer> enchantsPrepared,
        @NotNull Function<ExcellentEnchant, Integer> levelFunc
    ) {
        Map<Enchantment, Integer> enchantsToAdd = new HashMap<>(enchantsPrepared);

        ObtainSettings settings = Config.getObtainSettings(obtainType).orElse(null);
        if (settings == null || !Rnd.chance(settings.getEnchantsCustomGenerationChance())) return enchantsToAdd;

        int enchMax = settings.getEnchantsTotalMax();
        int enchRoll = Rnd.get(settings.getEnchantsCustomMin(), settings.getEnchantsCustomMax());

        // Класс для исключения неудачных попыток.
        EnchantPopulator populator = new EnchantPopulator(obtainType, item);

        // Херачим до талого, пока нужное количество не будет добавлено
        // или не закончатся чары и/или тиры.
        while (!populator.isEmpty() && enchRoll > 0) {
            // Достигнут максимум чар (любых) для итема, заканчиваем.
            if (enchantsToAdd.size() >= enchMax) break;

            Tier tier = populator.getTierByChance();
            if (tier == null) break; // Нет тира?

            ExcellentEnchant enchant = populator.getEnchantByChance(tier);
            // В тире нет подходящих чар (вообще) для итема, исключаем и идем дальше.
            if (enchant == null) {
                populator.getEnchants().remove(tier);
                continue;
            }

            // Среди уже добавленных чар есть конфликты с тем, что нашли.
            // Исключаем, идем дальше.
            if (enchantsToAdd.keySet().stream().anyMatch(has -> has.conflictsWith(enchant) || enchant.conflictsWith(has))) {
                populator.getEnchants(tier).remove(enchant);
                continue;
            }

            // Не получилось сгенерировать подходящий уровень.
            // Исключаем, идем дальше.
            int level = levelFunc.apply(enchant);
            if (level < enchant.getStartLevel()) {
                populator.getEnchants(tier).remove(enchant);
                continue;
            }

            // Добавляем чар, засчитываем попытку.
            populator.getEnchants(tier).remove(enchant);
            enchantsToAdd.put(enchant, level);
            enchRoll--;
        }

        return enchantsToAdd;
    }

    public static boolean populateEnchantments(@NotNull ItemStack item, @NotNull ObtainType obtainType) {
        int enchantsHad = EnchantManager.getEnchantmentsAmount(item);

        EnchantManager.getEnchantsToPopulate(item, obtainType).forEach((enchantment, level) -> {
            EnchantManager.addEnchantment(item, enchantment, level, false);
        });

        return EnchantManager.getEnchantmentsAmount(item) != enchantsHad;
    }

    public static boolean addEnchantment(@NotNull ItemStack item, @NotNull Enchantment enchantment, int level, boolean force) {
        if (!force && !enchantment.canEnchantItem(item)) return false;

        EnchantManager.removeEnchantment(item, enchantment);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            if (!storageMeta.addStoredEnchant(enchantment, level, true)) return false;
        } else {
            if (!meta.addEnchant(enchantment, level, true)) return false;
        }
        item.setItemMeta(meta);

        return true;
    }

    public static void removeEnchantment(@NotNull ItemStack item, @NotNull Enchantment enchantment) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            storageMeta.removeStoredEnchant(enchantment);
        } else {
            meta.removeEnchant(enchantment);
        }
        item.setItemMeta(meta);
    }

    @NotNull
    public static Map<Enchantment, Integer> getEnchantments(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasEnchants()) return Collections.emptyMap();

        return (meta instanceof EnchantmentStorageMeta storageMeta)
            ? storageMeta.getStoredEnchants()
            : meta.getEnchants();
    }

    public static int getEnchantmentsAmount(@NotNull ItemStack item) {
        return EnchantManager.getEnchantments(item).size();
    }

    public static boolean hasEnchantment(@NotNull ItemStack item, @NotNull Enchantment enchantment) {
        return EnchantManager.getEnchantmentLevel(item, enchantment) > 0;
    }

    public static int getEnchantmentLevel(@NotNull ItemStack item, @NotNull Enchantment enchant) {
        return EnchantManager.getEnchantments(item).getOrDefault(enchant, 0);
    }

    public static int getEnchantmentCharges(@NotNull ItemStack item, @NotNull ExcellentEnchant enchant) {
        return PDCUtil.getIntData(item, enchant.getChargesKey());
    }

    public static boolean isEnchantmentOutOfCharges(@NotNull ItemStack item, @NotNull ExcellentEnchant enchant) {
        return enchant.isChargesEnabled() && getEnchantmentCharges(item, enchant) == 0;
    }

    public static boolean isEnchantmentFullOfCharges(@NotNull ItemStack item, @NotNull ExcellentEnchant enchant) {
        if (!enchant.isChargesEnabled()) return true;

        int level = getEnchantmentLevel(item, enchant);
        int max = enchant.getChargesMax(level);
        return EnchantManager.getEnchantmentCharges(item, enchant) == max;
    }

    public static void consumeEnchantmentCharges(@NotNull ItemStack item, @NotNull ExcellentEnchant enchant) {
        if (!enchant.isChargesEnabled()) return;

        int level = getEnchantmentLevel(item, enchant);
        int has = getEnchantmentCharges(item, enchant);
        int use = enchant.getChargesConsumeAmount(level);
        EnchantManager.setEnchantmentCharges(item, enchant, has - use);
    }

    public static void restoreEnchantmentCharges(@NotNull ItemStack item, @NotNull ExcellentEnchant enchant) {
        if (!enchant.isChargesEnabled()) return;

        int level = getEnchantmentLevel(item, enchant);
        int max = enchant.getChargesMax(level);
        EnchantManager.setEnchantmentCharges(item, enchant, max);
    }

    public static void rechargeEnchantmentCharges(@NotNull ItemStack item, @NotNull ExcellentEnchant enchant) {
        if (!enchant.isChargesEnabled()) return;

        int level = getEnchantmentLevel(item, enchant);
        int recharge = enchant.getChargesRechargeAmount(level);
        int has = getEnchantmentCharges(item, enchant);
        EnchantManager.setEnchantmentCharges(item, enchant, has + recharge);
    }

    public static void setEnchantmentCharges(@NotNull ItemStack item, @NotNull ExcellentEnchant enchant, int charges) {
        if (!enchant.isChargesEnabled()) return;

        int level = getEnchantmentLevel(item, enchant);
        int max = enchant.getChargesMax(level);
        PDCUtil.setData(item, enchant.getChargesKey(), Math.max(0, Math.min(charges, max)));
    }

    public static int getExcellentEnchantmentsAmount(@NotNull ItemStack item) {
        return EnchantManager.getExcellentEnchantments(item).size();
    }

    @NotNull
    public static Map<ExcellentEnchant, Integer> getExcellentEnchantments(@NotNull ItemStack item) {
        Map<ExcellentEnchant, Integer> custom = new TreeMap<>(); // sort while inserting
        EnchantManager.getEnchantments(item).forEach((k, v) -> {
            ExcellentEnchant enchant = EnchantRegister.get(k.getKey());
            if (enchant != null)
                custom.put(enchant, v);
        });
        return custom;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T extends IEnchantment> Map<T, Integer> getExcellentEnchantments(@NotNull ItemStack item, @NotNull Class<T> clazz) {
        Map<T, Integer> custom = new TreeMap<>(); // sort while inserting
        EnchantManager.getEnchantments(item).forEach((k, v) -> {
            ExcellentEnchant enchant = EnchantRegister.get(k.getKey());
            if (enchant != null && clazz.isAssignableFrom(enchant.getClass()))
                custom.put((T) enchant, v);
        });
        return custom;
    }

    @Nullable
    public static ExcellentEnchant getEnchantmentByEffect(@NotNull LivingEntity entity, @NotNull PotionEffect effect) {
        Enchantment enchantment = ExcellentEnchantsAPI.PLUGIN.getEnchantNMS().getEnchantmentByEffect(entity, effect);
        if (enchantment instanceof ExcellentEnchant enchant) return enchant;
        return null;
    }

    public static boolean isEnchantmentEffect(@NotNull LivingEntity entity, @NotNull PotionEffect effect) {
        return getEnchantmentByEffect(entity, effect) != null;
    }

    public static boolean hasEnchantmentEffect(@NotNull LivingEntity entity, @NotNull ExcellentEnchant enchant) {
        return entity.getActivePotionEffects().stream().anyMatch(effect -> enchant.equals(getEnchantmentByEffect(entity, effect)));
    }

    @NotNull
    public static Map<EquipmentSlot, ItemStack> getEquipmentEnchanted(@NotNull LivingEntity entity) {
        Map<EquipmentSlot, ItemStack> equipment = EntityUtil.getEquippedItems(entity);
        equipment.entrySet().removeIf(entry -> {
            ItemStack item = entry.getValue();
            EquipmentSlot slot = entry.getKey();
            if (item == null || item.getType().isAir() || item.getType() == Material.ENCHANTED_BOOK) return true;
            if ((slot == EquipmentSlot.HAND || slot == EquipmentSlot.OFF_HAND) && ItemUtil.isArmor(item)) return true;
            return !item.hasItemMeta();
        });
        return equipment;
    }

    @NotNull
    public static Map<ItemStack, Map<ExcellentEnchant, Integer>> getEquippedEnchants(@NotNull LivingEntity entity) {
        Map<ItemStack, Map<ExcellentEnchant, Integer>> map = new HashMap<>();
        EnchantManager.getEquipmentEnchanted(entity).values().forEach(item -> {
            map.computeIfAbsent(item, k -> new LinkedHashMap<>()).putAll(EnchantManager.getExcellentEnchantments(item));
        });
        return map;
    }

    @NotNull
    public static <T extends IEnchantment> Map<ItemStack, Map<T, Integer>> getEquippedEnchants(@NotNull LivingEntity entity, @NotNull Class<T> clazz) {
        Map<ItemStack, Map<T, Integer>> map = new HashMap<>();
        EnchantManager.getEquipmentEnchanted(entity).values().forEach(item -> {
            map.computeIfAbsent(item, k -> new LinkedHashMap<>()).putAll(EnchantManager.getExcellentEnchantments(item, clazz));
        });
        return map;
    }

    public static void updateEquippedEnchantEffects(@NotNull LivingEntity entity) {
        EnchantManager.getEquippedEnchants(entity, PassiveEnchant.class).forEach((item, enchants) -> {
            enchants.forEach((enchant, level) -> {
                if (enchant instanceof Potioned potioned) {
                    if (enchant.isOutOfCharges(item)) return;
                    if (enchant.onTrigger(entity, item, level)) {
                        enchant.consumeCharges(item);
                    }
                }
            });
        });
    }
}
