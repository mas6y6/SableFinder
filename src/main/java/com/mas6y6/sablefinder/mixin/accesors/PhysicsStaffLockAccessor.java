package com.mas6y6.sablefinder.mixin.accesors;

import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
        targets = "dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler$Lock",
        remap = false
)
public interface PhysicsStaffLockAccessor {
    @Accessor("handle")
    PhysicsConstraintHandle sablefinder$handle();
}