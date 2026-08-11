# ADR-012: Bounded integrated-inventory prototype architecture

- Date: 2026-08-11
- P7-A status: Accepted architecture
- P7-B status: Authorized and implemented as a default-disabled prototype; manual acceptance pending
- Decision: Proceed only with a bounded, default-disabled prototype under the constraints below.

## Context

Scout26 already has a server-authoritative Pack Menu whose slots operate on immutable contents stored on the physical equipped bag `ItemStack`. P7 explores an optional classic layout that places those same bag slots around the vanilla player inventory. It must not introduce a second content store, trust client state, retarget a stale click to a replacement bag, or make the Pack Menu depend on vanilla-screen mixins.

The P7-A review used the exact locally resolved Minecraft 26.1.2 common and client source jars, Fabric Screen API from Fabric API 0.155.2+26.1.2, Fabric Loader's Mixin 0.8.7 implementation, Trinkets Updated 4.0.0-beta.3+26.1, and Scout-Recrafted commit `92d39facb151666c1b1b157d279fe01c78037c0e` as a behavior/risk reference. No Scout-Recrafted implementation code is copied by this ADR.

## Source findings that constrain the design

1. `Player` constructs one `InventoryMenu` on both logical sides and assigns it as container ID 0. Survival inventory clicks therefore already travel through `MultiPlayerGameMode.handleContainerInput`, `ServerboundContainerClickPacket`, and `ServerGamePacketListenerImpl.handleContainerClick`.
2. `ServerboundContainerClickPacket` permits up to 128 changed slots. Vanilla `InventoryMenu` has 46 slots; adding the fixed maxima of 18 satchel, 6 left-pouch, and 6 right-pouch slots yields 76.
3. `AbstractContainerMenu.addSlot` extends the normal last-slot and remote-slot tracking lists. Identical constructor-time slot ordering on both sides therefore participates in vanilla state-ID, hash, prediction, and resynchronization behavior.
4. `AbstractContainerScreen` renders and hovers only slots whose `isActive()` is true, but server click handling does not treat `isActive()` as an authorization check. Every integrated slot must also fail closed through `mayPlace`, `mayPickup`, and its bound container.
5. `InventoryMenu.quickMoveStack` naturally routes an appended slot back to player slots 9–44, but it does not route player slots into appended bag slots. A narrow quick-move injection is required for player-to-bag priority.
6. Creative inventory does not use the survival container-click path for its inventory tab. It delegates local clicks to `player.inventoryMenu` and sends `ServerboundSetCreativeModeSlotPacket`, whose server handler accepts only vanilla indices 1–45. Exposing mutable integrated slots there would produce client-only mutations.
7. `ClientPacketListener` handles container ID 0 updates against `player.inventoryMenu`, even while the creative screen is visible. Fixed identical slot counts avoid the historical out-of-range packet workaround.
8. Fabric Screen API supplies `AFTER_INIT`, per-screen `remove`, and background/extraction events. Screen lifecycle and panel drawing do not require dedicated lifecycle or packet-handler mixins.
9. Mixin constructor injection is supported only at `RETURN`; callback injection with explicit `require` counts fails deterministically when an expected target disappears.

## Rejected alternatives

### Client-only fake slots

A visual overlay without real menu slots cannot reuse the vanilla click protocol. Custom click packets would have to reproduce pickup, split, drag, double-click, hotbar swap, throw, and quick-craft semantics. Rejected because it duplicates the most failure-prone inventory logic.

### Replacing the inventory with a custom extended menu

`InventoryMenu` hardcodes container ID 0 and owns vanilla crafting, armor, offhand, recipe-book, and quick-move behavior. A replacement menu would need to copy or reimplement those rules and intercept a screen that normally opens only on the client. Rejected as a broader and less compatible architecture than bounded augmentation.

### Resolving whichever bag is current during each mutation

Retargeting a click to the current bag role would allow a client action based on an old equipped bag to mutate a replacement bag. Rejected because exact `EquippedBagHandle` identity remains mandatory before mutation.

### Reusing Scout-Recrafted's full mixin topology

The reviewed reference uses eight mixins, including global screen-handler, player-inventory, server-player death, client packet-handler, and recipe-book patches. Some exist to repair dynamic backing and creative synchronization behavior. Rejected because Scout26's immutable physical-stack storage, existing lifecycle behavior, and the 26.1.2 container-ID-0 path permit a smaller boundary.

## Decision

### 1. Fixed dormant slot topology

Append exactly 30 slots to every `InventoryMenu`, on client and server, at constructor `RETURN`:

| Menu indices | Role | Maximum | Classic position |
| --- | --- | ---: | --- |
| 46–63 | Satchel | 18 | Up to two 9-slot rows below the vanilla inventory |
| 64–69 | Left pouch | 6 | Up to two 3-slot columns on the left |
| 70–75 | Right pouch | 6 | Up to two 3-slot columns on the right |

