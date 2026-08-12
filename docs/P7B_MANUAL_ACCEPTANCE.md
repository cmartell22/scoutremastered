# P7-B Integrated Inventory manual acceptance

P7-B must remain active, not complete, until every item below is verified with no open P0/P1 issue. Use `integrated_inventory_enabled=true` in `config/scout26-client.properties` and restart the client for enabled checks. Restore `false` for the disabled fallback check.

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

- [ ] No integrated panels are visible and all appended slots remain dormant.
- [ ] Vanilla survival and creative inventory behavior is unchanged.
- [ ] The configurable B-key Pack Menu opens and behaves exactly as before P7-B.

## Equipment and capacity combinations

- [x] No bags.
- [ ] Base and upgraded satchel separately.
- [ ] Base and upgraded left pouch separately.
- [ ] Base and upgraded right pouch separately.
- [ ] Both pouches in every base/upgraded combination.
- [ ] Satchel plus each pouch combination, including all three upgraded bags.
- [ ] Only the concrete 9/18 satchel and 3/6 pouch capacity prefixes activate.

## Inventory interactions

- [ ] Normal left click and right-click split in both directions.
- [ ] Drag/quick-craft across bag and player slots.
- [ ] Double-click collection.
- [ ] Shift-click bag to player and player to satchel, left pouch, then right pouch.
- [ ] Hotbar number-key swap and offhand swap.
- [ ] Q and Ctrl+Q drop behavior.
- [ ] Every normal, split, drag, shift-click, number-key, offhand, and double-click nested-bag attempt is rejected.

## Exact identity and equipment lifecycle

- [ ] Equip and unequip each bag while the inventory is open.
- [ ] Replace each equipped bag with a distinct same-kind bag while open.
- [ ] Swap pouch ordering while open.
- [ ] Trigger a Trinkets resource/inventory rebuild while open.
- [ ] Every stale role fails closed and never mutates the replacement bag; close/reopen binds the replacement.

## Player lifecycle and persistence

- [ ] Death and respawn with `keepInventory=false`.
- [ ] Death and respawn with `keepInventory=true`.
- [ ] Logout/rejoin while bags contain items and while inventory is open.
- [ ] Client restart and dedicated-server restart preserve the correct physical bag contents.

## Creative exclusion

- [ ] Creative inventory, inventory tab, trash slot, and clone behavior leave integrated slots dormant.
- [ ] Creative equipment changes do not activate or desynchronize integrated slots.
- [ ] Creative B-key Pack Menu remains synchronized and preserves documented copy semantics.

## Layout compatibility

- [ ] Recipe book closed and open in wide layout for every left-pouch tier.
- [ ] Recipe book open in narrow overlay layout suppresses underlying container input.
- [ ] Active effects render beyond base and upgraded right-pouch panels.
- [ ] External panel clicks never throw the carried stack as an outside click.

## Multiplayer isolation

- [ ] Two clients connect to a dedicated server with unrelated equipped bags.
- [ ] Both perform concurrent normal and shift-click inventory actions.
- [ ] Neither client can see or mutate the other player's bag contents.
- [ ] Both rejoin and retain their own physical bag contents.

Record the Minecraft/Fabric/Trinkets versions, tested commit SHA, server mode, client identities, results, and any regression IDs in `SCOUT26_AGENT_STATE.yaml` before marking P7-B complete.
