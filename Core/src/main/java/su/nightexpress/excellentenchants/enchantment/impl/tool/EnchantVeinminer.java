package su.nightexpress.excellentenchants.enchantment.impl.tool;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import su.nexmedia.engine.api.config.JOption;
import su.nexmedia.engine.utils.EffectUtil;
import su.nexmedia.engine.utils.LocationUtil;
import su.nexmedia.engine.utils.Scaler;
import su.nightexpress.excellentenchants.ExcellentEnchants;
import su.nightexpress.excellentenchants.Placeholders;
import su.nightexpress.excellentenchants.api.enchantment.ExcellentEnchant;
import su.nightexpress.excellentenchants.api.enchantment.type.BlockBreakEnchant;
import su.nightexpress.excellentenchants.api.enchantment.util.EnchantPriority;
import su.nightexpress.excellentenchants.enchantment.EnchantManager;
import su.nightexpress.excellentenchants.enchantment.EnchantRegister;
import su.nightexpress.excellentenchants.enchantment.config.EnchantScaler;
import su.nightexpress.excellentenchants.enchantment.type.FitItemType;
import su.nightexpress.excellentenchants.hook.impl.NoCheatPlusHook;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnchantVeinminer extends ExcellentEnchant implements BlockBreakEnchant {

    public static final String ID = "veinminer";

    private static final BlockFace[] AREA = {BlockFace.UP, BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH};
    private static final String META_BLOCK_VEINED = ID + "_block_veined";
    private static final String PLACEHOLDER_BLOCK_LIMIT = "%enchantment_block_limit%";

    private Scaler blocksLimit;
    private Set<Material> blocksAffected;

    public EnchantVeinminer(@NotNull ExcellentEnchants plugin) {
        super(plugin, ID, EnchantPriority.HIGH);
    }

    @Override
    public void loadConfig() {
        super.loadConfig();

        this.blocksLimit = EnchantScaler.read(this, "Settings.Blocks.Max_At_Once", "6 + " + Placeholders.ENCHANTMENT_LEVEL,
            "How much amount of blocks can be destroted at single use?");

        this.blocksAffected = JOption.create("Settings.Blocks.Affected", new HashSet<>(),
                "List of blocks, that will be affected by this enchantment.",
                "https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html").read(this.cfg).stream()
            .map(type -> Material.getMaterial(type.toUpperCase())).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public @NotNull Set<Material> getBlocksAffected() {
        return this.blocksAffected;
    }

    public int getBlocksLimit(int level) {
        return (int) this.blocksLimit.getValue(level);
    }

    @Override
    public @NotNull UnaryOperator<String> replacePlaceholders(int level) {
        return str -> str
            .transform(super.replacePlaceholders(level))
            .replace(PLACEHOLDER_BLOCK_LIMIT, String.valueOf(this.getBlocksLimit(level)));
    }

    @Override
    public @NotNull FitItemType[] getFitItemTypes() {
        return new FitItemType[]{FitItemType.PICKAXE};
    }

    @Override
    public @NotNull EnchantmentTarget getItemTarget() {
        return EnchantmentTarget.TOOL;
    }

    private @NotNull Set<Block> getNearby(@NotNull Block block) {
        return Stream.of(AREA)
            .map(block::getRelative)
            .filter(blockAdded -> blockAdded.getType() == block.getType())
            .collect(Collectors.toSet());
    }

    private void vein(@NotNull Player player, @NotNull Block source, int level) {
        Set<Block> ores = new HashSet<>();
        Set<Block> prepare = new HashSet<>(this.getNearby(source));

        int limit = Math.min(this.getBlocksLimit(level), 30);
        if (limit < 0)
            return;

        while (ores.addAll(prepare) && ores.size() < limit) {
            Set<Block> nearby = new HashSet<>();
            prepare.forEach(prepared -> nearby.addAll(this.getNearby(prepared)));
            prepare.clear();
            prepare.addAll(nearby);
        }
        ores.remove(source);
        ores.forEach(ore -> {
            // Play block break particles before the block broken.
            EffectUtil.playEffect(LocationUtil.getCenter(ore.getLocation()), Particle.BLOCK_CRACK.name(), ore.getType().name(), 0.2, 0.2, 0.2, 0.1, 20);

            ore.setMetadata(META_BLOCK_VEINED, new FixedMetadataValue(this.plugin, true));
            //plugin.getNMS().breakBlock(player, ore);
            player.breakBlock(ore);
            ore.removeMetadata(META_BLOCK_VEINED, this.plugin);
        });
    }

    @Override
    public boolean onBreak(@NotNull BlockBreakEvent e, @NotNull Player player, @NotNull ItemStack tool, int level) {
        if (!this.isAvailableToUse(player))
            return false;
        if (EnchantRegister.TUNNEL != null && EnchantManager.hasEnchantment(tool, EnchantRegister.TUNNEL))
            return false;
        if (EnchantRegister.BLAST_MINING != null && EnchantManager.hasEnchantment(tool, EnchantRegister.BLAST_MINING))
            return false;

        Block block = e.getBlock();
        if (block.hasMetadata(META_BLOCK_VEINED))
            return false;
        if (block.getDrops(tool).isEmpty())
            return false;
        if (!this.getBlocksAffected().contains(block.getType()))
            return false;

        NoCheatPlusHook.exemptBlocks(player);
        this.vein(player, block, level);
        NoCheatPlusHook.unexemptBlocks(player);

        return true;
    }
}
