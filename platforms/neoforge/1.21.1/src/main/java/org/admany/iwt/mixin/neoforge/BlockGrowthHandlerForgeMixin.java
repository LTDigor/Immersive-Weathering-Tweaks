package org.admany.iwt.mixin.neoforge;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.ordana.immersive_weathering.configs.CommonConfigs;
import com.ordana.immersive_weathering.data.block_growths.BlockGrowthHandler;
import com.ordana.immersive_weathering.data.block_growths.IConditionalGrowingBlock;
import com.ordana.immersive_weathering.data.block_growths.TickSource;
import com.ordana.immersive_weathering.data.block_growths.growths.IBlockGrowth;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockGrowthHandler.class, remap = false)
public abstract class BlockGrowthHandlerForgeMixin {
    @Shadow @Final
    private static Map<TickSource, Map<Block, Set<IBlockGrowth>>> GROWTH_FOR_BLOCK;
    @Shadow @Final
    private static Map<TickSource, Set<IBlockGrowth>> UNIVERSAL_GROWTHS;

    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void iwt$evaluateGrowthOnce(TickSource source, BlockState state, ServerLevel level,
                                               BlockPos pos, CallbackInfo ci) {
        if (!CommonConfigs.BLOCK_GROWTHS.get()) {
            ci.cancel();
            return;
        }
        Block block = state.getBlock();
        if (block instanceof IConditionalGrowingBlock conditional && !conditional.canGrow(state)) {
            ci.cancel();
            return;
        }
        iwt$runGrowths(source, state, level, pos);
        ci.cancel();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void iwt$runGrowths(TickSource source, BlockState state, ServerLevel level, BlockPos pos) {
        Set<IBlockGrowth> universal = UNIVERSAL_GROWTHS.get(source);
        Map<Block, Set<IBlockGrowth>> byBlock = GROWTH_FOR_BLOCK.get(source);
        Set<IBlockGrowth> blockGrowths = byBlock == null ? null : byBlock.get(state.getBlock());
        if ((universal == null || universal.isEmpty()) && (blockGrowths == null || blockGrowths.isEmpty())) {
            return;
        }

        Supplier biome = Suppliers.memoize(() -> level.getBiome(pos));
        if (universal != null) {
            for (IBlockGrowth growth : universal) {
                growth.tryGrowing(pos, state, level, (java.util.function.Supplier) biome);
            }
        }
        if (blockGrowths != null) {
            for (IBlockGrowth growth : blockGrowths) {
                growth.tryGrowing(pos, state, level, (java.util.function.Supplier) biome);
            }
        }
    }

    @Inject(method = "performSkyAccessTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void iwt$skipImpossibleSkySamples(ServerLevel level, LevelChunk levelChunk,
                                                     int randomTickSpeed, CallbackInfo ci) {
        ChunkPos chunkPos = levelChunk.getPos();
        float chance = (float) randomTickSpeed / 48.0f;
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        boolean raining = level.isRaining();

        do {
            if (!(chance > level.getRandom().nextFloat())) {
                continue;
            }
            BlockPos firstAir = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING,
                level.getBlockRandomPos(minX, 0, minZ, 15)
            );
            BlockPos target = firstAir.below();
            BlockState state = level.getBlockState(target);
            Block block = state.getBlock();

            if (!raining) {
                if (!iwt$hasGrowth(TickSource.CLEAR_SKY, block)) {
                    continue;
                }
                iwt$runGrowths(TickSource.CLEAR_SKY, state, level, target.immutable());
                continue;
            }

            if (!iwt$hasGrowth(TickSource.RAIN, block) && !iwt$hasGrowth(TickSource.SNOW, block)) {
                continue;
            }
            Biome.Precipitation precipitation = level.getBiome(target).value().getPrecipitationAt(target);
            TickSource source = precipitation == Biome.Precipitation.SNOW ? TickSource.SNOW : TickSource.RAIN;
            if (!iwt$hasGrowth(source, block)) {
                continue;
            }
            iwt$runGrowths(source, state, level, target.immutable());
        } while ((chance -= 1.0f) > 0.0f);

        ci.cancel();
    }

    private static boolean iwt$hasGrowth(TickSource source, Block block) {
        Set<IBlockGrowth> universal = UNIVERSAL_GROWTHS.get(source);
        if (universal != null && !universal.isEmpty()) {
            return true;
        }
        Map<Block, Set<IBlockGrowth>> byBlock = GROWTH_FOR_BLOCK.get(source);
        if (byBlock == null) {
            return false;
        }
        Set<IBlockGrowth> growths = byBlock.get(block);
        return growths != null && !growths.isEmpty();
    }
}
