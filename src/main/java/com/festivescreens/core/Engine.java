package com.festivescreens.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The whole effects engine. It knows nothing about Minecraft: it simulates particles and draws through
 * {@link Canvas}. The client glue feeds it screen sizes, panel rectangles and elapsed time.
 */
public final class Engine {

    public static final class Options {
        public boolean bursts = true;
        public boolean ambient = true;
        public boolean decorations = true;
        public boolean panelTint = true;
        public boolean furnaceFx = true;
        public boolean otherScreens = false;
        public boolean reduceMotion = false;
        /** 0.25 .. 2.0, scales particle counts. */
        public float intensity = 1f;
    }

    private enum Kind { SPRITE, SPARK, ROCKET, FLYBY }

    private static final class P {
        Kind kind = Kind.SPRITE;
        float x, y, vx, vy, ax, ay, drag, age, life;
        Sprite[] frames;
        float fps = 0f;
        int scale = 1;
        int[] offX, offY;
        int color = 0xFFFFFFFF;
        int size = 2;
        boolean bounce;
        float floor;
        boolean fade = true;
        boolean twinkle;
        float swayAmp, swayFreq, swayPhase;
        float bobAmp, bobFreq;
        float trail;
        int[] trailPal;
        Sprite[] anim;
        /** Spawned by the ambient stream: fades out quickly once the screen closes. */
        boolean ambient;
        float fadeLen;
        /** RGB halo colour (0 = none) and whether it pulses like a heartbeat. */
        int glow;
        boolean beat;
    }

    private static final int MAX = 700;
    private static final float TAU = 6.2831855f;

    private static final int[] FIREWORK = {
            0xFFFFD54F, 0xFFFF5252, 0xFF40C4FF, 0xFF69F0AE, 0xFFE040FB, 0xFFFFFFFF, 0xFFFF9100};
    private static final int[] GOLD_TRAIL = {0xFFFFD54F, 0xFFFFF59D, 0xFFFFFFFF};
    private static final int[] RAINBOW = {
            0xFFFF1744, 0xFFFF9100, 0xFFFFEA00, 0xFF00E676, 0xFF00B0FF, 0xFF3D5AFE, 0xFFD500F9};
    private static final int[] BULBS = {0xFFE53935, 0xFF43A047, 0xFFFFC107, 0xFF1E88E5};

    public final Options opt = new Options();

    private final Random rng;
    private final List<P> ps = new ArrayList<>(MAX);
    private Theme theme = Theme.NONE;
    private float time;
    private float lastDt;
    private int screenW = 240;
    private int screenH = 200;
    private boolean ambientActive;
    private float ambA, ambB, furnaceAcc, rocketAcc;
    /** 1 right after a chest opens, fades to 0: brightens the border glow like light spilling out. */
    private float flash;
    /** Chest light-beam, panel shockwave and lightning timers (all count down / up from the open event). */
    private float beam, shock = 9f, lightning;
    private float beamX, beamTop;

    public Engine(long seed) {
        this.rng = new Random(seed);
    }

    // ------------------------------------------------------------------ state

    public Theme theme() {
        return theme;
    }

    public void setTheme(Theme t) {
        if (t != theme) {
            theme = t;
            ps.clear();
            ambA = ambB = furnaceAcc = rocketAcc = flash = beam = lightning = 0f;
            shock = 9f;
        }
    }

    public int particleCount() {
        return ps.size();
    }

    public void setAmbientActive(boolean v) {
        ambientActive = v;
    }

    public float time() {
        return time;
    }

    private float tt() {
        return opt.reduceMotion ? 0f : time;
    }

    // ------------------------------------------------------------------ helpers

    private float rf(float a, float b) {
        return a + rng.nextFloat() * (b - a);
    }

