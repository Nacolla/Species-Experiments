package com.ninni.species.server.item;

import com.ninni.species.server.criterion.SpeciesCriterion;
import com.ninni.species.server.entity.mob.update_3.Spectre;
import com.ninni.species.server.item.util.HasImportantInteraction;
import com.ninni.species.registry.SpeciesSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WickedMaskItem extends Item implements Equipable, HasImportantInteraction {
    public static final DispenseItemBehavior DISPENSE_ITEM_BEHAVIOR = new DefaultDispenseItemBehavior() {
        protected ItemStack execute(BlockSource blockSource, ItemStack stack) {
            return WickedMaskItem.dispenseMask(blockSource, stack) ? stack : super.execute(blockSource, stack);
        }
    };

    public static boolean dispenseMask(BlockSource blockSource, ItemStack stack) {
        BlockPos blockpos = blockSource.getPos().relative(blockSource.getBlockState().getValue(DispenserBlock.FACING));
        List<LivingEntity> list = blockSource.getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(blockpos), EntitySelector.NO_SPECTATORS.and(new EntitySelector.MobCanWearArmorEntitySelector(stack)));
        if (list.isEmpty()) return false;
        else {
            LivingEntity livingentity = list.get(0);
            EquipmentSlot equipmentslot = Mob.getEquipmentSlotForItem(stack);
            ItemStack itemstack = stack.split(1);
            livingentity.setItemSlot(equipmentslot, itemstack);
            if (livingentity instanceof Mob) {
                ((Mob)livingentity).setDropChance(equipmentslot, 2.0F);
                ((Mob)livingentity).setPersistenceRequired();
            }
            return true;
        }
    }

    public WickedMaskItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!stack.hasTag()) stack.setTag(new CompoundTag());
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isSecondaryUseActive()) return this.swapWithEquipmentSlot(this, level, player, hand);
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (tryImprint(stack, entity, player, hand)) {
            return InteractionResult.SUCCESS;
        }
        if (entity instanceof Allay allay && !allay.hasItemInHand()) return InteractionResult.SUCCESS;
        return super.interactLivingEntity(stack, player, entity, hand);
    }

    /** Server-side imprint of {@code entity}'s NBT into an empty mask. Returns {@code true} when
     *  the imprint ran. Public so the interaction-event handler can call it for multipart and
     *  chain-segment paths that vanilla doesn't route to {@code interactLivingEntity}. */
    public static boolean tryImprint(ItemStack stack, LivingEntity entity, Player player, InteractionHand hand) {
        if (!entity.isAlive() || entity.level().isClientSide
                || (stack.hasTag() && stack.getTag().contains("id"))
                || !player.isSecondaryUseActive()) {
            return false;
        }
        CompoundTag tag = stack.getOrCreateTag();

        // Resolve the id from the registry, not entity.getEncodeId(): some chain-segment classes
        // (AnacondaPart, BoneSerpentPart, VoidWormPart) override getEncodeId to return the head's
        // id for save round-trip, which would mis-imprint the head when the player aimed at a part.
        String fallbackEncodeId = entity.getEncodeId();
        if (fallbackEncodeId != null) {
            net.minecraft.resources.ResourceLocation typeKey =
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            String encodeId = typeKey != null ? typeKey.toString() : fallbackEncodeId;
            tag.putString("id", encodeId);
            tag.putBoolean("OnGround", true);
            if (entity instanceof Spectre spectre) {
                // Set base variant in NBT
                tag.putString("Type", Spectre.Type.SPECTRE.getSerializedName());
            }

            if (entity.hasCustomName() && !"jeb_".equals(entity.getName().getString())) {
                tag.putString("CustomName", Component.Serializer.toJson(entity.getCustomName()));
            }
            if (entity.isCustomNameVisible()) tag.putBoolean("CustomNameVisible", true);
            if (!entity.getTags().isEmpty()) {
                ListTag listtag = new ListTag();
                for (String s : entity.getTags()) listtag.add(StringTag.valueOf(s));
                tag.put("Tags", listtag);
            }
            entity.addAdditionalSaveData(tag);

            // Chain-segment redirect: when the captured type is a registered chain segment,
            // remember the head so disguise load can swap to it under the server config flag.
            // Stored unconditionally so toggling the config at runtime works without re-imprint.
            com.ninni.species.server.disguise.ChainHeadRegistry.headFor(entity.getType())
                    .map(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE::getKey)
                    .ifPresent(headKey -> tag.putString("species:head_id", headKey.toString()));

            if (entity instanceof WitherBoss && player instanceof ServerPlayer serverPlayer) {
                SpeciesCriterion.WICKED_MASK_WITHER.trigger(serverPlayer);
            }
        }

        player.setItemInHand(hand, stack);
        entity.level().playSound(null, player.getX(), player.getY(), player.getZ(), SpeciesSoundEvents.WICKED_MASK_LINK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains("id")) {
            EntityType<?> type = EntityType.byString(tag.getString("id")).orElse(null);
            if (type != null) {
                list.add(type.getDescription().copy().withStyle(Style.EMPTY.withColor(0xffca3d)));
            }
        } else {
            list.add(Component.translatable("item.species.wicked_mask.desc.disguise.1", Component.translatable("key.sneak"), Component.translatable("key.mouse.right")).withStyle(ChatFormatting.GRAY));
            list.add(Component.translatable("item.species.wicked_mask.desc.disguise.2").withStyle(ChatFormatting.GRAY));
        }
        list.add(Component.literal(""));
        list.add(Component.translatable("item.modifiers.head").withStyle(ChatFormatting.GRAY));
        list.add(Component.literal(" ").append(Component.translatable("item.species.wicked_mask.desc.apply").withStyle(Style.EMPTY.withColor(0xe72a8b))));
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SpeciesSoundEvents.WICKED_MASK_EQUIP.get();
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || (stack.hasTag() && stack.getTag().contains("id"));
    }
}
