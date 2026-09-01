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

`PENDING` means no default value has been accepted or promoted. A completed cell records
the acceptance commit and the exact effective transform `(X, Y, Z, RX, RY, RZ, Scale)`.

| Category | Left hip | Right hip | Back |
|---|---|---|---|
| Swords | PENDING | PENDING | PENDING |
| Axes | PENDING | PENDING | PENDING |
| Pickaxes | PENDING | PENDING | PENDING |
| Shovels | PENDING | PENDING | PENDING |
| Hoes | PENDING | PENDING | PENDING |
| Spears | PENDING | PENDING | PENDING |
| Bows | PENDING | PENDING | PENDING |
| Crossbows | PENDING | PENDING | PENDING |
| Shields | PENDING | PENDING | PENDING |
| Tridents | PENDING | PENDING | PENDING |

## First batch

Calibrate Swords with `minecraft:diamond_sword` in category scope at all three positions.
After Save and restart reproduction, also spot-check `minecraft:copper_sword` and
`minecraft:netherite_sword`. Do not tune Axes until the Swords values are captured and
promoted or explicitly left pending.
