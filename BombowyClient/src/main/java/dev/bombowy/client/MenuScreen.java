package dev.bombowy.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ekran główny wzorowany na układzie z referencyjnego screena:
 * - Lewy sidebar z ikoną bomby, napisem BOMBOWYMOD / BombowyClient, zakładkami,
 *   przyciskiem Edytor HUD oraz stopką v2.2 • `
 * - Prawy panel z nagłówkiem Moduły, polem wyszukiwania, filtrem ulubionych ♡
 *   oraz kartami modułów z dopracowanymi ikonami, bez ucinania nazw i z efektem kliknięcia.
 */
public class MenuScreen extends Screen {
    private static final int SIDE = 124;
    private static final int HEAD = 46;
    private static final int PAD = 10;
    private static final int GAP = 8;
    private static final int CARD_H = 88;
    private static final int MIN_CARD = 116;

    private static int lastTab = 0;
    private static boolean onlyFavorites = false;

    private TextFieldWidget search;
    private double scroll = 0;
    private int px, py, pw, ph;

    private final Map<String, Long> clickAnim = new HashMap<>();

    public MenuScreen() {
        super(Text.literal("BombowyMod"));
    }

    @Override
    protected void init() {
        pw = Math.min(this.width - 14, 660);
        ph = Math.min(this.height - 14, 372);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;

        int sw = Math.max(85, Math.min(148, pw - SIDE - PAD * 2 - 150));
        search = new TextFieldWidget(this.textRenderer, px + pw - PAD - sw - 24, py + 13, sw, 18, Text.literal("Szukaj"));
        search.setPlaceholder(Text.literal("Szukaj modułu..."));
        search.setChangedListener(s -> scroll = 0);
        this.addSelectableChild(search);
    }

    private int gridX() { return px + SIDE + PAD; }
    private int gridY() { return py + HEAD; }
    private int gridW() { return pw - SIDE - PAD * 2; }
    private int gridH() { return ph - HEAD - PAD; }
    private int cols() { return Math.max(2, Math.min(4, (gridW() + GAP) / (MIN_CARD + GAP))); }
    private int cardW() { int c = cols(); return (gridW() - GAP * (c - 1)) / c; }
    private int cardX(int i) { return gridX() + (i % cols()) * (cardW() + GAP); }
    private int cardY(int i) { return gridY() + (i / cols()) * (CARD_H + GAP) - (int) scroll; }

    private int maxScroll(int count) {
        int rows = (count + cols() - 1) / cols();
        return Math.max(0, rows * (CARD_H + GAP) - GAP - gridH());
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private List<Modules.Module> visible() {
        String q = search == null ? "" : search.getText().trim().toLowerCase();
        Modules.Tab currentTab = Modules.Tab.values()[lastTab];
        List<Modules.Module> out = new ArrayList<>();

        for (Modules.Module m : Modules.ALL) {
            if (onlyFavorites && !BombowyConfig.get().favorites.contains(m.id)) continue;

            boolean match;
            if (!q.isEmpty()) {
                match = m.name.toLowerCase().contains(q) || m.desc.toLowerCase().contains(q);
            } else if (currentTab == Modules.Tab.ALL) {
                match = true;
            } else if (currentTab == Modules.Tab.ENABLED) {
                match = m.isOn();
            } else {
                match = m.tab == currentTab;
            }
            if (match) out.add(m);
        }
        return out;
    }

    private static int countIn(Modules.Tab tab) {
        if (tab == Modules.Tab.ALL) return Modules.ALL.size();
        int n = 0;
        for (Modules.Module m : Modules.ALL) {
            if (tab == Modules.Tab.ENABLED) {
                if (m.isOn()) n++;
            } else if (m.tab == tab) {
                n++;
            }
        }
        return n;
    }

    private static void rrect(DrawContext c, int x, int y, int w, int h, int color) {
        if (w <= 2 || h <= 2) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        c.fill(x + 1, y, x + w - 1, y + h, color);
        c.fill(x, y + 1, x + w, y + h - 1, color);
    }

    private void playClickFeedback(String moduleId) {
        if (moduleId != null) {
            clickAnim.put(moduleId, System.currentTimeMillis());
        }
        if (this.client != null && this.client.player != null) {
            this.client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35f, 1.25f);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        TextRenderer tr = this.textRenderer;
        long now = System.currentTimeMillis();

        ctx.fill(0, 0, this.width, this.height, 0xB8000000);

        rrect(ctx, px - 1, py - 1, pw + 2, ph + 2, 0xFF2A2B32);
        rrect(ctx, px, py, pw, ph, 0xFF141519);

        ctx.fill(px + 1, py + 1, px + SIDE, py + ph - 1, 0xFF18191E);
        ctx.fill(px + SIDE, py + 1, px + SIDE + 1, py + ph - 1, 0xFF25262E);

        drawBombBranding(ctx, px + 11, py + 11);

        Modules.Tab[] tabs = Modules.Tab.values();
        boolean searching = search != null && !search.getText().trim().isEmpty();
        for (int i = 0; i < tabs.length; i++) {
            int tx = px + 7;
            int ty = py + 44 + i * 24;
            int tw = SIDE - 14;
            int th = 21;
            boolean sel = !searching && i == lastTab;
            boolean hov = inside(mx, my, tx, ty, tw, th);

            if (sel) {
                rrect(ctx, tx, ty, tw, th, 0xFF25272F);
                ctx.fill(tx, ty + 4, tx + 2, ty + th - 4, 0xFF3CCB6B);
            } else if (hov) {
                rrect(ctx, tx, ty, tw, th, 0xFF1E2027);
            }

            drawTabMiniIcon(ctx, tabs[i], tx + 7, ty + 6, sel ? 0xFFFFFFFF : 0xFF9EA0AC);
            ctx.drawText(tr, tabs[i].label, tx + 20, ty + 6, sel ? 0xFFFFFFFF : 0xFFA2A4B0, false);

            String cnt = String.valueOf(countIn(tabs[i]));
            ctx.drawText(tr, cnt, tx + tw - 6 - tr.getWidth(cnt), ty + 6, 0xFF6B6D7A, false);
        }

        int hudBtnX = px + 7;
        int hudBtnY = py + ph - 42;
        int hudBtnW = SIDE - 14;
        int hudBtnH = 22;
        boolean hudHov = inside(mx, my, hudBtnX, hudBtnY, hudBtnW, hudBtnH);
        rrect(ctx, hudBtnX, hudBtnY, hudBtnW, hudBtnH, hudHov ? 0xFF353844 : 0xFF272932);
        drawCrosshairMoveIcon(ctx, hudBtnX + 8, hudBtnY + 6, 0xFFFFFFFF);
        ctx.drawText(tr, Text.literal("Edytor HUD").styled(s -> s.withBold(true)), hudBtnX + 21, hudBtnY + 7, 0xFFFFFFFF, false);

        ctx.drawText(tr, "v2.2 • `", px + 11, py + ph - 14, 0xFF5D5F6B, false);

        int hx = px + SIDE + PAD;
        ctx.drawText(tr, Text.literal("Moduły").styled(s -> s.withBold(true)), hx, py + 10, 0xFFFFFFFF, false);
        String sub = searching ? "Wyniki wyszukiwania" : "Wszystkie moduły BombowyMod";
        ctx.drawText(tr, sub, hx, py + 23, 0xFF838592, false);

        if (search != null) search.render(ctx, mx, my, delta);
        int favBtnX = px + pw - PAD - 19;
        int favBtnY = py + 13;
        boolean favHov = inside(mx, my, favBtnX, favBtnY, 18, 18);
        rrect(ctx, favBtnX, favBtnY, 18, 18, onlyFavorites ? 0xFF3CCB6B : (favHov ? 0xFF2C2E38 : 0xFF21222A));
        ctx.drawText(tr, "♡", favBtnX + 6, favBtnY + 5, 0xFFFFFFFF, false);

        List<Modules.Module> list = visible();
        int gx = gridX(), gy = gridY(), gw = gridW(), gh = gridH();
        scroll = MathHelper.clamp(scroll, 0, maxScroll(list.size()));
        int cw = cardW();

        ctx.enableScissor(gx, gy, gx + gw, gy + gh);
        if (list.isEmpty()) {
            ctx.drawText(tr, "Brak modułów spełniających kryteria", gx + 6, gy + 8, 0xFF8A8A96, false);
        }

        for (int i = 0; i < list.size(); i++) {
            Modules.Module m = list.get(i);
            int cx = cardX(i);
            int cy = cardY(i);
            if (cy + CARD_H < gy || cy > gy + gh) continue;

            boolean inGrid = inside(mx, my, gx, gy, gw, gh);
            boolean hov = inGrid && inside(mx, my, cx, cy, cw, CARD_H);
            boolean on = m.isOn();
            boolean isFav = BombowyConfig.get().favorites.contains(m.id);

            long clickTime = clickAnim.getOrDefault(m.id, 0L);
            float pulse = MathHelper.clamp(1.0f - (now - clickTime) / 260.0f, 0.0f, 1.0f);
            int pressOffset = pulse > 0.5f ? 1 : 0;

            int borderCol = pulse > 0.05f
                    ? 0xFF56F08A
                    : (on ? 0xFF28593F : (hov ? 0xFF353742 : 0xFF25262E));
            int cardBg = hov ? 0xFF21232B : 0xFF1B1C22;

            rrect(ctx, cx - 1, cy - 1 + pressOffset, cw + 2, CARD_H + 2, borderCol);
            rrect(ctx, cx, cy + pressOffset, cw, CARD_H, cardBg);

            // Nazwa modułu z bezpiecznym marginesem przed serduszkiem (bez ucinania i nachodzenia)
            int maxTitleW = Math.max(24, cw - 28);
            String title = tr.trimToWidth(m.name, maxTitleW);
            ctx.drawText(tr, Text.literal(title).styled(s -> s.withBold(true)), cx + 8, cy + 7 + pressOffset, 0xFFF2F2F7, false);

            int heartX = cx + cw - 15;
            int heartY = cy + 7 + pressOffset;
            ctx.drawText(tr, "♡", heartX, heartY, isFav ? 0xFFFF5E7E : 0xFF6F717E, false);

            // Środkowa ikona karty
            int iconCenterX = cx + cw / 2;
            int iconCenterY = cy + 39 + pressOffset;
            drawSmoothCenterIcon(ctx, m.icon, iconCenterX, iconCenterY, on ? 0xFFFFFFFF : 0xFFD4D6E0);

            int gearX = cx + 7;
            int gearY = cy + CARD_H - 22 + pressOffset;
            int gearSize = 16;
            boolean gHov = inGrid && inside(mx, my, gearX, gearY, gearSize, gearSize);
            rrect(ctx, gearX, gearY, gearSize, gearSize, gHov ? 0xFF353742 : 0xFF25262E);
            ctx.drawText(tr, "⚙", gearX + 5, gearY + 4, m.hasSettings ? 0xFFD4D6E0 : 0xFF6B6D7A, false);

            int btnX = gearX + gearSize + 5;
            int btnY = gearY;
            int btnW = cw - 14 - gearSize - 5;
            int btnH = 16;
            boolean bHov = inGrid && inside(mx, my, btnX, btnY, btnW, btnH);

            int btnBg = on
                    ? (bHov ? 0xFF45DC78 : 0xFF39C968)
                    : (bHov ? 0xFF3A3C47 : 0xFF2D2F38);
            rrect(ctx, btnX, btnY, btnW, btnH, btnBg);

            String label = on ? "Włączony" : "Wyłączony";
            ctx.drawText(tr, label, btnX + (btnW - tr.getWidth(label)) / 2, btnY + 4,
                    on ? 0xFFFFFFFF : 0xFFAEB0BC, false);
        }
        ctx.disableScissor();

        int max = maxScroll(list.size());
        if (max > 0) {
            int barH = Math.max(20, gh * gh / (gh + max));
            int barY = gy + (int) ((gh - barH) * (scroll / max));
            ctx.fill(gx + gw + 3, barY, gx + gw + 5, barY + barH, 0xFF3A3C48);
        }
    }

    private void drawBombBranding(DrawContext ctx, int x, int y) {
        int bx = x + 2;
        int by = y + 5;
        rrect(ctx, bx, by, 12, 12, 0xFFFFFFFF);
        ctx.fill(bx + 4, by - 2, bx + 8, by, 0xFFFFFFFF);
        ctx.fill(bx + 7, by - 4, bx + 9, by - 2, 0xFFD4D6E0);
        long t = System.currentTimeMillis();
        int sparkCol = ((t / 250L) % 2 == 0) ? 0xFFFFB84D : 0xFFFF5E5E;
        ctx.fill(bx + 9, by - 6, bx + 11, by - 4, sparkCol);

        ctx.drawText(this.textRenderer, Text.literal("BOMBOWYMOD").styled(s -> s.withBold(true)),
                x + 17, y + 3, 0xFFFFFFFF, false);
        ctx.drawText(this.textRenderer, "BombowyClient",
                x + 17, y + 14, 0xFF7B7D8A, false);
    }

    private void drawTabMiniIcon(DrawContext ctx, Modules.Tab tab, int x, int y, int col) {
        switch (tab) {
            case ALL -> {
                ctx.fill(x, y, x + 3, y + 3, col);
                ctx.fill(x + 5, y, x + 8, y + 3, col);
                ctx.fill(x, y + 5, x + 3, y + 8, col);
                ctx.fill(x + 5, y + 5, x + 8, y + 8, col);
            }
            case ENABLED -> {
                ctx.fill(x + 1, y + 4, x + 3, y + 6, col);
                ctx.fill(x + 3, y + 6, x + 5, y + 8, col);
                ctx.fill(x + 5, y + 3, x + 8, y + 5, col);
            }
            case WALKA -> {
                ctx.fill(x + 1, y + 1, x + 3, y + 3, col);
                ctx.fill(x + 3, y + 3, x + 5, y + 5, col);
                ctx.fill(x + 5, y + 5, x + 8, y + 8, col);
            }
            case WYGLAD -> {
                ctx.fill(x + 4, y, x + 5, y + 9, col);
                ctx.fill(x, y + 4, x + 9, y + 5, col);
            }
            case HUD -> {
                rrect(ctx, x, y + 1, 9, 7, 0x33FFFFFF);
                ctx.fill(x, y + 1, x + 9, y + 2, col);
                ctx.fill(x, y + 7, x + 9, y + 8, col);
                ctx.fill(x, y + 1, x + 1, y + 8, col);
                ctx.fill(x + 8, y + 1, x + 9, y + 8, col);
            }
            case NARZEDZIA -> rrect(ctx, x + 1, y + 1, 7, 7, col);
        }
    }

    private void drawCrosshairMoveIcon(DrawContext ctx, int x, int y, int col) {
        ctx.fill(x + 4, y, x + 5, y + 9, col);
        ctx.fill(x, y + 4, x + 9, y + 5, col);
    }

    /** Dopracowane ikony wektorowe (bez klockowatych prostokątów). */
    private void drawSmoothCenterIcon(DrawContext ctx, Modules.Icon icon, int cx, int cy, int col) {
        switch (icon) {
            case HITBOX -> {
                // Sześcian 3D z przekątnymi i krawędziami
                rrect(ctx, cx - 7, cy - 6, 14, 13, 0x18FFFFFF);
                ctx.fill(cx - 7, cy - 6, cx + 7, cy - 5, col);
                ctx.fill(cx - 7, cy + 6, cx + 7, cy + 7, col);
                ctx.fill(cx - 7, cy - 6, cx - 6, cy + 7, col);
                ctx.fill(cx + 6, cy - 6, cx + 7, cy + 7, col);
                ctx.fill(cx - 7, cy - 6, cx - 3, cy - 3, col);
                ctx.fill(cx - 3, cy - 3, cx, cy - 1, col);
                ctx.fill(cx + 6, cy - 6, cx + 3, cy - 3, col);
                ctx.fill(cx + 3, cy - 3, cx, cy - 1, col);
                ctx.fill(cx, cy - 1, cx + 1, cy + 7, col);
            }
            case CLEAN_VIEW -> {
                // Zaokrąglony wizjer / celownik czystego obrazu
                rrect(ctx, cx - 8, cy - 5, 16, 10, 0x18FFFFFF);
                ctx.fill(cx - 6, cy - 5, cx + 6, cy - 4, col);
                ctx.fill(cx - 6, cy + 4, cx + 6, cy + 5, col);
                ctx.fill(cx - 8, cy - 3, cx - 7, cy + 3, col);
                ctx.fill(cx + 7, cy - 3, cx + 8, cy + 3, col);
                rrect(ctx, cx - 2, cy - 2, 4, 4, col);
            }
            case NO_PARTICLES -> {
                // Dwie gwiazdki / iskierki jak na wzorze
                ctx.fill(cx - 4, cy - 8, cx - 3, cy - 1, col);
                ctx.fill(cx - 7, cy - 5, cx, cy - 4, col);
                ctx.fill(cx - 5, cy - 6, cx - 2, cy - 3, col);

                ctx.fill(cx + 4, cy + 1, cx + 5, cy + 7, col);
                ctx.fill(cx + 2, cy + 4, cx + 7, cy + 5, col);
            }
            case COLOR_BOOST -> {
                // Zaokrąglona paleta kolorów z kropkami farb
                rrect(ctx, cx - 7, cy - 7, 14, 14, col);
                rrect(ctx, cx - 6, cy - 6, 12, 12, 0xFF1B1C22);
                rrect(ctx, cx - 4, cy - 4, 3, 3, col);
                rrect(ctx, cx + 1, cy - 4, 3, 3, col);
                rrect(ctx, cx - 4, cy + 1, 3, 3, col);
                rrect(ctx, cx + 1, cy + 1, 4, 3, 0x55FFFFFF);
            }
            case INVENTORY -> {
                // Siatka slotów 3x2 z lekkim zaokrągleniem
                rrect(ctx, cx - 8, cy - 6, 16, 12, 0x18FFFFFF);
                ctx.fill(cx - 8, cy - 6, cx + 8, cy - 5, col);
                ctx.fill(cx - 8, cy, cx + 8, cy + 1, col);
                ctx.fill(cx - 8, cy + 5, cx + 8, cy + 6, col);
                ctx.fill(cx - 8, cy - 6, cx - 7, cy + 6, col);
                ctx.fill(cx - 3, cy - 6, cx - 2, cy + 6, col);
                ctx.fill(cx + 2, cy - 6, cx + 3, cy + 6, col);
                ctx.fill(cx + 7, cy - 6, cx + 8, cy + 6, col);
            }
            case COOLDOWNS -> {
                // Pierścień cooldownu / perły jak na wzorze
                rrect(ctx, cx - 7, cy - 7, 14, 14, col);
                rrect(ctx, cx - 5, cy - 5, 10, 10, 0xFF1B1C22);
                rrect(ctx, cx - 2, cy - 2, 4, 4, col);
            }
            case SCOREBOARD -> {
                // Czysta tablica wyników z równymi liniami
                rrect(ctx, cx - 7, cy - 7, 14, 14, 0x22FFFFFF);
                ctx.fill(cx - 7, cy - 7, cx + 7, cy - 6, col);
                ctx.fill(cx - 5, cy - 4, cx + 4, cy - 3, col);
                ctx.fill(cx - 5, cy, cx + 5, cy + 1, col);
                ctx.fill(cx - 5, cy + 4, cx + 2, cy + 5, col);
            }
            case FPS_HUD -> {
                // Monitor z podstawką
                rrect(ctx, cx - 8, cy - 6, 16, 10, col);
                rrect(ctx, cx - 6, cy - 4, 12, 6, 0xFF1B1C22);
                ctx.fill(cx - 3, cy + 5, cx + 3, cy + 7, col);
            }
            case NAMETAGS -> {
                // Etykieta nicku
                rrect(ctx, cx - 8, cy - 4, 16, 8, 0x28FFFFFF);
                ctx.fill(cx - 5, cy - 1, cx + 5, cy + 1, col);
            }
            case OPTIMIZATION -> {
                // Piorun wydajności
                ctx.fill(cx + 1, cy - 8, cx + 3, cy - 4, col);
                ctx.fill(cx - 1, cy - 5, cx + 2, cy - 2, col);
                ctx.fill(cx - 4, cy - 1, cx + 4, cy, col);
                ctx.fill(cx - 2, cy + 1, cx + 1, cy + 4, col);
                ctx.fill(cx - 3, cy + 4, cx - 1, cy + 8, col);
            }
            case QUALITY -> {
                // Warstwy ostrości
                ctx.fill(cx - 6, cy - 4, cx + 6, cy - 3, col);
                ctx.fill(cx - 4, cy - 1, cx + 4, cy, col);
                ctx.fill(cx - 6, cy + 2, cx + 6, cy + 3, col);
            }
            case BRIGHTNESS -> {
                // Słońce z promieniami
                rrect(ctx, cx - 3, cy - 3, 6, 6, col);
                ctx.fill(cx - 1, cy - 7, cx, cy - 5, col);
                ctx.fill(cx - 1, cy + 5, cx, cy + 7, col);
                ctx.fill(cx - 7, cy - 1, cx - 5, cy, col);
                ctx.fill(cx + 5, cy - 1, cx + 7, cy, col);
            }
            case FAST_LIGHT -> {
                // Czysty kwadrat światła
                rrect(ctx, cx - 5, cy - 5, 10, 10, col);
            }
            case UNLIMITED_FPS -> {
                // Podwójne strzałki >>
                ctx.fill(cx - 6, cy - 4, cx - 4, cy - 2, col);
                ctx.fill(cx - 4, cy - 2, cx - 2, cy, col);
                ctx.fill(cx - 4, cy, cx - 2, cy + 2, col);
                ctx.fill(cx - 6, cy + 2, cx - 4, cy + 4, col);

                ctx.fill(cx + 1, cy - 4, cx + 3, cy - 2, col);
                ctx.fill(cx + 3, cy - 2, cx + 5, cy, col);
                ctx.fill(cx + 3, cy, cx + 5, cy + 2, col);
                ctx.fill(cx + 1, cy + 2, cx + 3, cy + 4, col);
            }
            case ENTITY_DIST, CHUNK_CAP, QUICK_JOIN -> {
                // Celownik / zasięg
                rrect(ctx, cx - 5, cy - 5, 10, 10, 0x18FFFFFF);
                ctx.fill(cx - 5, cy - 5, cx + 5, cy - 4, col);
                ctx.fill(cx - 5, cy + 4, cx + 5, cy + 5, col);
                ctx.fill(cx - 5, cy - 5, cx - 4, cy + 5, col);
                ctx.fill(cx + 4, cy - 5, cx + 5, cy + 5, col);
                ctx.fill(cx - 1, cy - 7, cx, cy + 7, col);
                ctx.fill(cx - 7, cy - 1, cx + 7, cy, col);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            Modules.Tab[] tabs = Modules.Tab.values();
            for (int i = 0; i < tabs.length; i++) {
                if (inside(mx, my, px + 7, py + 44 + i * 24, SIDE - 14, 21)) {
                    lastTab = i;
                    scroll = 0;
                    if (search != null) search.setText("");
                    playClickFeedback(null);
                    return true;
                }
            }

            int hudBtnX = px + 7;
            int hudBtnY = py + ph - 42;
            if (inside(mx, my, hudBtnX, hudBtnY, SIDE - 14, 22)) {
                playClickFeedback(null);
                this.client.setScreen(new HudEditorScreen(this));
                return true;
            }

            int favBtnX = px + pw - PAD - 19;
            int favBtnY = py + 13;
            if (inside(mx, my, favBtnX, favBtnY, 18, 18)) {
                onlyFavorites = !onlyFavorites;
                scroll = 0;
                playClickFeedback(null);
                return true;
            }

            if (inside(mx, my, gridX(), gridY(), gridW(), gridH())) {
                List<Modules.Module> list = visible();
                int cw = cardW();
                for (int i = 0; i < list.size(); i++) {
                    Modules.Module m = list.get(i);
                    int cx = cardX(i);
                    int cy = cardY(i);

                    int heartX = cx + cw - 17;
                    int heartY = cy + 5;
                    if (inside(mx, my, heartX, heartY, 14, 14)) {
                        if (BombowyConfig.get().favorites.contains(m.id)) {
                            BombowyConfig.get().favorites.remove(m.id);
                        } else {
                            BombowyConfig.get().favorites.add(m.id);
                        }
                        BombowyConfig.save();
                        playClickFeedback(m.id);
                        return true;
                    }

                    int gearX = cx + 7;
                    int gearY = cy + CARD_H - 22;
                    if (inside(mx, my, gearX, gearY, 16, 16)) {
                        playClickFeedback(m.id);
                        this.client.setScreen(new ModuleSettingsScreen(this, m));
                        return true;
                    }

                    int btnX = gearX + 21;
                    int btnY = gearY;
                    int btnW = cw - 35;
                    if (inside(mx, my, btnX, btnY, btnW, 16)) {
                        m.toggle();
                        playClickFeedback(m.id);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = MathHelper.clamp(scroll - verticalAmount * 20, 0, maxScroll(visible().size()));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_GRAVE_ACCENT && (search == null || !search.isFocused())) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
