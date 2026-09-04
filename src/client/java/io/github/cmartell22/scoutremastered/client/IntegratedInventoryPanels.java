package io.github.cmartell22.scoutremastered.client;

import io.github.cmartell22.scoutremastered.IntegratedInventoryData;
import io.github.cmartell22.scoutremastered.IntegratedInventoryLayout;
import io.github.cmartell22.scoutremastered.IntegratedInventoryMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;

/** ScreenEvents background renderer for the three classic external panels. */
final class IntegratedInventoryPanels {
	private static final int PANEL_COLOR = 0xFF9A6847;
	private static final int PANEL_BORDER = 0xFF3A2418;
	private static final int SLOT_BORDER = 0xFF4A2D1E;
	private static final int SLOT_COLOR = 0xFF70452E;
	private static final int SLOT_HIGHLIGHT = 0xFFC99A73;

	private IntegratedInventoryPanels() {
	}

	static void reposition(InventoryScreen screen) {
		if (!(screen instanceof IntegratedScreenLayoutAccess layout)
			|| !(screen.getMenu() instanceof IntegratedInventoryMenu menu)) {
			return;
		}
		int centeredVanillaTop = (screen.height - layout.scoutremastered$imageHeight()) / 2;
		int offset = IntegratedInventoryLayout.satchelCenteringOffset(
			menu.scoutremastered$clientLayoutData().satchelCapacity()
		);
		layout.scoutremastered$setTopPos(centeredVanillaTop - offset);
		int recipeButtonY = IntegratedInventoryLayout.recipeBookButtonY(
			screen.height,
			menu.scoutremastered$clientLayoutData().satchelCapacity()
		);
		for (var child : screen.children()) {
			if (child instanceof ImageButton button && button.getWidth() == 20 && button.getHeight() == 18) {
				button.setY(recipeButtonY);
			}
		}
	}

	static void render(InventoryScreen screen, GuiGraphicsExtractor graphics) {
		if (!(screen instanceof IntegratedScreenLayoutAccess layout)
			|| !(screen.getMenu() instanceof IntegratedInventoryMenu menu)) {
			return;
		}
		// Vanilla resets the recipe button to its unshifted Y whenever the book is toggled.
		// Normalize after input handling and before this frame extracts its widgets.
		reposition(screen);
		IntegratedInventoryData data = menu.scoutremastered$clientLayoutData();
		if (!data.hasAnyBag()) {
			return;
		}
		int left = layout.scoutremastered$leftPos();
		int top = layout.scoutremastered$topPos();
		int leftWidth = IntegratedInventoryLayout.leftPanelWidth(data.leftPouchCapacity());
		int rightWidth = IntegratedInventoryLayout.rightPanelWidth(data.rightPouchCapacity());
		int satchelHeight = IntegratedInventoryLayout.satchelPanelHeight(data.satchelCapacity());
		if (leftWidth > 0) {
			drawSidePanel(
				graphics,
				left - leftWidth + IntegratedInventoryLayout.SIDE_PANEL_OVERLAP,
				top + IntegratedInventoryLayout.POUCH_PANEL_Y,
				leftWidth,
				IntegratedInventoryLayout.POUCH_PANEL_HEIGHT,
				true
			);
		}
		if (rightWidth > 0) {
			drawSidePanel(
				graphics,
				left + IntegratedInventoryLayout.RIGHT_POUCH_PANEL_X,
				top + IntegratedInventoryLayout.POUCH_PANEL_Y,
				rightWidth,
				IntegratedInventoryLayout.POUCH_PANEL_HEIGHT,
				false
			);
		}
		if (satchelHeight > 0) {
			drawPanel(
				graphics,
				left + IntegratedInventoryLayout.SATCHEL_PANEL_X,
				top + IntegratedInventoryLayout.SATCHEL_PANEL_Y,
				IntegratedInventoryLayout.VANILLA_IMAGE_WIDTH,
				satchelHeight
			);
		}
		for (int index = IntegratedInventoryLayout.SATCHEL_START;
			index < IntegratedInventoryLayout.TOTAL_SLOT_COUNT;
			index++) {
			Slot slot = screen.getMenu().getSlot(index);
			if (IntegratedInventoryLayout.isActiveMenuSlot(index, data)) {
				drawSlot(graphics, left + slot.x - 1, top + slot.y - 1);
				if (isReadySlot(index)) {
					ReadySlotMarker.render(graphics, left + slot.x, top + slot.y);
				}
			}
		}
	}

