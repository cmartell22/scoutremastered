package com.example.scout26.client;

import com.example.scout26.IntegratedInventoryData;
import com.example.scout26.IntegratedInventoryLayout;
import com.example.scout26.IntegratedInventoryMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
		int centeredVanillaTop = (screen.height - layout.scout26$imageHeight()) / 2;
		int offset = IntegratedInventoryLayout.satchelCenteringOffset(
			menu.scout26$clientLayoutData().satchelCapacity()
		);
		layout.scout26$setTopPos(centeredVanillaTop - offset);
	}

	static void render(InventoryScreen screen, GuiGraphicsExtractor graphics) {
		if (!(screen instanceof IntegratedScreenLayoutAccess layout)
			|| !(screen.getMenu() instanceof IntegratedInventoryMenu menu)) {
			return;
		}
		IntegratedInventoryData data = menu.scout26$clientLayoutData();
		if (!data.hasAnyBag()) {
			return;
		}
		int left = layout.scout26$leftPos();
		int top = layout.scout26$topPos();
		int leftWidth = IntegratedInventoryLayout.leftPanelWidth(data.leftPouchCapacity());
		int rightWidth = IntegratedInventoryLayout.rightPanelWidth(data.rightPouchCapacity());
		int satchelHeight = IntegratedInventoryLayout.satchelPanelHeight(data.satchelCapacity());
		if (leftWidth > 0) {
			drawPanel(
				graphics,
				left - leftWidth + IntegratedInventoryLayout.SIDE_PANEL_OVERLAP,
				top + IntegratedInventoryLayout.POUCH_PANEL_Y,
				leftWidth,
				IntegratedInventoryLayout.POUCH_PANEL_HEIGHT
			);
		}
		if (rightWidth > 0) {
			drawPanel(
				graphics,
				left + IntegratedInventoryLayout.RIGHT_POUCH_PANEL_X,
				top + IntegratedInventoryLayout.POUCH_PANEL_Y,
				rightWidth,
				IntegratedInventoryLayout.POUCH_PANEL_HEIGHT
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

	private static void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.fill(x, y, x + 18, y + 18, SLOT_HIGHLIGHT);
		graphics.fill(x, y, x + 17, y + 17, SLOT_BORDER);
		graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_COLOR);
	}
}
