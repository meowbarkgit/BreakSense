package com.rcl.client;

import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.controller.dualsense.DualSenseComponent;
import dev.isxander.controlify.driver.sdl.dualsense.DualsenseTriggerEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;

public class BreakSenseLogic {

    private static int breakTicks = 0;

    public static void apply(Minecraft mc, BlockState state, float hardness, float progress, boolean justBroken) {

        Player player = mc.player;
        if (player == null) return;

        DualSenseComponent ds = getDualSense();
        if (ds == null) return;

        if (justBroken && breakTicks == 0) {
            breakTicks = 3;
        }

        if (breakTicks > 0) {
            breakTicks--;

            DualsenseTriggerEffect effect;

            if (breakTicks == 2) {
                effect = new DualsenseTriggerEffect.Vibration((byte) 9, (byte) 8, (byte) 9);
            } else {
                effect = new DualsenseTriggerEffect.Feedback((byte) 9, (byte) 0);
            }

            ds.setRightTriggerEffect(effect);
            return;
        }

        ItemStack tool = player.getMainHandItem();

        float efficiency = getEfficiency(tool);
        float haste = getHaste(player);
        float fatigue = getFatigue(player);

        float speed = (1.0f + efficiency * 0.08f) * haste * fatigue;
        float strength = Math.clamp((hardness / 10.0f) * progress / speed, 0.0f, 1.0f);

        MaterialType material = getMaterial(state);
        ToolType toolType = getToolType(tool);

        DualsenseTriggerEffect base = getMaterialEffect(material, progress, strength);
        DualsenseTriggerEffect finalEffect = applyToolModifier(toolType, base, progress);

        ds.setRightTriggerEffect(finalEffect);
    }

    public static void reset() {
        breakTicks = 0;

        getDualSenseOptional().ifPresent(ds ->
                ds.setRightTriggerEffect(new DualsenseTriggerEffect.Off())
        );
    }

    private static DualSenseComponent getDualSense() {
        return getDualSenseOptional().orElse(null);
    }

    private static Optional<DualSenseComponent> getDualSenseOptional() {
        return ControlifyApi.get().getCurrentController()
                .flatMap(controller -> controller.getComponent(DualSenseComponent.ID))
                .filter(c -> c instanceof DualSenseComponent)
                .map(c -> (DualSenseComponent) c);
    }

    enum MaterialType {
        STONE, WOOD, DIRT, METAL, GLASS, WOOL, OTHER
    }

    private static MaterialType getMaterial(BlockState state) {
        SoundType sound = state.getSoundType();

        if (sound == SoundType.STONE) return MaterialType.STONE;
        if (sound == SoundType.WOOD) return MaterialType.WOOD;
        if (sound == SoundType.GRAVEL || sound == SoundType.SAND) return MaterialType.DIRT;
        if (sound == SoundType.METAL) return MaterialType.METAL;
        if (sound == SoundType.GLASS) return MaterialType.GLASS;
        if (sound == SoundType.WOOL) return MaterialType.WOOL;

        return MaterialType.OTHER;
    }

    private static DualsenseTriggerEffect getMaterialEffect(MaterialType material, float progress, float strength) {

        switch (material) {

            case STONE -> {
                byte pulse = (byte) (Math.sin(progress * 30) * 2 + 4);
                return new DualsenseTriggerEffect.Vibration((byte) 6, pulse, (byte) 6);
            }

            case WOOD -> {
                return new DualsenseTriggerEffect.Vibration(
                        (byte) 5,
                        (byte) (3 + strength * 4),
                        (byte) 5
                );
            }

            case DIRT -> {
                return new DualsenseTriggerEffect.Vibration(
                        (byte) 3,
                        (byte) (2 + strength * 2),
                        (byte) 4
                );
            }

            case METAL -> {
                return new DualsenseTriggerEffect.Feedback(
                        (byte) 7,
                        (byte) (5 + strength * 4)
                );
            }

            case GLASS -> {
                return new DualsenseTriggerEffect.Vibration(
                        (byte) 8,
                        (byte) 9,
                        (byte) 2
                );
            }

            case WOOL -> {
                return new DualsenseTriggerEffect.Feedback(
                        (byte) 2,
                        (byte) 2
                );
            }

            default -> {
                return new DualsenseTriggerEffect.Feedback(
                        (byte) 4,
                        (byte) (3 + strength * 4)
                );
            }
        }
    }

    private static DualsenseTriggerEffect applyToolModifier(
            ToolType tool,
            DualsenseTriggerEffect base,
            float progress
    ) {
        if (tool == ToolType.PICKAXE) {
            byte pulse = (byte) (Math.sin(progress * 30) * 2 + 4);

            return new DualsenseTriggerEffect.Vibration(
                    (byte) 6,
                    pulse,
                    (byte) 6
            );
        }

        return base;
    }

    enum ToolType {
        PICKAXE, AXE, SHOVEL, HAND, OTHER
    }

    private static ToolType getToolType(ItemStack stack) {
        if (stack.isEmpty()) return ToolType.HAND;
        if (stack.is(ItemTags.PICKAXES)) return ToolType.PICKAXE;
        if (stack.is(ItemTags.AXES)) return ToolType.AXE;
        if (stack.is(ItemTags.SHOVELS)) return ToolType.SHOVEL;
        return ToolType.OTHER;
    }

    private static float getEfficiency(ItemStack stack) {
        var enchants = stack.getEnchantments();

        for (var entry : enchants.entrySet()) {
            if (entry.getKey().is(net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    private static float getHaste(Player player) {
        var effect = player.getEffect(MobEffects.HASTE);

        if (effect != null) {
            int lvl = effect.getAmplifier();
            return 1.0f + (lvl + 1) * 0.2f;
        }
        return 1.0f;
    }

    private static float getFatigue(Player player) {
        var effect = player.getEffect(MobEffects.MINING_FATIGUE);

        if (effect != null) {
            int lvl = effect.getAmplifier();

            return switch (lvl) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 0.00081f;
            };
        }

        return 1.0f;
    }
}