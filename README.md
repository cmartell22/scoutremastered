# Scout26 (temporary working name)

This is a clean, Fabric 26.1.x wearable-bag mod under staged development. P2 provides the immutable per-ItemStack storage core; equipment, menus, networking intents, recipes, and rendering remain later phases. No vanilla inventory GUI mixins are present.

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
