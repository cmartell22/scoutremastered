# Ready Slots presentation configuration

Ready Slots rendering is configured on each client through:

`config/scoutremastered-ready-slots.json`

The file is created from Scout Remastered's bundled, tested baseline the first time a client starts. RS7A can edit it in game through the **Open Ready Slots Editor** key binding (`O` by default). This configuration controls presentation only: it cannot change bag storage, equipment ownership, ready-slot contents, swaps, networking, or server persistence.

## Interactive editor

Open the editor while in a world. Its player model is a live preview of the current local player, including equipped Scout bags and their ready items.

- **Transform** mode edits the required Default Position, a category override, or an exact-item override for `left_hip`, `right_hip`, and `back`.
- Base edits act as global adjustments: existing override fields move by the same translation/rotation delta or scale ratio, so a category or exact-item override cannot silently mask the live control.
- Every slider has a numeric field for precise entry. Both paths enforce the same limits as the parser.
- **Reset** restores a base position from the bundled baseline or removes the selected category/item override.
- **Copy** and **Paste** transfer a complete transform. Each X/Y/Z and RX/RY/RZ row has its own **M** button, so only that field is mirrored. RZ advances by a single 90-degree step (`180` to `90`) instead of producing an equivalent but confusing `-180` value. Scale has no mirror axis.
- Category scope includes a **Visible** checkbox. Exact-item scope labels its category control explicitly; changing that control assigns the listed item to that whitelist/transform category.
- **Whitelist / Blacklist** mode contains two list tabs. Each tab has a scrollable removable selector list, **Clear**, and **Reset to default**. A selector may be an item ID present in the current game's item registry or a registered item tag written as `#namespace:path`. Adding a selector keeps the two lists mutually exclusive.
- The preview does not follow the mouse. Left/right/up/down buttons rotate it in fixed steps and **Reset** returns it to the forward view.
- **Save** atomically replaces only `config/scoutremastered-ready-slots.json` with deterministic, stable-order JSON.
- **Cancel** or Escape restores the exact configuration active when the editor opened and writes nothing.

Live preview only changes client rendering. It does not mutate inventory or ready-slot contents. To preview an exact item on the model, place that item in the corresponding equipped bag's ready slot.

## Failure behavior

- If the external file is missing, the bundled baseline remains active and is copied to the config directory.
- If the external path is not a regular file, the bundled baseline is used.
- If JSON syntax, schema, identifiers, field types, or value ranges are invalid, the entire external file is ignored and left untouched. The bundled baseline is used without partially applying the bad file.
- Unknown or misspelled fields and empty category/item override blocks are invalid, preventing silent no-op edits.
- If the packaged baseline itself is missing or invalid, Ready Slots rendering is disabled. Storage and server-authoritative swapping continue normally.

## Visibility precedence

Visibility is resolved in this order:

1. Any exact-item or item-tag selector in `item_blacklist` suppresses the item.
2. An exact item in `item_whitelist` selects its configured transform category.
3. Otherwise, matching item-tag whitelist selectors are evaluated in lexical order and the first match selects its configured category. Explicit whitelist selection applies even when that category is absent from `enabled_categories`.
4. Otherwise, the conservative built-in classifier identifies swords, axes, pickaxes, shovels, hoes, spears, bows, crossbows, shields, or tridents, and `enabled_categories` decides whether that category renders.
5. Items that match none of those paths remain valid bag contents and swap normally, but render nothing.

The editable category IDs are `sword`, `axe`, `pickaxe`, `shovel`, `hoe`, `spear`, `bow`, `crossbow`, `shield`, and `trident`. The legacy `handheld` ID remains accepted: its transform is applied beneath granular handheld categories, and its enabled state is expanded when one of those granular visibility controls is first edited. Policy selectors are complete namespaced item IDs such as `minecraft:diamond_sword` or registered item tags such as `#examplemod:tools`. Item overrides remain exact-item-only.

Example:

```json
"render_policy": {
  "enabled_categories": ["sword", "axe", "pickaxe", "shovel", "hoe", "spear", "bow", "crossbow", "shield", "trident"],
  "item_whitelist": {
    "examplemod:custom_blade": "sword",
    "#examplemod:tools": "pickaxe"
  },
  "item_blacklist": [
    "minecraft:wooden_sword"
  ]
}
```

If an item matches both lists, the blacklist wins. The bundled lists remain empty because Minecraft's built-in tool tags already feed the granular classifiers; tag selectors are intended for policy exceptions and mod interoperability.

## Transform precedence

Each rendered item resolves one complete transform in this order:

1. `base_transforms` supplies the required transform for `left_hip`, `right_hip`, or `back`.
2. `category_overrides` replaces any fields specified for that category and position.
3. `item_overrides` replaces any fields specified for that exact item and position.

Patches are field-by-field. Unspecified fields retain the value from the preceding level. An empty patch is invalid. An item override does not make an otherwise unsupported item visible; add that item to `item_whitelist` as well.

Example:

```json
"item_overrides": {
  "examplemod:custom_blade": {
    "left_hip": {
      "translate_y": 0.80,
      "rotate_x": 12.0,
      "scale": 0.55
    },
    "back": {
      "scale": 0.76
    }
  }
}
```

Transforms are applied as translation, then X/Y/Z rotations in degrees, then uniform scale. Hip transforms retain the RS5 root/pelvis anchor; back transforms retain the animated chest anchor.

## Value limits

The parser and RS7A controls share these inclusive limits:

| Field | Minimum | Maximum |
|---|---:|---:|
| `translate_x`, `translate_y`, `translate_z` | `-4.0` | `4.0` |
| `rotate_x`, `rotate_y`, `rotate_z` | `-360.0` | `360.0` |
| `scale` | `0.01` | `8.0` |

All numbers must be finite. These bounds allow broad placement around custom player and item models while rejecting values such as `5000` that would push a render far outside any useful area.

## Schema stability

`schema_version` is currently `1`. Unsupported schema versions fail as a complete file and use the bundled baseline. The default file is the authoritative example for every required section and base field.

RS7B remains separate work. It will use deterministic values saved by this GUI for human-guided, one-by-one calibration and promotion into shipped defaults.
