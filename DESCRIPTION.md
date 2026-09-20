# AnyFind

**Stop digging through forty chests looking for one stack of iron.**

AnyFind turns your storage room into a searchable index. Press a key, type what you want, and the
mod shows you every item stored around you, how many there are, and exactly which container holds
them — then lights the way there.

---

## How it works

1. **Press Ctrl + F.** A search screen opens and your containers are scanned right then.
2. **Type what you need.** The grid filters as you type, by item name or by id (`oak_log`).
3. **Click the item.** The chests holding it are outlined through the walls, and a trail of
   particles runs along the ground from your feet to the closest one.
4. **Open the chest.** The highlight turns itself off.

No index to rebuild, no block to craft, no recipe to unlock. Install it and press the key.

---

## Features

### Search that matches your storage
- **Live results.** Every search reads your containers at that moment, so what you see is what is
  actually in there — no stale cache after a friend reorganises the room.
- **Item grid** with totals, so you can tell "three stacks" from "three items" at a glance.
- **Filter by name or id**, in whatever language your game is set to.
- **Rescan button** to refresh without closing the screen.
- Reads chests, trapped chests, double chests, barrels and placed shulker boxes. Optionally also
  hoppers, droppers, dispensers, furnaces and anything else with an inventory.
- **Looks inside shulker boxes stored in your chests**, so the pickaxe you packed away still shows up.

### Guidance to the container
- A **box around every chest** holding the item, visible through walls, with the closest one in green.
- A **trail of particles laid on the ground**, flowing towards that chest — it climbs stairs and
  follows drops instead of floating through the air.
- A **column of particles above the chest**, so you spot it the moment you turn the corner.
- Everything switches off when you open the chest, or after a time you choose.

### Your chests, not the world's
- Chests generated inside **dungeons, villages, mineshafts and other structures are left out**, even
  after you have looted them, so village barrels do not drown your own storage in the results.
- **Zones:** mark your storage room once and searching inside it looks at the whole room, whatever
  its size and wherever you are standing in it. Built your base inside a village? Inside a zone,
  everything counts.
- Unopened loot chests are never read, so their loot is not rolled early.

### Built to sit quietly in a modpack
- **No mixins.** The game's classes are not patched at all; everything goes through Fabric API
  events, which removes the most common source of mod conflicts.
- **No background work.** The server only scans when you open the search. No per-tick tasks, no
  cache to maintain.
- **No blocks, items or recipes.** Nothing is added to the registries and world generation is
  untouched, so removing the mod leaves your world exactly as it was.
- Hard caps on scan size and packet size keep a huge storage room from hitting the server.

---

## Commands

| Command | What it does |
|---|---|
| `/anyfind scan [radius] [includeStructures]` | Scans and prints a summary in chat |
| `/anyfind zone create <name> [radius]` | Zone around you |
| `/anyfind zone create <name> <pos1> <pos2>` | Zone between two corners |
| `/anyfind zone list` | Lists the zones of this dimension |
| `/anyfind zone here` | Tells you which zone you are standing in |
| `/anyfind zone remove <name>` | Deletes a zone |

Find eight or more containers with no zone around them and the chat offers to save them as one —
a suggestion only, never created behind your back.

---

## Configuration

Everything is configurable from **Mod Menu**, or in `config/anyfind.json`:

- **Scan radius** — 16 to 128 blocks (ignored inside a zone, which is scanned whole).
- **Held key** — Ctrl, Alt, Shift or none, paired with a key you can rebind in the game's controls.
- **Open from inventories** — whether the shortcut also works with a chest open.
- **Ignore structures** — leave dungeon and village containers out.
- **Other containers** — include hoppers, furnaces and the like.
- **Look inside shulkers** — count items nested in stored shulker boxes.
- **Highlight chests / particle path / marker over the chest** — turn each cue on or off.
- **Highlight duration** — 15 seconds to 5 minutes.

---

## Requirements

- Minecraft **26.3**, Fabric Loader **0.19.5+**
- **Fabric API** (required)
- **Mod Menu** (optional, for the settings screen)

Works in singleplayer, on LAN worlds and on servers, including worlds hosted through Essential.
The scan runs on the server side, so whoever hosts the world needs the mod installed; a client
without it simply does not get the feature, and a client with it will say so clearly if the server
does not have it.

---

## Good to know

- Only **loaded chunks** are scanned. The mod never forces chunks to load, so containers in a part
  of the world the server is not keeping loaded will not appear.
- Items are grouped **by type**, so every enchanted book counts together regardless of enchantment.
- The trail shows you the **direction**, not a calculated route: it goes through a wall rather than
  around it.
- Results are a snapshot. If something changes after the scan, hit **Scan** again.

---

*Not affiliated with Mojang or Microsoft.*

---

## License

MIT, see the LICENSE file in the repository. Free to use, modify and redistribute,
including in modpacks, as long as the copyright notice is kept.
