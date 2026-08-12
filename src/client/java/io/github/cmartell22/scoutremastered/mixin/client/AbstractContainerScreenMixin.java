package io.github.cmartell22.scoutremastered.mixin.client;

import io.github.cmartell22.scoutremastered.IntegratedInventoryData;
import io.github.cmartell22.scoutremastered.IntegratedInventoryLayout;
import io.github.cmartell22.scoutremastered.IntegratedInventoryMenu;
import io.github.cmartell22.scoutremastered.client.IntegratedScreenLayoutAccess;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;

/**
 * ADR-012 target: {@link AbstractContainerScreen}.
 *
 * <p>Reason: expose protected layout coordinates to ScreenEvents and correct the outside-click
 * result after virtual recipe-book handling. Failure mode: a carried stack could be thrown when an
 * external active panel is misclassified. Version risk: medium; two exact input call sites require
 * one match each.</p>
 */
@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin implements IntegratedScreenLayoutAccess {
	@ModifyExpressionValue(
		method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;hasClickedOutside(DDII)Z"
		),
		require = 1
	)
	private boolean scoutremastered$keepPanelClicksInside(boolean original, MouseButtonEvent event, boolean doubleClick) {
		return this.scoutremastered$correctOutsideResult(original, event.x(), event.y());
	}

	@ModifyExpressionValue(
		method = "mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;hasClickedOutside(DDII)Z"
		),
		require = 1
	)
	private boolean scoutremastered$keepPanelReleasesInside(boolean original, MouseButtonEvent event) {
		return this.scoutremastered$correctOutsideResult(original, event.x(), event.y());
	}

	private boolean scoutremastered$correctOutsideResult(boolean original, double mouseX, double mouseY) {
		if (!original
			|| !((Object)this instanceof InventoryScreen inventoryScreen)
			|| !(inventoryScreen.getMenu() instanceof IntegratedInventoryMenu menu)) {
			return original;
		}
		IntegratedInventoryData data = menu.scoutremastered$clientLayoutData();
		return !IntegratedInventoryLayout.isInsideActivePanel(
			mouseX - this.scoutremastered$leftPos(),
			mouseY - this.scoutremastered$topPos(),
			data
		);
	}

	@Accessor("leftPos")
	@Override
	public abstract int scoutremastered$leftPos();

	@Accessor("topPos")
	@Override
	public abstract int scoutremastered$topPos();

	@Accessor("topPos")
	@Override
	public abstract void scoutremastered$setTopPos(int topPos);

	@Accessor("imageWidth")
	@Override
	public abstract int scoutremastered$imageWidth();

	@Accessor("imageHeight")
	@Override
	public abstract int scoutremastered$imageHeight();
}
