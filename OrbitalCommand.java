package com.orbitalstrike.mod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class OrbitalCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("orbital")
                .requires(source -> source.hasPermissionLevel(2)) // OP Level 2
                .then(CommandManager.literal("nuke")
                    .then(CommandManager.literal("give")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                            .then(CommandManager.argument("ring", IntegerArgumentType.integer(1, 20))
                                .executes(ctx -> giveStrikeRod(ctx.getSource(), "nuke",
                                        IntegerArgumentType.getInteger(ctx, "amount"),
                                        IntegerArgumentType.getInteger(ctx, "ring")))
                            )
                            .executes(ctx -> giveStrikeRod(ctx.getSource(), "nuke",
                                    IntegerArgumentType.getInteger(ctx, "amount"), 10))
                        )
                        .executes(ctx -> giveStrikeRod(ctx.getSource(), "nuke", 1, 10))
                    )
                )
                .then(CommandManager.literal("stab")
                    .then(CommandManager.literal("give")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                            .then(CommandManager.argument("ring", IntegerArgumentType.integer(1, 20))
                                .executes(ctx -> giveStrikeRod(ctx.getSource(), "stab",
                                        IntegerArgumentType.getInteger(ctx, "amount"),
                                        IntegerArgumentType.getInteger(ctx, "ring")))
                            )
                            .executes(ctx -> giveStrikeRod(ctx.getSource(), "stab",
                                    IntegerArgumentType.getInteger(ctx, "amount"), 1))
                        )
                        .executes(ctx -> giveStrikeRod(ctx.getSource(), "stab", 1, 1))
                    )
                )
        );
    }

    private static int giveStrikeRod(ServerCommandSource source, String type, int count, int ring) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendFeedback(() -> Text.literal("§cคำสั่งนี้ต้องใช้โดยผู้เล่นในเซิร์ฟเวอร์เท่านั้น"), false);
            return 0;
        }

        ItemStack rod = new ItemStack(Items.FISHING_ROD, count);

        if (type.equalsIgnoreCase("nuke")) {
            rod.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§c§l⚡ Nuke Shot Beacon ⚡").formatted(Formatting.RED, Formatting.BOLD));
            LoreComponent lore = new LoreComponent(List.of(
                    Text.literal("§7คลิกขวาเพื่อยิงสัญญาณเรียกฝูงขีปนาวุธ"),
                    Text.literal("§eระเบิด TNT จำนวน " + (ring > 0 ? ring : 10) + " วงแหวนซ้อน"),
                    Text.literal("§c[คำเตือน: เบ็ดจะแตกทำลายทันทีเมื่อใช้งาน]")
            ));
            rod.set(DataComponentTypes.LORE, lore);
            player.giveItemStack(rod);
            source.sendFeedback(() -> Text.literal("§a[Orbital Strike] มอบ Nuke Shot ให้ผู้เล่น " + player.getName().getString() + " จำนวน " + count + " ชิ้น (Rings: " + ring + ")"), true);
        } else {
            rod.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§b§l🗡 Stab Shot Beacon 🗡").formatted(Formatting.AQUA, Formatting.BOLD));
            LoreComponent lore = new LoreComponent(List.of(
                    Text.literal("§7คลิกขวาเพื่อยิงเสาเข็มทำลายล้างความสูงสูง"),
                    Text.literal("§bเจาะทะลุระเบิดตั้งแต่ Y:120 ลงไปถึง Y:-58"),
                    Text.literal("§c[คำเตือน: เบ็ดจะแตกทำลายทันทีเมื่อใช้งาน]")
            ));
            rod.set(DataComponentTypes.LORE, lore);
            player.giveItemStack(rod);
            source.sendFeedback(() -> Text.literal("§a[Orbital Strike] มอบ Stab Shot ให้ผู้เล่น " + player.getName().getString() + " จำนวน " + count + " ชิ้น"), true);
        }

        return 1;
    }
}
