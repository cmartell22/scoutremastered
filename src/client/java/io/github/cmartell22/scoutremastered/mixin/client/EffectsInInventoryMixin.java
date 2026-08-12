package io.github.cmartell22.scoutremastered.mixin.client;

import io.github.cmartell22.scoutremastered.IntegratedInventoryLayout;
import io.github.cmartell22.scoutremastered.IntegratedInventoryMenu;
import io.github.cmartell22.scoutremastered.client.IntegratedScreenLayoutAccess;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * ADR-012 target: {@link EffectsInInventory}.
 *
 * <p>Reason: treat the active right-pouch panel as part of inventory width in visibility and render
 * extraction. Failure mode: effect panels overlap pouch slots. Version risk: medium and
 * presentation-only; each exact field read requires one match.</p>
 */
@Mixin(EffectsInInventory.class)
abstract class EffectsInInventoryMixin {
	@Redirect(
		method = "canSeeEffects()Z",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;imageWidth:I"),
		require = 1
	)
	private int scoutremastered$offsetEffectVisibility(AbstractContainerScreen<?> screen) {
		return scoutremastered$effectiveImageWidth(screen);
	}

	@Redirect(
		method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;imageWidth:I"),
		require = 1
	)
	private int scoutremastered$offsetEffectRendering(AbstractContainerScreen<?> screen) {
		return scoutremastered$effectiveImageWidth(screen);
	}

	private static int scoutremastered$effectiveImageWidth(AbstractContainerScreen<?> screen) {
		int vanillaWidth = ((IntegratedScreenLayoutAccess)screen).scoutremastered$imageWidth();
		if (screen instanceof InventoryScreen && screen.getMenu() instanceof IntegratedInventoryMenu menu) {
			return vanillaWidth + IntegratedInventoryLayout.rightPanelWidth(
				menu.scoutremastered$clientLayoutData().rightPouchCapacity()
			);
		}
		return vanillaWidth;
	}
}
