package com.example.scout26;

/** Shared integer-only layout for server slot positions and the client screen. */
public final class PackMenuLayout {
	public static final int IMAGE_WIDTH = 176;
	public static final int TITLE_Y = 6;
	public static final int LEFT_POUCH_X = 26;
	public static final int RIGHT_POUCH_X = 98;
	private static final int SECTION_START_Y = 18;
	private static final int LABEL_TO_SLOTS = 12;
	private static final int SECTION_GAP = 5;
	private static final int PLAYER_INVENTORY_X = 8;
	private static final int HOTBAR_OFFSET = 58;

	private final PackMenuData data;
	private final int satchelLabelY;
	private final int satchelSlotsY;
	private final int pouchLabelY;
	private final int pouchSlotsY;
	private final int inventoryLabelY;
	private final int playerInventoryY;
	private final int imageHeight;

	public PackMenuLayout(PackMenuData data) {
		this.data = data;
		int nextY = SECTION_START_Y;

		if (data.satchelCapacity() > 0) {
			this.satchelLabelY = nextY;
			this.satchelSlotsY = nextY + LABEL_TO_SLOTS;
			nextY = this.satchelSlotsY + rows(data.satchelCapacity(), 9) * 18 + SECTION_GAP;
		} else {
			this.satchelLabelY = -1;
			this.satchelSlotsY = -1;
		}

		if (data.leftPouchCapacity() > 0 || data.rightPouchCapacity() > 0) {
			this.pouchLabelY = nextY;
			this.pouchSlotsY = nextY + LABEL_TO_SLOTS;
			int pouchRows = Math.max(rows(data.leftPouchCapacity(), 3), rows(data.rightPouchCapacity(), 3));
			nextY = this.pouchSlotsY + pouchRows * 18 + SECTION_GAP;
		} else {
			this.pouchLabelY = -1;
			this.pouchSlotsY = -1;
		}

		this.inventoryLabelY = nextY;
		this.playerInventoryY = nextY + LABEL_TO_SLOTS;
		int hotbarY = this.playerInventoryY + HOTBAR_OFFSET;
		this.imageHeight = hotbarY + 18 + 7;
	}

	public PackMenuData data() {
		return this.data;
	}

	public int imageHeight() {
		return this.imageHeight;
	}

	public int satchelLabelY() {
		return this.satchelLabelY;
	}

	public int pouchLabelY() {
		return this.pouchLabelY;
	}

	public int inventoryLabelY() {
		return this.inventoryLabelY;
	}

	public int playerInventoryX() {
		return PLAYER_INVENTORY_X;
	}

	public int playerInventoryY() {
		return this.playerInventoryY;
	}

	public int satchelSlotX(int slot) {
		return 8 + slot % 9 * 18;
	}

	public int satchelSlotY(int slot) {
		return this.satchelSlotsY + slot / 9 * 18;
	}

	public int leftPouchSlotX(int slot) {
		return LEFT_POUCH_X + slot % 3 * 18;
	}

	public int rightPouchSlotX(int slot) {
		return RIGHT_POUCH_X + slot % 3 * 18;
	}

	public int pouchSlotY(int slot) {
		return this.pouchSlotsY + slot / 3 * 18;
	}

	private static int rows(int slots, int columns) {
		return slots == 0 ? 0 : (slots + columns - 1) / columns;
	}
}
