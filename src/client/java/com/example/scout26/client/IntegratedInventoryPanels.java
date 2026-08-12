package com.example.scout26.client;

import com.example.scout26.IntegratedInventoryData;
import com.example.scout26.IntegratedInventoryLayout;
import com.example.scout26.IntegratedInventoryMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;

/** ScreenEvents background renderer for the three classic external panels. */
final class IntegratedInventoryPanels {
	private static final int PANEL_COLOR = 0xFFC6C6C6;
	private static final int PANEL_BORDER = 0xFF303030;
	private static final int SLOT_BORDER = 0xFF373737;
	private static final int SLOT_COLOR = 0xFF8B8B8B;
	private static final int SLOT_HIGHLIGHT = 0xFFFFFFFF;

	private IntegratedInventoryPanels() {
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
			drawPanel(graphics, left - leftWidth, top + 11, leftWidth, 66);
		}
		if (rightWidth > 0) {
			drawPanel(graphics, left + 176, top + 11, rightWidth, 66);
		}
		if (satchelHeight > 0) {
			drawPanel(graphics, left + 2, top + 168, 172, satchelHeight);
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
		graphics.fill(x, y, x + width, y + height, PANEL_COLOR);
		graphics.outline(x, y, width, height, PANEL_BORDER);
	}

	private static void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.fill(x, y, x + 18, y + 18, SLOT_HIGHLIGHT);
		graphics.fill(x, y, x + 17, y + 17, SLOT_BORDER);
		graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_COLOR);
	}
}