The slots always exist so client/server list sizes and indices cannot differ because of configuration or timing. They start dormant, return empty, reject insertion/removal, and do not render. Concrete bag capacity determines which prefix of each role becomes active after binding; serialized contents never determine capacity.

Disabled mode has a small protocol overhead from dormant slots, but no visible panel and no integrated mutation path. The independent B-key Pack Menu remains unchanged and mixin-free.

### 2. Explicit integrated-inventory session

The client feature flag defaults to false. When enabled, Fabric Screen API observes a survival `InventoryScreen` initialization and sends an open-session intent. The server validates that the player is alive, connected, non-spectating, in survival-style inventory mode, and using `inventoryMenu` as the current menu. It then captures the currently equipped bag handles in stable satchel/left/right order.

The server sends an acknowledgement containing only server-derived role capacities and then sends the full container-ID-0 state. Network ordering must activate the matching client bindings before slot contents arrive. The client captures its synchronized equipped stacks and enables only roles whose item kind and capacity agree with the acknowledgement.

Screen removal sends a close-session intent and clears client bindings immediately. Server close, disconnect, death, spectator transition, or handle invalidation clears or invalidates server bindings. Open/close payloads express intent and session metadata only; they never carry or mutate bag contents.

No automatic retargeting is allowed. If an equipped stack is replaced or Trinkets rebuilds its inventory while the screen is open, the captured handle becomes invalid, all slots for that role fail closed, and the panel becomes inactive. The player must close and reopen the inventory to bind the replacement physical stack.

### 3. Reuse the proven storage boundary

P7-B must extract the private `PackMenu.BagSlot` policy into a package-level `BagStorageSlot` used by both menus. Nested-bag rejection, slot capacity, pickup validity, and immediate persistence remain centralized.

Each integrated role owns an optional binding that wraps the existing `BagContainer(EquippedBagHandle)`. Its working list and every persistence call retain the exact captured handle check already tested through P5. Deactivation discards only the adapter; it never copies contents out of or writes contents back to another stack.

Quick-move priority remains satchel, left pouch, right pouch. Shared helpers should centralize merge-before-empty routing so P7 does not create a divergent storage policy.

### 4. Creative behavior is deliberately excluded

Integrated slots remain dormant whenever the player has infinite materials or a creative screen is active. P7-B must not widen `ServerboundSetCreativeModeSlotPacket` handling, mix into the client packet listener, or simulate creative bag writes locally.

Creative players retain the B-key Pack Menu, whose normal synchronized container protocol and P5 creative-copy behavior remain authoritative. P7-B acceptance must verify that the creative inventory tab, trash slot, cloned stacks, equipment changes, and Pack Menu do not mutate dormant integrated slots or desynchronize the equipped bag.

### 5. UI layout and Fabric events

Use the classic arrangement: satchel rows below the vanilla panel, left pouch columns on the left, and right pouch columns on the right. Fabric Screen API owns session start/stop and background panel drawing. Active vanilla `Slot` objects continue to supply all item, hover, tooltip, drag, number-key, Q/Ctrl+Q, and double-click rendering/input behavior.

The layout must account for three vanilla behaviors:

- external panel clicks must not be classified as outside clicks;
- an open recipe book must shift far enough to preserve the left pouch panel;
- active-effect panels must start beyond the right pouch panel.

Narrow recipe-book overlay mode already suppresses underlying container input in `AbstractRecipeBookScreen`; it must be manually reverified with every equipment combination.

## Authorized Mixin surface for P7-B

No `@Overwrite` is authorized. Every injector must use an exact descriptor where practical, `require = 1` per expected match, and a comment naming this ADR.

| Mixin target | Injection | Reason | Failure mode | Version risk |
| --- | --- | --- | --- | --- |
| `InventoryMenu` | constructor `RETURN`; `quickMoveStack` `HEAD` (conditional/cancellable) | Add the identical fixed slots on both sides and route eligible player stacks into active bags | Slot count/order mismatch can break container synchronization; incorrect interception can alter vanilla shift-click | High; common protocol boundary |
| `AbstractContainerScreen` | `hasClickedOutside` `HEAD` or `RETURN`, conditional on active `InventoryScreen`; expose protected layout coordinates through a Scout26 interface | Keep external active panels inside the click region and let the Screen API renderer locate them | A carried stack could be thrown when a panel is mistaken for outside | Medium; client input boundary |
| `RecipeBookComponent` | `updateScreenPosition` `RETURN`, conditional on active `InventoryMenu` binding | Reserve the active left-pouch width without replacing recipe-book behavior | Visual overlap or unreachable pouch slots | Medium; presentation only |
| `EffectsInInventory` | redirect the `imageWidth` read in `canSeeEffects` and `extractRenderState`, conditional on active `InventoryScreen` binding | Offset effects beyond the active right-pouch width | Visual overlap; no content mutation | Medium; presentation only |

