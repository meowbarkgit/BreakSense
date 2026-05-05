package com.rcl.client.mixin;

import com.rcl.client.BreakSenseLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MinecraftMixin {

    @Shadow private float destroyProgress;
    @Shadow private BlockPos destroyBlockPos;

    private float lastProgress = 0f;
    private BlockPos lastPos = null;

    @Inject(method = "continueDestroyBlock", at = @At("TAIL"))
    private void onContinueDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) return;
        if (!pos.equals(destroyBlockPos)) return;

        BlockState state = mc.level.getBlockState(pos);

        float progress = destroyProgress;
        float hardness = state.getDestroySpeed(mc.level, pos);

        boolean justBroken = progress >= 1.0f && lastProgress < 1.0f;

        if (lastPos == null || !lastPos.equals(pos)) {
            lastProgress = 0f;
            lastPos = pos;
        }

        BreakSenseLogic.apply(mc, state, hardness, progress, justBroken);

        lastProgress = progress;
    }

    @Inject(method = "stopDestroyBlock", at = @At("TAIL"))
    private void onStopDestroy(CallbackInfo ci) {
        resetState();
    }

    @Inject(method = "destroyBlock", at = @At("TAIL"))
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().level != null) {
            BreakSenseLogic.apply(
                    Minecraft.getInstance(),
                    Minecraft.getInstance().level.getBlockState(pos),
                    1.0f,
                    1.0f,
                    true
            );
        }
    }

    private void resetState() {
        lastProgress = 0f;
        lastPos = null;
        BreakSenseLogic.reset();
    }
}