package io.github.cmartell22.scoutremastered.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Faded eye-and-swap watermark drawn beneath items in Scout's three ready slots. */
final class ReadySlotMarker {
	private static final int SIZE = 16;
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
		"scoutremastered",
		"textures/gui/ready_slot_icon.png"
	);

	private ReadySlotMarker() {
	}

	static void render(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);
	}
}
