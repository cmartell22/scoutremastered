package io.github.cmartell22.scoutremastered;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** ScoutRemastered synchronized menu registrations. */
public final class ModMenus {
	public static final ExtendedMenuType<PackMenu, PackMenuData> PACK = Registry.register(
		BuiltInRegistries.MENU,
		Identifier.fromNamespaceAndPath(ScoutRemasteredMod.MOD_ID, "pack"),
		new ExtendedMenuType<>(PackMenu::new, PackMenuData.STREAM_CODEC)
	);

	private ModMenus() {
	}

	public static void initialize() {
		ScoutRemasteredMod.LOGGER.debug("ScoutRemastered pack menu registered");
	}
}
