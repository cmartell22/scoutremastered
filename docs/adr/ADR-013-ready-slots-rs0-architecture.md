# ADR-013: Ready Slots architecture and API lock

- Date: 2026-08-31
- RS0 status: Accepted
- Decision: Proceed to RS1 using the existing Trinkets renderer and the contracts below.

## Context

Ready Slots aliases bag-local slot 0 in each equipped Scout bag. Rendering is client-side and read-only. A later serverbound role-only intent will ask the server to swap the complete selected-hotbar stack with the complete ready-slot stack. The design must preserve the existing physical bag `ItemStack` as the only bag-content owner and must fail closed on stale equipment.

This review used the exact locally resolved Minecraft 26.1.2 common/client sources, Fabric Networking API 6.3.1+554860db4c from Fabric API 0.155.2+26.1.2, Trinkets Updated 4.0.0-beta.3+26.1, and the current Scout Remastered source at `f0c0225`. No game client was required for RS0.

## Locked decisions

### 1. Server mutation and synchronization

The swap service will execute only on the server thread and will use this order:

1. Resolve the requested stable role to an exact `EquippedBagHandle` and require that the currently equipped stack is the captured object.
2. Reject a missing/stale/wrong-role bag, missing bag-local slot 0, or a selected hand stack that is itself a Scout bag before changing either owner.
3. Copy both complete `ItemStack` values as the transaction snapshots.
4. Replace bag-local slot 0 through a narrow `BagContainer` whole-slot operation, which immediately replaces `BAG_CONTENTS` on that exact physical bag stack.
5. Replace the selected hotbar value with `Inventory.setSelectedItem(ItemStack)` and call `Inventory.setChanged()`.
6. If an unexpected second-write failure is representable, restore both snapshots before returning failure. No packet is an authorization or content source.
7. Call `ServerPlayer.inventoryMenu.broadcastChanges()` after a successful commit. The selected inventory index maps into container ID 0, and the existing fixed Trinkets slots expose the equipped bag stack there. Other open-menu behavior remains an explicit RS3 test case.

Minecraft 26.1.2 supplies `Inventory.getSelectedSlot()`, `getSelectedItem()`, `setSelectedItem(ItemStack)`, and `setChanged()`. `AbstractContainerMenu.broadcastChanges()` compares full stacks and sends changed slots through its synchronizer. The design does not mutate the selected index and does not construct a client-selected inventory slot.

### 2. Role-only payload codec

`SwapReadySlotPayload` will contain exactly one `ReadySlotRole`; it will never carry an `ItemStack`, bag capacity, bag-local index, or equipment reference. The role has a stable explicit network ID. Its `StreamCodec` will encode a VarInt and reject every negative or out-of-range ID during decode. It will not use an enum-array lookup that can throw an incidental bounds exception, and it will not map malformed IDs to a valid role.

The server receiver treats the role as intent only, resolves all state again on the server thread, and delegates to the same tested swap service used without networking in RS2.

### 3. Component synchronization to owner and observers

`BAG_CONTENTS` is both persistent and network-synchronized with `BagContents.STREAM_CODEC`. `ItemStack.OPTIONAL_STREAM_CODEC` carries its data-component patch, and Trinkets' `SyncInventoryPayload` uses that complete stack codec.

Trinkets' `LivingEntityMixin` keeps copies of the last equipped stacks. Each equipment-update pass calls Minecraft's `LivingEntity.equipmentHasChanged`, which delegates to `ItemStack.matches`; that comparison includes count, item, and components. A component-only `BAG_CONTENTS` replacement is therefore detected. Trinkets copies the changed stack into `SyncInventoryPayload` and calls `ServerChunkCache.sendToTrackingPlayers`, so remote player renderers receive it. Initial tracking also sends complete equipped stack copies through the same payload.

The owning client receives the successful mutation immediately from the explicit container-ID-0 broadcast. RS1 must still prove the full two-client visual path in a live dedicated-server scenario; the synchronization mechanism itself is no longer an architectural unknown.

### 4. Initial render whitelist

Visibility is conservative while storage and swapping remain permissive. RS1 will render a non-empty ready stack only when one of these is true:

- it is in `ItemTags.SWORDS`, `AXES`, `PICKAXES`, `SHOVELS`, or `HOES`; or
- its item is a `BowItem`, `CrossbowItem`, `TridentItem`, or `ShieldItem`.

The five tool/weapon constants exist in Minecraft 26.1.2. The four class checks cover the built-in items and compatible subclasses without abusing enchantability tags as category tags. Unsupported items remain valid bag contents and valid swap participants; they render nothing.

### 5. Renderer boundary

The existing `BagTrinketRenderer` remains the sole rendering integration. Its callback already creates an independent `ItemStackRenderState`, passes an arbitrary `ItemStack` to `ItemModelResolver.appendItemLayers(..., ItemDisplayContext.FIXED, ...)`, and submits that state to the callback's `SubmitNodeCollector`. The Minecraft 26.1.2 API has no requirement that this stack equal the equipped Trinkets stack.

RS1 may therefore push a second pose, derive a defensive slot-0 stack from synchronized `BAG_CONTENTS`, apply a position/category transform, and submit it through a second fresh render state. No player-render mixin, new equipment slot, new persistent component, or second inventory is authorized.

