# P7-B Integrated Inventory manual acceptance

> Historical acceptance record. P7-B was completed on 2026-08-12 at source commit
> `f9c92d56e7defedf99f3dc8b4965956e7971305f`, before the P8 release rename from the
> temporary `scout26` namespace. Its observations remain valid behavioral evidence; the release
> candidate uses `scoutremastered` and `config/scoutremastered-client.properties` instead.

## Accepted integrated UI baseline

Human acceptance on 2026-08-11 established source commit `797422cb707de27bfda4f704ffaf58579970ac90`
as the successful enabled-survival UI and interaction baseline. The accepted session covered extensive item movement,
repeated inventory open/close cycles with the recipe book both open and closed, and exchanging populated pouches and
satchels for upgraded versions. All panels, recipe-book controls/tabs, and bag interactions remained functional with
no observed loss, duplication, disappearance, clipping, flicker, or layout correction.

This checkpoint does not implicitly check unrelated rows below. In particular, disabled fallback, destructive player
lifecycle cases, creative exclusion, narrow-overlay/effects compatibility, and two-client dedicated-server isolation
still require their explicit acceptance passes before P7-B can be completed.

## Disabled fallback

- [x] No integrated panels are visible and all appended slots remain dormant.
- [x] Vanilla survival and creative inventory behavior is unchanged.
- [x] The configurable B-key Pack Menu opens and behaves exactly as before P7-B.

Evidence: On 2026-08-11, commit `f9c92d56e7defedf99f3dc8b4965956e7971305f` was run with
`integrated_inventory_enabled=false` in the `P7B-test` integrated-server world as development identity
`Player303` (Minecraft 26.1.2, Fabric Loader 0.19.3, Fabric API 0.155.2+26.1.2, Trinkets Updated
4.0.0-beta.3+26.1). The human explicitly reported all checks passed perfectly: no integrated panels or
bag-directed survival shift-click routing appeared, ordinary survival and creative inventory behavior was
unchanged, and the B-key Pack Menu retained the marker item across close/reopen with correct final counts.

## Equipment and capacity combinations

- [x] No bags.
- [x] Base and upgraded satchel separately.
- [x] Base and upgraded left pouch separately.
- [x] Base and upgraded right pouch separately.
- [x] Both pouches in every base/upgraded combination.
- [x] Satchel plus each pouch combination, including all three upgraded bags.
- [x] Only the concrete 9/18 satchel and 3/6 pouch capacity prefixes activate.

Evidence: On 2026-08-11, the enabled `P7B-test` integrated-server session at commit
`f9c92d56e7defedf99f3dc8b4965956e7971305f` continued as `Player168`. The human explicitly passed base and
upgraded satchels, left pouches, and right pouches separately. Each final visible slot accepted and retained its
marker across repeated inventory open/close paths (`E`, `Esc`, and recipe book open/closed), no slot beyond the
concrete 9/18 or 3/6 prefix activated, and the B-key Pack Menu exposed matching capacities and contents. With no
bag equipped, B displayed the expected instruction to equip a satchel or pouch. Recipe-book wide/narrow mode is
recorded separately under Layout compatibility and is not inferred from this evidence.

Both-pouch evidence: The same enabled `Player168` session explicitly passed base/base, base/upgraded,
upgraded/base, and upgraded/upgraded left/right pouch combinations. Exact 3/6 capacities appeared on the correct
sides, distinct left/right markers retained their physical-pouch ownership across close/reopen and tier changes,
no contents jumped between pouches, and the B-key Pack Menu agreed with integrated ordering, contents, and counts.

Satchel-plus-one-pouch evidence: The same session explicitly passed both satchel tiers paired separately with each
left/right pouch tier (eight configurations). Only the intended satchel and pouch panels appeared, distinct markers
remained in their physical bags across close/reopen and tier/side changes, and B agreed with integrated role order,
capacities, contents, and counts.

