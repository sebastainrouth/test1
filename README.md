# Schem Builder (Fabric, client-side, Minecraft 1.21.11)

A client-only mod that loads Sponge `.schem` files and Litematica `.litematic` files (multi-region ones are merged into one) and builds them for you: bottom layer first, skipping
anything it can't place yet and coming back down to it once it can.

**Status: source only.** It was written without access to Minecraft's jars or the Fabric Maven, so it has
NOT been compiled or run in-game. The schematic loader is tested; everything that touches Minecraft is not.
Expect to fix a few method names on the first build (see "If the build fails").

## Build the .jar
Requirements: JDK 21, Gradle 9.2+ (or generate a wrapper with the Fabric template at
https://fabricmc.net/develop/template/ and copy its `gradlew*` + `gradle/` folder here).

```
gradle build
```
The jar appears in `build/libs/schem-builder-1.0.0.jar` (ignore the `-sources` jar).
Install: Fabric Loader 0.18.1+ and Fabric API for 1.21.11, then drop the jar into `.minecraft/mods`.

## Use
Put `.schem` or `.litematic` files in `.minecraft/schematics/` (created on first launch). In game:

| Command | What it does |
|---|---|
| `/schem list` | list schematics |
| `/schem load <file>` | load one |
| `/schem build` | build with the schematic's minimum corner at your feet |
| `/schem build x y z` | build with the minimum corner at those coordinates (use F3) |
| `/schem stop` / `/schem status` | stop / progress |
| `/schem reach <blocks>` | max placement distance (4.5 normal, up to 6 in exploit mode) |
| `/schem speed <n>` | blocks per tick (default 2) |
| `/schem exploit on|off` | see below |

You walk; the mod places whatever is in reach. When nothing is placeable it tells you the lowest unfinished
layer and the nearest unfinished block. Creative mode spawns the blocks into hotbar slot 9; survival takes
them from your inventory and tells you what you ran out of.

## How it works
- **Bottom-up, skip and return:** each tick it works on the lowest layer in reach that has a placeable block.
  A block with no support yet fails its placement search and is retried every ~0.5 s, so it gets filled
  as soon as its neighbours exist and you are back in range.
- **Correct orientation:** for every block it tries clicked block + face + hit point + yaw + pitch through the
  block's own `getStateForPlacement` and picks the combination that yields the wanted facing/half/axis/
  hinge/slab type/rotation. It then sends that yaw/pitch to the server in a rotation packet before clicking
  (the server decides orientation from its own copy of your rotation), then restores your real rotation.
  Double slabs are built as bottom slab then merged.
- **Exploit mode** (`/schem exploit on`): drops the line-of-sight requirement (vanilla servers never check it)
  and lets reach go to the server's leeway (~5.5 from the eyes), with wider hit offsets. It does NOT make it
  possible to place against thin air; the server rejects clicks on air.

## Know before you use it
- Exploit mode, rotation snapping and max-reach clicks are exactly what anti-cheats flag, and many servers
  ban printer mods. Use it in singleplayer, on your own server, or where it's allowed.
- Not handled: block entity data (chest contents, sign text), entities, chest left/right, redstone `powered`
  states, floating pieces with no neighbour to click (reported, not built), blocks that need sneaking to place
  against interactive blocks (those faces are avoided), and legacy `.schematic` files (re-save those as `.schem`). For `.litematic`, only blocks are read: no tile entities, entities, or pending ticks.
- It won't break wrong blocks in the way; it reports them.

## If the build fails
The code uses Mojang mappings for 1.21.11. Likely friction points: `Inventory.setSelectedSlot`,
`BlockStateParser.parseForBlock`, `ServerboundMovePlayerPacket.Rot` constructor arguments,
`ClipContext` constructor, and the `ResourceLocation`->`Identifier` rename. Paste the compiler errors
back and they are quick fixes.

## GUI (Right Shift or /schem gui)
Press **Right Shift** in game (rebindable in Controls > Schem Builder) or type **/schem gui** for the Schem Builder screen: click a schematic to load it,
set the build origin (or "Use my position"), choose **rotation** (0/90/180/270) and **Mirror X / Mirror Z**,
adjust reach / blocks per tick, toggle exploit mode, then **Build**. Build closes the screen because the builder
only places while no menu is open. Rotation and mirror are applied each time you press Build (no reload needed).
