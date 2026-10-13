package com.mas6y6.sablefinder.mixin;

import com.mas6y6.sablefinder.mixin.accesors.PhysicsStaffLockAccessor;
import com.mas6y6.sablefinder.mixininterface.ISFPhysicsStaffServerHandler;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(PhysicsStaffServerHandler.class)
public abstract class PhysicsStaffServerHandlerMixin implements ISFPhysicsStaffServerHandler {
    @Shadow
    @Final
    private Map<UUID, ?> locks;

    @Shadow
    private ServerLevel level;

    @Shadow
    abstract void loadLocks(ListTag list);

    @Shadow
    abstract void setLocksDirty();

    @Override
    public void lockContraption(@NotNull UUID uuid) {
        if (this.locks.containsKey(uuid)) {
            return;
        }

        ListTag pending = new ListTag();
        pending.add(NbtUtils.createUUID(uuid));
        this.loadLocks(pending);
        this.setLocksDirty();

        SubLevel subLevel = SubLevelContainer.getContainer(this.level).getSubLevel(uuid);
        if (subLevel != null) {
            ((PhysicsStaffServerHandler) (Object) this).applyLockIfNeeded(subLevel);
        }
    }

    @Override
    public void unlockContraption(UUID uuid) {
        Object lock = this.locks.remove(uuid);
        if (lock == null) {
            return;
        }

        PhysicsConstraintHandle handle = ((PhysicsStaffLockAccessor) lock).sablefinder$handle();
        if (handle != null) {
            handle.remove();
        }

        this.setLocksDirty();
    }

    @Override
    public boolean isContraptionLocked(UUID uuid) {
        return this.locks.containsKey(uuid);
    }

    @Override
    public @NotNull Set<UUID> lockedContraptions() {
        return Set.copyOf(this.locks.keySet());
    }
}