package com.orbitalstrike.mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrbitalStrikeMod implements ModInitializer {
    public static final String MOD_ID = "orbitalstrike";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Orbital Strike 1.0.0 Initializing for Minecraft 1.21.1...");
        
        // Register /orbital command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            OrbitalCommand.register(dispatcher);
        });

        // Register Right-Click Item Callback
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isEmpty() && stack.getItem() == Items.FISHING_ROD) {
                Text name = stack.getName();
                String displayName = name.getString();

                if (displayName.contains("Nuke Shot")) {
                    if (!world.isClient()) {
                        executeNukeStrike(player, (ServerWorld) world, stack);
                    }
                    return TypedActionResult.success(stack, world.isClient());
                } else if (displayName.contains("Stab Shot")) {
                    if (!world.isClient()) {
                        executeStabStrike(player, (ServerWorld) world, stack);
                    }
                    return TypedActionResult.success(stack, world.isClient());
                }
            }
            return TypedActionResult.pass(stack);
        });
    }

    private void executeNukeStrike(PlayerEntity player, ServerWorld world, ItemStack stack) {
        // Raycast to find target block within 150 blocks
        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d reachVec = eyePos.add(lookVec.multiply(150.0));
        
        BlockHitResult hit = world.raycast(new RaycastContext(
                eyePos, reachVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player
        ));

        Vec3d targetPos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            targetPos = hit.getPos();
        } else {
            targetPos = player.getPos().add(lookVec.multiply(20.0));
        }

        // Break / consume rod immediately
        stack.decrement(1);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.5f, 0.7f);
        world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 4.0f, 0.8f);

        player.sendMessage(Text.literal("§c[ORBITAL] §eพิกัดได้รับการล็อค! กำลังยิง NUKE STRIKE (10 วง)..."), false);

        // Spawn 10 concentric circles of TNT
        int rings = 10;
        for (int ring = 1; ring <= rings; ring++) {
            double radius = ring * 3.2;
            int points = ring * 6; // Spacing increases with radius
            int fuseOffset = ring * 10; // Staggered detonations

            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = targetPos.x + radius * Math.cos(angle);
                double z = targetPos.z + radius * Math.sin(angle);
                double y = targetPos.y + 2.0;

                TntEntity tnt = new TntEntity(world, x, y, z, player);
                tnt.setFuse(fuseOffset + 20);
                world.spawnEntity(tnt);
            }
        }
    }

    private void executeStabStrike(PlayerEntity player, ServerWorld world, ItemStack stack) {
        // Raycast target pos or current player forward pos
        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d reachVec = eyePos.add(lookVec.multiply(100.0));
        BlockHitResult hit = world.raycast(new RaycastContext(
                eyePos, reachVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player
        ));

        double targetX = (hit.getType() == HitResult.Type.BLOCK) ? hit.getBlockPos().getX() + 0.5 : player.getX();
        double targetZ = (hit.getType() == HitResult.Type.BLOCK) ? hit.getBlockPos().getZ() + 0.5 : player.getZ();

        // Break rod
        stack.decrement(1);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.5f, 0.7f);

        player.sendMessage(Text.literal("§6[ORBITAL] §bลำแสง STAB SHOT กำลังเจาะทะลุจาก Y=120 ถึง Y=-58!"), false);

        // Penetrating vertical strike from Y=120 down to Y=-58
        for (int y = 120; y >= -58; y -= 4) {
            final int currentY = y;
            TntEntity tnt = new TntEntity(world, targetX, currentY, targetZ, player);
            tnt.setFuse(10 + (120 - currentY) / 4); // Cascading drill explosion effect
            world.spawnEntity(tnt);
        }

        world.playSound(null, targetX, player.getY(), targetZ,
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 5.0f, 0.5f);
    }
}
