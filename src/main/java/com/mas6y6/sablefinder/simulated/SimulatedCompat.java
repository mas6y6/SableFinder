package com.mas6y6.sablefinder.simulated;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.mixininterface.ISFPhysicsStaffServerHandler;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class SimulatedCompat {
    private static SimulatedCompat INSTANCE;

    private SimulatedCompat() {

    }

    public static @NotNull Optional<SimulatedCompat> getInstance() {
        try {
            Class.forName("dev.simulated_team.simulated.Simulated");
        } catch (ClassNotFoundException e) {
            SableFinder.LOGGER.error("Simulated not found (Why and how are you using sable without aeronautics?!)");

            return Optional.empty();
        }

        if (INSTANCE == null) {
            INSTANCE = new SimulatedCompat();
        }

        return Optional.of(INSTANCE);
    }

    public static boolean isSimulatedInstalled() {
        try {
            Class.forName("dev.simulated_team.simulated.Simulated");
        } catch (ClassNotFoundException e) {
            return false;
        }

        return true;
    }


    public @Nullable ISFPhysicsStaffServerHandler getPhysicsStaffServerHandler(@Nullable MinecraftServer server) {
        if (server == null) {
            return null;
        }

        var level = server.overworld();

        if (PhysicsStaffServerHandler.get(level) instanceof ISFPhysicsStaffServerHandler handler) {
            return handler;
        }

        return null;
    }
    public @NotNull Set<UUID> getLockedContraptions(@Nullable MinecraftServer server) {
        final ISFPhysicsStaffServerHandler handler = getPhysicsStaffServerHandler(server);

        return handler == null ? Set.of() : handler.lockedContraptions();
    }
}
