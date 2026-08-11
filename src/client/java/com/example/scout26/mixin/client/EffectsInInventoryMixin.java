package com.example.scout26.mixin.client;

import com.example.scout26.IntegratedInventoryLayout;
import com.example.scout26.IntegratedInventoryMenu;
import com.example.scout26.client.IntegratedScreenLayoutAccess;
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
	private int scout26$offsetEffectVisibility(AbstractContainerScreen<?> screen) {
		return scout26$effectiveImageWidth(screen);
	}

	@Redirect(
		method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;imageWidth:I"),
		require = 1
	)
	private int scout26$offsetEffectRendering(AbstractContainerScreen<?> screen) {
		return scout26$effectiveImageWidth(screen);
	}

	private static int scout26$effectiveImageWidth(AbstractContainerScreen<?> screen) {
		int vanillaWidth = ((IntegratedScreenLayoutAccess)screen).scout26$imageWidth();
		if (screen instanceof InventoryScreen && screen.getMenu() instanceof IntegratedInventoryMenu menu) {
			return vanillaWidth + IntegratedInventoryLayout.rightPanelWidth(
				menu.scout26$integratedInventoryData().rightPouchCapacity()
			);
		}
		return vanillaWidth;
	}
}
