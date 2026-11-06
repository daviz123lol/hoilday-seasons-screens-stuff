package com.festivescreens.core;

/** All pixel art, drawn as text. See {@link Sprite#of}. */
public final class Sprites {
    private Sprites() {}

    private static Sprite s(String legend, String... rows) {
        return Sprite.of(legend, rows);
    }

    private static Sprite[] arr(Sprite... a) {
        return a;
    }

    // ---------------------------------------------------------------- Halloween
    public static final Sprite BAT_UP = s("p=2B1B45,o=FF8A1F",
            "p.......p",
            "pp.p.p.pp",
            "ppopppopp",
            ".ppppppp.",
            "..p...p..");
    public static final Sprite BAT_DOWN = s("p=2B1B45,o=FF8A1F",
            "..p...p..",
            ".ppp.ppp.",
            "ppopppopp",
            "pp.ppp.pp",
            "p.......p");
    public static final Sprite[] BAT = arr(BAT_UP, BAT_DOWN);

    private static final String[] PUMPKIN_ROWS = {
            "....g....",
            "..oodoo..",
            ".ooodooo.",
            "oyoodooyo",
            "ooooooooo",
            "oyoyoyoyo",
            ".ooyyyoo.",
            "..ooooo.."};

    private static Sprite pumpkin(String eyeHex, boolean face) {
        String[] rows = new String[PUMPKIN_ROWS.length];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = face ? PUMPKIN_ROWS[i] : PUMPKIN_ROWS[i].replace('y', 'o');
        }
        return s("o=EF6C00,d=BF360F,g=2E7D32,y=" + eyeHex, rows);
    }

    public static final Sprite PUMPKIN_A = pumpkin("FFEB3B", true);
    public static final Sprite PUMPKIN_B = pumpkin("FFA000", true);
    public static final Sprite PUMPKIN_PLAIN = pumpkin("FFEB3B", false);

    private static Sprite cobweb() {
        String[] rows = new String[9];
        for (int y = 0; y < 9; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < 9; x++) {
                double d = Math.sqrt(x * x + y * y);
                boolean spoke = x == 0 || y == 0 || x == y || (x == 2 * y && x > 2) || (y == 2 * x && y > 2);
                boolean ring = Math.abs(d - 3.4) < 0.5 || Math.abs(d - 6.3) < 0.5;
                sb.append((spoke || ring) && d < 9.4 ? 'w' : '.');
            }
            rows[y] = sb.toString();
        }
        return s("w=E6E0E0F0", rows);
    }

    public static final Sprite COBWEB = cobweb();
    public static final Sprite SPIDER = s("k=1A1A1A,r=FF1744",
            ".k...k.",
            "k.kkk.k",
            ".krkrk.",
            "k.kkk.k",
            ".k...k.");

    // ---------------------------------------------------------------- Valentine's
    private static Sprite heart(String hex) {
        return s("r=" + hex + ",l=88FFFFFF",
                ".rr.rr.",
                "rlrrrrr",
                "rrrrrrr",
                ".rrrrr.",
                "..rrr..",
                "...r...");
    }

    public static final Sprite HEART_PINK = heart("FF4F7B");
    public static final Sprite HEART_RED = heart("E53935");
    public static final Sprite HEART_LIGHT = heart("FF8FB1");
    public static final Sprite HEART_DEEP = heart("C2185B");
    public static final Sprite[][] HEARTS = {{HEART_PINK}, {HEART_RED}, {HEART_LIGHT}, {HEART_DEEP}};
    public static final Sprite HEART_MINI = s("r=FF4F7B",
            ".r.r.",
            "rrrrr",
            ".rrr.",
            "..r..");

    // ---------------------------------------------------------------- Christmas
    public static final Sprite SNOWFLAKE = s("w=FFFFFF",
            "..w..",
            "w.w.w",
            ".www.",
            "w.w.w",
            "..w..");

    private static Sprite ornament(String hex) {
        return s("r=" + hex + ",y=FFD54F,w=88FFFFFF",
                "..y..",
                ".rrr.",
                "rwrrr",
                "rrrrr",
                ".rrr.");
    }

    public static final Sprite[][] ORNAMENTS = {
            {ornament("E53935")}, {ornament("43A047")}, {ornament("1E88E5")}, {ornament("FFC107")}};

    public static final Sprite HOLLY = s("d=1B5E20,g=43A047,r=E53935",
            ".dd...dd.",
            "dggd.dggd",
            ".dggrggd.",
            "..dgrgd..",
            "...rrr...",
            "....r....");
    public static final Sprite CANDY_CANE = s("r=E53935,w=FFFFFF",
            ".rwr.",
            "r...w",
            "w....",
            "r....",
            "w....",
            "r....",
            "w....");
    public static final Sprite SLEIGH = s("r=D32F2F,w=FFFFFF,f=FFCBA4,b=6D4C41,y=FFC107",
            "....rr..........",
            "...rrrw.........",
            "...ffff.........",
            "..wwwwww..bbbb..",
            "rrrrrrrrrrrrrrr.",
            "rrrrrrrrrrrrrrrr",
            ".yyyyyyyyyyyyyyy",
            "yy.............y");
    public static final Sprite REINDEER = s("n=8D6E63,a=5D4037,R=FF1744",
            ".....a.a.",
            "....nnnn.",
            "nnnnnnnnR",
            "nnnnnnnn.",
            "nnnnnnn..",
            "n.n..n.n.",
            "n.n..n.n.");

    // ---------------------------------------------------------------- New Year's
    private static Sprite sparkle(String hex) {
        return s("y=" + hex,
                "..y..",
                ".yyy.",
                "yyyyy",
                ".yyy.",
                "..y..");
    }

    public static final Sprite SPARKLE_GOLD = sparkle("FFD54F");
    public static final Sprite SPARKLE_SILVER = sparkle("E0E0E0");

    // ---------------------------------------------------------------- St. Patrick's
    public static final Sprite SHAMROCK = s("g=2E9E3E,l=7ED957,s=1B5E20",
            ".gg.gg.",
            "gllgllg",
            ".ggggg.",
            "..ggg..",
            ".ggggg.",
            "gllgllg",
            ".gg.gg.",
            "...s...");
    public static final Sprite COIN_A = s("y=FFC107,Y=FFF59D,d=FF8F00",
            ".yyy.",
            "yYyyy",
            "yyyyy",
            "yyyyd",
            ".ddd.");
    public static final Sprite COIN_B = s("y=FFC107,Y=FFF59D,d=FF8F00",
            ".y.",
            "yYy",
            "yyy",
            "yyd",
            ".d.");
    public static final Sprite COIN_C = s("y=FFC107,Y=FFF59D,d=FF8F00",
            "y",
            "Y",
            "y",
            "y",
            "d");
    public static final Sprite[] COIN = arr(COIN_A, COIN_B, COIN_C, COIN_B);
    public static final Sprite POT_OF_GOLD = s("k=212121,K=424242,y=FFC107,Y=FFF59D",
            "..yYyYy..",
            ".yyyYyyy.",
            "kkkkkkkkk",
            ".kKkkkkk.",
            "kKkkkkkkk",
            "kkkkkkkkk",
            ".kkkkkkk.",
            "..k...k..");

    // ---------------------------------------------------------------- Thanksgiving
    private static Sprite leaf(String hex) {
        return s("o=" + hex + ",b=5D4037",
                "..o..",
                ".ooo.",
                "ooooo",
                ".ooo.",
                "..b..");
    }

    public static final Sprite[][] LEAVES = {
            {leaf("C62828")}, {leaf("EF6C00")}, {leaf("F9A825")}, {leaf("8D6E63")}};
    public static final Sprite ACORN = s("b=5D4037,k=A1887F",
            "bbbb",
            "bbbb",
            ".kk.",
            ".kk.",
            "..k.");
    public static final Sprite TURKEY = s("r=D32F2F,o=F57C00,y=FDD835,n=6D4C41,b=5D4037,k=FFB300,R=E53935",
            "..oyyyo....",
            ".oyrrryo.nk",
            "oyrrrrrynnR",
            "oyrrrrrynn.",
            ".oyrrrynnn.",
            "..oyyynnn..",
            ".....b.b...",
            ".....b.b...",
            "....kk.kk..");

    // ---------------------------------------------------------------- Spring
    private static Sprite petal(String hex) {
        return s("p=" + hex, "pp.", "ppp", ".pp");
    }

    public static final Sprite[][] PETALS = {{petal("FFB7C5")}, {petal("FFFFFF")}, {petal("D1B3FF")}};

    private static Sprite flower(String hex) {
        return s("p=" + hex + ",y=FFD54F,g=43A047,G=2E7D32",
                ".ppppp.",
                "pppyppp",
                "ppyyypp",
                "pppyppp",
                ".ppppp.",
                "...g...",
                "..Gg...",
                "...g...");
    }

    public static final Sprite FLOWER_PINK = flower("FF80AB");
    public static final Sprite FLOWER_WHITE = flower("FFFFFF");
    public static final Sprite FLOWER_PURPLE = flower("B39DDB");
    public static final Sprite FLOWER_ORANGE = flower("FFB74D");
    public static final Sprite LEAF_GREEN = leaf("43A047");

    private static Sprite[] butterfly(String hex) {
        Sprite up = s("p=" + hex + ",k=212121",
                "pp...pp",
                "ppp.ppp",
                "pppkppp",
                ".pp.pp.",
                "..p.p..");
        Sprite down = s("p=" + hex + ",k=212121",
                ".......",
                "..p.p..",
                ".ppkpp.",
                ".pp.pp.",
                "..p.p..");
        return arr(up, down);
    }

    public static final Sprite[][] BUTTERFLIES = {butterfly("BA68C8"), butterfly("FFB74D"), butterfly("4FC3F7")};

    // ---------------------------------------------------------------- Chest
    public static final Sprite CHEST_LID = s("d=5D3A1A,b=8B5A2B,y=FFC107",
            "dddddddddddd",
            "bbbbbbbbbbbb",
            "bbbbyyyybbbb",
            "dddddddddddd");

    // ---------------------------------------------------------------- Summer
    public static final Sprite BEACH_BALL = s("r=E8383D,w=FFFFFF,b=3B8EEA",
            "..rwwb..",
            ".rrwwbb.",
            "rrrwwbbb",
            "rrrwwbbb",
            "rrrwwbbb",
            "rrrwwbbb",
            ".rrwwbb.",
            "..rwwb..");
    public static final Sprite STARFISH = s("o=FF8A65",
            "...o...",
            "..ooo..",
            "ooooooo",
            ".ooooo.",
            ".oo.oo.",
            "oo...oo");
    public static final Sprite SHELL = s("s=FFCCBC,d=FF8A65",
            ".ssss.",
            "ssdsss",
            "sdsdss",
            "sdsdss",
            ".dddd.");

    private static Sprite sun(boolean diagonal) {
        char[][] g = new char[11][11];
        for (int y = 0; y < 11; y++) {
            for (int x = 0; x < 11; x++) {
                double dx = x - 5, dy = y - 5;
                g[y][x] = Math.sqrt(dx * dx + dy * dy) <= 3.3 ? 'y' : '.';
            }
        }
        if (!diagonal) {
            int[][] rays = {{5, 0}, {5, 1}, {5, 9}, {5, 10}, {0, 5}, {1, 5}, {9, 5}, {10, 5}};
            for (int[] r : rays) g[r[1]][r[0]] = 'o';
        } else {
            int[][] rays = {{1, 1}, {2, 2}, {9, 1}, {8, 2}, {1, 9}, {2, 8}, {9, 9}, {8, 8}};
            for (int[] r : rays) g[r[1]][r[0]] = 'o';
        }
        String[] rows = new String[11];
        for (int y = 0; y < 11; y++) rows[y] = new String(g[y]);
        return s("y=FFD54F,o=FFA726", rows);
    }

    public static final Sprite SUN_A = sun(false);
    public static final Sprite SUN_B = sun(true);

    /** Every sprite, for tests and the preview sheet. */
    public static Sprite[] all() {
        return new Sprite[] {
                BAT_UP, BAT_DOWN, PUMPKIN_A, PUMPKIN_B, PUMPKIN_PLAIN, COBWEB, SPIDER,
                HEART_PINK, HEART_RED, HEART_LIGHT, HEART_DEEP, HEART_MINI,
                SNOWFLAKE, ORNAMENTS[0][0], ORNAMENTS[1][0], ORNAMENTS[2][0], ORNAMENTS[3][0],
                HOLLY, CANDY_CANE, SLEIGH, REINDEER, SPARKLE_GOLD, SPARKLE_SILVER,
                SHAMROCK, COIN_A, COIN_B, COIN_C, POT_OF_GOLD,
                LEAVES[0][0], LEAVES[1][0], LEAVES[2][0], LEAVES[3][0], ACORN, TURKEY,
                PETALS[0][0], PETALS[1][0], PETALS[2][0],
                FLOWER_PINK, FLOWER_WHITE, FLOWER_PURPLE, FLOWER_ORANGE, LEAF_GREEN,
                BUTTERFLIES[0][0], BUTTERFLIES[0][1],
                BEACH_BALL, STARFISH, SHELL, SUN_A, SUN_B, CHEST_LID};
    }
}
