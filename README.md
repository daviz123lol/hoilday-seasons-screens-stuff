# Festive Screens

Animated, seasonal container screens for **NeoForge 26.1.2** (Minecraft 26.1.2). Client-side only: nothing to install
on a server, safe to add to or remove from a modpack (including All the Mods 11).

Every container screen gets the theme: inventory, chests, ender chests, crafting tables, **furnaces, blast furnaces,
smokers**, anvils, enchanting tables, shulker boxes, creative inventory, and container screens added by other mods.

| Theme | In season | What you get |
|---|---|---|
| Christmas | Dec 1 - 25 | Santa's sleigh + reindeer fly across on open, snow, bouncing ornaments, blinking lights, holly, candy canes |
| New Year's | Dec 26 - Jan 6 | Fireworks launch and explode, confetti, gold/silver sparkles |
| Valentine's Day | Feb 1 - 15 | Hearts burst out and float up, heartbeat-pulsing glow, tiny hearts over furnace flames |
| St. Patrick's Day | Mar 1 - 18 | Rainbow over the panel, bouncing gold coins, shamrocks, pots of gold |
| Spring | Mar 19 - Jun 14 | Petals, butterflies, flowers in the corners |
| Summer | Jun 15 - Aug 31 | Bouncing beach balls, animated waves, spinning sun, starfish and shells |
| Halloween | Oct 1 - Nov 1 | Pixel bats burst out of the screen when you open **and** close it, spider on a thread, jack-o'-lanterns, cobwebs |
| Thanksgiving | Nov 2 - 30 | Falling leaves, a turkey crossing the screen, acorns, pumpkins |

**Chests get their own animation.** Opening a chest, ender chest, barrel or shulker box pops a wooden lid up off the
panel, flashes golden light out of the border, and shoots themed items out of the top (coins on St. Patrick's Day, bats
and sparks on Halloween, ornaments at Christmas, hearts, petals, beach balls, leaves...).

Outside those windows (most of late January, late February, September) the game looks vanilla. Pick any theme any
time in the config. Lit furnaces, blast furnaces and smokers get themed sparks over their flame.

## Building (two jars)

You need a **JDK 25** (Minecraft 26.1.2 uses Java 25). The first build downloads and decompiles Minecraft, which can take a while.

    ./build-both.sh        # Linux / macOS
    build-both.bat         # Windows

Output goes to `dist/`:

| Jar | Built against | Use it for |
|---|---|---|
| `festivescreens-neoforge-26.1.2.109-1.0.0.jar` | NeoForge 26.1.2.109 | All the Mods 11 and any pack on 26.1.2.109 or newer |
| `festivescreens-neoforge-26.1.2.126-1.0.0.jar` | NeoForge 26.1.2.126 | Newest NeoForge 26.1.2 (requires 26.1.2.126 or newer) |

26.1.2.126 was the newest build I could confirm when this was written. When a newer one is out, change `LATEST_NEO` in
the build script. To build a single jar: `./gradlew build -Pneo_version=26.1.2.109`.
To test in game: `./gradlew runClient`.

No JDK 25 handy? Push the project to your own GitHub repo: the included workflow builds both jars on GitHub's servers
(Actions tab > Run workflow), and you download them from the run's Artifacts.

## Config

In game: **Mods > Festive Screens > Config**. Or `config/festivescreens-client.toml`.

`mode` (AUTO by date, a specific holiday, or OFF), `openCloseAnimations`, `ambientParticles`, `decorations`,
`furnaceEffects`, `otherScreens`, `reduceMotion` (static decorations only), `intensity` (25-200 %).

## Compatibility notes

- Draws on top of the normal screen with additive Mixin injections. It does not replace textures, widgets or
  any other mod's rendering, so it should coexist with JEI/EMI, Jade and modded container screens.
- Do not run it together with another full GUI re-theme mod (e.g. Immersive Screens): both would draw at once.
- If something is wrong, the log says so. Look for `Festive Screens:` in `latest.log`. A hook that fails to apply
  logs a warning instead of crashing, and any runtime error switches the mod off for the session instead of crashing.

## How it is organised

- `core/` - the effects engine and all pixel art. No Minecraft code, so it is unit-testable and portable.
- `client/` + `mixin/` - the thin layer that hooks Minecraft's screen rendering.
- `Settings.java` - the config.

## v1.1 additions
- **Panel re-colour**: container panels (inventory, chests, furnaces...) take the theme's colours (`panelTint` option).
- **Edge dressing**: icicles + snowman, spooky goo, heart garland, bunting, vines, streamers, snow/grass/sand/leaf ground.
- **Theme music**: one looping track per theme while a container is open (`music`, `musicVolume` options). Tracks are
  original synthesized arrangements (public-domain tunes or originals).

## v1.1.0 (bigger update)
- Brand-new music: 8 fully arranged loops (lead, chords, arpeggios, bass, percussion, reverb), ~1-2 minutes each.
- Plays on container screens AND on the title screen / pause / options / world list (`menuMusic`). Vanilla music stays quiet while it plays.
- Title screen and menus are themed too (`menuScreens`): glowing screen edge, corner decorations, icicles/bunting/vines,
  snow/grass/sand ground, falling particles.
- Effects: glowing halos, spinning beach balls with floor shadows, heartbeat-pulsing hearts with trails and a heart ring,
  gift boxes, confetti, fireworks in 3 patterns, Halloween lightning + ghost, Cupid's arrow, seagulls.
- Opening any screen sends a shockwave ring out from the panel; chests also shoot up a beam of light.