All-three-bags evidence: The same session explicitly passed all eight satchel/left/right base/upgraded combinations,
including the 18/6/6 all-upgraded case. Exact capacities, physical-stack ownership, close/reopen persistence, and
B-menu agreement passed with no clipping, loss, duplication, or role crossover. The human noted a minor one-time
item-paint delay can sometimes be visible immediately after opening, most noticeably with 18/6/6, and believes it
has been present throughout; all behavior remains correct. Source review confirmed this matches the intentional
ADR-012 sequence: pre-init previews layout only, the client sends its open intent after screen init, and bag mirrors
receive authoritative contents in the ordered ACK/full-container-state/READY exchange. Minecraft 26.1.2 applies the
full container-ID-0 packet through `ClientPacketListener.handleContainerContent` and `InventoryMenu.initializeContents`.
This is recorded as a minor synchronization-timing observation, not a regression; no `BUG-P7B-013` was assigned.

## Inventory interactions

- [x] Normal left click and right-click split in both directions.
- [x] Drag/quick-craft across bag and player slots.
- [x] Double-click collection.
- [x] Shift-click bag to player and player to satchel, left pouch, then right pouch.
- [x] Hotbar number-key swap and offhand swap.
- [x] Q and Ctrl+Q drop behavior.
- [x] Every normal, split, drag, shift-click, number-key, offhand, and double-click nested-bag attempt is rejected.

Direct-interaction evidence: On 2026-08-11, the enabled `P7B-test` integrated-server session at commit
`f9c92d56e7defedf99f3dc8b4965956e7971305f` continued as `Player168`. The human explicitly passed ordinary
left-click moves, bidirectional right-click splitting/single placement, even and one-per-slot quick-craft across
mixed bag/player slots, and double-click collection spanning bag and player slots. Exact totals were preserved and
the final contents/counts agreed after E close/reopen and in the B-key Pack Menu.

Shift/swap/drop evidence: The same session explicitly passed merge-before-empty player shift-click routing in
satchel, left-pouch, right-pouch order; bag-to-player shift-click from every role; number-key swaps; offhand swaps;
and Q/Ctrl+Q single/full-stack drops. Dropped entities existed and were collectible, all totals remained correct,
and final E and B views agreed.

Nested-bag evidence: The same session explicitly passed left-click, right-click/split, quick-craft/drag,
shift-click, number-key, offhand, and double-click attempts using bag items against integrated storage. Every
attempt was rejected, no bag appeared in any satchel or pouch slot after E/B reopen, ordinary contents stayed
unchanged, and no bag disappeared, duplicated, or changed its own contents.

## Exact identity and equipment lifecycle

- [x] Equip and unequip each bag while the inventory is open.
- [x] Replace each equipped bag with a distinct same-kind bag while open.
- [x] Swap pouch ordering while open.
- [x] Trigger a Trinkets resource/inventory rebuild while open.
- [x] Every stale role fails closed and never mutates the replacement bag; close/reopen binds the replacement.

Open-screen identity evidence: On 2026-08-11, the enabled `P7B-test` integrated-server session at commit
`f9c92d56e7defedf99f3dc8b4965956e7971305f` continued as `Player168`. The human explicitly passed open-screen
equip and unequip for satchel, left pouch, and right pouch, plus direct replacement by a physically distinct
same-kind/tier A/B bag for every role. Panels activated/deactivated correctly, stale presentation never changed a
replacement, and all six A/B physical bags retained their distinct contents and exact counts through E close/reopen
and independent B-key Pack Menu verification.

Ordering/rebuild evidence: The same session explicitly passed both directions of open-screen left/right pouch
exchange using distinct populated physical pouches. It also completed an in-screen F3+T resource reload with all
three roles populated. Stale clicks failed closed or operated only after the correct refreshed binding was visible;
close/reopen bound the rebuilt/reordered physical stacks, and E/B showed exact final contents with no loss,
duplication, or crossover. The exact-identity/equipment-lifecycle section is complete.

## Player lifecycle and persistence

- [x] Death and respawn with `keepInventory=false`.
- [x] Death and respawn with `keepInventory=true`.
- [x] Logout/rejoin while bags contain items and while inventory is open.
- [x] Client restart and dedicated-server restart preserve the correct physical bag contents.

