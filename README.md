# Scout26 (temporary working name)

This is a clean, Fabric 26.1.x wearable-bag mod under staged development. P6 adds presentation to the immutable per-ItemStack storage, Trinkets Updated equipment discovery, and dedicated synchronized Pack Inventory menu hardened through P5. No vanilla inventory GUI mixins are present.

The temporary identifiers are `scout26` and `com.example.scout26`. They are not release identifiers; the final public name, mod ID, Java package, and license remain open project decisions.

## Prerequisites

- Java 25 JDK
- Internet access for Gradle dependency resolution

## Build

```powershell
$env:JAVA_HOME = 'C:\\Program Files\\Microsoft\\jdk-25.0.4.7-hotspot'
$env:Path = "$env:JAVA_HOME\\bin;$env:Path"
.\\gradlew.bat clean build
```

## P2 storage semantics

- Each bag ItemStack owns a `scout26:bag_contents` typed data component.
- Component values contain immutable `ItemStackTemplate` snapshots and return new ItemStacks on read.
- Capacity comes from the concrete bag item: satchel 9, upgraded satchel 18, pouch 3, upgraded pouch 6.
- Negative, excessive, unknown-version, nested-bag, and over-capacity data fails closed or is normalized.
- Bag contents are persistent and network-synchronized using the Minecraft 26.1.2 component codecs.
- Vanilla `ItemStack.copy()` intentionally copies a bag's contents. The copies share an immutable value initially and diverge safely when either bag is mutated. In creative mode this means copying a non-empty bag also copies its contents by design.

## P3 equipment semantics

- Satchels equip only in Trinkets Updated's built-in `chest/back` slot.
- Pouches equip only in `legs/belt`; Scout26 requests two belt indices, where index 0 is left and index 1 is right.
- Server-side discovery returns bags in stable satchel, left-pouch, right-pouch order.
- An equipped-bag handle re-resolves its live Trinkets slot and fails closed if that slot is rebuilt, emptied, or replaced by another ItemStack.
- Equipping and unequipping never extracts bag contents; the physical equipped ItemStack remains their owner.

## P4 pack menu semantics

- Press the configurable Open Pack Inventory key, initially `B`, to request the menu.
- The C2S payload is empty intent only. The server rejects invalid player/menu state and re-discovers equipped bags through Trinkets Updated.
- Slots are ordered satchel, left pouch, right pouch, player inventory, then hotbar.
- Player-to-bag shift-click first merges compatible stacks across bags in that order, then fills empty eligible slots in the same order.
- Bag-to-player shift-click fills the main inventory before the hotbar.
- Every menu action requires every captured equipped-bag handle to remain live. Replacement, unequip, or Trinkets inventory rebuild invalidates the menu and causes it to close safely.
- Bag slots reject bag items and any stack carrying bag contents through normal clicks, drag, number-key swap, and shift-click.

## P5 lifecycle hardening

- Persistent and streamed entries are normalized to the concrete item's actual maximum stack size; malformed non-positive or ineligible entries are discarded.
- Server-backed Pack Inventories become invalid for dead, removed, disconnected, or spectator players as well as stale equipped-bag handles.
- Every accepted bag-slot mutation replaces the physical equipped ItemStack's component immediately; closing the menu is not a write-back boundary.
- Vanilla death, respawn, logout, and restart behavior follows the physical equipped ItemStack managed by Trinkets Updated. A Trinkets inventory rebuild creates new physical stacks and invalidates every previously captured menu handle.

## P6 presentation

- Bag tooltips summarize occupied slots and show up to five stored stacks without mutating bag contents.
- Equipped satchels render on the back and belt-indexed pouches render separately at the left and right waist in third person.
- Wearable rendering is client-only and reuses the normal item models and textures as an intentional placeholder. Custom wearable geometry is deferred to a possible future visuals release; body-worn bags are intentionally not rendered over first-person arms.
- Base pouches use leather and string; base satchels add a chest. Gold-ingot transmute recipes upgrade either bag while preserving the original ItemStack components, including all stored contents.

## Optional Integrated Inventory status

P7-A architecture is documented in [ADR-012](docs/adr/ADR-012-p7a-integrated-inventory-architecture.md). No Integrated Inventory prototype is implemented yet: the proposed feature remains default-disabled, the existing B-key Pack Menu is unchanged, and P7-B requires separate authorization.
