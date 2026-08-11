package com.example.scout26.client;

import com.example.scout26.PackMenu;
import com.example.scout26.PackMenuData;
import com.example.scout26.PackMenuLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Minimal extractor-era screen for the dedicated unified pack menu. */
public final class PackScreen extends AbstractContainerScreen<PackMenu> {
	private static final int PANEL_COLOR = 0xFFC6C6C6;
	private static final int PANEL_BORDER = 0xFF303030;
	private static final int SLOT_BORDER = 0xFF373737;
	private static final int SLOT_COLOR = 0xFF8B8B8B;
	private static final int SLOT_HIGHLIGHT = 0xFFFFFFFF;
	private static final int LABEL_COLOR = 0xFF404040;
	private static final Component SATCHEL = Component.translatable("container.scout26.satchel");
	private static final Component LEFT_POUCH = Component.translatable("container.scout26.left_pouch");
	private static final Component RIGHT_POUCH = Component.translatable("container.scout26.right_pouch");

	public PackScreen(PackMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, PackMenuLayout.IMAGE_WIDTH, menu.layout().imageHeight());
		this.inventoryLabelY = menu.layout().inventoryLabelY();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int x = this.leftPos;
		int y = this.topPos;
		graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL_COLOR);
		graphics.outline(x, y, this.imageWidth, this.imageHeight, PANEL_BORDER);

		for (Slot slot : this.menu.slots) {
			int slotX = x + slot.x - 1;
			int slotY = y + slot.y - 1;
			graphics.fill(slotX, slotY, slotX + 18, slotY + 18, SLOT_BORDER);
			graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, SLOT_COLOR);
			graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 2, SLOT_HIGHLIGHT);
			graphics.fill(slotX + 1, slotY + 1, slotX + 2, slotY + 17, SLOT_HIGHLIGHT);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		PackMenuData data = this.menu.data();
		PackMenuLayout layout = this.menu.layout();
		graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
		if (data.satchelCapacity() > 0) {
			graphics.text(this.font, SATCHEL, 8, layout.satchelLabelY(), LABEL_COLOR, false);
		}
		if (data.leftPouchCapacity() > 0) {
			graphics.text(this.font, LEFT_POUCH, 8, layout.pouchLabelY(), LABEL_COLOR, false);
		}
		if (data.rightPouchCapacity() > 0) {
			graphics.text(this.font, RIGHT_POUCH, PackMenuLayout.RIGHT_POUCH_X, layout.pouchLabelY(), LABEL_COLOR, false);
		}
		graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, layout.inventoryLabelY(), LABEL_COLOR, false);
	}

	@Override
	public Component getNarrationMessage() {
		PackMenuData data = this.menu.data();
		return Component.translatable(
			"narration.scout26.pack",
			this.title,
			capacityNarration(data.satchelCapacity()),
			capacityNarration(data.leftPouchCapacity()),
			capacityNarration(data.rightPouchCapacity())
		);
	}

	private static Component capacityNarration(int capacity) {
		return capacity > 0
			? Component.translatable("narration.scout26.slot_count", capacity)
			: Component.translatable("narration.scout26.not_equipped");
	}
}
