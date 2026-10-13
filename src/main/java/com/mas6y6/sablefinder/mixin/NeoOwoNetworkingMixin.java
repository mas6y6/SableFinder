package com.mas6y6.sablefinder.mixin;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "io.wispforest.owo.network.neoforge.NeoOwoNetworking", remap = false)
public class NeoOwoNetworkingMixin {

    @Redirect(
            method = "onNetworkRegister",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/network/event/RegisterPayloadHandlersEvent;registrar(Ljava/lang/String;)Lnet/neoforged/neoforge/network/registration/PayloadRegistrar;"
            )
    )
    private static @NotNull PayloadRegistrar sablefinder$optionalOwoRegistrar(@NotNull RegisterPayloadHandlersEvent event, @NotNull String version) {
        return event.registrar(version).optional();
    }
}
