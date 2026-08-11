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
	public static final int SATCHEL_X = 8;
	public static final int SATCHEL_Y = 174;
	public static final int LEFT_INNER_X = -20;
	public static final int RIGHT_INNER_X = 180;
	public static final int POUCH_Y = 17;

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

	public static int satchelPanelHeight(int capacity) {
		return capacity <= 0 ? 0 : capacity <= 9 ? 24 : 42;
	}

	public static boolean isInsideActivePanel(double relativeX, double relativeY, IntegratedInventoryData data) {
		int leftWidth = leftPanelWidth(data.leftPouchCapacity());
		if (leftWidth > 0 && relativeX >= -leftWidth && relativeX < 0 && relativeY >= 11 && relativeY < 77) {
			return true;
		}
		int rightWidth = rightPanelWidth(data.rightPouchCapacity());
		if (rightWidth > 0 && relativeX >= 176 && relativeX < 176 + rightWidth && relativeY >= 11 && relativeY < 77) {
			return true;
		}
		int satchelHeight = satchelPanelHeight(data.satchelCapacity());
		return satchelHeight > 0
			&& relativeX >= 2
			&& relativeX < 174
			&& relativeY >= 168
			&& relativeY < 168 + satchelHeight;
	}
}