Death/respawn evidence: On 2026-08-12, the enabled `P7B-test` integrated-server session at commit
`f9c92d56e7defedf99f3dc8b4965956e7971305f` continued as `Player168`. The human explicitly passed both game-rule
modes using one distinct populated physical bag per role. With `keepInventory=false`, the equipped bags dropped,
were recovered as exactly one copy each, retained their own contents, and rebound correctly after re-equipping.
With `keepInventory=true`, the bags remained equipped through respawn with exact E/B contents and no duplicate
drops. No loss, duplication, or role crossover occurred; the test rule was restored to false.

Logout/rejoin evidence: The same enabled world passed a normal Save-and-Quit-to-Title/rejoin with populated equipped
bags, followed by an Alt+F4 client termination while the integrated inventory was open with an empty cursor. The log
showed an orderly render-thread stop, player disconnect, player/world saves, and all dimensions saved. Codex relaunched
the client into `P7B-test` as development identity `Player540`; the human explicitly confirmed all physical bags,
roles, marker slots/counts, E/B views, and a post-restart mutation persisted with no duplicate bags/items. The client
restart half passed; the dedicated-server half is recorded below.

Dedicated restart evidence: The dedicated server was stopped twice through the permission-level-4 `/stop` command.
Each shutdown logged player/world saves plus successful overworld, Nether, and End saves. After both stops, Alice and
Bob's fixed playerdata files had the identical shutdown timestamp. Codex restarted the same world and relaunched the
same fixed clients; server logs confirmed both rejoined. The human explicitly confirmed each physical bag and marker,
no duplicate bags/drops, E/B agreement, persisted post-restart slot moves, and continued cross-player isolation.

## Creative exclusion

- [x] Creative inventory, inventory tab, trash slot, and clone behavior leave integrated slots dormant.
- [x] Creative equipment changes do not activate or desynchronize integrated slots.
- [x] Creative B-key Pack Menu remains synchronized and preserves documented copy semantics.

Evidence: On 2026-08-11, commit `f9c92d56e7defedf99f3dc8b4965956e7971305f` was run with
`integrated_inventory_enabled=true` in the `P7B-test` integrated-server world as development identity
`Player168`, using the versions recorded in the disabled-fallback evidence above. The human explicitly reported
all checks passed: creative tabs/inventory/trash remained vanilla with no integrated panels, creative equipment
changes did not activate or desynchronize integrated slots, and the B-key Pack Menu remained synchronized.
Middle-clicking a populated bag copied its contents at clone time; subsequent mutations of the original and clone
diverged independently with correct counts and no loss or duplication.

## Layout compatibility

- [x] Recipe book closed and open in wide layout for every left-pouch tier.
- [x] Recipe book open in narrow overlay layout suppresses underlying container input.
- [x] Active effects render beyond base and upgraded right-pouch panels.
- [x] External panel clicks never throw the carried stack as an outside click.

Wide-layout/boundary evidence: On 2026-08-11, the enabled `P7B-test` integrated-server session at commit
`f9c92d56e7defedf99f3dc8b4965956e7971305f` continued as `Player168`. With an upgraded satchel and right pouch
present, the human explicitly passed recipe-book closed/open/reopen/toggle behavior for both base and upgraded left
pouches in true side-by-side wide mode. Panels stayed visible/aligned/clickable on the first frame, and recipe tabs,
search, filtering, scrolling, and selection remained functional. With a carried counted stack, clicks on left/right
pouch edges/joins and satchel lower borders/corners never threw the stack; real integrated slots still accepted and
returned it normally, and final counts persisted.

Narrow-overlay evidence: The same session was resized below Minecraft 26.1.2's 379-logical-pixel threshold and the
human explicitly confirmed the true overlaid recipe-book composition. Left, right, shift, drag, and carried-stack
click attempts at underlying integrated locations were suppressed; recipe controls remained functional; closing the
overlay restored normal input; and final E/B contents and counts were exact. While overlaid, both vanilla and bag
slot items appeared empty, while the separate world HUD hotbar remained populated. Exact 26.1.2 source confirms this
is vanilla behavior: `AbstractRecipeBookScreen.extractRenderState` renders the background instead of
`super.extractContents` when the recipe book is visible and `widthTooNarrow`, and `mouseClicked` consumes otherwise
unhandled clicks. No regression ID was assigned.