    private int ri(int n) {
        return rng.nextInt(n);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private int count(float base, float k) {
        return Math.max(1, Math.round(base * k));
    }

    private static int rgbAlpha(int rgb, float a) {
        int al = Math.round(clamp(a, 0f, 1f) * 255f);
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    private static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private P add(P p) {
        if (ps.size() < MAX) ps.add(p);
        return p;
    }

    private P spr(Sprite[] fr, float x, float y, float vx, float vy, float ax, float ay, float life, int scale) {
        P p = new P();
        p.kind = Kind.SPRITE;
        p.frames = fr;
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.ax = ax;
        p.ay = ay;
        p.life = life;
        p.scale = scale;
        switch (theme) {
            case VALENTINES -> { p.glow = 0xFF4F7B; p.beat = true; }
            case ST_PATRICKS -> p.glow = 0xFFD54F;
            case SUMMER -> p.glow = 0x4FC3F7;
            case CHRISTMAS -> p.glow = 0xBBDEFB;
            case NEW_YEARS -> p.glow = 0xFFD54F;
            case HALLOWEEN -> p.glow = 0x9C4DFF;
            default -> { }
        }
        return add(p);
    }

    private P spark(float x, float y, float vx, float vy, float ay, float life, int color, int size) {
        P p = new P();
        p.kind = Kind.SPARK;
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.ay = ay;
        p.life = life;
        p.color = color | 0xFF000000;
        p.size = size;
        return add(p);
    }

    private float[] dir(float minSp, float maxSp) {
        float a = rf(0f, TAU);
        float s = rf(minSp, maxSp);
        return new float[] {(float) Math.cos(a) * s, (float) Math.sin(a) * s};
    }

    private int pick(int[] pal) {
        return pal[ri(pal.length)];
    }

    // ------------------------------------------------------------------ simulation

    public void update(float dt, int w, int h) {
        screenH = h;
        screenW = w;
        lastDt = dt;
        if (dt <= 0f || theme == Theme.NONE) return;
        time += dt;
        flash = Math.max(0f, flash - dt * 1.6f);
        beam = Math.max(0f, beam - dt * 0.7f);
        lightning = Math.max(0f, lightning - dt);
        shock += dt;

        for (int i = ps.size() - 1; i >= 0; i--) {
            P p = ps.get(i);
            p.age += dt;
            if (!ambientActive && p.ambient && p.life - p.age > 0.8f) {
                p.life = p.age + 0.8f;
                p.fade = true;
                p.fadeLen = 0.8f;
            }
            boolean dead = p.age >= p.life;

            if (p.trailPal != null) {
                p.trail += dt;
                float step = p.kind == Kind.ROCKET ? 0.025f : 0.045f;
                while (p.trail > step) {
                    p.trail -= step;
                    float ox = p.kind == Kind.FLYBY ? rf(-6f, 10f) : 0f;
                    float oy = p.kind == Kind.FLYBY ? rf(8f, 26f) : 3f;
                    spark(p.x + ox, p.y + oy, rf(-7f, 7f), rf(5f, 25f), 45f, rf(0.35f, 0.7f), pick(p.trailPal), 1);
                }
            }
            if (p.kind == Kind.ROCKET && p.vy >= -8f) dead = true;

            if (dead) {
                if (p.kind == Kind.ROCKET) explode(p.x, p.y);
                ps.remove(i);
                continue;
            }

            p.vx += p.ax * dt;
            p.vy += p.ay * dt;
            if (p.drag > 0f) {
                float k = Math.max(0f, 1f - p.drag * dt);
                p.vx *= k;
                p.vy *= k;
            }
            p.x += p.vx * dt;
            p.y += p.vy * dt;

            if (p.bounce && p.vy > 0f && p.y > p.floor) {
                p.y = p.floor;
                p.vy = -p.vy * 0.72f;
                p.vx *= 0.92f;
                if (Math.abs(p.vy) < 34f) {
                    p.vy = 0f;
                    p.ay = 0f;
                    p.vx = 0f;
                }
            }

            if (p.kind != Kind.ROCKET && (p.x < -140 || p.x > w + 140 || p.y > h + 60 || p.y < -120)) {
                ps.remove(i);
            }
        }

        if (ambientActive && opt.ambient && !opt.reduceMotion) ambient(dt);
    }

    private void explode(float x, float y) {
        float k = opt.intensity;
        int main = pick(FIREWORK);
        int alt = pick(FIREWORK);
        int n = count(44, k);
        int pattern = ri(3); // 0 sphere, 1 double ring, 2 willow
        for (int i = 0; i < n; i++) {
            float a = (i / (float) n) * TAU + rf(-0.08f, 0.08f);
            float sp = pattern == 1 ? (i % 2 == 0 ? 96f : 52f) : (i % 3 == 0 ? 88f : rf(30f, 90f));
            P p = spark(x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp, 62f, rf(0.9f, 1.5f),
                    i % 2 == 0 ? main : alt, 2);
            p.drag = pattern == 2 ? 1.4f : 0.9f;
            if (pattern == 2) { p.ay = 30f; p.life += 1.0f; }
            p.twinkle = true;
        }
        for (int i = 0; i < 6; i++) {
            float[] d = dir(5f, 30f);
            P p = spark(x, y, d[0], d[1], 20f, rf(0.2f, 0.4f), 0xFFFFFFFF, 3);
            p.drag = 2f;
        }
    }

    // ------------------------------------------------------------------ bursts

    /** The open/close animation. {@code w == 0} means "no panel, use the screen centre". */
    public void burst(boolean open, int l, int t, int w, int h) {
        burst(open, l, t, w, h, false);
    }

    /** Same, with {@code chest = true} for chests, ender chests, barrels and shulker boxes: adds the lid-pop effect. */
    public void burst(boolean open, int l, int t, int w, int h, boolean chest) {
        if (theme == Theme.NONE || !opt.bursts || opt.reduceMotion) return;
        float k = opt.intensity * (open ? 1f : 0.7f) * 1.25f;
        if (w > 0) shock = open ? 0f : 0.15f;
        if (open && theme == Theme.HALLOWEEN) lightning = 0.55f;
        float cx = w > 0 ? l + w / 2f : screenW / 2f;
        float cy = w > 0 ? t + h / 2f : screenH / 2f;
        switch (theme) {
            case HALLOWEEN -> burstHalloween(k, cx, cy);
            case CHRISTMAS -> burstChristmas(k, cx, cy);
            case NEW_YEARS -> burstNewYears(open, k, l, t, w, h);
            case VALENTINES -> burstValentines(k, cx, cy);
            case ST_PATRICKS -> burstStPatricks(k, cx, cy);
            case SPRING -> burstSpring(open, k, cx, cy);
            case SUMMER -> burstSummer(k, cx, cy);
            case THANKSGIVING -> burstThanksgiving(k, cx, cy);
            default -> { }
        }
        if (chest) chestPop(open, k, l, t, w);
        if (open) flyby(t);
    }

    private void burstHalloween(float k, float cx, float cy) {
        int n = count(16, k);
        for (int i = 0; i < n; i++) {
            float[] d = dir(90f, 260f);
            P p = spr(Sprites.BAT, cx + rf(-30f, 30f), cy + rf(-20f, 20f), d[0], d[1] * 0.85f - 20f, 0f, 0f,
                    rf(1.6f, 2.8f), 2);
            p.fps = 9f;
            p.drag = 0.35f;
            p.swayAmp = rf(3f, 8f);
            p.swayFreq = rf(6f, 10f);
            p.swayPhase = rf(0f, TAU);
        }
        int[] pal = {0xFFFF8A1F, 0xFFFFB347, 0xFF9C4DFF};
        for (int i = 0; i < count(10, k); i++) {
            float[] d = dir(30f, 120f);
            P p = spark(cx, cy, d[0], d[1], -10f, rf(0.7f, 1.3f), pick(pal), 2);
            p.drag = 0.8f;
            p.twinkle = true;
        }
    }

    private void burstChristmas(float k, float cx, float cy) {
        for (int i = 0; i < count(18, k); i++) {
            float[] d = dir(40f, 140f);
            if (ri(2) == 0) {
                P p = spr(new Sprite[] {Sprites.SNOWFLAKE}, cx, cy, d[0], d[1], 0f, 25f, rf(1.8f, 3f), 1 + ri(2));
                p.drag = 0.5f;
                p.swayAmp = rf(1f, 3f);
                p.swayFreq = rf(3f, 6f);
            } else {
                P p = spark(cx, cy, d[0], d[1], 25f, rf(1.8f, 3f), 0xFFFFFFFF, 2);
                p.drag = 0.5f;
            }
        }
        for (int i = 0; i < count(7, k); i++) {
            float vx = rf(-80f, 80f);
            Sprite sp = i % 3 == 0 ? Sprites.GIFT : (i % 3 == 1 ? Sprites.GIFT_GREEN : Sprites.ORNAMENTS[ri(4)][0]);
            P p = spr(new Sprite[] {sp}, cx, cy, vx, rf(-170f, -80f), 0f, 230f, rf(3.2f, 4.2f), 2);
            p.bounce = true;
            p.floor = screenH - 10;
        }
    }

    private void burstNewYears(boolean open, float k, int l, int t, int w, int h) {
        int rockets = Math.max(1, Math.round((open ? 3 : 2) * k));
        for (int i = 0; i < rockets; i++) launchRocket(l, t, w, h);
        float cx = w > 0 ? l + w / 2f : screenW / 2f;
        float cy = w > 0 ? t + h / 2f : screenH / 2f;
        for (int i = 0; i < count(30, k); i++) {
            float[] d = dir(60f, 200f);
            P p = spark(cx, cy, d[0], d[1], 60f, rf(1.8f, 2.8f), pick(FIREWORK), 2);
            p.drag = 0.9f;
            p.twinkle = true;
        }
        // confetti: lots of little coloured strips that flutter down
        for (int i = 0; i < count(50, k); i++) {
            float[] d = dir(40f, 210f);
            P p = spark(cx, cy, d[0], d[1] - 40f, 70f, rf(2.2f, 3.6f), pick(FIREWORK), 2);
            p.drag = 1.3f;
            p.twinkle = true;
            p.swayAmp = rf(1f, 3f);
            p.swayFreq = rf(6f, 11f);
        }
    }

    private void launchRocket(int l, int t, int w, int h) {
        float x0 = (w > 0 ? l + w / 2f : screenW / 2f) + rf(-(w > 0 ? w * 0.45f : 60f), (w > 0 ? w * 0.45f : 60f));
        float y0 = Math.min(screenH - 4f, (w > 0 ? t + h : screenH) + 8f);
        float topY = w > 0 ? t : screenH * 0.2f;
        float ty = rf(topY - 30f, topY + (w > 0 ? h * 0.45f : 40f));
        ty = Math.max(10f, Math.min(ty, y0 - 70f));
        float g = 130f;
        float v = (float) Math.sqrt(2f * g * (y0 - ty));
        float tx = clamp(x0 + rf(-60f, 60f), 12f, screenW - 12f);
        P p = new P();
        p.kind = Kind.ROCKET;
        p.x = x0;
        p.y = y0;
        p.vy = -v;
        p.ay = g;
        p.vx = (tx - x0) / (v / g);
        p.life = 6f;
        p.fade = false;
        p.trailPal = GOLD_TRAIL;
        add(p);
    }

    private void burstValentines(float k, float cx, float cy) {
        for (int i = 0; i < count(26, k); i++) {
            float[] d = dir(40f, 190f);
            int sc = ri(4) == 0 ? 4 : (ri(3) == 0 ? 3 : 2);
            P p = spr(Sprites.HEARTS[ri(4)], cx, cy, d[0], d[1], 0f, -14f, rf(2.2f, 3.6f), sc);
            p.drag = 0.8f;
            p.swayAmp = rf(2f, 6f);
            p.swayFreq = rf(3f, 6f);
            p.swayPhase = rf(0f, TAU);
            if (sc >= 3) p.trailPal = new int[] {0xFFFF80AB, 0xFFF8BBD0, 0xFFFFFFFF};
        }
        // a ring of little hearts expanding from the middle, like a pulse
        int ring = count(16, k);
        for (int i = 0; i < ring; i++) {
            float a = i / (float) ring * TAU;
            P p = spr(new Sprite[] {Sprites.HEART_MINI}, cx, cy, (float) Math.cos(a) * 120f, (float) Math.sin(a) * 120f,
                    0f, 0f, 1.1f, 1);
            p.drag = 1.6f;
        }
    }

    private void burstStPatricks(float k, float cx, float cy) {
        for (int i = 0; i < count(18, k); i++) {
            P p = spr(Sprites.COIN, cx, cy, rf(-120f, 120f), rf(-220f, -110f), 0f, 300f, rf(3.2f, 4.5f),
                    ri(3) == 0 ? 2 : 1);
            p.fps = 9f;
            p.bounce = true;
            p.floor = screenH - 8;
        }
        for (int i = 0; i < count(10, k); i++) {
            float[] d = dir(40f, 120f);
            P p = spr(new Sprite[] {Sprites.SHAMROCK}, cx, cy, d[0], d[1], 0f, 20f, rf(2.4f, 3.6f), 1 + ri(2));
            p.drag = 0.6f;
            p.swayAmp = rf(2f, 6f);
            p.swayFreq = rf(3f, 5f);
        }
    }

    private void burstSpring(boolean open, float k, float cx, float cy) {
        for (int i = 0; i < count(26, k); i++) {
            float[] d = dir(30f, 120f);
            P p = spr(Sprites.PETALS[ri(3)], cx, cy, d[0], d[1], 0f, 12f, rf(2.4f, 3.8f), 1 + ri(2));
            p.drag = 0.7f;
            p.swayAmp = rf(4f, 8f);
            p.swayFreq = rf(3f, 5f);
            p.swayPhase = rf(0f, TAU);
        }
        for (int i = 0; i < count(open ? 4 : 2, k); i++) {
            float[] d = dir(40f, 110f);
            P p = spr(Sprites.BUTTERFLIES[ri(3)], cx, cy, d[0], d[1], 0f, 0f, rf(3f, 4.5f), 2);
            p.fps = 7f;
            p.drag = 0.2f;
            p.swayAmp = 8f;
            p.swayFreq = 3f;
            p.bobAmp = 4f;
            p.bobFreq = 4f;
        }
    }

    private void burstSummer(float k, float cx, float cy) {
        for (int i = 0; i < count(6, k) + 1; i++) {
            P p = spr(Sprites.BEACH_BALLS, cx, cy, rf(-170f, 170f), rf(-280f, -130f), 0f, 320f,
                    rf(4.5f, 6f), ri(3) == 0 ? 3 : 2);
            p.fps = rf(7f, 13f) * (ri(2) == 0 ? 1 : -1);
            p.bounce = true;
            p.floor = screenH - 18;
        }
        int[] pal = {0xFF4FC3F7, 0xFFFFFFFF, 0xFF81D4FA};
        for (int i = 0; i < count(20, k); i++) {
            float[] d = dir(40f, 150f);
            spark(cx, cy, d[0], d[1], 160f, rf(0.7f, 1.2f), pick(pal), 2);
        }
    }

    private void burstThanksgiving(float k, float cx, float cy) {
        for (int i = 0; i < count(22, k); i++) {
            float[] d = dir(50f, 160f);
            P p = spr(Sprites.LEAVES[ri(4)], cx, cy, d[0], d[1], 0f, 18f, rf(2.2f, 3.6f), ri(3) == 0 ? 1 : 2);
            p.drag = 0.6f;
            p.swayAmp = rf(5f, 9f);
            p.swayFreq = rf(3f, 5f);
            p.swayPhase = rf(0f, TAU);
        }
        for (int i = 0; i < count(5, k); i++) {
            P p = spr(new Sprite[] {Sprites.ACORN}, cx, cy, rf(-80f, 80f), rf(-150f, -70f), 0f, 220f,
                    rf(2.5f, 3.5f), 2);
            p.bounce = true;
            p.floor = screenH - 10;
        }
    }

    /** The chest-specific effect: lid pops up, light spills out, and themed items shoot out of the top. */
    private void chestPop(boolean open, float k, int l, int t, int w) {
        float cx = w > 0 ? l + w / 2f : screenW / 2f;
        float top = w > 0 ? t + 4f : screenH / 3f;
        float span = w > 0 ? w : 120f;
        flash = open ? 1f : 0.5f;
        beam = open ? 1f : 0.55f;
        beamX = cx;
        beamTop = top;
        if (open) {
            P lid = spr(new Sprite[] {Sprites.CHEST_LID}, cx, top, 0f, -70f, 0f, 40f, 0.55f, 3);
            lid.bounce = false;
        }
        int n = count(open ? 16 : 9, k);
        for (int i = 0; i < n; i++) {
            float x = cx + rf(-span * 0.3f, span * 0.3f);
            float vx = rf(-70f, 70f);
            float vy = -rf(170f, 300f);
            switch (theme) {
                case HALLOWEEN -> {
                    if (i % 4 == 0) {
                        P p = spr(Sprites.BAT, x, top, vx, vy * 0.6f, 0f, 60f, rf(1.4f, 2.2f), 2);
                        p.fps = 9f;
                    } else {
                        P p = spark(x, top, vx, vy, 340f, rf(1.1f, 1.7f), pick(new int[] {0xFFFF8A1F, 0xFFFFB347, 0xFF9C4DFF}), 2);
                        p.twinkle = true;
                    }
                }
                case CHRISTMAS -> {
                    if (i % 2 == 0) {
                        spr(Sprites.ORNAMENTS[ri(4)], x, top, vx, vy, 0f, 360f, rf(1.5f, 2.2f), 2);
                    } else {
                        P p = spark(x, top, vx, vy, 340f, rf(1.1f, 1.7f), pick(GOLD_TRAIL), 2);
                        p.twinkle = true;
                    }
                }
                case NEW_YEARS -> {
                    P p = spark(x, top, vx, vy, 340f, rf(1.1f, 1.8f), pick(FIREWORK), 2);
                    p.twinkle = true;
                }
                case VALENTINES -> {
                    P p = spr(Sprites.HEARTS[ri(4)], x, top, vx * 0.5f, vy * 0.45f, 0f, 120f, rf(1.6f, 2.4f), 1 + ri(2));
                    p.swayAmp = 3f;
                    p.swayFreq = 5f;
                }
                case ST_PATRICKS -> {
                    P p = spr(Sprites.COIN, x, top, vx, vy, 0f, 360f, rf(2.2f, 3.2f), 1 + ri(2));
                    p.fps = 9f;
                    p.bounce = true;
                    p.floor = screenH - 8;
                }
                case SPRING -> {
                    if (i % 5 == 0) {
                        P p = spr(Sprites.BUTTERFLIES[ri(3)], x, top, vx * 0.5f, vy * 0.5f, 0f, 50f, rf(2f, 3f), 2);
                        p.fps = 7f;
                    } else {
                        P p = spr(Sprites.PETALS[ri(3)], x, top, vx, vy * 0.6f, 0f, 140f, rf(1.6f, 2.6f), 1 + ri(2));
                        p.swayAmp = 5f;
                        p.swayFreq = 4f;
                    }
                }
                case SUMMER -> {
                    if (i % 6 == 0) {
                        P p = spr(Sprites.BEACH_BALLS, x, top, vx, vy, 0f, 340f, rf(3.5f, 4.8f), 2);
                        p.fps = rf(8f, 12f);
                        p.bounce = true;
                        p.floor = screenH - 14;
                    } else {
                        spark(x, top, vx, vy, 340f, rf(0.9f, 1.4f), ri(2) == 0 ? 0xFF4FC3F7 : 0xFFFFFFFF, 2);
                    }
                }
                case THANKSGIVING -> {
                    if (i % 4 == 0) {
                        P p = spr(new Sprite[] {Sprites.ACORN}, x, top, vx, vy, 0f, 360f, rf(1.6f, 2.4f), 2);
                        p.bounce = true;
                        p.floor = screenH - 10;
                    } else {
                        P p = spr(Sprites.LEAVES[ri(4)], x, top, vx, vy * 0.55f, 0f, 120f, rf(1.8f, 2.8f), 2);
                        p.swayAmp = 6f;
                        p.swayFreq = 4f;
                    }
                }
                default -> { }
            }
        }
    }

    /** Santa's sleigh and the turkey cross the top of the screen once per open. */
    private void flyby(int panelTop) {
        float y = clamp(panelTop - 30f, 10f, screenH * 0.4f);
        if (theme == Theme.CHRISTMAS) {
            float vx = (screenW + 150f) / 3.4f;
            P p = new P();
            p.kind = Kind.FLYBY;
            p.frames = new Sprite[] {Sprites.SLEIGH, Sprites.REINDEER, Sprites.REINDEER};
            p.offX = new int[] {0, 36, 58};
            p.offY = new int[] {0, 4, 4};
            p.scale = 2;
            p.x = -90f;
            p.y = y;
            p.vx = vx;
            p.life = 8f;
            p.fade = false;
            p.bobAmp = 3f;
            p.bobFreq = 3f;
            p.trailPal = GOLD_TRAIL;
            add(p);
        } else if (theme == Theme.HALLOWEEN) {
            P p = new P();
            p.kind = Kind.FLYBY;
            p.frames = new Sprite[] {Sprites.GHOST_A};
            p.anim = Sprites.GHOST;
            p.fps = 3f;
            p.offX = new int[] {0};
            p.offY = new int[] {0};
            p.scale = 3;
            p.x = -40f;
            p.y = y;
            p.vx = (screenW + 100f) / 5.5f;
            p.life = 8f;
            p.fade = false;
            p.bobAmp = 6f;
            p.bobFreq = 2.4f;
            p.trailPal = new int[] {0xFFB39DDB, 0xFFD1C4E9, 0xFFFFFFFF};
            add(p);
        } else if (theme == Theme.VALENTINES) {
            P p = new P();
            p.kind = Kind.FLYBY;
            p.frames = new Sprite[] {Sprites.ARROW};
            p.offX = new int[] {0};
            p.offY = new int[] {0};
            p.scale = 3;
            p.x = screenW + 40f;
            p.y = y + 10f;
            p.vx = -(screenW + 100f) / 1.6f;
            p.vy = 12f;
            p.life = 3f;
            p.fade = false;
            p.trailPal = new int[] {0xFFFF80AB, 0xFFF8BBD0, 0xFFFFFFFF};
            add(p);
        } else if (theme == Theme.SUMMER) {
            for (int i = 0; i < 2; i++) {
                P p = new P();
                p.kind = Kind.FLYBY;
                p.frames = new Sprite[] {Sprites.GULL_A};
                p.anim = Sprites.GULL;
                p.fps = 3.5f;
                p.offX = new int[] {0};
                p.offY = new int[] {0};
                p.scale = 2;
                p.x = -30f - i * 34f;
                p.y = y + i * 12f;
                p.vx = (screenW + 100f) / 5f;
                p.life = 8f;
                p.fade = false;
                p.bobAmp = 3f;
                p.bobFreq = 2.2f + i;
                add(p);
            }
        } else if (theme == Theme.THANKSGIVING) {
            float vx = (screenW + 100f) / 4.4f;
            P p = new P();
            p.kind = Kind.FLYBY;
            p.frames = new Sprite[] {Sprites.TURKEY};
            p.offX = new int[] {0};
            p.offY = new int[] {0};
            p.scale = 2;
            p.x = -40f;
            p.y = y + 4f;
            p.vx = vx;
            p.life = 8f;
            p.fade = false;
            p.bobAmp = 2.5f;
            p.bobFreq = 9f;
            add(p);
        }
    }

    // ------------------------------------------------------------------ ambient

    private void ambient(float dt) {
        float k = opt.intensity;
        float rateA, rateB = 0f;
        switch (theme) {
            case HALLOWEEN -> { rateA = 0.35f; rateB = 5f; }
            case CHRISTMAS -> { rateA = 16f; rateB = 2.2f; }
            case NEW_YEARS -> { rateA = 12f; rateB = 0.2f; }
            case VALENTINES -> { rateA = 5f; rateB = 2.5f; }
            case ST_PATRICKS -> { rateA = 3f; rateB = 6f; }
            case SPRING -> { rateA = 7f; rateB = 0.15f; }
            case SUMMER -> { rateA = 6f; rateB = 2f; }
            case THANKSGIVING -> rateA = 7f;
            default -> { return; }
        }
        ambA += dt * rateA * k;
        ambB += dt * rateB * k;
        while (ambA >= 1f) {
            ambA -= 1f;
            int n0 = ps.size();
            ambientA();
            markAmbient(n0);
        }
        while (ambB >= 1f) {
            ambB -= 1f;
            int n0 = ps.size();
            ambientB();
            markAmbient(n0);
        }
    }

    private void markAmbient(int from) {
        for (int i = from; i < ps.size(); i++) ps.get(i).ambient = true;
    }

    private void ambientA() {
        float x = rf(0f, screenW);
        float top = -6f;
        switch (theme) {
            case HALLOWEEN -> {
                boolean fromLeft = ri(2) == 0;
                float vx = rf(60f, 110f) * (fromLeft ? 1 : -1);
                P p = spr(Sprites.BAT, fromLeft ? -20f : screenW + 20f, rf(10f, screenH * 0.7f), vx, rf(-15f, 15f),
                        0f, 0f, (screenW + 60f) / Math.abs(vx), 2);
                p.fps = 9f;
                p.swayAmp = 3f;
                p.swayFreq = 5f;
                p.fade = false;
            }
            case CHRISTMAS -> {
                float vy = rf(18f, 35f);
                float life = (screenH + 20f) / vy;
                if (ri(2) == 0) {
                    P p = spr(new Sprite[] {Sprites.SNOWFLAKE}, x, top, 0f, vy, 0f, 0f, life, 1 + ri(2));
                    p.swayAmp = rf(2f, 5f);
                    p.swayFreq = rf(1.5f, 3f);
                    p.swayPhase = rf(0f, TAU);
                    p.fade = false;
                } else {
                    P p = spark(x, top, 0f, vy, 0f, life, 0xFFFFFFFF, 2);
                    p.swayAmp = rf(2f, 5f);
                    p.swayFreq = rf(1.5f, 3f);
                    p.fade = false;
                }
            }
            case NEW_YEARS -> {
                float vy = rf(20f, 45f);
                P p = spark(x, top, 0f, vy, 0f, (screenH + 20f) / vy, pick(FIREWORK), 2);
                p.swayAmp = rf(3f, 8f);
                p.swayFreq = rf(2f, 4f);
                p.twinkle = true;
                p.fade = false;
            }
            case VALENTINES -> {
                float vy = -rf(18f, 38f);
                P p = spr(Sprites.HEARTS[ri(4)], x, screenH + 6f, 0f, vy, 0f, 0f, (screenH + 20f) / -vy,
                        ri(3) == 0 ? 2 : 1);
                p.swayAmp = rf(3f, 7f);
                p.swayFreq = rf(1.5f, 3f);
                p.swayPhase = rf(0f, TAU);
                p.fade = false;
            }
            case ST_PATRICKS -> {
                float vy = rf(28f, 48f);
                P p = ri(2) == 0
                        ? spr(Sprites.COIN, x, top, 0f, vy, 0f, 0f, (screenH + 20f) / vy, 1)
                        : spr(new Sprite[] {Sprites.SHAMROCK}, x, top, 0f, vy, 0f, 0f, (screenH + 20f) / vy, 1);
                p.fps = 8f;
                p.swayAmp = rf(2f, 5f);
                p.swayFreq = rf(1.5f, 3f);
                p.fade = false;
            }
            case SPRING -> {
                float vy = rf(15f, 30f);
                P p = spr(Sprites.PETALS[ri(3)], x, top, 0f, vy, 0f, 0f, (screenH + 20f) / vy, 1 + ri(2));
                p.swayAmp = rf(4f, 9f);
                p.swayFreq = rf(1.5f, 3f);
                p.swayPhase = rf(0f, TAU);
                p.fade = false;
            }
            case SUMMER -> {
                float vy = -rf(14f, 30f);
                P p = spark(x, screenH + 4f, 0f, vy, 0f, (screenH + 20f) / -vy, ri(2) == 0 ? 0xFFB3E5FC : 0xFFFFFFFF, 2);
                p.swayAmp = rf(2f, 4f);
                p.swayFreq = rf(2f, 4f);
                p.fade = false;
            }
            case THANKSGIVING -> {
                float vy = rf(18f, 34f);
                P p = spr(Sprites.LEAVES[ri(4)], x, top, rf(-6f, 6f), vy, 0f, 0f, (screenH + 20f) / vy, ri(3) == 0 ? 1 : 2);
                p.swayAmp = rf(6f, 11f);
                p.swayFreq = rf(1.5f, 2.8f);
                p.swayPhase = rf(0f, TAU);
                p.fade = false;
            }
            default -> { }
        }
    }

    private void ambientB() {
        switch (theme) {
            case HALLOWEEN -> {
                float x = rf(screenW * 0.2f, screenW * 0.8f);
                P p = spark(x, screenH + 2f, 0f, -rf(20f, 45f), -5f, rf(2f, 3.5f), 0xFFFF8A1F, 1);
                p.twinkle = true;
            }
            case NEW_YEARS -> rocketAcc += 1f;
            case CHRISTMAS, VALENTINES, SUMMER -> {
                P p = spr(new Sprite[] {Sprites.TWINKLE}, rf(0f, screenW), rf(0f, screenH), 0f, 0f, 0f, 0f, rf(0.5f, 0.9f), 1 + ri(2));
                p.glow = 0;
            }
            case ST_PATRICKS -> {
                P p = spark(rf(0f, screenW), rf(0f, screenH), 0f, 0f, 0f, rf(0.4f, 0.8f), 0xFFFFD54F, 2);
                p.twinkle = true;
            }
            case SPRING -> {
                boolean fromLeft = ri(2) == 0;
                float vx = rf(30f, 55f) * (fromLeft ? 1 : -1);
                P p = spr(Sprites.BUTTERFLIES[ri(3)], fromLeft ? -12f : screenW + 12f, rf(20f, screenH * 0.7f), vx,
                        rf(-10f, 10f), 0f, 0f, (screenW + 40f) / Math.abs(vx), 2);
                p.fps = 7f;
                p.swayAmp = 6f;
                p.swayFreq = 3f;
                p.bobAmp = 5f;
                p.bobFreq = 4f;
                p.fade = false;
            }
            default -> { }
        }
        while (rocketAcc >= 1f) {
            rocketAcc -= 1f;
            launchRocket(0, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ furnace flames

    /** Themed sparks over the flame of a lit furnace, blast furnace or smoker. */
    public void furnace(int l, int t, boolean lit) {
        if (!lit || theme == Theme.NONE || !opt.furnaceFx || opt.reduceMotion) return;
        furnaceAcc += lastDt * 22f * opt.intensity;
        float fx = l + 63f;
        float fy = t + 46f;
        while (furnaceAcc >= 1f) {
            furnaceAcc -= 1f;
            float x = fx + rf(-5f, 5f);
            float vy = -rf(22f, 55f);
            float life = rf(0.5f, 1.0f);
            switch (theme) {
                case VALENTINES -> {
                    P p = spr(new Sprite[] {Sprites.HEART_MINI}, x, fy, rf(-6f, 6f), vy * 0.7f, 0f, -8f,
                            life + 0.4f, 1);
                    p.swayAmp = 2f;
                    p.swayFreq = 6f;
                }
                case HALLOWEEN -> spark(x, fy, rf(-8f, 8f), vy, -20f, life,
                        pick(new int[] {0xFFFF8A1F, 0xFFFFB347, 0xFF9C4DFF}), ri(2) + 1);
                case CHRISTMAS -> spark(x, fy, rf(-8f, 8f), vy, -20f, life,
                        pick(new int[] {0xFFFFFFFF, 0xFFFFCDD2, 0xFFFFD54F}), ri(2) + 1);
                case NEW_YEARS -> spark(x, fy, rf(-8f, 8f), vy, -20f, life, pick(FIREWORK), ri(2) + 1);
                case ST_PATRICKS -> spark(x, fy, rf(-8f, 8f), vy, -20f, life,
                        pick(new int[] {0xFF69F0AE, 0xFFFFD54F, 0xFF2E9E3E}), ri(2) + 1);
                case SPRING -> spark(x, fy, rf(-8f, 8f), vy, -20f, life,
                        pick(new int[] {0xFFA5D6A7, 0xFFF8BBD0, 0xFFFFF59D}), ri(2) + 1);
                case SUMMER -> spark(x, fy, rf(-8f, 8f), vy, -20f, life,
                        pick(new int[] {0xFFFFD54F, 0xFFFFAB40, 0xFFFFF59D}), ri(2) + 1);
                case THANKSGIVING -> spark(x, fy, rf(-8f, 8f), vy, -20f, life,
                        pick(new int[] {0xFFFF8F00, 0xFFFFB300, 0xFFE65100}), ri(2) + 1);
                default -> { }
            }
        }
    }

    // ------------------------------------------------------------------ drawing: particles

    private float alpha(P p) {
        float a = 1f;
        if (p.fade) {
            float fl = p.fadeLen > 0f ? p.fadeLen : p.life * 0.3f;
            a = Math.min(1f, (p.life - p.age) / fl);
        }
        a = Math.min(a, p.age / 0.12f + 0.0f);
        return clamp(a, 0f, 1f);
    }

    private int halos;

    /** A soft round glow made of a few stacked horizontal bars. */
    private void halo(Canvas c, int cx, int cy, int r, int rgb, float a) {
        int col = rgbAlpha(rgb, a);
        for (int dy = -r; dy < r; dy += 2) {
            int half = (int) Math.sqrt(Math.max(0, r * r - (dy + 1) * (dy + 1)));
            if (half > 0) c.fill(cx - half, cy + dy, cx + half, cy + dy + 2, col);
        }
    }

    public void renderParticles(Canvas c) {
        if (theme == Theme.NONE) return;
        halos = 0;
        for (int i = 0; i < ps.size(); i++) {
            P p = ps.get(i);
            float a = alpha(p);
            if (a <= 0.02f) continue;
            float sx = p.swayAmp == 0f ? 0f : (float) Math.sin(p.age * p.swayFreq + p.swayPhase) * p.swayAmp;
            float by = p.bobAmp == 0f ? 0f : (float) Math.sin(p.age * p.bobFreq) * p.bobAmp;
            int x = (int) Math.floor(p.x + sx);
            int y = (int) Math.floor(p.y + by);
            switch (p.kind) {
                case SPRITE -> {
                    int fi = p.frames.length == 1 ? 0 : Math.floorMod((int) Math.floor(p.age * p.fps), p.frames.length);
                    boolean resting = p.bounce && p.ay == 0f && p.vy == 0f;
                    if (resting) fi = 0;
                    Sprite s = p.frames[fi];
                    int sw = s.w * p.scale, sh = s.h * p.scale;
                    if (p.bounce && !resting) {
                        float hgt = Math.max(0f, p.floor - p.y);
                        float sk = clamp(1f - hgt / 190f, 0.15f, 1f);
                        int shw = Math.round(sw * (0.5f + 0.5f * sk));
                        c.fill(x - shw / 2, (int) p.floor + sh / 2, x + shw / 2, (int) p.floor + sh / 2 + 2, rgbAlpha(0x000000, 0.28f * sk * a));
                    }
                    if (p.glow != 0 && halos < 40 && (p.scale >= 2 || p.beat)) {
                        halos++;
                        float pu = p.beat && !opt.reduceMotion ? 0.45f + 0.55f * (float) Math.pow(Math.max(0, Math.sin(p.age * 6.5 + p.x * 0.05)), 6) : 0.75f;
                        int r = Math.max(sw, sh) / 2 + 5;
                        halo(c, x, y, r, p.glow, 0.13f * a * pu);
                        halo(c, x, y, r * 2 / 3, p.glow, 0.15f * a * pu);
                    }
                    s.draw(c, x - sw / 2, y - sh / 2, p.scale, a, false, false);
                }
                case SPARK -> {
                    float tw = p.twinkle ? 0.55f + 0.45f * (float) Math.sin(p.age * 18f + p.x) : 1f;
                    c.fill(x, y, x + p.size, y + p.size, Sprite.withAlpha(p.color, a * tw));
                }
                case ROCKET -> {
                    c.fill(x, y, x + 2, y + 3, 0xFFFFFFFF);
                    c.fill(x, y + 3, x + 2, y + 5, 0xCCFFB300);
                }
                case FLYBY -> {
                    if (p.anim != null) {
                        Sprite f = p.anim[((int) (p.age * p.fps)) % p.anim.length];
                        if (theme == Theme.HALLOWEEN) halo(c, x + f.w * p.scale / 2, y + f.h * p.scale / 2, f.w * p.scale / 2 + 6, 0xB39DDB, 0.14f);
                        f.draw(c, x, y, p.scale, theme == Theme.HALLOWEEN ? 0.85f : a, false, false);
                    } else {
                        for (int j = 0; j < p.frames.length; j++) {
                            p.frames[j].draw(c, x + p.offX[j], y + p.offY[j], p.scale, a, false, false);
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ drawing: backdrop (behind the panel)

    public void renderBackdrop(Canvas c, int l, int t, int w, int h) {
        if (theme == Theme.NONE || !opt.decorations) return;
        if (lightning > 0f) {
            float la = lightning > 0.47f ? 0.30f : (lightning > 0.38f ? 0f : (lightning > 0.28f ? 0.2f : 0f));
            if (la > 0f) c.fill(0, 0, screenW, screenH, rgbAlpha(0xE8E0FF, la));
        }
        if (beam > 0f && beamTop > 4f) lightBeam(c);
        glow(c, l, t, w, h);
        shockwave(c, l, t, w, h);
        if (theme == Theme.ST_PATRICKS) rainbow(c, l, t, w, 0.8f);
        if (theme == Theme.SPRING) rainbow(c, l, t, w, 0.45f);
        if (theme == Theme.SUMMER) waves(c, l, t, w, h);
    }

    /** Expanding glowing outline around the panel right after it opens. */
    private void shockwave(Canvas c, int l, int t, int w, int h) {
        if (shock > 0.9f || w <= 0) return;
        int col = mix(theme.glowA, theme.glowB, 0.5f);
        for (int ring = 0; ring < 2; ring++) {
            float u = (shock - ring * 0.12f) / 0.75f;
            if (u <= 0f || u >= 1f) continue;
            int e = 4 + Math.round(u * 54f);
            float a = (1f - u) * (1f - u) * 0.85f;
            int cc = rgbAlpha(col, a);
            int th = 3;
            c.fill(l - e, t - e, l + w + e, t - e + th, cc);
            c.fill(l - e, t + h + e - th, l + w + e, t + h + e, cc);
            c.fill(l - e, t - e + th, l - e + th, t + h + e - th, cc);
            c.fill(l + w + e - th, t - e + th, l + w + e, t + h + e - th, cc);
        }
    }

    /** Shafts of light rising out of an opening chest. */
    private void lightBeam(Canvas c) {
        int top = Math.round(beamTop);
        int col = mix(0xFFFFFF, theme.glowB, 0.35f);
        float tt = tt();
        for (int y = 0; y < top; y += 2) {
            float f = y / (float) top;
            float a = beam * (0.10f + 0.34f * f);
            int half = 8 + Math.round((1f - f) * 26f);
            int cx = Math.round(beamX);
            c.fill(cx - half, y, cx + half, y + 2, rgbAlpha(col, a * 0.55f));
            int half2 = half / 2;
            c.fill(cx - half2, y, cx + half2, y + 2, rgbAlpha(0xFFFFFF, a * 0.8f));
            int off = Math.round((1f - f) * 52f);
            int wob = Math.round((float) Math.sin(tt * 3f + y * 0.05f) * 3f);
            c.fill(cx - off - 3 + wob, y, cx - off + 3 + wob, y + 2, rgbAlpha(col, a * 0.5f));
            c.fill(cx + off - 3 - wob, y, cx + off + 3 - wob, y + 2, rgbAlpha(col, a * 0.5f));
        }
    }

    // ------------------------------------------------------------------ menus (title, pause, options...)

    /** Themed dressing for whole-screen menus: edge glow, corner decorations, fringe along the top, ground below. */
    public void renderScreen(Canvas c, int w, int h) {
        if (theme == Theme.NONE || !opt.decorations) return;
        float s = (float) (Math.sin(tt() * 1.1) + 1.0) / 2f;
        int col = mix(theme.glowA, theme.glowB, s);
        float pulse = pulse();
        for (int i = 0; i < 7; i++) {
            float f = 1f - i / 7f;
            int cc = rgbAlpha(col, 0.5f * pulse * f * f);
            int d = i * 2;
            c.fill(d, d, w - d, d + 2, cc);
            c.fill(d, h - d - 2, w - d, h - d, cc);
            c.fill(d, d + 2, d + 2, h - d - 2, cc);
            c.fill(w - d - 2, d + 2, w - d, h - d - 2, cc);
        }
        renderDecor(c, 6, 6, w - 12, h - 12);
    }

    private float pulse() {
        float tt = tt();
        if (opt.reduceMotion) return 0.8f;
        switch (theme) {
            case VALENTINES: {
                double s = Math.sin(tt * 6.0);
                double s2 = Math.sin(tt * 6.0 - 1.0);
                float beat = (float) (Math.pow(Math.max(0, s), 8) * 0.6 + Math.pow(Math.max(0, s2), 8) * 0.4);
                return 0.55f + 0.45f * beat;
            }
            case HALLOWEEN:
                return 0.7f + 0.3f * (float) (Math.sin(tt * 9) * Math.sin(tt * 3.1));
            default:
                return 0.65f + 0.35f * (float) Math.sin(tt * 2.2);
        }
    }

    private void glow(Canvas c, int l, int t, int w, int h) {
        float s = (float) (Math.sin(tt() * 1.1) + 1.0) / 2f;
        int col = mix(theme.glowA, theme.glowB, s);
        float pulse = pulse();
        int rings = 8;
        for (int i = 0; i < rings; i++) {
            float f = 1f - i / (float) rings;
            int cc = rgbAlpha(col, Math.min(1f, (0.72f * pulse + flash * 0.9f) * f * f));
            int d = i * 2;
            c.fill(l - d - 2, t - d - 2, l + w + d + 2, t - d, cc);
            c.fill(l - d - 2, t + h + d, l + w + d + 2, t + h + d + 2, cc);
            c.fill(l - d - 2, t - d, l - d, t + h + d, cc);
            c.fill(l + w + d, t - d, l + w + d + 2, t + h + d, cc);
        }
    }

    private void rainbow(Canvas c, int l, int t, int w, float alpha) {
        float half = w / 2f;
        float cx = l + half;
        for (int x = l; x < l + w; x += 4) {
            float u = (x + 2f - cx) / half;
            float arc = (float) Math.sqrt(Math.max(0f, 1f - u * u));
            int base = t - 2 - Math.round(arc * 14f);
            for (int i = 0; i < RAINBOW.length; i++) {
                int y = base - i * 2;
                if (y < 2) continue;
                c.fill(x, y - 2, x + 4, y, Sprite.withAlpha(RAINBOW[i], alpha));
            }
        }
    }

    private void waves(Canvas c, int l, int t, int w, int h) {
        int base = t + h + 6;
        if (base + 9 > screenH) return;
        float tt = tt();
        for (int x = l - 6; x < l + w + 6; x += 2) {
            int yy = base + Math.round((float) Math.sin(x * 0.22f + tt * 3f) * 1.6f);
            c.fill(x, yy, x + 2, yy + 2, 0xCCFFFFFF);
            c.fill(x, yy + 2, x + 2, yy + 6, 0xAA29B6F6);
            c.fill(x, yy + 6, x + 2, yy + 9, 0x880277BD);
        }
    }

    // ------------------------------------------------------------------ drawing: decorations (above the panel)

    private void corner(Canvas c, Sprite s, int scale, int l, int t, int w, int h, boolean right, boolean bottom,
                        int overhang, boolean mirrorY) {
        int sw = s.w * scale;
        int sh = s.h * scale;
        int x = right ? l + w + overhang - sw : l - overhang;
        int y = bottom ? t + h + overhang - sh : t - overhang;
        s.draw(c, x, y, scale, 1f, right, mirrorY && bottom);
    }

    public void renderDecor(Canvas c, int l, int t, int w, int h) {
        if (theme == Theme.NONE || !opt.decorations) return;
        float tt = tt();
        switch (theme) {
            case HALLOWEEN -> {
                corner(c, Sprites.COBWEB, 2, l, t, w, h, false, false, 3, false);
                corner(c, Sprites.COBWEB, 2, l, t, w, h, true, false, 3, false);
                boolean flick = Math.sin(tt * 8f) > -0.2;
                Sprite pk = flick ? Sprites.PUMPKIN_A : Sprites.PUMPKIN_B;
                corner(c, pk, 2, l, t, w, h, false, true, 8, true);
                corner(c, pk, 2, l, t, w, h, true, true, 8, true);
                int sx = l + (int) (w * 0.75f);
                int spy = t - 10 + Math.round((float) Math.sin(tt * 1.6f) * 4f);
                int top = Math.max(0, t - 38);
                c.fill(sx, top, sx + 1, spy, 0x99FFFFFF);
                Sprites.SPIDER.draw(c, sx - 7, spy, 2, 1f, false, false);
            }
            case CHRISTMAS -> {
                c.fill(l + 2, t - 5, l + w - 2, t - 4, 0xCC1B5E20);
                c.fill(l + 2, t + h + 3, l + w - 2, t + h + 4, 0xCC1B5E20);
                int i = 0;
                for (int x = l + 6; x < l + w - 4; x += 10, i++) {
                    float tw = clamp(0.65f + 0.35f * (float) Math.sin(tt * 4f + i * 1.9f), 0f, 1f);
                    int col = Sprite.withAlpha(BULBS[(i + (int) (tt * 0.5f)) % 4], tw);
                    c.fill(x, t - 4, x + 3, t - 1, col);
                    c.fill(x + 1, t - 5, x + 2, t - 4, 0xFF263238);
                    c.fill(x, t + h + 4, x + 3, t + h + 7, col);
                    c.fill(x + 1, t + h + 3, x + 2, t + h + 4, 0xFF263238);
                }
                corner(c, Sprites.HOLLY, 2, l, t, w, h, false, false, 6, false);
                corner(c, Sprites.CANDY_CANE, 2, l, t, w, h, true, false, 6, false);
                corner(c, Sprites.CANDY_CANE, 2, l, t, w, h, false, true, 6, false);
                corner(c, Sprites.HOLLY, 2, l, t, w, h, true, true, 6, true);
            }
            case NEW_YEARS -> {
                boolean alt = ((int) (tt * 2f)) % 2 == 0;
                Sprite a = alt ? Sprites.SPARKLE_GOLD : Sprites.SPARKLE_SILVER;
                Sprite b = alt ? Sprites.SPARKLE_SILVER : Sprites.SPARKLE_GOLD;
                corner(c, a, 2, l, t, w, h, false, false, 5, false);
                corner(c, b, 2, l, t, w, h, true, false, 5, false);
                corner(c, b, 2, l, t, w, h, false, true, 5, false);
                corner(c, a, 2, l, t, w, h, true, true, 5, false);
            }
            case VALENTINES -> {
                corner(c, Sprites.HEART_PINK, 3, l, t, w, h, false, false, 9, false);
                corner(c, Sprites.HEART_DEEP, 2, l, t, w, h, true, false, 6, false);
                corner(c, Sprites.HEART_LIGHT, 2, l, t, w, h, false, true, 6, false);
                corner(c, Sprites.HEART_RED, 3, l, t, w, h, true, true, 9, false);
            }
            case ST_PATRICKS -> {
                corner(c, Sprites.SHAMROCK, 2, l, t, w, h, false, false, 6, false);
                corner(c, Sprites.SHAMROCK, 2, l, t, w, h, true, false, 6, false);
                corner(c, Sprites.POT_OF_GOLD, 2, l, t, w, h, false, true, 8, false);
                corner(c, Sprites.POT_OF_GOLD, 2, l, t, w, h, true, true, 8, false);
            }
            case SPRING -> {
                corner(c, Sprites.FLOWER_PINK, 2, l, t, w, h, false, false, 8, false);
                corner(c, Sprites.FLOWER_PURPLE, 2, l, t, w, h, true, false, 8, false);
                corner(c, Sprites.FLOWER_WHITE, 2, l, t, w, h, false, true, 8, false);
                corner(c, Sprites.FLOWER_ORANGE, 2, l, t, w, h, true, true, 8, false);
                corner(c, Sprites.LEAF_GREEN, 2, l, t, w, h, false, true, -6, false);
                corner(c, Sprites.LEAF_GREEN, 2, l, t, w, h, true, true, -6, false);
            }
            case SUMMER -> {
                int bob = Math.round((float) Math.sin(tt * 2.5f) * 2f);
                Sprites.BEACH_BALL.draw(c, l - 8, t - 8 + bob, 2, 1f, false, false);
                Sprite sun = ((int) (tt * 2f)) % 2 == 0 ? Sprites.SUN_A : Sprites.SUN_B;
                sun.draw(c, l + w - 14, t - 16, 2, 1f, false, false);
                corner(c, Sprites.STARFISH, 2, l, t, w, h, false, true, 6, false);
                corner(c, Sprites.SHELL, 2, l, t, w, h, true, true, 6, false);
            }
            case THANKSGIVING -> {
                corner(c, Sprites.LEAVES[0][0], 3, l, t, w, h, false, false, 8, false);
                corner(c, Sprites.LEAVES[1][0], 3, l, t, w, h, true, false, 8, false);
                Sprites.LEAVES[2][0].draw(c, l + 6, t - 9, 3, 1f, false, false);
                Sprites.LEAVES[3][0].draw(c, l + w - 21, t - 9, 3, 1f, true, false);
                corner(c, Sprites.PUMPKIN_PLAIN, 2, l, t, w, h, false, true, 8, true);
                corner(c, Sprites.PUMPKIN_PLAIN, 2, l, t, w, h, true, true, 8, true);
            }
            default -> { }
        }
        fringe(c, l, t, w);
        ground(c, l, t, w, h);
    }

    // ------------------------------------------------------------------ panel retheme

    /** Multiply colour for the container panel texture (white = unchanged). Slowly shifts between two shades. */
    public int panelTint() {
        float s = (float) (Math.sin(tt() * 0.8) + 1.0) / 2f;
        int a, b;
        switch (theme) {
            case CHRISTMAS -> { a = 0xA9D2FF; b = 0xCDE6FF; }
            case HALLOWEEN -> { a = 0xB48CE0; b = 0xD6A2F0; }
            case NEW_YEARS -> { a = 0xB4C4FF; b = 0xD2B8FF; }
            case VALENTINES -> { a = 0xFFAECB; b = 0xFFD3DF; }
            case ST_PATRICKS -> { a = 0xA8F0A0; b = 0xD2FFB0; }
            case SPRING -> { a = 0xC9F5B8; b = 0xFFD2E8; }
            case SUMMER -> { a = 0xB5E8FF; b = 0xCDF6FF; }
            case THANKSGIVING -> { a = 0xFFC48A; b = 0xE8A465; }
            default -> { return 0xFFFFFFFF; }
        }
        return 0xFF000000 | mix(a, b, s);
    }

    private static int hash(int x) {
        int h = x * 0x9E3779B1;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return h & 0x7FFFFFFF;
    }

    /** Things hanging from the top edge of the panel: icicles, goo, garlands, bunting, vines, streamers. */
    private void fringe(Canvas c, int l, int t, int w) {
        float tt = tt();
        switch (theme) {
            case CHRISTMAS -> {
                for (int x = l + 1; x < l + w - 1; x += 6) {
                    int bump = hash(x) % 3;
                    c.fill(x, t - 1 - bump / 2, x + 6, t + 3, 0xF5FFFFFF);
                    c.fill(x, t + 3, x + 6, t + 4, 0xFFBBD7F2);
                }
                for (int x = l + 4; x < l + w - 4; x += 4) {
                    int len = 4 + hash(x * 3) % 9;
                    c.fill(x, t + 4, x + 3, t + 4 + len / 2, 0xE8CDE8FF);
                    c.fill(x + 1, t + 4 + len / 2, x + 2, t + 4 + len, 0xE8CDE8FF);
                    c.fill(x, t + 4, x + 1, t + 4 + len / 2, 0xFFFFFFFF);
                }
            }
            case HALLOWEEN -> {
                c.fill(l + 1, t - 1, l + w - 1, t + 3, 0xF0512DA8);
                for (int x = l + 4; x < l + w - 4; x += 5) {
                    int len = 3 + hash(x) % 10;
                    float grow = opt.reduceMotion ? 1f : 0.85f + 0.15f * (float) Math.sin(tt * 2f + x);
                    int ll = Math.round(len * grow);
                    c.fill(x, t + 3, x + 2, t + 3 + ll, 0xF0512DA8);
                    c.fill(x - 1, t + 3 + ll, x + 3, t + 5 + ll, 0xF07E57C2);
                    c.fill(x, t + 3, x + 1, t + 3 + ll, 0xFF9575CD);
                }
            }
            case VALENTINES -> {
                int last = -1;
                for (int x = l + 2; x < l + w - 2; x += 2) {
                    float u = (x - l) / (float) w;
                    int y = t + 2 + Math.round((float) Math.sin(u * Math.PI * 6) * 3f + 3f);
                    c.fill(x, y, x + 2, y + 1, 0xFFF8BBD0);
                    if (x - last >= 14 && x < l + w - 10) {
                        last = x;
                        Sprites.HEARTS[(x / 14) % 4][0].draw(c, x - 2, y + 1, 1, 1f, false, false);
                    }
                }
            }
            case ST_PATRICKS -> bunting(c, l, t, w, new int[] {0xFF2E9E3E, 0xFFFFC107, 0xFFFFFFFF, 0xFF66BB6A});
            case SUMMER -> bunting(c, l, t, w, new int[] {0xFFFF7043, 0xFFFFD54F, 0xFF26C6DA, 0xFFFFFFFF});
            case THANKSGIVING -> bunting(c, l, t, w, new int[] {0xFFE65100, 0xFF8D6E63, 0xFFFFB300, 0xFFBF360C});
            case SPRING -> {
                c.fill(l + 1, t - 1, l + w - 1, t + 2, 0xFF2E7D32);
                for (int x = l + 3; x < l + w - 3; x += 4) {
                    int len = 3 + hash(x) % 10;
                    c.fill(x, t + 2, x + 1, t + 2 + len, 0xFF43A047);
                    c.fill(x + 1, t + 4 + len / 2, x + 3, t + 5 + len / 2, 0xFF66BB6A);
                    if (hash(x + 7) % 3 == 0) {
                        int fy = t + 2 + len;
                        c.fill(x - 1, fy, x + 2, fy + 3, hash(x) % 2 == 0 ? 0xFFF48FB1 : 0xFFFFFFFF);
                        c.fill(x, fy + 1, x + 1, fy + 2, 0xFFFFD54F);
                    }
                }
            }
            case NEW_YEARS -> {
                int[] cols = {0xFFFFD54F, 0xFFE0E0E0, 0xFF64B5F6, 0xFFF06292, 0xFF81C784};
                for (int x = l + 4; x < l + w - 4; x += 5) {
                    int len = 6 + hash(x) % 12;
                    int col = cols[hash(x + 3) % cols.length];
                    for (int y = 0; y < len; y++) {
                        int sx = Math.round((float) Math.sin(tt * 3f + x * 0.3f + y * 0.45f) * (opt.reduceMotion ? 0f : 1.5f));
                        c.fill(x + sx, t + 1 + y, x + sx + 2, t + 2 + y, col);
                    }
                }
            }
            default -> { }
        }
    }

    private void bunting(Canvas c, int l, int t, int w, int[] pal) {
        float tt = tt();
        int i = 0;
        for (int x = l + 3; x < l + w - 9; x += 9, i++) {
            float u = (x - l) / (float) w;
            int y = t + 1 + Math.round((float) Math.sin(u * Math.PI * 3) * 2f + 2f);
            c.fill(x - 1, y - 1, x + 10, y, 0xFF5D4037);
            int sway = opt.reduceMotion ? 0 : Math.round((float) Math.sin(tt * 2f + i) * 1f);
            int col = pal[i % pal.length];
            for (int r = 0; r < 6; r++) {
                c.fill(x + r / 2 + sway, y + r, x + 8 - r / 2 + sway, y + r + 1, col);
            }
        }
    }

    /** Ground along the bottom edge: snow drifts, grass, sand, leaf piles, a snowman. */
    private void ground(Canvas c, int l, int t, int w, int h) {
        int b = t + h;
        boolean full = l <= 6 && w >= screenW - 16;
        if (b + 8 > screenH && !full) return;
        switch (theme) {
            case CHRISTMAS -> {
                mound(c, l, b, w, 0xFFFFFFFF, 0xFFD7E8F7);
                if (full) Sprites.SNOWMAN.draw(c, l + 8, b + 5 - 28, 2, 1f, false, false);
                else if (b - 20 > 0 && l + w + 22 < screenW) Sprites.SNOWMAN.draw(c, l + w + 2, b + 5 - 28, 2, 1f, false, false);
            }
            case SPRING -> {
                mound(c, l, b, w, 0xFF66BB6A, 0xFF2E7D32);
                for (int x = l + 3; x < l + w - 3; x += 5) {
                    int hh = 3 + hash(x) % 5;
                    c.fill(x, b + 1 - hh, x + 1, b + 1, 0xFF81C784);
                }
            }
            case SUMMER -> mound(c, l, b, w, 0xFFFFE082, 0xFFE6C06A);
            case THANKSGIVING -> mound(c, l, b, w, 0xFFE65100, 0xFF8D4A1B);
            case HALLOWEEN -> mound(c, l, b, w, 0xFF3A2455, 0xFF1F1230);
            case ST_PATRICKS -> {
                mound(c, l, b, w, 0xFF43A047, 0xFF1B5E20);
                for (int x = l + 6; x < l + w - 6; x += 14) {
                    int py = b + 1;
                    c.fill(x, py, x + 4, py + 2, 0xFFFFC107);
                    c.fill(x + 1, py - 1, x + 3, py, 0xFFFFE082);
                }
            }
            default -> { }
        }
    }

    private void mound(Canvas c, int l, int b, int w, int top, int body) {
        for (int x = l - 2; x < l + w + 2; x += 2) {
            float u = x * 0.13f;
            int hgt = 3 + Math.round((float) (Math.sin(u) + Math.sin(u * 2.3 + 1)) * 1.4f + 1.4f);
            c.fill(x, b + 4 - hgt, x + 2, b + 5, top);
            c.fill(x, b + 4, x + 2, b + 6, body);
        }
    }
}
