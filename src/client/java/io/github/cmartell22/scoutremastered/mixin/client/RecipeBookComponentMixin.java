package io.github.cmartell22.scoutremastered.mixin.client;

import io.github.cmartell22.scoutremastered.IntegratedInventoryLayout;
import io.github.cmartell22.scoutremastered.IntegratedInventoryMenu;
import java.util.List;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ADR-012 target: {@link RecipeBookComponent}.
 *
 * <p>Reason: reserve the exposed left-pouch width and translate the complete wide composition when
 * its right pouch would overflow. Failure mode: the book overlaps a pouch or the right pouch is
 * clipped. Version risk: medium and presentation-only; both exact descriptors require one match.</p>
 */
@Mixin(RecipeBookComponent.class)
abstract class RecipeBookComponentMixin {
	@Shadow @Final protected RecipeBookMenu menu;
	@Shadow @Final private List<RecipeBookTabButton> tabButtons;
	@Shadow private int width;
	@Shadow private boolean widthTooNarrow;
	@Shadow public abstract boolean isVisible();

	@Inject(method = "getXOrigin()I", at = @At("RETURN"), cancellable = true, require = 1)
	private void scoutremastered$shiftRecipeBookWithInventory(CallbackInfoReturnable<Integer> callback) {
		// setVisible(true) builds the book's child widgets before vanilla flips its visible flag.
		// Key this shared origin only to wide mode so that first construction and later rendering agree.
		if (!this.widthTooNarrow && this.menu instanceof IntegratedInventoryMenu integrated) {
			int shift = IntegratedInventoryLayout.wideRecipeBookShift(
				this.width,
				integrated.scoutremastered$clientLayoutData()
			);
			callback.setReturnValue(callback.getReturnValue() - shift);
		}
	}

	@Inject(method = "updateTabs(Z)V", at = @At("TAIL"), require = 1)
	private void scoutremastered$shiftRecipeBookTabs(boolean isFiltering, CallbackInfo callback) {
		if (!this.widthTooNarrow && this.menu instanceof IntegratedInventoryMenu integrated) {
			int shift = IntegratedInventoryLayout.wideRecipeBookShift(
				this.width,
				integrated.scoutremastered$clientLayoutData()
			);
			if (shift > 0) {
				for (RecipeBookTabButton tab : this.tabButtons) {
					tab.setPosition(tab.getX() - shift, tab.getY());
				}
			}
		}
	}

	@Inject(method = "updateScreenPosition(II)I", at = @At("RETURN"), cancellable = true, require = 1)
	private void scoutremastered$positionCompleteWideLayout(int width, int imageWidth, CallbackInfoReturnable<Integer> callback) {
		if (this.isVisible() && !this.widthTooNarrow && this.menu instanceof IntegratedInventoryMenu integrated) {
			var data = integrated.scoutremastered$clientLayoutData();
			int exposedLeft = IntegratedInventoryLayout.leftPanelExtension(data.leftPouchCapacity());
			int shift = IntegratedInventoryLayout.wideRecipeBookShift(width, data);
			callback.setReturnValue(callback.getReturnValue() + exposedLeft - shift);
		}
	}
}