Active-effects evidence: The same enabled session explicitly passed multiple simultaneous effects with base and
upgraded right pouches in a stressed wide layout. Effect entries began beyond each pouch's outer edge, remained
fully visible, and never overlapped or blocked pouch slots/items. Hover, pouch interactions, close/reopen first-frame
placement, and recipe-book toggle remained correct. The Layout compatibility section is complete.

## Multiplayer isolation

- [x] Two clients connect to a dedicated server with unrelated equipped bags.
- [x] Both perform concurrent normal and shift-click inventory actions.
- [x] Neither client can see or mutate the other player's bag contents.
- [x] Both rejoin and retain their own physical bag contents.

Dedicated concurrency/isolation evidence: On 2026-08-12, commit
`f9c92d56e7defedf99f3dc8b4965956e7971305f` ran on a dedicated Minecraft 26.1.2 server bound only to
`127.0.0.1:25565`, temporarily in offline development mode with secure profiles disabled so two fixed local test
identities could connect. `ScoutAlice` (`d7d5a6bd-6768-3034-baba-ff917b32ea6a`) and `ScoutBob`
(`e169c006-2bde-3797-bf2c-7ee781a5e59d`) used separate game/config directories with Integrated Inventory enabled.
Alice held unrelated upgraded bags containing 11 diamonds/12 emeralds/13 gold ingots; Bob held unrelated upgraded
bags containing 21 redstone/22 lapis/23 iron ingots. The human explicitly passed overlapping normal and shift-click
actions in both open inventories, visibility isolation, mutation isolation in both directions, E/B reopen checks,
and exact restored counts with no loss, duplication, crossover, or desynchronization. Server logs independently
record both fixed identities connected concurrently. Controlled restart/rejoin evidence follows.

Dedicated restart/rejoin evidence: Alice retained 11 diamonds/12 emeralds/13 gold ingots and Bob retained 21
redstone/22 lapis/23 iron ingots, with exactly one upgraded satchel and two upgraded pouches per player. Their
post-restart slot moves persisted and neither client changed the other's contents. After final verification, the
second clean `/stop` saved both playerdata files and all dimensions. The temporary server was loopback-only; secure
online-mode settings and an empty ops list were then restored, and both temporary client directories were removed.
The dedicated playerdata remains in the ignored test world as persistence evidence. All manual rows are complete.

## Completion verification

P7-B is accepted on 2026-08-12 against source/test commit
`f9c92d56e7defedf99f3dc8b4965956e7971305f` using Minecraft 26.1.2, Fabric Loader 0.19.3,
Fabric API 0.155.2+26.1.2, Trinkets Updated 4.0.0-beta.3+26.1, Java 25, and Loom 1.17.19.

- Every manual row above has explicit human evidence from integrated-server and dedicated-server modes.
- Development identities were `Player303`, `Player168`, and `Player540`; fixed dedicated identities were
  `ScoutAlice` (`d7d5a6bd-6768-3034-baba-ff917b32ea6a`) and `ScoutBob`
  (`e169c006-2bde-3797-bf2c-7ee781a5e59d`).
- BUG-P7B-001 through BUG-P7B-012 are fixed. No BUG-P7B-013 was assigned; the two noted visual timing/rendering
  observations match the documented authoritative synchronization and vanilla narrow-overlay render paths.
- All 39 automated tests passed with zero failures, errors, or skips, followed by a clean build with all 10 tasks
  executed.
- Exactly four Scout26 Mixin targets remain: `InventoryMenu`, `AbstractContainerScreen`, `RecipeBookComponent`,
  and `EffectsInInventory`. No `@Overwrite`, forbidden Mixin target, common-source client reference, or packaged test
  class exists.
- A secure dedicated-server smoke loaded Scout26 and reached `Done`; a default-disabled client smoke loaded Scout26
  and resources to the title screen. Only the known non-fatal third-party Trinkets server warning and expected
  unauthenticated development-client service errors appeared.
- Runtime cleanup restored Integrated Inventory to disabled, restored secure online server settings and empty ops,
  stopped every Scout26 runtime process, and left TCP port 25565 free.

P7-B is complete. Integrated Inventory remains default-disabled. P8 remains blocked and requires separate human
authorization.
