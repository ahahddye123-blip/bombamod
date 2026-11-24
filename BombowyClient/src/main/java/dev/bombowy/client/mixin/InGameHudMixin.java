package dev.bombowy.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bombowy.client.BombowyConfig;
import dev.bombowy.client.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.scoreboard.number.NumberFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * - Czysty obraz: brak winiety
 * - Scoreboard: przezroczyste tło (w 1.21.4 tło sidebaru jest przekazywane jako argument koloru do DrawContext.fill) + brak czerwonych liczb
 * - Boost kolorów: żywy filtr montażowy
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void bombowy$noVignette(DrawContext context, Entity entity, CallbackInfo ci) {
        if (Modules.isOn("cleanview") && BombowyConfig.get().noVignette) {
            ci.cancel();
        }
    }

    // W 1.21.4 tło scoreboaradu jest rysowane z kolorem argb (domyślnie półprzezroczysty czarny)
    @ModifyVariable(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("STORE"),
            ordinal = 4,
            require = 0)
    private int bombowy$transparentScoreboardBodyColor(int color) {
        if (Modules.isOn("scoreboard") && BombowyConfig.get().scoreboardTransparent) {
            return 0x00000000;
        }
        return color;
    }

    @ModifyVariable(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("STORE"),
            ordinal = 5,
            require = 0)
    private int bombowy$transparentScoreboardHeaderColor(int color) {
        if (Modules.isOn("scoreboard") && BombowyConfig.get().scoreboardTransparent) {
            return 0x00000000;
        }
        return color;
    }

    @Redirect(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/ScoreboardObjective;getNumberFormatOr(Lnet/minecraft/scoreboard/number/NumberFormat;)Lnet/minecraft/scoreboard/number/NumberFormat;"),
            require = 0)
    private NumberFormat bombowy$hideRedScoreboardNumbers(ScoreboardObjective objective, NumberFormat fallback) {
        if (Modules.isOn("scoreboard") && BombowyConfig.get().scoreboardHideNumbers) {
            return BlankNumberFormat.INSTANCE;
        }
        return objective.getNumberFormatOr(fallback);
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void bombowy$colorBoostOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!Modules.isOn("colorboost")) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) return;

        BombowyConfig cfg = BombowyConfig.get();
        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();

        int vibranceAlpha = (int) Math.min(36, Math.max(0, (cfg.colorVibrance - 1.0f) * 30.0f));
        if (vibranceAlpha > 0) {
            RenderSystem.enableBlend();
            context.fill(0, 0, w, h, (vibranceAlpha << 24) | 0xFF9E44);
        }

        int tintAlpha = (int) Math.min(90, Math.max(0, cfg.colorTintStrength * 255.0f));
        if (tintAlpha > 0) {
            context.fill(0, 0, w, h, (tintAlpha << 24) | (cfg.colorTintHex & 0xFFFFFF));
        }
    }
}