The following targets are explicitly forbidden unless P7-A is reopened with new evidence:

- `ServerGamePacketListenerImpl` or creative-slot validation;
- `ClientPacketListener` container synchronization;
- `AbstractContainerMenu` global click/copy behavior;
- `Player`, `ServerPlayer`, player inventory, death, respawn, or logout lifecycle;
- Trinkets internals;
- replacement of `InventoryScreen` or `InventoryMenu`;
- any second authoritative bag store or custom packet that directly changes a slot.

## Feature-flag behavior

The user-facing integrated-inventory flag is client-side, persisted, and false by default. Disabled clients never request a session. The server need not persist preference state; it only keeps the currently validated binding on that player's existing `InventoryMenu`.

The fixed dormant slot topology is always installed to keep protocol symmetry. Therefore “disabled returns to the Pack Menu experience” means no visible integrated panels, no active integrated slots, no altered quick-move routing, and unchanged B-key Pack Menu behavior—not that Mixin bytecode is unloaded at runtime.

## P7-B implementation order

1. Add tests for fixed ranges, layout math, binding activation/deactivation, capacity prefixes, stale-handle failure, and nested-bag rejection.
2. Extract the shared bag-slot policy without behavior changes; rerun the P5/P6 suite.
3. Add dormant fixed slots through `InventoryMenuMixin`; prove identical count/order and disabled no-op behavior on both logical sides.
4. Add the open/ack/close session protocol and exact-identity bindings.
5. Add survival normal-click interactions, then shared quick-move routing.
6. Add Screen API panel rendering and the three presentation/input compatibility mixins.
7. Run the complete interaction, lifecycle, creative, multiplayer, recipe-book, effects, disabled-mode, client, and dedicated-server matrices.

## Required P7-B verification

- Disabled flag: vanilla inventory behavior and B-key Pack Menu remain unchanged.
- No bag, each single role, both pouches, and all roles; base and upgraded capacities.
- Normal click, right-click split, drag, double-click, shift-click both directions, number-key/offhand swap, Q/Ctrl+Q, and every nesting attempt.
- Equip, unequip, replace, swap pouch ordering, and Trinkets rebuild while open; stale roles fail closed and never target replacements.
- Death with both keep-inventory modes, respawn, disconnect, rejoin, server restart, and client restart.
- Creative inventory/trash/clone/equipment tests confirm integrated slots stay dormant and Pack Menu remains correct.
- Recipe book closed/open in wide and narrow layouts; active effects with both right-pouch tiers.
- Two-client dedicated-server isolation and concurrent unrelated inventory actions.
- Jar audit, common-source client-reference audit, and exact documentation of every applied mixin.

## Hard-stop conditions

Stop P7-B and recommend shipping without integrated inventory if any of these becomes necessary:

- widening the authorized four-target mixin surface into packet handlers, player lifecycle, global menu clicks, or Trinkets internals;
- implementing custom click semantics instead of the vanilla container protocol;
- weakening exact `EquippedBagHandle` identity or trusting client capacities/content;
- allowing creative integrated mutation through indices outside vanilla's accepted range;
- duplicating bag contents or delayed close-time write-back;
- an unexplained duplication, loss, corruption, cross-player mutation, or persistent desynchronization.

## Go/no-go recommendation

**GO for a bounded P7-B prototype, but do not enable it by default.** Exact Minecraft 26.1.2 sources provide a viable 76-slot container-ID-0 topology and vanilla authoritative click path. The four-target surface is substantially smaller than the reviewed historical implementation and keeps storage on existing physical bag stacks.

P7-B authorization was granted on 2026-08-11. Implementation does not itself constitute acceptance: the prototype remains separately gated by the required automated and manual matrix, and any hard-stop condition changes the recommendation to **NO-GO: ship the proven Pack Menu without Integrated Inventory**.

## References

- Minecraft 26.1.2 Loom-resolved sources: `minecraft-common-6bbee272a2-26.1.2-sources.jar` and `minecraft-clientOnly-6bbee272a2-26.1.2-sources.jar`.
- Fabric Screen API artifact: `fabric-screen-api-v1-5.1.0+981dd9b24c.jar` from Fabric API 0.155.2+26.1.2.
- [Fabric `fabric.mod.json` mixin configuration](https://docs.fabricmc.net/develop/loader/fabric-mod-json)
- [SpongePowered Mixin callback injectors](https://github.com/SpongePowered/Mixin/wiki/Advanced-Mixin-Usage---Callback-Injectors)
- [Scout-Recrafted behavior/risk reference](https://github.com/danieelkx/Scout-Recrafted) at `92d39facb151666c1b1b157d279fe01c78037c0e`.
