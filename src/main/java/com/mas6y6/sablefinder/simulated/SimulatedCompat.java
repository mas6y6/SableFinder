package com.mas6y6.sablefinder.simulated;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.mixininterface.ISFPhysicsStaffServerHandler;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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

    public @Nullable ISFPhysicsStaffServerHandler getPhysicsStaffServerHandler() {
        var level = SableFinder.getServer().overworld();

        if (PhysicsStaffServerHandler.get(level) instanceof ISFPhysicsStaffServerHandler handler) {
            return handler;
        }

        return null;
    }
}
