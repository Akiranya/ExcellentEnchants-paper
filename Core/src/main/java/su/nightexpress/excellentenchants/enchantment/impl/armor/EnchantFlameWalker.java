package su.nightexpress.excellentenchants.enchantment.impl.armor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nexmedia.engine.api.manager.ICleanable;
import su.nexmedia.engine.api.task.AbstractTask;
import su.nexmedia.engine.utils.EffectUtil;
import su.nexmedia.engine.utils.random.Rnd;
import su.nightexpress.excellentenchants.ExcellentEnchants;
import su.nightexpress.excellentenchants.api.enchantment.ExcellentEnchant;
import su.nightexpress.excellentenchants.api.enchantment.util.EnchantPriority;
import su.nightexpress.excellentenchants.enchantment.EnchantManager;
import su.nightexpress.excellentenchants.enchantment.config.EnchantScaler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class EnchantFlameWalker extends ExcellentEnchant implements ICleanable {

    public static final String ID = "flame_walker";

    private static final BlockFace[] FACES = {BlockFace.SOUTH, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST};
    private static final Map<Block, Long> BLOCKS_TO_DESTROY = new ConcurrentHashMap<>();

    private EnchantScaler blockDecayTime;
    private BlockTickTask blockTickTask;

    public EnchantFlameWalker(@NotNull ExcellentEnchants plugin) {
        super(plugin, ID, EnchantPriority.MEDIUM);

        this.blockTickTask = new BlockTickTask(plugin);
        this.blockTickTask.start();
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        this.blockDecayTime = EnchantScaler.read(this, "Settings.Block_Decay", "12.0",
            "Sets up to how long (in seconds) blocks will stay before turn back to lava.");
    }

    @Override
    public void clear() {
        if (this.blockTickTask != null) {
            this.blockTickTask.stop();
            this.blockTickTask = null;
        }
        BLOCKS_TO_DESTROY.keySet().forEach(block -> block.setType(Material.LAVA));
        BLOCKS_TO_DESTROY.clear();
    }

    public static void addBlock(@NotNull Block block, double seconds) {
        BLOCKS_TO_DESTROY.put(block, (long) (System.currentTimeMillis() + seconds * 1000L));
    }

    @Override
    @NotNull
    public EnchantmentTarget getItemTarget() {
        return EnchantmentTarget.ARMOR_FEET;
    }

    public double getBlockDecayTime(int level) {
        return this.blockDecayTime.getValue(level);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (player.isFlying() || !this.isAvailableToUse(player))
            return;

        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null)
            return;
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ())
            return;

        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || boots.getType().isAir())
            return;

        int level = EnchantManager.getEnchantmentLevel(boots, this);
        if (level <= 0)
            return;

        Block bTo = to.getBlock().getRelative(BlockFace.DOWN);
        boolean hasLava = Stream.of(FACES).anyMatch(face -> bTo.getRelative(face).getType() == Material.LAVA);
        if (!hasLava)
            return;

        plugin.getEnchantNMS().handleFlameWalker(player, player.getLocation(), level).forEach(block -> {
            addBlock(block, Rnd.getDouble(this.getBlockDecayTime(level)) + 1);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFlameWalkerBlock(BlockBreakEvent e) {
        if (BLOCKS_TO_DESTROY.containsKey(e.getBlock())) {
            e.setDropItems(false);
            e.setExpToDrop(0);
            e.getBlock().setType(Material.LAVA);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMagmaDamage(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.HOT_FLOOR)
            return;
        if (!(e.getEntity() instanceof LivingEntity livingEntity))
            return;
        if (!this.isAvailableToUse(livingEntity))
            return;

        EntityEquipment equipment = livingEntity.getEquipment();
        if (equipment == null)
            return;

        ItemStack boots = equipment.getBoots();
        if (boots == null || boots.getType().isAir())
            return;

        int level = EnchantManager.getEnchantmentLevel(boots, this);
        if (level <= 0)
            return;

        e.setCancelled(true);
    }

    static class BlockTickTask extends AbstractTask<ExcellentEnchants> {

        public BlockTickTask(@NotNull ExcellentEnchants plugin) {
            super(plugin, 1, false);
        }

        @Override
        public void action() {
            long now = System.currentTimeMillis();

            BLOCKS_TO_DESTROY.keySet().removeIf(block -> {
                if (block.isEmpty())
                    return true;

                long time = BLOCKS_TO_DESTROY.get(block);
                if (now >= time) {
                    block.setType(Material.LAVA);
                    EffectUtil.playEffect(block.getLocation(), Particle.BLOCK_CRACK, Material.MAGMA_BLOCK.name(), 0.5, 0.7, 0.5, 0.03, 50);
                    return true;
                }
                return false;
            });
        }
    }
}