package dev.bombowy.client.mixin;

import dev.bombowy.client.BombowyConfig;
import dev.bombowy.client.Modules;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dodatkowa optymalizacja FPS: ukrycie opadów deszczu/śniegu gdy włączona Optymalizacja. */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true, require = 0)
    private void bombowy$skipWeatherForFps(LightmapTextureManager lightmap, float tickDelta,
                                          double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (Modules.isOn("optimization") && BombowyConfig.get().disableWeather) {
            ci.cancel();
        }
    }
}
