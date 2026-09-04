# Scout Remastered

Scout Remastered is a Fabric mod for Minecraft 26.1.2 that adds wearable satchels and belt pouches. Each bag owns its own contents, so its inventory stays with the physical ItemStack when the bag is moved, equipped, dropped, copied in Creative mode, upgraded, or recovered after death.

This is release candidate `1.0.0-rc.1`.

## Requirements

- Minecraft `26.1.2`
- Java 25
- Fabric Loader `0.19.3` or later
- Fabric API `0.155.2+26.1.2` or later
- Trinkets Updated `4.0.0-beta.3+26.1` or later

Install Scout Remastered, Fabric API, and Trinkets Updated on both the client and dedicated server. Clients and servers must use compatible Scout Remastered versions.

## Installation

1. Install Fabric Loader for Minecraft 26.1.2.
2. Put the Scout Remastered JAR, Fabric API JAR, and Trinkets Updated JAR in the instance or server's `mods` directory.
3. Start Minecraft or the dedicated server once. No server-side configuration is required.

## Using bags

- Equip a satchel in Scout Remastered's dedicated `chest/lower_back` Trinkets slot.
- Equip pouches in the dedicated `legs/left_hip` and `legs/right_hip` Trinkets slots.
- Trinkets Updated's shared `chest/back` and `legs/belt` slots remain available for items from other mods.
- Press `B` to open the Pack Inventory. The key is configurable through Minecraft's Controls menu.
- The Pack Inventory shows satchel, left pouch, right pouch, player inventory, then hotbar. Player shift-clicking merges/fills bags in that same role order; bag shift-clicking fills the player inventory before the hotbar.
- Bags cannot be stored inside bags. This is enforced for clicks, drag actions, quick-move, number-key/offhand swaps, and double-click collection.

## Integrated Inventory (optional)

The separate B-key Pack Inventory is always available and is the default experience. The optional Integrated Inventory display is **disabled by default**.

After the client has started once, set the following property and restart the client:

```properties
# config/scoutremastered-client.properties
integrated_inventory_enabled=true
```

When enabled in a survival-style inventory, the feature displays active bag slots around the vanilla inventory. It remains dormant in Creative inventory screens. The server remains authoritative for every mutation, and replacing or rebuilding an equipped bag while the screen is open invalidates its old view safely; close and reopen the inventory to bind the replacement.

## Ready Slots presentation configuration

Ready Slots render bag-local slot 0 at the left hip, right hip, or back. On first client start, Scout Remastered creates `config/scoutremastered-ready-slots.json` from the bundled tested defaults. The configurable **Open Ready Slots Editor** key (`O` by default) opens a live client-only editor for bounded position, rotation, scale, category visibility, and exact-item whitelist/blacklist rules. The same deterministic JSON remains directly inspectable and editable outside the game.

Invalid or out-of-range external configuration falls back to the complete bundled baseline and cannot affect storage or server-authoritative swapping. See [docs/READY_SLOTS_CONFIG.md](docs/READY_SLOTS_CONFIG.md) for the schema, precedence, limits, and examples.

## Compatibility and limitations

- Scout Remastered targets Fabric only; Forge and NeoForge are not supported.
- There is no migration from the temporary pre-release `scout26` namespace or from historical Scout/Scout-Recrafted data. Do not use the release candidate to upgrade worlds that contain those development-only bags without a backup.
- When upgrading from `1.0.0-rc.1`, unequip Scout bags before replacing the mod. RC1 used the shared `chest/back` slot and expanded `legs/belt` to two entries; the dedicated-slot update returns those generic Trinkets slots to their normal ownership and capacity.
- The optional Integrated Inventory can occasionally paint synchronized high-capacity bag items a frame late immediately after opening. Contents and input remain server-authoritative; the B-key Pack Inventory remains available.
- Ready Slots use each item's ordinary fixed-display model. Extremely unusual third-party models may need a client-side transform override.

## Building from source

```powershell
$env:JAVA_HOME = 'C:\\Program Files\\Microsoft\\jdk-25.0.4.7-hotspot'
$env:Path = "$env:JAVA_HOME\\bin;$env:Path"
.\\gradlew.bat clean build --no-daemon --console=plain
```

The production JAR is written to `build/libs/scoutremastered-1.0.0-rc.1.jar`.

## License and attribution

Scout Remastered is licensed under the [MIT License](LICENSE). It is a clean implementation inspired by the gameplay of Scout-Recrafted; no Scout-Recrafted source code has been copied into this project. Scout-Recrafted is separately licensed under LGPL-2.1.

See [CHANGELOG.md](CHANGELOG.md) for release notes and [docs/P7B_MANUAL_ACCEPTANCE.md](docs/P7B_MANUAL_ACCEPTANCE.md) for the completed integrated-inventory acceptance record.
