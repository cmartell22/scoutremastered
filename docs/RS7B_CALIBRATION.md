# RS7B Ready Slots default calibration

RS7B promotes only human-accepted values produced by the RS7A editor into
`src/client/resources/assets/scoutremastered/config/ready-slots-default.json`.
The calibration profile is a temporary copy of the bundled baseline; the user's
pre-existing external config is backed up and restored when calibration ends.

## Promotion rules

1. Tune category scope first for `left_hip`, `right_hip`, and `back`.
2. Save through the GUI, close normally, and preserve the deterministic external JSON.
3. Restart with that saved JSON and visually confirm the same placement before promotion.
4. Promote only the accepted category/position patch into the bundled baseline.
5. Add an exact-item override only when a demonstrated model outlier cannot share its category default.
6. Keep every value finite and within translation `-4..4`, rotation `-360..360`, and scale `0.01..8`.
7. Before RS7B completion, remove the calibration override and verify the bundled baseline alone.

Each accepted row must look clean in the paper-doll preview and in third person while
standing, walking, and crouching. Left/right placement must remain distinct; back
placement must avoid body, armor, and cape z-fighting where those interactions apply.

## Locked Minecraft 26.1.2 representatives

The six tool-family memberships below were read from the exact locally resolved
Minecraft 26.1.2 item-tag resources. A diamond item is the primary tuning representative;
copper and netherite are final geometry spot-checks for each material family.

| Category | Primary representative | Covered built-in family |
|---|---|---|
| Swords | `minecraft:diamond_sword` | diamond, stone, golden, netherite, wooden, iron, copper swords |
| Axes | `minecraft:diamond_axe` | diamond, stone, golden, netherite, wooden, iron, copper axes |
| Pickaxes | `minecraft:diamond_pickaxe` | diamond, stone, golden, netherite, wooden, iron, copper pickaxes |
| Shovels | `minecraft:diamond_shovel` | diamond, stone, golden, netherite, wooden, iron, copper shovels |
| Hoes | `minecraft:diamond_hoe` | diamond, stone, golden, netherite, wooden, iron, copper hoes |
| Spears | `minecraft:diamond_spear` | diamond, stone, golden, netherite, wooden, iron, copper spears |
| Bows | `minecraft:bow` | bow class |
| Crossbows | `minecraft:crossbow` | crossbow class |
| Shields | `minecraft:shield` | shield class |
| Tridents | `minecraft:trident` | trident class |

## Acceptance ledger

The human accepted the following effective transforms after GUI Save, normal close,
restart reproduction from the deterministic external JSON, and a final unchanged
restart reproduction. The calibration candidate SHA-256 was
`5E16EC06A75F352860C8D35F6B1A1E8C5F425773A7A0990C78DE7999744B163F`.

The values were promoted in commit `9292fa635e40e019a90d5329b59b9f929fd37bda`.
Each cell records the exact effective transform `(X, Y, Z, RX, RY, RZ, Scale)`.

| Category | Left hip | Right hip | Back |
|---|---|---|---|
| Swords | `(0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.1, -0.1, 0.45, 0, 0, -72, 0.75)` |
| Axes | `(0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.1, -0.1, 0.45, 0, 0, -72, 0.75)` |
| Pickaxes | `(0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.1, -0.1, 0.45, 0, 0, -72, 0.75)` |
| Shovels | `(0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.1, -0.1, 0.45, 0, 0, -72, 0.75)` |
| Hoes | `(0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.1, -0.1, 0.45, 0, 0, -72, 0.75)` |
| Spears | `(0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.1, -0.1, 0.45, 0, 0, -72, 0.75)` |
| Bows | `(0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.1, -0.1, 0.45, 0, 0, -72, 0.75)` |
| Crossbows | `(0.325, 0.755, 0.027, 0, 90, 90, 0.75)` | `(-0.325, 0.755, 0.027, 0, 90, 90, 0.75)` | `(-0.1, -0.1, 0.45, 0, 0, 10, 0.75)` |
| Shields | `(0.325, 0.755, 0.027, 0, -90, -25, 1)` | `(-0.24, 0.755, 0.027, 0, 90, 25, 1)` | `(0, -0.15, 0.3, 0, 180, 10, 1.2)` |
| Tridents | `(0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.325, 0.755, 0.027, 0, 90, 0, 0.75)` | `(-0.1, -0.1, 0.45, 0, 0, -72, 0.75)` |

## Promotion shape

The bundled baseline carries the common transform used by swords, axes, pickaxes,
shovels, hoes, spears, bows, and tridents. Crossbows retain only their three `rotate_z`
patches. Shields retain only their demonstrated position/rotation/scale differences.
There are no exact-item defaults and no legacy `handheld` patch, so legacy user files
remain supported without forcing an obsolete scale beneath new granular defaults.
