package com.example.scout26;

/** Fixed menu indices and classic external-panel coordinates from ADR-012. */
public final class IntegratedInventoryLayout {
	public static final int VANILLA_SLOT_COUNT = 46;
	public static final int SATCHEL_START = 46;
	public static final int SATCHEL_END = 64;
	public static final int LEFT_POUCH_START = 64;
	public static final int LEFT_POUCH_END = 70;
	public static final int RIGHT_POUCH_START = 70;
	public static final int RIGHT_POUCH_END = 76;
	public static final int TOTAL_SLOT_COUNT = 76;
	public static final int SLOT_SPACING = 18;
	public static final int VANILLA_IMAGE_WIDTH = 176;
	public static final int VANILLA_IMAGE_HEIGHT = 166;
	public static final int SATCHEL_X = 8;
	public static final int SATCHEL_Y = 172;
	public static final int SATCHEL_PANEL_X = 0;
	public static final int SATCHEL_PANEL_Y = VANILLA_IMAGE_HEIGHT;
	public static final int LEFT_INNER_X = -10;
	public static final int RIGHT_INNER_X = 170;
	public static final int POUCH_Y = 84;
	public static final int POUCH_PANEL_Y = 77;
	public static final int POUCH_PANEL_HEIGHT = 66;
	public static final int SIDE_PANEL_OVERLAP = 7;
	public static final int RIGHT_POUCH_PANEL_X = VANILLA_IMAGE_WIDTH - SIDE_PANEL_OVERLAP;
	private static final int RECIPE_BOOK_WIDTH = 147;
	private static final int RECIPE_BOOK_X_OFFSET = 86;
	private static final int RECIPE_BOOK_TAB_OVERHANG = 30;

	private IntegratedInventoryLayout() {
	}

	public static int menuStart(IntegratedInventoryRole role) {
		return switch (role) {
			case SATCHEL -> SATCHEL_START;
			case LEFT_POUCH -> LEFT_POUCH_START;
			case RIGHT_POUCH -> RIGHT_POUCH_START;
		};
	}

	public static int menuEnd(IntegratedInventoryRole role) {
		return menuStart(role) + role.maximumCapacity();
	}

	public static int slotX(IntegratedInventoryRole role, int slot) {
		return switch (role) {
			case SATCHEL -> SATCHEL_X + slot % 9 * SLOT_SPACING;
			case LEFT_POUCH -> LEFT_INNER_X - slot / 3 * SLOT_SPACING;
			case RIGHT_POUCH -> RIGHT_INNER_X + slot / 3 * SLOT_SPACING;
		};
	}

	public static int slotY(IntegratedInventoryRole role, int slot) {
		return switch (role) {
			case SATCHEL -> SATCHEL_Y + slot / 9 * SLOT_SPACING;
			case LEFT_POUCH, RIGHT_POUCH -> POUCH_Y + slot % 3 * SLOT_SPACING;
		};
	}

	public static int leftPanelWidth(int capacity) {
		return capacity <= 0 ? 0 : capacity <= 3 ? 24 : 42;
	}

	public static int rightPanelWidth(int capacity) {
		return leftPanelWidth(capacity);
	}

	/** Width by which a left pouch extends beyond the vanilla inventory after its chamfered join. */
	public static int leftPanelExtension(int capacity) {
		return Math.max(0, leftPanelWidth(capacity) - SIDE_PANEL_OVERLAP);
	}

	/** Right edge of the combined inventory and right-pouch surface, relative to vanilla leftPos. */
	public static int combinedRightEdge(int rightPouchCapacity) {
		int rightWidth = rightPanelWidth(rightPouchCapacity);
		return rightWidth == 0
			? VANILLA_IMAGE_WIDTH
			: Math.max(VANILLA_IMAGE_WIDTH, RIGHT_POUCH_PANEL_X + rightWidth);
	}

	/**
	 * Moves the complete wide recipe-book composition left just enough to keep its right pouch on
	 * screen. The shift is capped before the recipe tabs themselves would cross the left edge.
	 */
	public static int wideRecipeBookShift(int screenWidth, IntegratedInventoryData data) {
		int vanillaInventoryLeft = 177 + (screenWidth - VANILLA_IMAGE_WIDTH - 200) / 2;
		int desiredInventoryLeft = vanillaInventoryLeft + leftPanelExtension(data.leftPouchCapacity());
		int overflow = Math.max(0, desiredInventoryLeft + combinedRightEdge(data.rightPouchCapacity()) - screenWidth);
		int vanillaBookLeft = (screenWidth - RECIPE_BOOK_WIDTH) / 2 - RECIPE_BOOK_X_OFFSET;
		int tabSafeShift = Math.max(0, vanillaBookLeft - RECIPE_BOOK_TAB_OVERHANG);
		return Math.min(overflow, tabSafeShift);
	}

	public static int satchelPanelHeight(int capacity) {
		return capacity <= 0 ? 0 : capacity <= 9 ? 28 : 46;
	}

	/** Centers the vanilla inventory and its attached satchel panel as one combined surface. */
	public static int satchelCenteringOffset(int capacity) {
		return satchelPanelHeight(capacity) / 2;
	}

	public static int recipeBookButtonY(int screenHeight, int satchelCapacity) {
		return screenHeight / 2 - 22 - satchelCenteringOffset(satchelCapacity);
	}

	public static boolean isActiveMenuSlot(int menuSlot, IntegratedInventoryData data) {
		for (IntegratedInventoryRole role : IntegratedInventoryRole.values()) {
			int start = menuStart(role);
			if (menuSlot >= start && menuSlot < start + data.capacity(role)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isInsideActivePanel(double relativeX, double relativeY, IntegratedInventoryData data) {
		int leftWidth = leftPanelWidth(data.leftPouchCapacity());
		if (leftWidth > 0
			&& relativeX >= -leftWidth + SIDE_PANEL_OVERLAP
			&& relativeX < SIDE_PANEL_OVERLAP
			&& relativeY >= POUCH_PANEL_Y
			&& relativeY < POUCH_PANEL_Y + POUCH_PANEL_HEIGHT) {
			return true;
		}
		int rightWidth = rightPanelWidth(data.rightPouchCapacity());
		if (rightWidth > 0
			&& relativeX >= RIGHT_POUCH_PANEL_X
			&& relativeX < RIGHT_POUCH_PANEL_X + rightWidth
			&& relativeY >= POUCH_PANEL_Y
			&& relativeY < POUCH_PANEL_Y + POUCH_PANEL_HEIGHT) {
			return true;
		}
		int satchelHeight = satchelPanelHeight(data.satchelCapacity());
		return satchelHeight > 0
			&& relativeX >= SATCHEL_PANEL_X
			&& relativeX < SATCHEL_PANEL_X + VANILLA_IMAGE_WIDTH
			&& relativeY >= SATCHEL_PANEL_Y
			&& relativeY < SATCHEL_PANEL_Y + satchelHeight;
	}
}
