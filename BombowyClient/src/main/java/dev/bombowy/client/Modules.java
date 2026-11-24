package dev.bombowy.client;

import java.util.List;
import java.util.function.Consumer;

public final class Modules {
    private Modules() {}

    public enum Tab {
        ALL("Wszystkie"),
        ENABLED("Włączone"),
        WALKA("Walka"),
        WYGLAD("Wygląd"),
        HUD("HUD"),
        NARZEDZIA("Narzędzia");

        public final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    public enum Icon {
        HITBOX,
        CLEAN_VIEW,
        NO_PARTICLES,
        COLOR_BOOST,
        INVENTORY,
        COOLDOWNS,
        SCOREBOARD,
        FPS_HUD,
        NAMETAGS,
        OPTIMIZATION,
        QUALITY,
        BRIGHTNESS,
        FAST_LIGHT,
        UNLIMITED_FPS,
        ENTITY_DIST,
        CHUNK_CAP,
        QUICK_JOIN
    }

    public static final class Module {
        public final String id;
        public final String name;
        public final String desc;
        public final Tab tab;
        public final Icon icon;
        public final boolean hasSettings;
        private final Consumer<Boolean> action;

        Module(String id, String name, String desc, Tab tab, Icon icon, boolean hasSettings, Consumer<Boolean> action) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.tab = tab;
            this.icon = icon;
            this.hasSettings = hasSettings;
            this.action = action;
        }

        public boolean isOn() {
            return Boolean.TRUE.equals(BombowyConfig.get().modules.get(id));
        }

        public void setOn(boolean on) {
            action.accept(on);
            BombowyConfig.get().modules.put(id, on);
            if ("fpshud".equals(id)) BombowyConfig.get().hudFps.enabled = on;
            if ("inventoryhud".equals(id)) BombowyConfig.get().hudInventory.enabled = on;
            if ("cooldowns".equals(id)) BombowyConfig.get().hudCooldowns.enabled = on;
            Features.writeOptions();
            BombowyConfig.save();
        }

        public void toggle() {
            setOn(!isOn());
        }
    }

    public static final List<Module> ALL = List.of(
            // ---- Walka ----
            new Module("hitboxes", "Hitboxy",
                    "Ramki graczy: czerwone przy celowaniu, białe z boku.",
                    Tab.WALKA, Icon.HITBOX, true, on -> {}),

            // ---- Wygląd ----
            new Module("cleanview", "Czysty obraz",
                    "Niski ogień, bez winiety i trzęsienia kamery.",
                    Tab.WYGLAD, Icon.CLEAN_VIEW, true, on -> {}),
            new Module("noparticles", "Bez cząsteczek",
                    "Bez okruchów bloków przy kopaniu i więcej FPS.",
                    Tab.WYGLAD, Icon.NO_PARTICLES, false, on -> {}),
            new Module("colorboost", "Boost kolorów",
                    "Żywe kolory świata jak w montażach.",
                    Tab.WYGLAD, Icon.COLOR_BOOST, true, on -> {}),
            new Module("quality", "Jakość 2K/4K",
                    "Ostrzejszy obraz w wysokiej rozdzielczości.",
                    Tab.WYGLAD, Icon.QUALITY, false, Features::quality),
            new Module("fastlight", "Szybkie światło",
                    "Wyłącza gładkie oświetlenie (AO) dla więcej FPS.",
                    Tab.WYGLAD, Icon.FAST_LIGHT, false, Features::fastLighting),
            new Module("brightness", "Jasność",
                    "Wyraźny obraz w nocy i w jaskiniach.",
                    Tab.WYGLAD, Icon.BRIGHTNESS, false, on -> {}),

            // ---- HUD ----
            new Module("inventoryhud", "Ekwipunek HUD",
                    "Twój ekwipunek zawsze na ekranie (przezroczysty).",
                    Tab.HUD, Icon.INVENTORY, true, on -> BombowyConfig.get().hudInventory.enabled = on),
            new Module("cooldowns", "Cooldowny",
                    "Czas do perły, tarczy i innych przedmiotów.",
                    Tab.HUD, Icon.COOLDOWNS, true, on -> BombowyConfig.get().hudCooldowns.enabled = on),
            new Module("scoreboard", "Scoreboard",
                    "Przezroczysty, bez czerwonych liczb.",
                    Tab.HUD, Icon.SCOREBOARD, true, on -> {}),
            new Module("fpshud", "Pokaż FPS",
                    "Przezroczysty licznik FPS na ekranie.",
                    Tab.HUD, Icon.FPS_HUD, true, on -> BombowyConfig.get().hudFps.enabled = on),
            new Module("nametags", "Nicki",
                    "Przezroczyste tło nicków i własny nick w F5.",
                    Tab.HUD, Icon.NAMETAGS, false, on -> {}),

            // ---- Narzędzia ----
            new Module("optimization", "Optymalizacja",
                    "Więcej FPS i tryb słabego komputera.",
                    Tab.NARZEDZIA, Icon.OPTIMIZATION, true, Features::optimization),
            new Module("fpsboost", "FPS Boost",
                    "Szybka grafika, mniej cząsteczek, bez chmur.",
                    Tab.NARZEDZIA, Icon.OPTIMIZATION, false, Features::fpsBoost),
            new Module("unlimitedfps", "Unlimited FPS",
                    "Bez limitu FPS i bez VSync.",
                    Tab.NARZEDZIA, Icon.UNLIMITED_FPS, false, Features::unlimitedFps),
            new Module("entitydist", "Dystans encji",
                    "Mniejszy dystans renderowania encji dla FPS.",
                    Tab.NARZEDZIA, Icon.ENTITY_DIST, false, Features::entityDistance),
            new Module("chunkcap", "Limit chunków",
                    "Ogranicza dystans renderowania i symulacji.",
                    Tab.NARZEDZIA, Icon.CHUNK_CAP, false, Features::chunkCap),
            new Module("quickjoin", "Szybki podgląd",
                    "Podgląd serwera po wpisaniu adresu IP.",
                    Tab.NARZEDZIA, Icon.QUICK_JOIN, false, on -> {})
    );

    public static boolean isOn(String id) {
        return Boolean.TRUE.equals(BombowyConfig.get().modules.get(id));
    }

    public static Module byId(String id) {
        for (Module m : ALL) {
            if (m.id.equals(id)) return m;
        }
        return null;
    }
}
