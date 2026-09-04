$ErrorActionPreference = "Stop"

$RepoRoot = (Get-Location).Path

function Replace-Exact {
    param(
        [Parameter(Mandatory = $true)][string]$RelativePath,
        [Parameter(Mandatory = $true)][string]$Old,
        [Parameter(Mandatory = $true)][string]$New
    )

    $Path = Join-Path $RepoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Missing expected file: $RelativePath"
    }

    $Text = [System.IO.File]::ReadAllText($Path)
    $UsesCrLf = $Text.Contains("`r`n")
    $Normalized = $Text.Replace("`r`n", "`n")
    $OldNormalized = $Old.Replace("`r`n", "`n")
    $NewNormalized = $New.Replace("`r`n", "`n")

    $Count = [regex]::Matches($Normalized, [regex]::Escape($OldNormalized)).Count
    if ($Count -ne 1) {
        throw "Expected exactly one match in $RelativePath, found $Count. No write performed for this replacement."
    }

    $Normalized = $Normalized.Replace($OldNormalized, $NewNormalized)

    if ($UsesCrLf) {
        $Normalized = $Normalized.Replace("`n", "`r`n")
    }

    [System.IO.File]::WriteAllText(
        $Path,
        $Normalized,
        [System.Text.UTF8Encoding]::new($false)
    )
}

# Production: Ready Slots must identify left/right pouches by dedicated slot ID,
# since both dedicated hip slots now use index 0.
Replace-Exact `
    "src/main/java/io/github/cmartell22/scoutremastered/ReadySlotRole.java" `
    @'
import eu.pb4.trinkets.api.DefaultTrinketSlots;
import java.util.Optional;
'@ `
    @'
import java.util.Optional;
'@

Replace-Exact `
    "src/main/java/io/github/cmartell22/scoutremastered/ReadySlotRole.java" `
    @'
	LEFT_POUCH(0, BagEquipmentRole.POUCH, DefaultTrinketSlots.LEGS_BELT, TrinketsIntegration.LEFT_POUCH_INDEX),
	RIGHT_POUCH(1, BagEquipmentRole.POUCH, DefaultTrinketSlots.LEGS_BELT, TrinketsIntegration.RIGHT_POUCH_INDEX),
	SATCHEL(2, BagEquipmentRole.SATCHEL, DefaultTrinketSlots.CHEST_BACK, TrinketsIntegration.SATCHEL_INDEX);
'@ `
    @'
	LEFT_POUCH(0, BagEquipmentRole.POUCH, TrinketsIntegration.LEFT_POUCH_SLOT, TrinketsIntegration.LEFT_POUCH_INDEX),
	RIGHT_POUCH(1, BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_SLOT, TrinketsIntegration.RIGHT_POUCH_INDEX),
	SATCHEL(2, BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX);
'@

# Production: Integrated Inventory also needs exact slot identity now that left/right
# pouch indexes are both zero.
Replace-Exact `
    "src/main/java/io/github/cmartell22/scoutremastered/IntegratedInventoryRole.java" `
    @'
	SATCHEL(BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_INDEX, 18),
	LEFT_POUCH(BagEquipmentRole.POUCH, TrinketsIntegration.LEFT_POUCH_INDEX, 6),
	RIGHT_POUCH(BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_INDEX, 6);

	private final BagEquipmentRole equipmentRole;
	private final int equipmentIndex;
	private final int maximumCapacity;

	IntegratedInventoryRole(BagEquipmentRole equipmentRole, int equipmentIndex, int maximumCapacity) {
		this.equipmentRole = equipmentRole;
		this.equipmentIndex = equipmentIndex;
		this.maximumCapacity = maximumCapacity;
	}
'@ `
    @'
	SATCHEL(BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX, 18),
	LEFT_POUCH(BagEquipmentRole.POUCH, TrinketsIntegration.LEFT_POUCH_SLOT, TrinketsIntegration.LEFT_POUCH_INDEX, 6),
	RIGHT_POUCH(BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_SLOT, TrinketsIntegration.RIGHT_POUCH_INDEX, 6);

	private final BagEquipmentRole equipmentRole;
	private final String slotId;
	private final int equipmentIndex;
	private final int maximumCapacity;

	IntegratedInventoryRole(BagEquipmentRole equipmentRole, String slotId, int equipmentIndex, int maximumCapacity) {
		this.equipmentRole = equipmentRole;
		this.slotId = slotId;
		this.equipmentIndex = equipmentIndex;
		this.maximumCapacity = maximumCapacity;
	}
