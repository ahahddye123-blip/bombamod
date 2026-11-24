package dev.bombowy.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModuleSettingsScreen extends Screen {
    private final Screen parent;
    private final Modules.Module module;

    private int px, py, pw, ph;
    private double scroll = 0;

    private static final int[] COLOR_PRESETS = {
            0xFF2E2E,
            0xFFFFFF,
            0x3CCB6B,
            0x3B82F6,
            0xFACC15,
            0xA855F7,
            0x06B6D4,
            0x111115
    };

    private interface SettingItem {
        int height();
        void render(DrawContext ctx, int x, int y, int w, int mx, int my);
        boolean click(int x, int y, int w, double mx, double my);
    }

    public ModuleSettingsScreen(Screen parent, Modules.Module module) {
        super(Text.literal("Ustawienia: " + module.name));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        pw = Math.min(this.width - 24, 430);
        ph = Math.min(this.height - 24, 305);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
    }

    private List<SettingItem> buildItems() {
        List<SettingItem> list = new ArrayList<>();
        BombowyConfig cfg = BombowyConfig.get();

        list.add(toggleItem("Status modułu", module::isOn, val -> module.setOn(val)));

        switch (module.id) {
            case "hitboxes" -> {
                list.add(colorPickerItem("Kolor przy celowaniu (domyślnie czerwony)",
                        () -> cfg.hitboxAimColor, c -> cfg.hitboxAimColor = c));
                list.add(colorPickerItem("Kolor z boku (domyślnie biały)",
                        () -> cfg.hitboxIdleColor, c -> cfg.hitboxIdleColor = c));
                list.add(sliderItem("Zasięg wykrywania celowania",
                        () -> (cfg.hitboxReach - 2.0f) / 4.0f,
                        v -> cfg.hitboxReach = 2.0f + v * 4.0f,
                        () -> String.format("%.1f kratek", cfg.hitboxReach)));
                list.add(toggleItem("Pokazuj ramki graczy z boku",
                        () -> cfg.hitboxShowAll, v -> cfg.hitboxShowAll = v));
            }
            case "cleanview" -> {
                list.add(toggleItem("Niski ogień na ekranie",
                        () -> cfg.lowFire, v -> cfg.lowFire = v));
                list.add(sliderItem("Obniżenie ognia",
                        () -> cfg.fireOffset / 0.6f,
                        v -> cfg.fireOffset = v * 0.6f,
                        () -> String.format("-%d%%", (int) (cfg.fireOffset * 100))));
                list.add(toggleItem("Bez winiety (ciemnych rogów)",
                        () -> cfg.noVignette, v -> cfg.noVignette = v));
                list.add(toggleItem("Bez trzęsienia kamery (NoHurtCam)",
                        () -> cfg.noHurtCam, v -> cfg.noHurtCam = v));
            }
            case "colorboost" -> {
                list.add(sliderItem("Nasycenie kolorów (Vibrance)",
                        () -> (cfg.colorVibrance - 1.0f) / 1.0f,
                        v -> cfg.colorVibrance = 1.0f + v * 1.0f,
                        () -> String.format("%d%%", (int) (cfg.colorVibrance * 100))));
                list.add(sliderItem("Kontrast montażowy",
                        () -> (cfg.colorContrast - 1.0f) / 0.5f,
                        v -> cfg.colorContrast = 1.0f + v * 0.5f,
                        () -> String.format("%d%%", (int) (cfg.colorContrast * 100))));
                list.add(sliderItem("Poświata odcienia",
                        () -> cfg.colorTintStrength / 0.35f,
                        v -> cfg.colorTintStrength = v * 0.35f,
                        () -> String.format("%d%%", (int) (cfg.colorTintStrength * 100))));
                list.add(colorPickerItem("Odcień filtru",
                        () -> cfg.colorTintHex, c -> cfg.colorTintHex = c));
            }
            case "scoreboard" -> {
                list.add(toggleItem("Przezroczyste tło scoreboarda",
                        () -> cfg.scoreboardTransparent, v -> cfg.scoreboardTransparent = v));
                list.add(toggleItem("Ukryj czerwone liczby po prawej",
                        () -> cfg.scoreboardHideNumbers, v -> cfg.scoreboardHideNumbers = v));
            }
            case "optimization" -> {
                list.add(toggleItem("Tryb słabego komputera (Potato Mode)",
                        () -> cfg.potatoMode, v -> {
                            cfg.potatoMode = v;
                            if (module.isOn()) Features.optimization(true);
                        }));
                list.add(toggleItem("Ukryj deszcz i śnieg (więcej FPS)",
                        () -> cfg.disableWeather, v -> cfg.disableWeather = v));
                list.add(toggleItem("Ogranicz zbędne animacje",
                        () -> cfg.reduceAnimations, v -> cfg.reduceAnimations = v));
            }
            case "fpshud" -> addHudSettings(list, cfg.hudFps);
            case "inventoryhud" -> {
                addHudSettings(list, cfg.hudInventory);
                list.add(toggleItem("Pokazuj również pasek podręczny (Hotbar)",
                        () -> cfg.invShowHotbar, v -> cfg.invShowHotbar = v));
                list.add(toggleItem("Kompaktowe sloty",
                        () -> cfg.invCompact, v -> cfg.invCompact = v));
            }
            case "cooldowns" -> {
                addHudSettings(list, cfg.hudCooldowns);
                list.add(toggleItem("Perła Endu (Ender Pearl)", () -> cfg.cdPearl, v -> cfg.cdPearl = v));
                list.add(toggleItem("Tarcza (Shield)", () -> cfg.cdShield, v -> cfg.cdShield = v));
                list.add(toggleItem("Owoc refrenusu (Chorus)", () -> cfg.cdChorus, v -> cfg.cdChorus = v));
                list.add(toggleItem("Złote jabłko", () -> cfg.cdGoldenApple, v -> cfg.cdGoldenApple = v));
                list.add(toggleItem("Wietrzna kula (Wind Charge)", () -> cfg.cdWindCharge, v -> cfg.cdWindCharge = v));
                list.add(toggleItem("Pozostałe przedmioty", () -> cfg.cdOther, v -> cfg.cdOther = v));
            }
        }
        return list;
    }

    private void addHudSettings(List<SettingItem> list, BombowyConfig.HudElement el) {
        list.add(sliderItem("Przezroczystość tła (0% = pełna przezroczystość)",
                () -> el.bgOpacity,
                v -> el.bgOpacity = v,
                () -> String.format("%d%%", (int) (el.bgOpacity * 100))));
        list.add(sliderItem("Rozmiar elementu na HUD",
                () -> (el.scale - 0.5f) / 2.0f,
                v -> el.scale = 0.5f + v * 2.0f,
                () -> String.format("%.2fx", el.scale)));
        list.add(toggleItem("Kolor tęczowy (Chroma)",
                () -> el.chroma, v -> el.chroma = v));
        list.add(colorPickerItem("Kolor tekstu / paska",
                () -> el.textColor, c -> {
                    el.textColor = c;
                    el.chroma = false;
                }));
    }

    private SettingItem toggleItem(String label, java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
        return new SettingItem() {
            @Override public int height() { return 28; }

            @Override
            public void render(DrawContext ctx, int x, int y, int w, int mx, int my) {
                boolean on = getter.getAsBoolean();
                ctx.fill(x, y, x + w, y + 24, 0xFF1B1C22);
                ctx.drawText(textRenderer, label, x + 8, y + 8, 0xFFE8E8F0, false);

                int bw = 64, bh = 16;
                int bx = x + w - bw - 6, by = y + 4;
                boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
                ctx.fill(bx, by, bx + bw, by + bh, on ? (hov ? 0xFF38D168 : 0xFF2EBD5B) : (hov ? 0xFF40404B : 0xFF32323B));
                String t = on ? "Włączony" : "Wyłączony";
                ctx.drawText(textRenderer, t, bx + (bw - textRenderer.getWidth(t)) / 2, by + 4, 0xFFFFFFFF, false);
            }

            @Override
            public boolean click(int x, int y, int w, double mx, double my) {
                if (mx >= x && mx <= x + w && my >= y && my <= y + 24) {
                    setter.accept(!getter.getAsBoolean());
                    BombowyConfig.save();
                    return true;
                }
                return false;
            }
        };
    }

    private SettingItem sliderItem(String label, java.util.function.Supplier<Float> getter,
                                   java.util.function.Consumer<Float> setter, java.util.function.Supplier<String> valText) {
        return new SettingItem() {
            @Override public int height() { return 36; }

            @Override
            public void render(DrawContext ctx, int x, int y, int w, int mx, int my) {
                float p = MathHelper.clamp(getter.get(), 0f, 1f);
                ctx.fill(x, y, x + w, y + 32, 0xFF1B1C22);
                ctx.drawText(textRenderer, label, x + 8, y + 5, 0xFFE8E8F0, false);
                String vt = valText.get();
                ctx.drawText(textRenderer, vt, x + w - 8 - textRenderer.getWidth(vt), y + 5, 0xFF3CCB6B, false);

                int sx = x + 8, sy = y + 20, sw = w - 16, sh = 5;
                ctx.fill(sx, sy, sx + sw, sy + sh, 0xFF2E2E38);
                int fw = (int) (sw * p);
                ctx.fill(sx, sy, sx + fw, sy + sh, 0xFF3CCB6B);
                ctx.fill(sx + fw - 3, sy - 2, sx + fw + 3, sy + sh + 2, 0xFFFFFFFF);
            }

            @Override
            public boolean click(int x, int y, int w, double mx, double my) {
                int sx = x + 8, sy = y + 14, sw = w - 16, sh = 16;
                if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh) {
                    float norm = MathHelper.clamp((float) ((mx - sx) / sw), 0f, 1f);
                    setter.accept(norm);
                    BombowyConfig.save();
                    return true;
                }
                return false;
            }
        };
    }

    private SettingItem colorPickerItem(String label, java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
        return new SettingItem() {
            @Override public int height() { return 42; }

            @Override
            public void render(DrawContext ctx, int x, int y, int w, int mx, int my) {
                int cur = getter.getAsInt() & 0xFFFFFF;
                ctx.fill(x, y, x + w, y + 38, 0xFF1B1C22);
                ctx.drawText(textRenderer, label, x + 8, y + 5, 0xFFE8E8F0, false);

                for (int i = 0; i < COLOR_PRESETS.length; i++) {
                    int cx = x + 8 + i * 24;
                    int cy = y + 18;
                    int col = COLOR_PRESETS[i];
                    boolean sel = (col == cur);
                    if (sel) {
                        ctx.fill(cx - 2, cy - 2, cx + 18, cy + 16, 0xFF3CCB6B);
                    }
                    ctx.fill(cx, cy, cx + 16, cy + 14, 0xFF000000 | col);
                }
            }

            @Override
            public boolean click(int x, int y, int w, double mx, double my) {
                for (int i = 0; i < COLOR_PRESETS.length; i++) {
                    int cx = x + 8 + i * 24;
                    int cy = y + 18;
                    if (mx >= cx && mx <= cx + 16 && my >= cy && my <= cy + 14) {
                        setter.accept(COLOR_PRESETS[i]);
                        BombowyConfig.save();
                        return true;
                    }
                }
                return false;
            }
        };
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xB8000000);
        ctx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF2B2B33);
        ctx.fill(px, py, px + pw, py + ph, 0xFF131418);

        ctx.drawText(this.textRenderer, Text.literal("⚙ " + module.name).styled(s -> s.withBold(true)),
                px + 12, py + 11, 0xFFFFFFFF, false);
        ctx.drawText(this.textRenderer, module.desc, px + 12, py + 24, 0xFF8A8A96, false);

        int bx = px + pw - 68, by = py + 10;
        boolean bHov = mx >= bx && mx <= bx + 56 && my >= by && my <= by + 18;
        ctx.fill(bx, by, bx + 56, by + 18, bHov ? 0xFF38D168 : 0xFF2A2A32);
        ctx.drawText(this.textRenderer, "Powrót", bx + 11, by + 5, 0xFFFFFFFF, false);

        List<SettingItem> items = buildItems();
        int listX = px + 12;
        int listY = py + 42;
        int listW = pw - 24;
        int listH = ph - 52;

        ctx.enableScissor(listX, listY, listX + listW, listY + listH);
        int curY = listY - (int) scroll;
        for (SettingItem item : items) {
            item.render(ctx, listX, curY, listW, mx, my);
            curY += item.height();
        }
        ctx.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        int bx = px + pw - 68, by = py + 10;
        if (mx >= bx && mx <= bx + 56 && my >= by && my <= by + 18) {
            this.client.setScreen(parent);
            return true;
        }

        List<SettingItem> items = buildItems();
        int listX = px + 12;
        int listY = py + 42;
        int listW = pw - 24;
        int curY = listY - (int) scroll;
        for (SettingItem item : items) {
            if (item.click(listX, curY, listW, mx, my)) {
                return true;
            }
            curY += item.height();
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = MathHelper.clamp(scroll - verticalAmount * 16, 0, 220);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_GRAVE_ACCENT) {
            this.client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
