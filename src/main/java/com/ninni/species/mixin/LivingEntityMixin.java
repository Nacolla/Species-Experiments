package com.ninni.species.mixin;

import com.ninni.species.server.entity.util.CustomDeathParticles;
import com.ninni.species.mixin_util.LivingEntityAccess;
import com.ninni.species.server.packet.SnatchedPacket;
import com.ninni.species.server.packet.TankedPacket;
import com.ninni.species.registry.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Optional;


@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements LivingEntityAccess {
    @Shadow public abstract boolean hasEffect(MobEffect effect);
    @Shadow @Nullable public abstract MobEffectInstance getEffect(MobEffect p_21125_);
    @Shadow public abstract RandomSource getRandom();
    @Shadow public abstract ItemStack getItemBySlot(EquipmentSlot p_21127_);
    @OnlyIn(Dist.CLIENT)
    private @Unique boolean snatched;
    @OnlyIn(Dist.CLIENT)
    private @Unique boolean tanked;
    private @Unique EntityType disguisedEntityType;
    private @Unique LivingEntity disguisedEntity;
    private @Unique String lastDisguiseId;

    // Snapshot pre-baseTick rotation history. baseTick ends with yBodyRotO = yBodyRot etc.,
    // destroying the delta that Citadel-style chain buffers (IaF dragon tail/neck) require.
    // Snapshot before baseTick, restore after, so the wearer's delta survives into
    // dragon tick logic that runs after super.tick() returns.
    private @Unique float species$snapshotYBodyRotO;
    private @Unique float species$snapshotYHeadRotO;
    private @Unique float species$snapshotYRotO;
    private @Unique float species$snapshotXRotO;
    private @Unique boolean species$shouldRestoreRotationDelta;

    public LivingEntityMixin(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }


    @Inject(method = "baseTick", at = @At("HEAD"))
    private void species$snapshotRotationDelta(CallbackInfo ci) {
        // Clear unconditionally so a flag left set by a prior baseTick that threw
        // (RETURN inject skipped) cannot make the next RETURN restore stale values.
        species$shouldRestoreRotationDelta = false;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.getTags().contains(com.ninni.species.client.events.WickedMaskDisguiseEvents.DISGUISE_TAG)) return;
        try {
            com.ninni.species.api.disguise.DisguiseBehavior behavior =
                    com.ninni.species.server.disguise.DisguiseBehaviorRegistry.get(self);
            if (!behavior.preserveRotationDeltaInAiStep()) return;
        } catch (Throwable ignored) {
            return;
        }
        species$shouldRestoreRotationDelta = true;
        species$snapshotYBodyRotO = self.yBodyRotO;
        species$snapshotYHeadRotO = self.yHeadRotO;
        species$snapshotYRotO = self.yRotO;
        species$snapshotXRotO = self.xRotO;
    }

    @Inject(method = "baseTick", at = @At("RETURN"))
    private void species$restoreRotationDelta(CallbackInfo ci) {
        if (!species$shouldRestoreRotationDelta) return;
        species$shouldRestoreRotationDelta = false;
        LivingEntity self = (LivingEntity) (Object) this;
        self.yBodyRotO = species$snapshotYBodyRotO;
        self.yHeadRotO = species$snapshotYHeadRotO;
        self.yRotO = species$snapshotYRotO;
        self.xRotO = species$snapshotXRotO;
    }

    @Inject(method = "tickEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;updateGlowingStatus()V", ordinal = 0))
    private void onStatusEffectChange(CallbackInfo ci) {
        SpeciesNetwork.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this), new SnatchedPacket(this.getId(), this.hasEffect(SpeciesStatusEffects.SNATCHED.get())));
        SpeciesNetwork.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this), new TankedPacket(this.getId(), this.hasEffect(SpeciesStatusEffects.TANKED.get())));
    }

    @Inject(method = "tickEffects", at = @At("HEAD"))
    public void applySpeciesEffects(CallbackInfo ci) {
        Level level = this.level();

        if (this.hasEffect(SpeciesStatusEffects.GUT_FEELING.get()) ) {
            if (this.getEffect(SpeciesStatusEffects.GUT_FEELING.get()).getDuration() < 20 * 60 * 5) {
                if (this.getRandom().nextInt(200) == 0) this.playSound(SpeciesSoundEvents.GUT_FEELING_ROAR.get(), 0.2f, 0);
            }
            else {
                if (this.getRandom().nextInt(800) == 0) this.playSound(SpeciesSoundEvents.GUT_FEELING_ROAR.get(), 0.2f, 0);
            }
        }

        if (this.hasEffect(SpeciesStatusEffects.BIRTD.get()) && level instanceof ServerLevel world) {
            if (this.tickCount % 10 == 1) {
                world.sendParticles(SpeciesParticles.BIRTD.get(), this.getX(), this.getEyeY() + 0.5F, this.getZ() - 0.5, 1,0, 0, 0, 0);
            }
        }

    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        // Fast-path: skip head-slot lookup and tag parsing for non-wearers.
        // The lastDisguiseId check ensures entry to the slow path on the first tick
        // after a mask is equipped (head slot non-empty, lastDisguiseId still null).
        ItemStack headItem = this.getItemBySlot(EquipmentSlot.HEAD);
        if (headItem.isEmpty() && this.lastDisguiseId == null
                && this.getDisguisedEntity() == null
                && this.getDisguisedEntityType() == null) {
            return;
        }
        CompoundTag tag = headItem.getTag();
        String currentId = (headItem.is(SpeciesItems.WICKED_MASK.get()) && tag != null && tag.contains("id"))
                ? tag.getString("id") : null;

        if (currentId == null) {
            if (this.getDisguisedEntity() != null || this.getDisguisedEntityType() != null) {
                LivingEntity oldDisguise = this.getDisguisedEntity();
                if (oldDisguise != null) {
                    try {
                        com.ninni.species.server.disguise.DisguiseBehaviorRegistry.get(oldDisguise)
                                .onDestroyed((LivingEntity) (Object) this, oldDisguise);
                    } catch (Throwable t) {
                        com.ninni.species.server.disguise.DisguiseLogging.rateLimited(
                                "behavior.onDestroyed", t);
                    }
                    species$removeWearerEffects((LivingEntity) (Object) this, oldDisguise);
                }
                com.ninni.species.server.disguise.GumWormSegmentManager.removeSegments((LivingEntity) (Object) this);
                com.ninni.species.server.disguise.BoundroidPairManager.removeCompanion((LivingEntity) (Object) this);
                com.ninni.species.server.disguise.MineGuardianAnchorManager.removeAnchor((LivingEntity) (Object) this);
                com.ninni.species.server.disguise.DisguiseBodyRegistry.unregister(oldDisguise);
                this.setDisguisedEntity(null);
                this.setDisguisedEntityType(null);
                this.lastDisguiseId = null;
            }
            return;
        }

        if (!currentId.equals(this.lastDisguiseId) || this.getDisguisedEntity() == null) {
            LivingEntity oldDisguise = this.getDisguisedEntity();
            if (oldDisguise != null) {
                try {
                    com.ninni.species.server.disguise.DisguiseBehaviorRegistry.get(oldDisguise)
                            .onDestroyed((LivingEntity) (Object) this, oldDisguise);
                } catch (Throwable t) {
                    com.ninni.species.server.disguise.DisguiseLogging.rateLimited(
                            "behavior.onDestroyed", t);
                }
                species$removeWearerEffects((LivingEntity) (Object) this, oldDisguise);
            }
            com.ninni.species.server.disguise.GumWormSegmentManager.removeSegments((LivingEntity) (Object) this);
            com.ninni.species.server.disguise.BoundroidPairManager.removeCompanion((LivingEntity) (Object) this);
            com.ninni.species.server.disguise.MineGuardianAnchorManager.removeAnchor((LivingEntity) (Object) this);
            com.ninni.species.server.disguise.DisguiseBodyRegistry.unregister(oldDisguise);
            Optional<EntityType<?>> entityType = EntityType.byString(currentId);
            if (entityType.isPresent()) {
                Entity rawEntity = entityType.get().create(this.level());
                if (rawEntity instanceof LivingEntity living) {
                    // Entity.load already dispatches readAdditionalSaveData; mob NBT is read once.
                    living.load(tag);
                    if (living instanceof Mob mob) {
                        // Clear AI: Mob.aiStep ticks goals unconditionally; AC attack goals mutate
                        // animation state without re-checking canUse. Behaviors that write state directly
                        // (e.g. AutoFlightSync) are unaffected; re-add via onCreated() if needed.
                        try {
                            mob.goalSelector.removeAllGoals(g -> true);
                            mob.targetSelector.removeAllGoals(g -> true);
                        } catch (Throwable ignored) {}
                    }
                    living.addTag(com.ninni.species.client.events.WickedMaskDisguiseEvents.DISGUISE_TAG);
                    living.setSilent(!com.ninni.species.registry.SpeciesConfig.PLAY_DISGUISE_SOUNDS.get());
                    // Must never die: death triggers dropAllDeathLoot (real ItemEntities) and
                    // freezes the tick (no more sound/pose updates).
                    living.setInvulnerable(true);
                    try {
                        com.ninni.species.server.disguise.DisguiseBehaviorRegistry.get(living)
                                .onCreated((LivingEntity) (Object) this, living);
                    } catch (Throwable ignored) {}
                    // Apply cosmetic wearer-effects on the equip transition.
                    if (!this.level().isClientSide) {
                        try {
                            LivingEntity selfWearer = (LivingEntity) (Object) this;
                            com.ninni.species.api.disguise.DisguiseCosmetics cosmetics =
                                    com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticRegistry.get(living);
                            for (net.minecraft.world.effect.MobEffectInstance e : cosmetics.wearerEffectsWhileWorn(selfWearer, living)) {
                                selfWearer.addEffect(new net.minecraft.world.effect.MobEffectInstance(e));
                            }
                        } catch (Throwable ignored) {}
                    }
                    this.setDisguisedEntity(living);
                    this.setDisguisedEntityType(entityType.get());
                    this.lastDisguiseId = currentId;
                    // Register so id-tracking particle systems resolve via ClientLevelMixin fallback.
                    com.ninni.species.server.disguise.DisguiseBodyRegistry.register(living, (LivingEntity) (Object) this);
                    // Companion managers — each gates internally on type and side.
                    com.ninni.species.server.disguise.GumWormSegmentManager
                            .onDisguiseCreated((LivingEntity) (Object) this, living);
                    com.ninni.species.server.disguise.BoundroidPairManager
                            .onDisguiseCreated((LivingEntity) (Object) this, living);
                    com.ninni.species.server.disguise.MineGuardianAnchorManager
                            .onDisguiseCreated((LivingEntity) (Object) this, living);
                } else {
                    this.setDisguisedEntity(null);
                    this.setDisguisedEntityType(null);
                    this.lastDisguiseId = currentId;
                }
            } else {
                this.setDisguisedEntity(null);
                this.setDisguisedEntityType(null);
                this.lastDisguiseId = currentId;
            }
        }

        tickDisguise();
    }

    @Unique
    private void tickDisguise() {
        // Ticks both sides: server tick broadcasts mob ambient/hurt sounds. Death-side leaks
        // are blocked by setInvulnerable + per-tick health reset; world mutations by removeAllGoals.
        LivingEntity disguise = this.getDisguisedEntity();
        if (disguise == null || disguise.isRemoved()) return;

        ItemStack disguiseHead = disguise.getItemBySlot(EquipmentSlot.HEAD);
        if (disguiseHead.is(SpeciesItems.WICKED_MASK.get())) return;

        LivingEntity self = (LivingEntity) (Object) this;

        // Snapshot wearer pos before disguise.tick — aiStep's pushEntities accumulates impulse
        // on the real wearer; restore after to isolate it.
        double wearerX = self.getX();
        double wearerY = self.getY();
        double wearerZ = self.getZ();
        Vec3 wearerDelta = self.getDeltaMovement();

        com.ninni.species.api.disguise.DisguiseBehavior behavior = com.ninni.species.server.disguise.DisguiseBehaviorRegistry.get(disguise);
        float yawOffset = behavior.yawOffset(disguise);
        boolean shouldApplyXRot = behavior.shouldApplyXRot(self, disguise, false);
        float xRotForDisguise = shouldApplyXRot ? self.getXRot() : 0.0F;
        float xRotOForDisguise = shouldApplyXRot ? self.xRotO : 0.0F;

        disguise.setPos(self.getX(), self.getY(), self.getZ());
        disguise.xo = self.xo;
        disguise.yo = self.yo;
        disguise.zo = self.zo;
        disguise.setDeltaMovement(self.getDeltaMovement());
        disguise.setYRot(self.getYRot() + yawOffset);
        disguise.yRotO = self.yRotO + yawOffset;
        disguise.setXRot(xRotForDisguise);
        disguise.xRotO = xRotOForDisguise;
        disguise.yBodyRot = self.yBodyRot + yawOffset;
        disguise.yBodyRotO = self.yBodyRotO + yawOffset;
        disguise.yHeadRot = self.yHeadRot + yawOffset;
        disguise.yHeadRotO = self.yHeadRotO + yawOffset;
        disguise.setOnGround(self.onGround());

        // Propagate movement-state flags so renderers (vanilla, GeckoLib, Citadel, LionFish)
        // pick up the correct animation track via setupAnim without per-renderer integration.
        disguise.setSharedFlag(7, self.isFallFlying()); // FALL_FLYING
        disguise.setSwimming(self.isSwimming());
        disguise.setSprinting(self.isSprinting());

        // Defend against state changes that setInvulnerable doesn't cover: direct setHealth
        // calls (Slime split, drowning), accumulated fallDistance, lingering hurt animation.
        disguise.fallDistance = 0;
        disguise.hurtTime = 0;
        disguise.deathTime = 0;
        if (disguise.getHealth() < disguise.getMaxHealth()) {
            disguise.setHealth(disguise.getMaxHealth());
        }

        try {
            behavior.preTick(self, disguise);
        } catch (Throwable t) {
            com.ninni.species.server.disguise.DisguiseLogging.rateLimited(
                    "behavior.preTick:" + behavior.getClass().getName(), t);
        }

        // Suppress particles emitted during disguise.tick() for the local first-person
        // wearer — disguise mouth is near the camera for tall mobs. See DisguiseTickContext.
        // Third-person and remote wearers are unaffected; context is per-thread.
        boolean suppressDisguiseParticles = self.level().isClientSide
                && com.ninni.species.client.events.LocalFirstPersonCheck.isLocalFirstPersonWearer(self);
        if (suppressDisguiseParticles) {
            com.ninni.species.server.disguise.DisguiseTickContext.pushSuppress();
        }
        try {
            disguise.tick();
        } catch (Throwable t) {
            com.ninni.species.server.disguise.DisguiseLogging.rateLimited(
                    "disguise.tick:" + disguise.getType().toString(), t);
        } finally {
            if (suppressDisguiseParticles) {
                com.ninni.species.server.disguise.DisguiseTickContext.popSuppress();
            }
        }

        try {
            behavior.postTick(self, disguise);
        } catch (Throwable t) {
            com.ninni.species.server.disguise.DisguiseLogging.rateLimited(
                    "behavior.postTick:" + behavior.getClass().getName(), t);
        }

        // Restore wearer state — neutralize push/teleport from disguise's aiStep.
        if (self.getX() != wearerX || self.getY() != wearerY || self.getZ() != wearerZ) {
            self.setPos(wearerX, wearerY, wearerZ);
        }
        self.setDeltaMovement(wearerDelta);

        // Multipart realign: setPos snaps the body but parts stay at AI-moved positions; translate
        // each part by the delta back onto the wearer.
        double dx = self.getX() - disguise.getX();
        double dy = self.getY() - disguise.getY();
        double dz = self.getZ() - disguise.getZ();
        if (dx != 0 || dy != 0 || dz != 0) {
            if (disguise.isMultipartEntity()) {
                net.minecraftforge.entity.PartEntity<?>[] parts = disguise.getParts();
                if (parts != null) {
                    for (net.minecraftforge.entity.PartEntity<?> part : parts) {
                        part.setPos(part.getX() + dx, part.getY() + dy, part.getZ() + dz);
                    }
                }
            }
            disguise.setPos(self.getX(), self.getY(), self.getZ());
        }

        // Re-sync rotation history: disguise.tick zeros yBodyRotO=yBodyRot etc.; restore from wearer.
        disguise.setDeltaMovement(self.getDeltaMovement());
        disguise.yRotO = self.yRotO + yawOffset;
        disguise.setYRot(self.getYRot() + yawOffset);
        disguise.xRotO = xRotOForDisguise;
        disguise.setXRot(xRotForDisguise);
        disguise.yBodyRotO = self.yBodyRotO + yawOffset;
        disguise.yBodyRot = self.yBodyRot + yawOffset;
        disguise.yHeadRotO = self.yHeadRotO + yawOffset;
        disguise.yHeadRot = self.yHeadRot + yawOffset;

        // Companion managers — each gates internally on type and side.
        com.ninni.species.server.disguise.GumWormSegmentManager.tickSegments(self);
        com.ninni.species.server.disguise.BoundroidPairManager.tickCompanion(self);
        com.ninni.species.server.disguise.MineGuardianAnchorManager.tickAnchor(self);

        // Cosmetic particle hook (client-side only).
        if (self.level().isClientSide) {
            try {
                com.ninni.species.api.disguise.DisguiseCosmetics cosmetics =
                        com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticRegistry.get(disguise);
                cosmetics.emitParticles(self, disguise, self.level(), self.getRandom());
            } catch (Throwable ignored) {}
        }

        // Ambient sound: ~once every ~5 seconds (matches Mob.playAmbientSound cadence).
        if (!self.level().isClientSide && self.tickCount > 0 && self.getRandom().nextInt(100) == 0) {
            try {
                com.ninni.species.api.disguise.DisguiseCosmetics cosmetics =
                        com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticRegistry.get(disguise);
                net.minecraft.sounds.SoundEvent ambient = cosmetics.overrideAmbientSound(self, disguise);
                if (ambient != null) self.playSound(ambient, 1.0F, 1.0F);
            } catch (Throwable ignored) {}
        }

    }

    /** Removes the cosmetic's wearer-effects. Called on un-equip / swap. */
    @Unique
    private void species$removeWearerEffects(LivingEntity self, LivingEntity disguise) {
        try {
            com.ninni.species.api.disguise.DisguiseCosmetics cosmetics =
                    com.ninni.species.server.disguise.cosmetic.DisguiseCosmeticRegistry.get(disguise);
            for (net.minecraft.world.effect.MobEffectInstance e : cosmetics.wearerEffectsWhileWorn(self, disguise)) {
                self.removeEffect(e.getEffect());
            }
        } catch (Throwable ignored) {}
    }

    // Hurt/death sound cosmetic hooks live in PlayerMixin: Player overrides those without super,
    // so an inject on LivingEntity never fires for player wearers.

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    public void applyBirtd(CallbackInfo ci) {
        if (this.hasEffect(SpeciesStatusEffects.BIRTD.get()) || this.hasEffect(SpeciesStatusEffects.STUCK.get())) ci.cancel();
    }

    @Inject(method = "makePoofParticles", at = @At("HEAD"), cancellable = true)
    public void S$makePoofParticles(CallbackInfo ci) {
        if (this instanceof CustomDeathParticles customDeathParticles) {
            ci.cancel();
            customDeathParticles.makeDeathParticles();
        }
    }


    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    public void S$isPushable(CallbackInfoReturnable<Boolean> cir) {
        if (this.hasEffect(SpeciesStatusEffects.BIRTD.get()) || this.hasEffect(SpeciesStatusEffects.STUCK.get())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
    private void onGetVisibilityPercent(@Nullable Entity observer, CallbackInfoReturnable<Double> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        double visibility = cir.getReturnValue();

        if (observer == null) return;

        ItemStack headItem = self.getItemBySlot(EquipmentSlot.HEAD);
        EntityType<?> observerType = observer.getType();

        if (observerType == SpeciesEntities.GHOUL.get() && headItem.is(SpeciesItems.GHOUL_HEAD.get()) ||
                observerType == SpeciesEntities.WICKED.get() && headItem.is(SpeciesItems.WICKED_CANDLE.get()) ||
                observerType == SpeciesEntities.BEWEREAGER.get() && headItem.is(SpeciesItems.BEWEREAGER_HEAD.get()) ||
                observerType == SpeciesEntities.QUAKE.get() && headItem.is(SpeciesItems.QUAKE_HEAD.get())) {
            visibility *= 0.5D;
            cir.setReturnValue(visibility);
        }
    }

    @Override
    public @Unique boolean hasSnatched() {
        return this.snatched;
    }
    @Override
    public @Unique void setSnatched(boolean snatched) {
        this.snatched = snatched;
    }

    @Override
    public @Unique boolean hasTanked() {
        return this.tanked;
    }
    @Override
    public @Unique void setTanked(boolean tanked) {
        this.tanked = tanked;
    }

    @Override
    public @Unique EntityType getDisguisedEntityType() {
        return disguisedEntityType;
    }
    @Override
    public @Unique void setDisguisedEntityType(EntityType disguisedEntityType) {
        this.disguisedEntityType = disguisedEntityType;
    }

    @Override
    public @Unique LivingEntity getDisguisedEntity() {
        return disguisedEntity;
    }
    @Override
    public @Unique void setDisguisedEntity(LivingEntity disguisedEntity) {
        this.disguisedEntity = disguisedEntity;
    }
}