	private static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		// Two-unit cut corners reproduce Scout's diagonal joins without a texture dependency.
		graphics.fill(x + 2, y, x + width - 2, y + 1, PANEL_BORDER);
		graphics.fill(x + 1, y + 1, x + width - 1, y + 2, PANEL_BORDER);
		graphics.fill(x, y + 2, x + width, y + height - 2, PANEL_BORDER);
		graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, PANEL_BORDER);
		graphics.fill(x + 2, y + height - 1, x + width - 2, y + height, PANEL_BORDER);
		graphics.fill(x + 2, y + 1, x + width - 2, y + 2, PANEL_COLOR);
		graphics.fill(x + 1, y + 2, x + width - 1, y + height - 2, PANEL_COLOR);
		graphics.fill(x + 2, y + height - 2, x + width - 2, y + height - 1, PANEL_COLOR);
	}

	private static void drawSidePanel(
		GuiGraphicsExtractor graphics,
		int x,
		int y,
		int width,
		int height,
		boolean joinsVanillaOnRight
	) {
		int join = IntegratedInventoryLayout.SIDE_PANEL_OVERLAP;
		for (int row = 0; row < height; row++) {
			int distanceFromEnd = Math.min(row, height - 1 - row);
			int joinInset = Math.max(0, join - distanceFromEnd);
			int outerInset = distanceFromEnd == 0 ? 2 : distanceFromEnd == 1 ? 1 : 0;
			int rowLeft = x + (joinsVanillaOnRight ? outerInset : joinInset);
			int rowRight = x + width - (joinsVanillaOnRight ? joinInset : outerInset);
			graphics.fill(rowLeft, y + row, rowRight, y + row + 1, PANEL_COLOR);
		}

		// Border only the exposed outer body. The seven-unit join over vanilla remains brown,
		// including its diagonal, so it cannot shadow either outer vanilla inventory slot.
		if (joinsVanillaOnRight) {
			int joinStart = x + width - join;
			graphics.fill(x + 2, y, joinStart, y + 1, PANEL_BORDER);
			graphics.fill(x + 1, y + 1, x + 2, y + 2, PANEL_BORDER);
			graphics.fill(x, y + 2, x + 1, y + height - 2, PANEL_BORDER);
			graphics.fill(x + 1, y + height - 2, x + 2, y + height - 1, PANEL_BORDER);
			graphics.fill(x + 2, y + height - 1, joinStart, y + height, PANEL_BORDER);
		} else {
			int joinEnd = x + join;
			graphics.fill(joinEnd, y, x + width - 2, y + 1, PANEL_BORDER);
			graphics.fill(x + width - 2, y + 1, x + width - 1, y + 2, PANEL_BORDER);
			graphics.fill(x + width - 1, y + 2, x + width, y + height - 2, PANEL_BORDER);
			graphics.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, PANEL_BORDER);
			graphics.fill(joinEnd, y + height - 1, x + width - 2, y + height, PANEL_BORDER);
		}
	}

	private static boolean isReadySlot(int menuIndex) {
		return menuIndex == IntegratedInventoryLayout.SATCHEL_START
			|| menuIndex == IntegratedInventoryLayout.LEFT_POUCH_START
			|| menuIndex == IntegratedInventoryLayout.RIGHT_POUCH_START;
	}

	private static void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.fill(x, y, x + 18, y + 18, SLOT_HIGHLIGHT);
		graphics.fill(x, y, x + 17, y + 17, SLOT_BORDER);
		graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_COLOR);
	}
}
