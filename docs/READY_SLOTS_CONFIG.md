# Ready Slots presentation configuration

Ready Slots rendering is configured on each client through:

`config/scoutremastered-ready-slots.json`

The file is created from Scout Remastered's bundled, tested baseline the first time a client starts with RS6. Changes take effect after restarting the client. This configuration controls presentation only: it cannot change bag storage, equipment ownership, ready-slot contents, swaps, networking, or server persistence.

## Failure behavior

- If the external file is missing, the bundled baseline remains active and is copied to the config directory.
- If the external path is not a regular file, the bundled baseline is used.
- If JSON syntax, schema, identifiers, field types, or value ranges are invalid, the entire external file is ignored and left untouched. The bundled baseline is used without partially applying the bad file.
- Unknown or misspelled fields and empty category/item override blocks are invalid, preventing silent no-op edits.
- If the packaged baseline itself is missing or invalid, Ready Slots rendering is disabled. Storage and server-authoritative swapping continue normally.

## Visibility precedence

Visibility is resolved in this order:

1. `item_blacklist` always suppresses the item.
2. `item_whitelist` explicitly selects a transform category for that item, even when that category is absent from `enabled_categories`.
3. Otherwise, the conservative built-in classifier identifies swords/tools, bows, crossbows, shields, or tridents, and `enabled_categories` decides whether that category renders.
4. Items that match none of those paths remain valid bag contents and swap normally, but render nothing.

The supported category IDs are `handheld`, `bow`, `crossbow`, `shield`, and `trident`. Item keys must be complete namespaced IDs such as `minecraft:diamond_sword`.

Example:

```json
"render_policy": {
  "enabled_categories": ["handheld", "bow", "crossbow", "shield", "trident"],
  "item_whitelist": {
    "examplemod:custom_blade": "handheld"
  },
  "item_blacklist": [
    "minecraft:wooden_sword"
  ]
}
```

If an item appears in both lists, the blacklist wins.

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

The parser and future RS7A controls share these inclusive limits:

| Field | Minimum | Maximum |
|---|---:|---:|
| `translate_x`, `translate_y`, `translate_z` | `-4.0` | `4.0` |
| `rotate_x`, `rotate_y`, `rotate_z` | `-360.0` | `360.0` |
| `scale` | `0.01` | `8.0` |

All numbers must be finite. These bounds allow broad placement around custom player and item models while rejecting values such as `5000` that would push a render far outside any useful area.

## Schema stability

`schema_version` is currently `1`. Unsupported schema versions fail as a complete file and use the bundled baseline. The default file is the authoritative example for every required section and base field.

RS7A will provide the interactive editor. RS7B will use deterministic values saved by that GUI for human-guided, one-by-one calibration and promotion into shipped defaults.
