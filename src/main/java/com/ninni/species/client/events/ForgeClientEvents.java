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

    /** Per-type one-shot diagnostic flag for the disguise render path. Logs once per EntityType per session. */
    private static final java.util.Set<net.minecraft.world.entity.EntityType<?>> DIAG_LOGGED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Behavior-owned poses skipped by {@link #syncWearerStateToDisguise}'s pose resync so
     *  {@code onSyncedDataUpdated} animation hooks don't cancel mid-frame. */
    private static final java.util.Set<net.minecraft.world.entity.Pose> BEHAVIOR_OWNED_POSES;
    static {
        java.util.Set<net.minecraft.world.entity.Pose> set =
                java.util.EnumSet.noneOf(net.minecraft.world.entity.Pose.class);
        for (com.ninni.species.server.entity.util.SpeciesPose sp :
                com.ninni.species.server.entity.util.SpeciesPose.values()) {
            set.add(sp.get());
        }
        set.add(net.minecraft.world.entity.Pose.ROARING);
        BEHAVIOR_OWNED_POSES = java.util.Collections.unmodifiableSet(set);
    }

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

            // Diagnostic: one-shot per disguise type. Confirms the render path is reached and reports
            // the values the rest of the pipeline depends on. Useful for "model invisible" reports.
            if (DIAG_LOGGED.add(disguise.getType())) {
                float diagYOffset = com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry.getWorldYOffset(disguise);
                Species.LOGGER.info("[Species disguise] type={} renderer={} bbox={}x{} pos=({},{},{}) worldYOffset={}",
                        net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(disguise.getType()),
                        baseRenderer != null ? baseRenderer.getClass().getName() : "<null>",
                        disguise.getBbWidth(), disguise.getBbHeight(),
                        disguise.getX(), disguise.getY(), disguise.getZ(),
                        diagYOffset);
            }

            if (baseRenderer != null) {
                PoseStack poseStack = event.getPoseStack();
                MultiBufferSource buffer = event.getMultiBufferSource();
                int light = event.getPackedLight();
                // Armor stand statue render: pin partialTick=0 so the model's time-driven
                // micro-anims (sin/cos of ageInTicks for breathing/bobbing/floating) don't
                // oscillate frame-to-frame on the otherwise-frozen tickCount.
                boolean wearerIsArmorStand = entity instanceof net.minecraft.world.entity.decoration.ArmorStand;
                float partialTick = wearerIsArmorStand ? 0.0F : event.getPartialTick();

                boolean inInventory = Minecraft.getInstance().getEntityRenderDispatcher() instanceof EntityRenderDispatcherAccess access
                        && access.getRenderingInventoryEntity();

                syncWearerStateToDisguise(entity, disguise);

                com.ninni.species.api.disguise.DisguiseBehavior behavior =
                        com.ninni.species.server.disguise.DisguiseBehaviorRegistry.get(disguise);

                // entityYaw passed to the renderer is what MobRenderer.setupRotations uses to
                // rotate the body model — using the wearer's lagging yBodyRot here would
                // re-introduce the lag for snake/worm-like disguises that natively pin
                // yBodyRot = getYRot(); read camera yaw directly when the behavior opts in.
                float bodyYaw = behavior.bodyYawTracksCamera(disguise)
                        ? Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())
                        : Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);

                // Inventory render is GUI context — fullbright. In-world recomputes via the
                // disguise's renderer for per-renderer overrides. Single-assignment so downstream
                // lambdas can capture it.
                final int disguiseLight;
                if (inInventory) {
                    disguiseLight = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
                } else {
                    int computed = light;
                    try {
                        computed = ((EntityRenderer) baseRenderer).getPackedLightCoords(disguise, partialTick);
                    } catch (Throwable ignored) {}
                    disguiseLight = computed;
                }
                com.ninni.species.server.disguise.util.BehaviorHooks.run(
                        "disguise.preRender:" + disguise.getType(),
                        () -> behavior.preRender(entity, disguise, partialTick, inInventory));
                if (inInventory) {
                    com.ninni.species.server.disguise.util.BehaviorHooks.run(
                            "disguise.preInventoryPose:" + disguise.getType(),
                            () -> behavior.preInventoryPose(entity, disguise, partialTick));
                }

                // Push wearer onto DisguiseCosmeticContext for the texture-override mixin.
                com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticContext.push(entity);
                poseStack.pushPose();
                // Per-type world Y offset: lifts/sinks the disguise relative to the wearer's feet.
                // Used for ground-flat creatures (e.g. Triops) whose model sits at Y=0..0.2 and
                // would otherwise render below normal sightlines.
                float worldYOffset = com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry.getWorldYOffset(disguise);
                if (worldYOffset != 0.0F) poseStack.translate(0.0, worldYOffset, 0.0);
                // Per-behavior dynamic Y offset — used for animation-driven hops (e.g. Spawn Seal
                // bounce) whose visual height comes from physical impulse the disguise can't apply.
                float behaviorYOffset = behavior.renderYOffset(entity, disguise, partialTick);
                if (behaviorYOffset != 0.0F) poseStack.translate(0.0, behaviorYOffset, 0.0);
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
                        com.ninni.species.server.disguise.util.BehaviorHooks.run(
                                "disguise.renderOverride:" + disguise.getType(),
                                () -> renderOverride.render(entity, disguise, baseRenderer, bodyYaw, partialTick, poseStack, buffer, disguiseLight));
                    } else {
                        ((EntityRenderer) baseRenderer).render(disguise, bodyYaw, partialTick, poseStack, buffer, disguiseLight);
                    }
                } catch (Throwable t) {
                    com.ninni.species.server.disguise.DisguiseLogging.rateLimited(
                            "disguise.render:" + disguise.getType(), t);
                } finally {
                    poseStack.popPose();
                    com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticContext.pop();
                }

                // Extra render layers drawn after the body (registered via DisguiseTopologyRegistry.registerRenderLayer).
                java.util.List<com.ninni.species.api.disguise.DisguiseRenderLayer> renderLayers =
                        com.ninni.species.server.disguise.panacea.DisguiseTopologyRegistry.getRenderLayers(disguise);
                if (!renderLayers.isEmpty()) {
                    // Layer crashes are visual-only; continue rendering. Rate-limited dedup keeps the
                    // log signal useful for layer authors without spamming the console per frame.
                    for (com.ninni.species.api.disguise.DisguiseRenderLayer layer : renderLayers) {
                        com.ninni.species.server.disguise.util.BehaviorHooks.run(
                                "disguise.renderLayer:" + disguise.getType(),
                                () -> layer.render(entity, disguise, poseStack, buffer, partialTick, disguiseLight, inInventory));
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
                            final int subLightFinal = subLight;
                            com.ninni.species.server.disguise.util.BehaviorHooks.run(
                                    "disguise.subEntityRender:" + sub.getType(),
                                    () -> ((EntityRenderer) subRenderer).render(sub, sub.getYRot(), subPartialTick, subPoseStack, subBuffer, subLightFinal));
                            subPoseStack.popPose();
                        });

                if (inInventory) {
                    com.ninni.species.server.disguise.util.BehaviorHooks.run(
                            "disguise.postInventoryPose:" + disguise.getType(),
                            () -> behavior.postInventoryPose(entity, disguise, partialTick));
                }
                com.ninni.species.server.disguise.util.BehaviorHooks.run(
                        "disguise.postRender:" + disguise.getType(),
                        () -> behavior.postRender(entity, disguise, partialTick, inInventory));
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
        // Armor stand pose drives head/pitch — the wearer's xRot would point wherever the player
        // happened to be looking when nudging the stand.
        boolean isArmorStand = wearer instanceof net.minecraft.world.entity.decoration.ArmorStand;
        float xRotForDisguise;
        float xRotOForDisguise;
        if (isArmorStand) {
            net.minecraft.core.Rotations head = ((net.minecraft.world.entity.decoration.ArmorStand) wearer).getHeadPose();
            xRotForDisguise = head.getX();
            xRotOForDisguise = head.getX();
        } else {
            boolean shouldApplyXRot = behavior.shouldApplyXRot(wearer, disguise, inInventory);
            xRotForDisguise = shouldApplyXRot ? wearer.getXRot() : 0.0F;
            xRotOForDisguise = shouldApplyXRot ? wearer.xRotO : 0.0F;
        }
        // Snake/worm-like disguises pin yBodyRot = getYRot() in their own tick; sourcing the
        // body yaw from the wearer's lagging yBodyRot would re-introduce the lag every render
        // frame and freeze the head while chain segments swing past it.
        boolean bodyTracksCamera = behavior.bodyYawTracksCamera(disguise);
        float yBodyRotForDisguise = (bodyTracksCamera ? wearer.getYRot() : wearer.yBodyRot) + yawOffset;
        float yBodyRotOForDisguise = (bodyTracksCamera ? wearer.yRotO : wearer.yBodyRotO) + yawOffset;

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

        disguise.setXRot(xRotForDisguise);
        disguise.xRotO = xRotOForDisguise;
        // Armor stand: the HeadPose Y delta drives the whole-body facing direction so segment
        // chains follow head orientation; non-chain mobs end up with head/body aligned (head-bone
        // delta = 0). Other wearers keep the standard split between body and head yaw.
        if (isArmorStand) {
            float headYawDelta = ((net.minecraft.world.entity.decoration.ArmorStand) wearer).getHeadPose().getY();
            float poseYaw = wearer.getYRot() + headYawDelta + yawOffset;
            float poseYawO = wearer.yRotO + headYawDelta + yawOffset;
            disguise.setYRot(poseYaw);
            disguise.yRotO = poseYawO;
            disguise.yBodyRot = poseYaw;
            disguise.yBodyRotO = poseYawO;
            disguise.yHeadRot = poseYaw;
            disguise.yHeadRotO = poseYawO;
        } else {
            disguise.setYRot(wearer.getYRot() + yawOffset);
            disguise.yRotO = wearer.yRotO + yawOffset;
            disguise.yBodyRot = yBodyRotForDisguise;
            disguise.yBodyRotO = yBodyRotOForDisguise;
            disguise.yHeadRot = wearer.yHeadRot + yawOffset;
            disguise.yHeadRotO = wearer.yHeadRotO + yawOffset;
        }

        // Armor stand statue render: freeze tickCount so setupAnim's ageInTicks is constant —
        // otherwise time-driven micro-animations would tick on the motionless model.
        if (!isArmorStand) {
            disguise.tickCount = wearer.tickCount;
        }
        disguise.hurtTime = wearer.hurtTime;
        disguise.hurtDuration = wearer.hurtDuration;
        disguise.deathTime = wearer.deathTime;
        disguise.attackAnim = wearer.attackAnim;
        disguise.oAttackAnim = wearer.oAttackAnim;
        disguise.swinging = wearer.swinging;
        disguise.swingTime = wearer.swingTime;
        disguise.swingingArm = wearer.swingingArm;
        // Only sync the pose when the disguise isn't currently in a behavior-managed pose
        // (e.g. SpeciesPose.ATTACK / LAYING_DOWN set by onSpecialAction). Without this gate the
        // resync runs every render frame, flickering the custom pose back to STANDING and
        // killing the animation that started in onSyncedDataUpdated.
        if (!BEHAVIOR_OWNED_POSES.contains(disguise.getPose())) {
            disguise.setPose(wearer.getPose());
        }

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