'@

Replace-Exact `
    "src/main/java/io/github/cmartell22/scoutremastered/IntegratedInventoryRole.java" `
    @'
	public BagEquipmentRole equipmentRole() {
		return this.equipmentRole;
	}

	public int equipmentIndex() {
'@ `
    @'
	public BagEquipmentRole equipmentRole() {
		return this.equipmentRole;
	}

	public String slotId() {
		return this.slotId;
	}

	public int equipmentIndex() {
'@

Replace-Exact `
    "src/main/java/io/github/cmartell22/scoutremastered/IntegratedBagContainer.java" `
    @'
		return candidate != null
			&& candidate.equipmentRole() == this.role.equipmentRole()
			&& candidate.slotIndex() == this.role.equipmentIndex()
'@ `
    @'
		return candidate != null
			&& candidate.equipmentRole() == this.role.equipmentRole()
			&& candidate.slotId().equals(this.role.slotId())
			&& candidate.slotIndex() == this.role.equipmentIndex()
'@

# IntegratedInventoryTest
Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/IntegratedInventoryTest.java" `
    @'
import eu.pb4.trinkets.api.DefaultTrinketSlots;
'@ `
    ""

Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/IntegratedInventoryTest.java" `
    @'
	private static EquippedBagHandle capture(AtomicReference<ItemStack> live, IntegratedInventoryRole role) {
		String slotId = role == IntegratedInventoryRole.SATCHEL
			? DefaultTrinketSlots.CHEST_BACK
			: DefaultTrinketSlots.LEGS_BELT;
		return EquippedBagHandle.capture(slotId, role.equipmentIndex(), role.equipmentRole(), live::get).orElseThrow();
	}
'@ `
    @'
	private static EquippedBagHandle capture(AtomicReference<ItemStack> live, IntegratedInventoryRole role) {
		return EquippedBagHandle.capture(role.slotId(), role.equipmentIndex(), role.equipmentRole(), live::get).orElseThrow();
	}
'@

# P5HardeningTest only creates satchel handles.
Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/P5HardeningTest.java" `
    @'
import eu.pb4.trinkets.api.DefaultTrinketSlots;
'@ `
    ""

Replace-Exact "src/test/java/io/github/cmartell22/scoutremastered/P5HardeningTest.java" `
    "EquippedBagHandle oldHandle = capture(liveSlot, BagEquipmentRole.SATCHEL, 0);" `
    "EquippedBagHandle oldHandle = captureSatchel(liveSlot);"

Replace-Exact "src/test/java/io/github/cmartell22/scoutremastered/P5HardeningTest.java" `
    "EquippedBagHandle rebuiltHandle = capture(liveSlot, BagEquipmentRole.SATCHEL, 0);" `
    "EquippedBagHandle rebuiltHandle = captureSatchel(liveSlot);"

Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/P5HardeningTest.java" `
    @'
		EquippedBagHandle firstHandle = capture(firstSlot, BagEquipmentRole.SATCHEL, 0);
		EquippedBagHandle secondHandle = capture(secondSlot, BagEquipmentRole.SATCHEL, 0);
'@ `
    @'
		EquippedBagHandle firstHandle = captureSatchel(firstSlot);
		EquippedBagHandle secondHandle = captureSatchel(secondSlot);
'@

Replace-Exact "src/test/java/io/github/cmartell22/scoutremastered/P5HardeningTest.java" `
    "PackMenu menu = serverMenu(capture(liveSlot, BagEquipmentRole.SATCHEL, 0));" `
    "PackMenu menu = serverMenu(captureSatchel(liveSlot));"

Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/P5HardeningTest.java" `
    @'
	private static EquippedBagHandle capture(
		AtomicReference<ItemStack> liveSlot,
		BagEquipmentRole role,
		int slotIndex
	) {
		String slotId = role == BagEquipmentRole.SATCHEL
			? DefaultTrinketSlots.CHEST_BACK
			: DefaultTrinketSlots.LEGS_BELT;
		return EquippedBagHandle.capture(slotId, slotIndex, role, liveSlot::get).orElseThrow();
	}
'@ `
    @'
	private static EquippedBagHandle captureSatchel(AtomicReference<ItemStack> liveSlot) {
		return EquippedBagHandle.capture(
			TrinketsIntegration.SATCHEL_SLOT,
			TrinketsIntegration.SATCHEL_INDEX,
			BagEquipmentRole.SATCHEL,
			liveSlot::get
		).orElseThrow();
	}
'@

# PackMenuTest
Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/PackMenuTest.java" `
    @'
import eu.pb4.trinkets.api.DefaultTrinketSlots;
'@ `
    ""

Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/PackMenuTest.java" `
    @'
		EquippedBagHandle satchel = capture(new AtomicReference<>(new ItemStack(ModItems.UPGRADED_SATCHEL)), BagEquipmentRole.SATCHEL, 0);
		EquippedBagHandle left = capture(new AtomicReference<>(new ItemStack(ModItems.POUCH)), BagEquipmentRole.POUCH, 0);
		EquippedBagHandle right = capture(new AtomicReference<>(new ItemStack(ModItems.UPGRADED_POUCH)), BagEquipmentRole.POUCH, 1);
'@ `
    @'
		EquippedBagHandle satchel = capture(new AtomicReference<>(new ItemStack(ModItems.UPGRADED_SATCHEL)), BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX);
		EquippedBagHandle left = capture(new AtomicReference<>(new ItemStack(ModItems.POUCH)), BagEquipmentRole.POUCH, TrinketsIntegration.LEFT_POUCH_SLOT, TrinketsIntegration.LEFT_POUCH_INDEX);
		EquippedBagHandle right = capture(new AtomicReference<>(new ItemStack(ModItems.UPGRADED_POUCH)), BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_SLOT, TrinketsIntegration.RIGHT_POUCH_INDEX);
'@

Replace-Exact "src/test/java/io/github/cmartell22/scoutremastered/PackMenuTest.java" `
    "EquippedBagHandle handle = capture(liveSlot, BagEquipmentRole.SATCHEL, 0);" `
    "EquippedBagHandle handle = capture(liveSlot, BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX);"

Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/PackMenuTest.java" `
    @'
	private static EquippedBagHandle capture(
		AtomicReference<ItemStack> liveSlot,
		BagEquipmentRole role,
		int slotIndex
	) {
		String slotId = role == BagEquipmentRole.SATCHEL
			? DefaultTrinketSlots.CHEST_BACK
			: DefaultTrinketSlots.LEGS_BELT;
		return EquippedBagHandle.capture(slotId, slotIndex, role, liveSlot::get).orElseThrow();
	}
'@ `
    @'
	private static EquippedBagHandle capture(
		AtomicReference<ItemStack> liveSlot,
		BagEquipmentRole role,
		String slotId,
		int slotIndex
	) {
		return EquippedBagHandle.capture(slotId, slotIndex, role, liveSlot::get).orElseThrow();
	}
'@

# ReadySlotNetworkingTest
Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/ReadySlotNetworkingTest.java" `
    @'
import eu.pb4.trinkets.api.DefaultTrinketSlots;
'@ `
    ""

Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/ReadySlotNetworkingTest.java" `
    @'
		TrinketsIntegration.EquippedBags rightHandleInLeftRole = new TrinketsIntegration.EquippedBags(
			Optional.of(capture(wrongRole.satchel(), BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_INDEX)),
			Optional.of(capture(wrongRole.right(), BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_INDEX)),
			Optional.empty()
		);
'@ `
    @'
		TrinketsIntegration.EquippedBags rightHandleInLeftRole = new TrinketsIntegration.EquippedBags(
			Optional.of(capture(wrongRole.satchel(), BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX)),
			Optional.of(capture(wrongRole.right(), BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_SLOT, TrinketsIntegration.RIGHT_POUCH_INDEX)),
			Optional.empty()
		);
'@

Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/ReadySlotNetworkingTest.java" `
    @'
		TrinketsIntegration.EquippedBags bags = new TrinketsIntegration.EquippedBags(
			Optional.of(capture(satchel, BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_INDEX)),
			Optional.of(capture(left, BagEquipmentRole.POUCH, TrinketsIntegration.LEFT_POUCH_INDEX)),
			Optional.of(capture(right, BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_INDEX))
		);
'@ `
    @'
		TrinketsIntegration.EquippedBags bags = new TrinketsIntegration.EquippedBags(
			Optional.of(capture(satchel, BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX)),
			Optional.of(capture(left, BagEquipmentRole.POUCH, TrinketsIntegration.LEFT_POUCH_SLOT, TrinketsIntegration.LEFT_POUCH_INDEX)),
			Optional.of(capture(right, BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_SLOT, TrinketsIntegration.RIGHT_POUCH_INDEX))
		);
'@

Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/ReadySlotNetworkingTest.java" `
    @'
	private static EquippedBagHandle capture(
		AtomicReference<ItemStack> slot,
		BagEquipmentRole role,
		int index
	) {
		String slotId = role == BagEquipmentRole.SATCHEL
			? DefaultTrinketSlots.CHEST_BACK
			: DefaultTrinketSlots.LEGS_BELT;
		return EquippedBagHandle.capture(slotId, index, role, slot::get).orElseThrow();
	}
'@ `
    @'
	private static EquippedBagHandle capture(
		AtomicReference<ItemStack> slot,
		BagEquipmentRole role,
		String slotId,
		int index
	) {
		return EquippedBagHandle.capture(slotId, index, role, slot::get).orElseThrow();
	}
'@

# ReadySlotSwapServiceTest
Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/ReadySlotSwapServiceTest.java" `
    @'
import eu.pb4.trinkets.api.DefaultTrinketSlots;
'@ `
    ""

Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/ReadySlotSwapServiceTest.java" `
    @'
		inventory.setSelectedSlot(2);
		inventory.setSelectedItem(hand);
		EquippedBagHandle handle = capture(satchel, BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_INDEX);
'@ `
    @'
		inventory.setSelectedSlot(2);
		inventory.setSelectedItem(hand);
		EquippedBagHandle handle = capture(satchel, BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX);
'@

Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/ReadySlotSwapServiceTest.java" `
    "void wrongRoleOrWrongPouchIndexIsRejectedWithZeroMutation() {" `
    "void wrongRoleOrWrongPouchSlotIsRejectedWithZeroMutation() {"

Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/ReadySlotSwapServiceTest.java" `
    @'
		inventory.setSelectedItem(hand);
		AtomicReference<ItemStack> satchel = new AtomicReference<>(bag(ModItems.SATCHEL, new ItemStack(Items.DIAMOND, 19)));
		EquippedBagHandle handle = capture(satchel, BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_INDEX);
'@ `
    @'
		inventory.setSelectedItem(hand);
		AtomicReference<ItemStack> satchel = new AtomicReference<>(bag(ModItems.SATCHEL, new ItemStack(Items.DIAMOND, 19)));
		EquippedBagHandle handle = capture(satchel, BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX);
'@

Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/ReadySlotSwapServiceTest.java" `
    @'
		TrinketsIntegration.EquippedBags bags = new TrinketsIntegration.EquippedBags(
			Optional.of(capture(satchel, BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_INDEX)),
			Optional.of(capture(left, BagEquipmentRole.POUCH, TrinketsIntegration.LEFT_POUCH_INDEX)),
			Optional.of(capture(right, BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_INDEX))
		);
'@ `
    @'
		TrinketsIntegration.EquippedBags bags = new TrinketsIntegration.EquippedBags(
			Optional.of(capture(satchel, BagEquipmentRole.SATCHEL, TrinketsIntegration.SATCHEL_SLOT, TrinketsIntegration.SATCHEL_INDEX)),
			Optional.of(capture(left, BagEquipmentRole.POUCH, TrinketsIntegration.LEFT_POUCH_SLOT, TrinketsIntegration.LEFT_POUCH_INDEX)),
			Optional.of(capture(right, BagEquipmentRole.POUCH, TrinketsIntegration.RIGHT_POUCH_SLOT, TrinketsIntegration.RIGHT_POUCH_INDEX))
		);
'@

Replace-Exact `
    "src/test/java/io/github/cmartell22/scoutremastered/ReadySlotSwapServiceTest.java" `
    @'
	private static EquippedBagHandle capture(
		AtomicReference<ItemStack> liveSlot,
		BagEquipmentRole role,
		int index
	) {
		String slotId = role == BagEquipmentRole.SATCHEL
			? DefaultTrinketSlots.CHEST_BACK
			: DefaultTrinketSlots.LEGS_BELT;
		return EquippedBagHandle.capture(slotId, index, role, liveSlot::get).orElseThrow();
	}
'@ `
    @'
	private static EquippedBagHandle capture(
		AtomicReference<ItemStack> liveSlot,
		BagEquipmentRole role,
		String slotId,
		int index
	) {
		return EquippedBagHandle.capture(slotId, index, role, liveSlot::get).orElseThrow();
	}
'@

Write-Host ""
Write-Host "Corrective dedicated-slot edits applied successfully." -ForegroundColor Green
Write-Host "Running git diff --check..." -ForegroundColor Cyan
git diff --check
if ($LASTEXITCODE -ne 0) {
    throw "git diff --check reported a problem."
}
Write-Host "git diff --check passed." -ForegroundColor Green