### 6. Persistence and abrupt-process safety

The selected hotbar stack and equipped Trinkets bag are serialized into the same player-data snapshot:

- `Player.addAdditionalSaveData` writes the whole inventory, including the selected hotbar stack, into the current `ValueOutput`.
- Trinkets injects at the tail of that same method and writes the equipped inventories under the `trinkets` child of the same output.
- `PlayerDataStorage.save` builds one `CompoundTag`, writes it to a temporary file, and then calls `Util.safeReplaceFile` with the live `.dat` and `.dat_old` paths.
- Loading falls back to `.dat_old` when the live file is absent or unreadable.

Both in-memory replacements run synchronously on the server thread, so a player save cannot interleave between the two writes. A process killed before the next durable save recovers the previous complete player snapshot; a completed save recovers the new complete snapshot. A kill during file replacement recovers either the live snapshot or its backup. This supports a strict pre-state-or-post-state invariant and rules out a durably persisted half-swap under the reviewed APIs.

This is an architectural argument, not a substitute for destructive testing. Any observed loss, duplication, component corruption, or mixed pre/post pair remains a release blocker.

## Automated hard-termination harness for RS4

The harness will be an external controller so the killed JVM cannot run `/stop`, save-all, normal disconnect, shutdown hooks, or test cleanup:

1. Copy a versioned fixture world to an isolated run directory and start the target JVM with an exact captured PID.
2. Use deterministic player identities and marker stacks whose item, count, damage, enchantments, custom name, and other components distinguish the pre- and post-swap states.
3. Establish and verify a durable pre-state, perform a numbered swap through the real server path, and emit machine-readable server-side commit/ack markers without forcing a save.
4. Force-terminate only the captured PID at multiple offsets: before commit, immediately after commit, after client acknowledgement, around normal save cadence, and during repeated swaps.
5. Restart the same world and identities. Read the authoritative state through the server/test protocol and, where useful, independently inspect player data.
6. Require the hotbar/bag pair and its exact component multiset to equal one complete allowed snapshot. Reject a mixed pair even when total item count happens to match.
7. Repeat for an integrated client/server JVM and a dedicated-server JVM. Separately force-kill an automated dedicated client while the server remains alive, reconnect it, and require the server-authoritative post-state.
8. Run enough deterministic repetitions per timing window to make race-sensitive failures reproducible and retain the seed, PID, timestamps, logs, and recovered state on failure.

Automated client profiles should set Minecraft sound categories to zero before launch where the locked client options format permits it. Human visual testing is used only for observations automation cannot credibly judge, and the existing readiness gate applies before any such client launch.

## Clean-room boundary

Combat Amenities is an all-rights-reserved behavior/API reference only. Scout Remastered may reproduce the independently described user behavior and use public Minecraft/Fabric/Trinkets APIs, but will copy no source, constants, transforms, assets, comments, class structure, packet layout, or tests from that mod. No Combat Amenities source was opened or used during RS0.

## Rejected alternatives

- Client-side swapping or an `ItemStack` payload: rejected because it trusts client content and permits duplication/desynchronization.
- A second ready-slot inventory or component: rejected because it creates another persistence owner.
- Re-resolving whichever bag occupies a role after validation: rejected because a stale action could mutate a replacement bag.
- Direct enum ordinal array lookup or malformed-ID fallback: rejected because malformed input must fail closed.
- A new player-render mixin: rejected because the existing Trinkets callback accepts the required arbitrary stack.
- Graceful `/stop` crash tests: rejected because they exercise save-on-shutdown rather than sudden-loss behavior.

## Verification status and gate

The release-candidate code baseline has not changed since commit `ddcdb89`; only the two planning YAML files changed before this ADR. Its retained test reports contain 39 tests with zero failures or errors. A fresh Gradle invocation was attempted both through the wrapper and cached Gradle 9.5.1, but this execution host could not establish Gradle's required loopback connection for its single-use daemon. This is recorded as a host limitation, not counted as a fresh pass.

RS0 is complete because every API and persistence question in its scope is resolved from the exact locked sources/binaries, and no implementation code changed. RS1 remains responsible for compilation, automated client/dedicated-server checks, and the local/remote rendering acceptance matrix before read-only rendering can be accepted.

## Exact evidence inspected

- Minecraft common/client sources: `minecraft-common-6bbee272a2-26.1.2-sources.jar` and `minecraft-clientOnly-6bbee272a2-26.1.2-sources.jar`.
- Minecraft classes: `Inventory`, `Player`, `LivingEntity`, `ItemStack`, `AbstractContainerMenu`, `PlayerDataStorage`, `Util`, `ItemModelResolver`, and `ItemStackRenderState`.
- Trinkets Updated 4.0.0-beta.3+26.1 binary: `LivingEntityMixin`, `ServerEntityMixin`, `PlayerListMixin`, `LivingEntityTrinketAttachment`, `TrinketInventoryImpl`, and `SyncInventoryPayload`.
- Scout Remastered classes: `BagContainer`, `BagContents`, `EquippedBagHandle`, `TrinketsIntegration`, `ModDataComponents`, `BagTrinketRenderer`, and the existing typed networking payloads.
