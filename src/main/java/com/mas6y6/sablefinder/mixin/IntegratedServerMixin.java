package com.mas6y6.sablefinder.mixin;

import com.mas6y6.sablefinder.SableFinder;
import com.mas6y6.sablefinder.interfaces.Toastable;
import dev.ryanhcode.sable.index.SableToasts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({IntegratedServer.class})
public class IntegratedServerMixin implements Toastable {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Override
    public void sableFinder$showToast(Component title, Component message) {
        SystemToast.addOrUpdate(this.minecraft.getToasts(), SableFinder.SYSTEM_TOAST_ID,title, message);
    }
}
