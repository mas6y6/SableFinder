package com.mas6y6.sablefinder.simulated;

import com.mas6y6.sablefinder.SableFinder;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Optional;

public class SimulatedCompat {
    private static SimulatedCompat INSTANCE;

    private SimulatedCompat() {

    }

    public static Optional<SimulatedCompat> getInstance() {
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

    public PhysicsStaffServerHandler getPhysicsStaffServerHandler() {
        var level = SableFinder.getServer().overworld();

        var storage = PhysicsStaffServerHandler.get(level);


        return storage;
    }
}
