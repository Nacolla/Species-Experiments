package com.ninni.species.client.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ninni.species.Species;
import com.ninni.species.mixin_util.EntityRenderDispatcherAccess;
import com.ninni.species.mixin_util.LivingEntityAccess;
import com.ninni.species.registry.SpeciesItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Species.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeClientEvents {

    @SubscribeEvent
    public static void onRenderLivingSpecialPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();

        if (Minecraft.getInstance().getEntityRenderDispatcher() instanceof EntityRenderDispatcherAccess access && access.getRenderingInventoryEntity()) {
            if (entity.getItemBySlot(EquipmentSlot.HEAD).is(SpeciesItems.WICKED_MASK.get())) {
                LivingEntity disguise = ((LivingEntityAccess) entity).getDisguisedEntity();
                if (disguise != null) {
                    // Y offset applied before scale (pre-scale world units) so the model
                    // centres inside the inventory frame. Both are no-ops at default values.
                    float yOffset = com.ninni.species.server.disguise.panacea.DisguiseTopology.getInventoryYOffset(disguise);
                    if (yOffset != 0.0F) {
                        event.getPoseStack().translate(0.0F, yOffset, 0.0F);
                    }
                    float scale = com.ninni.species.server.disguise.panacea.DisguiseTopology.getInventoryScale(disguise);
                    if (scale != 1.0F) {
                        event.getPoseStack().scale(scale, scale, scale);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void livingEntityRenderer(RenderLivingEvent<LivingEntity, EntityModel<LivingEntity>> event) {
        LivingEntity entity = event.getEntity();
        ItemStack headItem = entity.getItemBySlot(EquipmentSlot.HEAD);
        LivingEntity disguise = ((LivingEntityAccess) entity).getDisguisedEntity();

        boolean shouldDisguise = headItem.is(SpeciesItems.WICKED_MASK.get())
                && headItem.hasTag()
                && headItem.getTag().contains("id")
                && disguise != null
                && !disguise.isRemoved()
                && disguise.getType() != null
                && !entity.hasEffect(MobEffects.INVISIBILITY)
                && !(entity instanceof Player player && player.isSpectator());

        if (shouldDisguise) {
            event.setCanceled(true);

            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            EntityRenderer<?> baseRenderer = dispatcher.getRenderer(disguise);

            if (baseRenderer != null) {
                PoseStack poseStack = event.getPoseStack();
                MultiBufferSource buffer = event.getMultiBufferSource();
                int light = event.getPackedLight();
                float partialTick = event.getPartialTick();

                boolean inInventory = Minecraft.getInstance().getEntityRenderDispatcher() instanceof EntityRenderDispatcherAccess access
                        && access.getRenderingInventoryEntity();

                syncWearerStateToDisguise(entity, disguise);

                float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);

                // Recompute light via the disguise's renderer so per-renderer overrides apply.
                int disguiseLight = light;
                try {
                    disguiseLight = ((EntityRenderer) baseRenderer).getPackedLightCoords(disguise, partialTick);
                } catch (Throwable ignored) {}

                com.ninni.species.api.disguise.DisguiseBehavior behavior =
                        com.ninni.species.server.disguise.DisguiseBehaviorRegistry.get(disguise);
                try {
                    behavior.preRender(entity, disguise, partialTick, inInventory);
                } catch (Throwable ignored) {}
                if (inInventory) {
                    try {
                        behavior.preInventoryPose(entity, disguise, partialTick);
                    } catch (Throwable ignored) {}
                }

                // Push wearer onto DisguiseCosmeticContext for the texture-override mixin.
                com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticContext.push(entity);
                poseStack.pushPose();
                // bbHeight compensation: only ceiling-anchored types in hanging state.
                if (isHeadAnchoredDisguise(disguise)
                        && com.ninni.species.server.disguise.dsl.WearerPredicates.CEILING_HANGING.test(entity)) {
                    float bbDelta = entity.getBbHeight() - disguise.getBbHeight();
                    if (bbDelta > 0.0F) poseStack.translate(0.0, bbDelta, 0.0);
                }
                try {
                    com.ninni.species.api.disguise.DisguiseCosmetics cosmetics =
                            com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticRegistry.get(disguise);
                    float worldScale = cosmetics.overrideWorldScale(entity, disguise);
                    if (!Float.isNaN(worldScale) && worldScale > 0.0F && worldScale != 1.0F) {
                        poseStack.scale(worldScale, worldScale, worldScale);
                    }
                    net.minecraft.network.chat.Component nameOverride = cosmetics.overrideNameTag(entity, disguise);
                    if (nameOverride != null) {
                        disguise.setCustomName(nameOverride);
                        disguise.setCustomNameVisible(true);
                    }
                    com.ninni.species.api.disguise.DisguiseRenderer renderOverride =
                            cosmetics.overrideRenderer(entity, disguise);
                    if (renderOverride != null) {
                        try {
                            renderOverride.render(entity, disguise, baseRenderer, bodyYaw, partialTick, poseStack, buffer, disguiseLight);
                        } catch (Throwable ignored) {}
                    } else {
                        ((EntityRenderer) baseRenderer).render(disguise, bodyYaw, partialTick, poseStack, buffer, disguiseLight);
                    }
                } catch (Throwable ignored) {
                } finally {
                    poseStack.popPose();
                    com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticContext.pop();
                }

                // Extra render layers drawn after the body (registered via DisguiseTopologyRegistry.registerRenderLayer).
                java.util.List<com.ninni.species.api.disguise.DisguiseRenderLayer> renderLayers =
                        com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry.getRenderLayers(disguise);
                if (!renderLayers.isEmpty()) {
                    for (com.ninni.species.api.disguise.DisguiseRenderLayer layer : renderLayers) {
                        try {
                            layer.render(entity, disguise, poseStack, buffer, partialTick, disguiseLight, inInventory);
                        } catch (Throwable ignored) {
                            // Layer crashes are visual-only; continue rendering.
                        }
                    }
                }

                // Render sub-entities (Gum Worm segments, Boundroid companion). Not in ClientLevel,
                // so the dispatcher skips them naturally; render manually with pose-translate.
                // Forge multipart parts are excluded — drawn by the parent renderer.
                final EntityRenderDispatcher subDispatcher = dispatcher;
                final PoseStack subPoseStack = poseStack;
                final MultiBufferSource subBuffer = buffer;
                final int subBaseLight = light;
                final float subPartialTick = partialTick;
                com.ninni.species.server.disguise.panacea.DisguiseTopology.forEachRenderableSubEntity(
                        entity, disguise,
                        (sub, dx, dy, dz) -> {
                            EntityRenderer<?> subRenderer = subDispatcher.getRenderer(sub);
                            if (subRenderer == null) return;
                            int subLight = subBaseLight;
                            try {
                                subLight = ((EntityRenderer) subRenderer).getPackedLightCoords(sub, subPartialTick);
                            } catch (Throwable ignored) {}
                            subPoseStack.pushPose();
                            subPoseStack.translate(dx, dy, dz);
                            try {
                                ((EntityRenderer) subRenderer).render(sub, sub.getYRot(), subPartialTick, subPoseStack, subBuffer, subLight);
                            } catch (Throwable ignored) {}
                            subPoseStack.popPose();
                        });

                if (inInventory) {
                    try {
                        behavior.postInventoryPose(entity, disguise, partialTick);
                    } catch (Throwable ignored) {}
                }
                try {
                    behavior.postRender(entity, disguise, partialTick, inInventory);
                } catch (Throwable ignored) {}
            }
        }

        if (((LivingEntityAccess) entity).hasTanked()) event.getPoseStack().scale(1.35F, 1.125F, 1.35F);
        if (((LivingEntityAccess) entity).hasSnatched()) event.getPoseStack().scale(0.85F, 1.125F, 0.85F);
    }

    /** True for ceiling-anchored types (Winch, Vesper) needing bbHeight render compensation. */
    private static boolean isHeadAnchoredDisguise(LivingEntity disguise) {
        if (disguise == null) return false;
        net.minecraft.resources.ResourceLocation key =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(disguise.getType());
        if (key == null) return false;
        if (!"alexscaves".equals(key.getNamespace())) return false;
        String path = key.getPath();
        return "boundroid_winch".equals(path) || "vesper".equals(path);
    }

    private static void syncWearerStateToDisguise(LivingEntity wearer, LivingEntity disguise) {
        com.ninni.species.api.disguise.DisguiseBehavior behavior =
                com.ninni.species.server.disguise.DisguiseBehaviorRegistry.get(disguise);

        boolean inInventory = Minecraft.getInstance().getEntityRenderDispatcher() instanceof EntityRenderDispatcherAccess access
                && access.getRenderingInventoryEntity();

        float yawOffset = behavior.yawOffset(disguise);
        boolean shouldApplyXRot = behavior.shouldApplyXRot(wearer, disguise, inInventory);
        float xRotForDisguise = shouldApplyXRot ? wearer.getXRot() : 0.0F;
        float xRotOForDisguise = shouldApplyXRot ? wearer.xRotO : 0.0F;

        disguise.setPos(wearer.getX(), wearer.getY(), wearer.getZ());
        disguise.xo = wearer.xo;
        disguise.yo = wearer.yo;
        disguise.zo = wearer.zo;
        // Some renderers read xOld/yOld/zOld for world-space effects
        // (e.g. TeletorRenderer:73-75 translates antler trails via -xOld/-zOld).
        // Without this sync those effects render at entity-construction origin (0,0,0).
        disguise.xOld = wearer.xOld;
        disguise.yOld = wearer.yOld;
        disguise.zOld = wearer.zOld;
        disguise.setDeltaMovement(wearer.getDeltaMovement());

        disguise.setYRot(wearer.getYRot() + yawOffset);
        disguise.yRotO = wearer.yRotO + yawOffset;
        disguise.setXRot(xRotForDisguise);
        disguise.xRotO = xRotOForDisguise;
        disguise.yBodyRot = wearer.yBodyRot + yawOffset;
        disguise.yBodyRotO = wearer.yBodyRotO + yawOffset;
        disguise.yHeadRot = wearer.yHeadRot + yawOffset;
        disguise.yHeadRotO = wearer.yHeadRotO + yawOffset;

        disguise.tickCount = wearer.tickCount;
        disguise.hurtTime = wearer.hurtTime;
        disguise.hurtDuration = wearer.hurtDuration;
        disguise.deathTime = wearer.deathTime;
        disguise.attackAnim = wearer.attackAnim;
        disguise.oAttackAnim = wearer.oAttackAnim;
        disguise.swinging = wearer.swinging;
        disguise.swingTime = wearer.swingTime;
        disguise.swingingArm = wearer.swingingArm;
        disguise.setPose(wearer.getPose());

        WalkAnimationState src = wearer.walkAnimation;
        WalkAnimationState dst = disguise.walkAnimation;
        dst.speed = src.speed;
        dst.speedOld = src.speedOld;
        dst.position = src.position;

        if (disguise instanceof Mob mobDisguise) {
            if (wearer instanceof Mob mobWearer) {
                mobDisguise.setLeftHanded(mobWearer.isLeftHanded());
            } else if (wearer instanceof Player playerWearer) {
                mobDisguise.setLeftHanded(playerWearer.getMainArm() == HumanoidArm.LEFT);
            }
        }

        if (disguise instanceof EnderMan enderMan) {
            enderMan.setCarriedBlock(pickCarriedBlock(wearer));
        }
    }


    private static BlockState pickCarriedBlock(LivingEntity wearer) {
        ItemStack mainHand = wearer.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof BlockItem blockItem) {
            BlockState state = blockItem.getBlock().defaultBlockState();
            if (!state.is(Blocks.AIR)) return state;
        }
        return null;
    }
}
