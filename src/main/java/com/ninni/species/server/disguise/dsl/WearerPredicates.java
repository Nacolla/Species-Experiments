package com.ninni.species.server.disguise.dsl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/** Stateless wearer-state {@link Predicate}s shared across behaviors. */
public final class WearerPredicates {

    private WearerPredicates() {}

    /** Elytra glide or creative-fly. Matches the Subterranodon/Vesper flying definition. */
    public static final Predicate<LivingEntity> FLYING = wearer ->
            wearer.isFallFlying()
                    || (wearer instanceof Player p && p.getAbilities().flying);

    /** Elytra glide, creative-fly, or swimming. For mobs whose flight pose covers swimming (e.g. IaF dragons). */
    public static final Predicate<LivingEntity> FLYING_OR_SWIMMING = wearer ->
            wearer.isFallFlying() || wearer.isSwimming()
                    || (wearer instanceof Player p && p.getAbilities().flying);

    /** Creative-flying and essentially stationary (horizontal speed² &lt; 0.005). For "wings extended, not flapping" poses. */
    public static final Predicate<LivingEntity> HOVERING = wearer ->
            (wearer instanceof Player p && p.getAbilities().flying)
                    && wearer.getDeltaMovement().horizontalDistanceSqr() < 0.005;

    /** Creative-flying, not elytra-gliding, essentially stationary; counterpart to {@link #CEILING_HANGING} without a ceiling probe. */
    public static final Predicate<LivingEntity> PERCHED = wearer ->
            (wearer instanceof Player p && p.getAbilities().flying)
                    && !wearer.isFallFlying()
                    && wearer.getDeltaMovement().horizontalDistanceSqr() < 0.005;

    /** Always false. Forces a flag OFF every tick regardless of wearer state. */
    public static final Predicate<LivingEntity> ALWAYS_FALSE = wearer -> false;

    /** Constantly true. Symmetric counterpart to {@link #ALWAYS_FALSE}. */
    public static final Predicate<LivingEntity> ALWAYS_TRUE = wearer -> true;

    /** Flying with a sturdy-bottom block above the wearer's head; gates the ceiling-cling pose. */
    public static final Predicate<LivingEntity> CEILING_HANGING = wearer -> {
        if (!FLYING.test(wearer)) return false;
        Level level = wearer.level();
        // Small epsilon prevents sampling the block the wearer is already inside.
        double topY = wearer.getY() + wearer.getBbHeight() + 0.05;
        BlockPos pos = BlockPos.containing(wearer.getX(), topY, wearer.getZ());
        BlockState bs = level.getBlockState(pos);
        if (bs.isFaceSturdy(level, pos, Direction.DOWN)) return true;
        BlockPos above = pos.above();
        BlockState bsAbove = level.getBlockState(above);
        return bsAbove.isFaceSturdy(level, above, Direction.DOWN);
    };

    /** Flying with no ceiling above; complement of {@link #CEILING_HANGING} within {@link #FLYING}. */
    public static final Predicate<LivingEntity> FLYING_NOT_HANGING = wearer ->
            FLYING.test(wearer) && !CEILING_HANGING.test(wearer);
}
