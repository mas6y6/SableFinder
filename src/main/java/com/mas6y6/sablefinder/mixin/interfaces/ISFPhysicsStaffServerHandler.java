package com.mas6y6.sablefinder.mixin.interfaces;

import java.util.UUID;

public interface ISFPhysicsStaffServerHandler {

    /**
     * Locks a contraption in place at its current position, whether or not it is currently loaded.
     * <p>
     * If the sublevel is loaded a physics constraint is attached immediately. If it is not
     * loaded, the lock is stored without a constraint and {@code PhysicsStaffSubLevelObserver}
     * attaches the constraint when the sublevel is added to the world.
     * <p>
     * This is a no-op if the contraption is already locked.
     */
    void lockContraption(UUID uuid);

    /**
     * Removes any lock held on the given contraption, destroying the physics constraint if one
     * exists. Safe to call for contraptions that are not currently loaded.
     */
    void unlockContraption(UUID uuid);

    /**
     * @return {@code true} if a lock entry exists for this contraption, regardless of whether
     * the contraption is currently loaded or whether its constraint is still valid.
     */
    boolean isContraptionLocked(UUID uuid);
}