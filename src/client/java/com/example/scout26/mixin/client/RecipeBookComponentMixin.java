package com.example.scout26.mixin.client;

import com.example.scout26.IntegratedInventoryLayout;
import com.example.scout26.IntegratedInventoryMenu;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ADR-012 target: {@link RecipeBookComponent}.
 *
 * <p>Reason: reserve the active left-pouch width after vanilla computes wide recipe-book layout.
 * Failure mode: book and pouch slots overlap. Version risk: medium and presentation-only; the exact
 * two-int/one-int descriptor requires one match.</p>
 */
@Mixin(RecipeBookComponent.class)
abstract class RecipeBookComponentMixin {
	@Shadow @Final protected RecipeBookMenu menu;
	@Shadow private boolean widthTooNarrow;
	@Shadow public abstract boolean isVisible();

	@Inject(method = "updateScreenPosition(II)I", at = @At("RETURN"), cancellable = true, require = 1)
	private void scout26$reserveLeftPouchWidth(int width, int imageWidth, CallbackInfoReturnable<Integer> callback) {
		if (this.isVisible() && !this.widthTooNarrow && this.menu instanceof IntegratedInventoryMenu integrated) {
			int offset = IntegratedInventoryLayout.leftPanelWidth(integrated.scout26$clientLayoutData().leftPouchCapacity());
			if (offset > 0) {
				callback.setReturnValue(callback.getReturnValue() + offset);
			}
		}
	}
}
