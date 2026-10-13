package com.mas6y6.sablefinder.mixininterface;

import java.util.UUID;

public interface ISFPhysicsStaffServerHandler {
    void lockContraption(UUID uuid);
    void unlockContraption(UUID uuid);
    boolean isContraptionLocked(UUID uuid);
}