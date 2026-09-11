package org.admany.iwt.neoforge;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public final class AreaCheckForgeScratch {
    public long[] offsets = new long[0];
    public final Random random = new Random();
    public final RandomSource ruleRandom = RandomSource.create();
    public final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    public void ensure(int length) {
        if (offsets.length < length) {
            offsets = new long[length];
        }
    }
}
