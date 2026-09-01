package io.github.cmartell22.scoutremastered.client;

import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig.Category;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig.Position;
import io.github.cmartell22.scoutremastered.ReadySlotPresentationConfig.Transform;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** RS7A client-only, live-preview editor for bounded Ready Slots presentation settings. */
final class ReadySlotConfigScreen extends Screen {
	private static final int PANEL = 0xCC171717;
	private static final int PANEL_BORDER = 0xFF707070;
	private static final int TEXT = 0xFFE0E0E0;
	private static final int MUTED = 0xFFA0A0A0;
	private static final int ERROR = 0xFFFF6060;
	private static final int SUCCESS = 0xFF60FF80;
	private static final int ROW_HEIGHT = 12;
	private static final String DEFAULT_ITEM_ID = "minecraft:diamond_sword";
	private static final List<Category> EDITABLE_CATEGORIES = List.of(
		Category.SWORD, Category.AXE, Category.PICKAXE, Category.SHOVEL, Category.HOE,
		Category.SPEAR, Category.BOW, Category.CROSSBOW, Category.SHIELD, Category.TRIDENT
	);

	private final ReadySlotPresentationConfig openingConfig;
	private ReadySlotPresentationConfig draft;
	private Mode mode = Mode.TRANSFORM;
	private Scope scope = Scope.BASE;
	private Position position = Position.LEFT_HIP;
	private Category category = Category.SWORD;
	private PolicyList policyView = PolicyList.WHITELIST;
	private String itemId = DEFAULT_ITEM_ID;
	private Transform copiedTransform;
	private float previewYaw;
	private float previewPitch;
	private boolean finished;
	private boolean syncingWidgets;
	private Component status = Component.translatable("screen.scoutremastered.ready_slots.unsaved");
	private int statusColor = MUTED;
	private CycleButton<Category> categoryButton;
	private PolicyListWidget policyList;
	private final Map<TransformField, BoundedSlider> sliders = new EnumMap<>(TransformField.class);
	private final Map<TransformField, EditBox> numericBoxes = new EnumMap<>(TransformField.class);

	ReadySlotConfigScreen() {
		super(Component.translatable("screen.scoutremastered.ready_slots.title"));
		this.openingConfig = ReadySlotConfig.current();
		this.draft = this.openingConfig;
	}

	@Override
	protected void init() {
		this.sliders.clear();
		this.numericBoxes.clear();
		this.categoryButton = null;
		this.policyList = null;
		int controlsX = controlsX();
		int controlsWidth = Math.max(240, this.width - controlsX - 10);
		int half = (controlsWidth - 4) / 2;
		addRenderableWidget(CycleButton.builder(Mode::label, this.mode)
			.withValues(List.of(Mode.values()))
			.create(controlsX, 25, controlsWidth, 20, Component.translatable("screen.scoutremastered.ready_slots.mode"), (button, value) -> {
				this.mode = value;
				rebuildWidgets();
			}));
		if (this.mode == Mode.TRANSFORM) {
			buildTransformControls(controlsX, controlsWidth, half);
		} else {
			buildPolicyControls(controlsX, controlsWidth, half);
		}
		buildPreviewControls();
		int bottom = this.height - 24;
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.save"), button -> save())
			.bounds(controlsX, bottom, half, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.cancel"), button -> cancel())
			.bounds(controlsX + half + 4, bottom, controlsWidth - half - 4, 20).build());
	}

	private void buildTransformControls(int x, int width, int half) {
		addRenderableWidget(CycleButton.builder(Scope::label, this.scope)
			.withValues(List.of(Scope.values()))
			.create(x, 47, half, 20, Component.translatable("screen.scoutremastered.ready_slots.scope"), (button, value) -> {
				this.scope = value;
				if (value == Scope.ITEM) synchronizeCategoryFromItem();
				rebuildWidgets();
			}));
		addRenderableWidget(CycleButton.builder(value -> Component.literal(value.id()), this.position)
			.withValues(List.of(Position.values()))
			.create(x + half + 4, 47, width - half - 4, 20, Component.translatable("screen.scoutremastered.ready_slots.position"), (button, value) -> {
				this.position = value;
				rebuildWidgets();
			}));
		if (this.scope != Scope.BASE) {
			Component categoryTitle = Component.translatable(this.scope == Scope.ITEM
				? "screen.scoutremastered.ready_slots.item_category"
				: "screen.scoutremastered.ready_slots.category");
			this.categoryButton = CycleButton.builder(ReadySlotConfigScreen::categoryLabel, this.category)
				.withValues(EDITABLE_CATEGORIES)
				.create(x, 69, half, 20, categoryTitle, (button, value) -> {
					this.category = value;
					if (this.scope == Scope.ITEM) assignCurrentItemCategory();
					rebuildWidgets();
				});
			addRenderableWidget(this.categoryButton);
		}
		if (this.scope == Scope.CATEGORY) {
			addRenderableWidget(Checkbox.builder(Component.translatable("screen.scoutremastered.ready_slots.visible"), this.font)
				.pos(x + half + 8, 71).maxWidth(width - half - 8)
				.selected(this.draft.categoryEnabled(this.category))
				.onValueChange((checkbox, enabled) -> updateDraft(this.draft.withCategoryEnabled(this.category, enabled)))
				.build());
		} else if (this.scope == Scope.ITEM) {
			addRenderableWidget(itemIdBox(x + half + 4, 69, width - half - 4));
		}
		int firstRow = this.scope == Scope.BASE ? 69 : 91;
		int labelWidth = 36;
		int valueWidth = 54;
		int mirrorWidth = 18;
		int sliderWidth = width - labelWidth - valueWidth - mirrorWidth - 10;
		Transform transform = selectedTransform();
		for (TransformField field : TransformField.values()) {
			int rowY = firstRow + field.ordinal() * ROW_HEIGHT;
			BoundedSlider slider = new BoundedSlider(x + labelWidth, rowY, sliderWidth, ROW_HEIGHT, field,
				field.read(transform), value -> applyField(field, value));
			this.sliders.put(field, slider);
			addRenderableWidget(slider);
			int valueX = x + labelWidth + sliderWidth + 4;
			EditBox numeric = new EditBox(this.font, valueX, rowY, valueWidth, ROW_HEIGHT, field.label());
			numeric.setMaxLength(12);
			numeric.setValue(format(field.read(transform)));
			numeric.setResponder(value -> applyNumeric(field, value));
			this.numericBoxes.put(field, numeric);
			addRenderableWidget(numeric);
			if (field.mirrorable()) {
				Button mirror = Button.builder(Component.literal("M"), button -> mirrorField(field))
					.bounds(valueX + valueWidth + 2, rowY, mirrorWidth, ROW_HEIGHT).build();
				mirror.setTooltip(Tooltip.create(Component.translatable("screen.scoutremastered.ready_slots.mirror_field", field.label())));
				addRenderableWidget(mirror);
			}
		}
		int actionsY = firstRow + TransformField.values().length * ROW_HEIGHT + 2;
		int buttonWidth = (width - 8) / 3;
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.reset"), button -> resetTransform())
			.bounds(x, actionsY, buttonWidth, 16).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.copy"), button -> copyTransform())
			.bounds(x + buttonWidth + 4, actionsY, buttonWidth, 16).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.paste"), button -> pasteTransform())
			.bounds(x + (buttonWidth + 4) * 2, actionsY, width - (buttonWidth + 4) * 2, 16).build());
	}

	private void buildPolicyControls(int x, int width, int half) {
		addRenderableWidget(Button.builder(policyTabLabel(PolicyList.WHITELIST), button -> selectPolicyList(PolicyList.WHITELIST))
			.bounds(x, 47, half, 20).build());
		addRenderableWidget(Button.builder(policyTabLabel(PolicyList.BLACKLIST), button -> selectPolicyList(PolicyList.BLACKLIST))
			.bounds(x + half + 4, 47, width - half - 4, 20).build());
		int addWidth = 54;
		addRenderableWidget(itemIdBox(x, 69, width - addWidth - 4));
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.add"), button -> addPolicyItem())
			.bounds(x + width - addWidth, 69, addWidth, 20).build());
		int listY;
		if (this.policyView == PolicyList.WHITELIST) {
			this.categoryButton = CycleButton.builder(ReadySlotConfigScreen::categoryLabel, this.category)
				.withValues(EDITABLE_CATEGORIES)
				.create(x, 91, width, 20, Component.translatable("screen.scoutremastered.ready_slots.item_category"), (button, value) -> this.category = value);
			addRenderableWidget(this.categoryButton);
			listY = 113;
		} else {
			listY = 91;
		}
		int listBottom = this.height - 68;
		this.policyList = new PolicyListWidget(x, Math.max(24, listBottom - listY), listY, width);
		addRenderableWidget(this.policyList);
		int actionsY = this.height - 64;
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.reset_default"), button -> resetPolicyList())
			.bounds(x, actionsY, half, 18).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.clear_all"), button -> clearPolicyList())
			.bounds(x + half + 4, actionsY, width - half - 4, 18).build());
	}

	private void buildPreviewControls() {
		int x = 12;
		int width = controlsX() - 26;
		int y = this.height - 43;
		int small = Math.max(18, (width - 12) / 6);
		addRenderableWidget(Button.builder(Component.literal("<"), button -> this.previewYaw -= 15.0F).bounds(x, y, small, 16).build());
		addRenderableWidget(Button.builder(Component.literal(">"), button -> this.previewYaw += 15.0F).bounds(x + small + 3, y, small, 16).build());
		addRenderableWidget(Button.builder(Component.literal("^"), button -> this.previewPitch = Math.max(-45.0F, this.previewPitch - 10.0F))
			.bounds(x + (small + 3) * 2, y, small, 16).build());
		addRenderableWidget(Button.builder(Component.literal("v"), button -> this.previewPitch = Math.min(45.0F, this.previewPitch + 10.0F))
			.bounds(x + (small + 3) * 3, y, small, 16).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.preview_reset"), button -> {
			this.previewYaw = 0.0F;
			this.previewPitch = 0.0F;
		}).bounds(x + (small + 3) * 4, y, Math.max(small, width - (small + 3) * 4), 16).build());
	}

	private EditBox itemIdBox(int x, int y, int width) {
		EditBox itemBox = new EditBox(this.font, x, y, width, 20, Component.translatable("screen.scoutremastered.ready_slots.item_id"));
		itemBox.setMaxLength(128);
		itemBox.setHint(Component.translatable("screen.scoutremastered.ready_slots.item_id_hint"));
		itemBox.setValue(this.itemId);
		itemBox.setResponder(value -> {
			this.itemId = value.trim();
			if (this.mode == Mode.TRANSFORM && this.scope == Scope.ITEM && isRegisteredItemId()) {
				synchronizeCategoryFromItem();
				if (this.categoryButton != null) this.categoryButton.setValue(this.category);
				synchronizeTransformWidgets();
			}
		});
		return itemBox;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int previewRight = controlsX() - 10;
		graphics.fill(8, 20, previewRight, this.height - 8, PANEL);
		graphics.outline(8, 20, previewRight - 8, this.height - 28, PANEL_BORDER);
		if (this.minecraft.player != null) {
			extractEntityPreview(graphics, 12, 28, previewRight - 4, this.height - 48,
				Math.min(65, Math.max(30, (previewRight - 16) / 2)), 0.0625F,
				this.previewYaw, this.previewPitch, this.minecraft.player);
		}
		graphics.text(this.font, this.title, 10, 8, TEXT, false);
		graphics.text(this.font, Component.translatable("screen.scoutremastered.ready_slots.preview"), 14, 24, MUTED, false);
		if (this.mode == Mode.TRANSFORM) {
			int firstRow = this.scope == Scope.BASE ? 69 : 91;
			for (TransformField field : TransformField.values()) {
				graphics.text(this.font, field.label(), controlsX(), firstRow + field.ordinal() * ROW_HEIGHT + 3, TEXT, false);
			}
		}
		int statusY = this.height - 43;
		List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(this.status, this.width - controlsX() - 12);
		for (int index = 0; index < Math.min(2, lines.size()); index++) {
			graphics.text(this.font, lines.get(index), controlsX(), statusY + index * 9, this.statusColor, false);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private static void extractEntityPreview(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1,
		int size, float offsetY, float yaw, float pitch, LivingEntity entity) {
		Quaternionf rotation = new Quaternionf().rotateZ((float)Math.PI);
		Quaternionf xRotation = new Quaternionf().rotateX(pitch * (float)(Math.PI / 180.0));
		rotation.mul(xRotation);
		EntityRenderState renderState = extractPreviewRenderState(entity);
		if (renderState instanceof LivingEntityRenderState livingRenderState) {
			livingRenderState.bodyRot = 180.0F + yaw;
			livingRenderState.yRot = yaw;
			livingRenderState.xRot = livingRenderState.pose == Pose.FALL_FLYING ? 0.0F : -pitch;
			livingRenderState.boundingBoxWidth /= livingRenderState.scale;
			livingRenderState.boundingBoxHeight /= livingRenderState.scale;
			livingRenderState.scale = 1.0F;
		}
		Vector3f translation = new Vector3f(0.0F, renderState.boundingBoxHeight / 2.0F + offsetY, 0.0F);
		graphics.entity(renderState, size, translation, rotation, xRotation, x0, y0, x1, y1);
	}

	private static EntityRenderState extractPreviewRenderState(LivingEntity entity) {
		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
		EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);
		renderState.shadowPieces.clear();
		renderState.outlineColor = 0;
		return renderState;
	}

	private int controlsX() { return Math.min(180, Math.max(132, this.width / 3)); }

	private Transform selectedTransform() {
		return switch (this.scope) {
			case BASE -> this.draft.baseTransform(this.position);
			case CATEGORY -> this.draft.resolveCategory(this.position, this.category);
			case ITEM -> isRegisteredItemId() ? this.draft.resolve(this.position, this.category, this.itemId)
				: this.draft.resolveCategory(this.position, this.category);
		};
	}

	private void applyField(TransformField field, float value) {
		if (this.syncingWidgets) return;
		applySelectedTransform(field.write(selectedTransform(), value));
		synchronizeTransformWidgets();
	}

	private void applyNumeric(TransformField field, String text) {
		if (this.syncingWidgets || text.isBlank() || text.equals("-") || text.equals(".")) return;
		try {
			float value = Float.parseFloat(text);
			if (!field.inBounds(value)) {
				setError(Component.translatable("screen.scoutremastered.ready_slots.out_of_bounds", format(field.minimum), format(field.maximum)));
				return;
			}
			applySelectedTransform(field.write(selectedTransform(), value));
			BoundedSlider slider = this.sliders.get(field);
			if (slider != null) {
				this.syncingWidgets = true;
				slider.setActualValue(value);
				this.syncingWidgets = false;
			}
		} catch (NumberFormatException exception) {
			setError(Component.translatable("screen.scoutremastered.ready_slots.invalid_number"));
		}
	}

	private boolean applySelectedTransform(Transform transform) {
		if (this.scope == Scope.ITEM && !requireValidItemId()) return false;
		try {
			ReadySlotPresentationConfig updated = switch (this.scope) {
				case BASE -> this.draft.withBaseTransformPropagatingOverrides(this.position, transform);
				case CATEGORY -> this.draft.withCategoryTransform(this.category, this.position, transform);
				case ITEM -> this.draft.withItemTransform(this.itemId, this.position, transform);
			};
			updateDraft(updated);
			return true;
		} catch (IllegalArgumentException exception) {
			setError(Component.translatable("screen.scoutremastered.ready_slots.adjustment_out_of_bounds"));
			return false;
		}
	}

	private void synchronizeTransformWidgets() {
		if (this.sliders.isEmpty()) return;
		Transform transform = selectedTransform();
		this.syncingWidgets = true;
		for (TransformField field : TransformField.values()) {
			float value = field.read(transform);
			this.sliders.get(field).setActualValue(value);
			this.numericBoxes.get(field).setValue(format(value));
		}
		this.syncingWidgets = false;
	}

	private void resetTransform() {
		ReadySlotPresentationConfig updated;
		switch (this.scope) {
			case BASE -> updated = this.draft.withBaseTransformPropagatingOverrides(this.position,
				ReadySlotConfig.bundledBaseline().baseTransform(this.position));
			case CATEGORY -> updated = this.draft.withoutCategoryOverride(this.category, this.position);
			case ITEM -> {
				if (!requireValidItemId()) return;
				updated = this.draft.withoutItemOverride(this.itemId, this.position);
			}
			default -> throw new IllegalStateException("Unexpected scope " + this.scope);
		}
		updateDraft(updated);
		synchronizeTransformWidgets();
		setSuccess(Component.translatable("screen.scoutremastered.ready_slots.reset_done"));
	}

	private void copyTransform() {
		this.copiedTransform = selectedTransform();
		setSuccess(Component.translatable("screen.scoutremastered.ready_slots.copied"));
	}

	private void pasteTransform() {
		if (this.copiedTransform == null) {
			setError(Component.translatable("screen.scoutremastered.ready_slots.nothing_copied"));
			return;
		}
		if (applySelectedTransform(this.copiedTransform)) {
			synchronizeTransformWidgets();
			setSuccess(Component.translatable("screen.scoutremastered.ready_slots.pasted"));
		}
	}

	private void mirrorField(TransformField field) {
		Transform current = selectedTransform();
		if (applySelectedTransform(field.write(current, field.mirrorValue(field.read(current))))) {
			synchronizeTransformWidgets();
			setSuccess(Component.translatable("screen.scoutremastered.ready_slots.field_mirrored", field.label()));
		}
	}

	private void synchronizeCategoryFromItem() {
		if (!isRegisteredItemId()) return;
		Category configured = this.draft.whitelistedCategory(this.itemId).orElse(null);
		Category builtIn = ReadySlotRenderPolicy.builtInCategory(
			new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(this.itemId))));
		if (configured != null && configured != Category.HANDHELD) this.category = configured;
		else if (builtIn != null) this.category = builtIn;
	}

	private void assignCurrentItemCategory() {
		if (!requireValidItemId()) return;
		updateDraft(this.draft.withoutBlacklistedItem(this.itemId).withWhitelistedItem(this.itemId, this.category));
		setSuccess(Component.translatable("screen.scoutremastered.ready_slots.category_assigned", this.itemId, categoryLabel(this.category)));
	}

	private void selectPolicyList(PolicyList selected) {
		this.policyView = selected;
		rebuildWidgets();
	}

	private Component policyTabLabel(PolicyList list) {
		Component label = list.label();
		return this.policyView == list ? Component.literal("> ").append(label).append(" <") : label;
	}

	private void addPolicyItem() {
		if (!requireValidItemId()) return;
		if (this.policyView == PolicyList.WHITELIST) {
			updateDraft(this.draft.withoutBlacklistedItem(this.itemId).withWhitelistedItem(this.itemId, this.category));
			setSuccess(Component.translatable("screen.scoutremastered.ready_slots.whitelisted", this.itemId));
		} else {
			updateDraft(this.draft.withoutWhitelistedItem(this.itemId).withBlacklistedItem(this.itemId));
			setSuccess(Component.translatable("screen.scoutremastered.ready_slots.blacklisted", this.itemId));
		}
		this.policyList.rebuildEntries();
	}

	private void removePolicyEntry(String removedId) {
		ReadySlotPresentationConfig updated = this.policyView == PolicyList.WHITELIST
			? this.draft.withoutWhitelistedItem(removedId) : this.draft.withoutBlacklistedItem(removedId);
		updateDraft(updated);
		this.policyList.rebuildEntries();
		setSuccess(Component.translatable("screen.scoutremastered.ready_slots.policy_removed", removedId));
	}

	private void clearPolicyList() {
		ReadySlotPresentationConfig updated = this.draft;
		if (this.policyView == PolicyList.WHITELIST) {
			for (String id : List.copyOf(updated.itemWhitelist().keySet())) updated = updated.withoutWhitelistedItem(id);
		} else {
			for (String id : List.copyOf(updated.itemBlacklist())) updated = updated.withoutBlacklistedItem(id);
		}
		updateDraft(updated);
		this.policyList.rebuildEntries();
		setSuccess(Component.translatable("screen.scoutremastered.ready_slots.list_cleared", this.policyView.label()));
	}

	private void resetPolicyList() {
		ReadySlotPresentationConfig baseline = ReadySlotConfig.bundledBaseline();
		ReadySlotPresentationConfig updated = this.draft;
		if (this.policyView == PolicyList.WHITELIST) {
			for (String id : List.copyOf(updated.itemWhitelist().keySet())) updated = updated.withoutWhitelistedItem(id);
			for (Map.Entry<String, Category> entry : baseline.itemWhitelist().entrySet()) {
				updated = updated.withWhitelistedItem(entry.getKey(), entry.getValue());
			}
		} else {
			for (String id : List.copyOf(updated.itemBlacklist())) updated = updated.withoutBlacklistedItem(id);
			for (String id : baseline.itemBlacklist()) updated = updated.withBlacklistedItem(id);
		}
		updateDraft(updated);
		this.policyList.rebuildEntries();
		setSuccess(Component.translatable("screen.scoutremastered.ready_slots.list_reset", this.policyView.label()));
	}

	private boolean requireValidItemId() {
		if (!isRegisteredItemId()) {
			setError(Component.translatable("screen.scoutremastered.ready_slots.invalid_item_id"));
			return false;
		}
		return true;
	}

	private boolean isRegisteredItemId() {
		if (!ReadySlotPresentationConfig.isValidItemId(this.itemId)) return false;
		Identifier identifier = Identifier.tryParse(this.itemId);
		return identifier != null && BuiltInRegistries.ITEM.containsKey(identifier);
	}

	private void updateDraft(ReadySlotPresentationConfig updated) {
		this.draft = updated;
		ReadySlotConfig.preview(updated);
		this.status = Component.translatable("screen.scoutremastered.ready_slots.unsaved");
		this.statusColor = MUTED;
	}

	private void save() {
		try {
			ReadySlotConfig.save(this.draft);
			this.finished = true;
			this.minecraft.setScreen(null);
		} catch (IOException exception) {
			setError(Component.translatable("screen.scoutremastered.ready_slots.save_failed", exception.getMessage()));
		}
	}

	private void cancel() {
		ReadySlotConfig.restore(this.openingConfig);
		this.finished = true;
		this.minecraft.setScreen(null);
	}

	@Override public void onClose() { cancel(); }
	@Override public void removed() { if (!this.finished) ReadySlotConfig.restore(this.openingConfig); }
	private void setError(Component message) { this.status = message; this.statusColor = ERROR; }
	private void setSuccess(Component message) { this.status = message; this.statusColor = SUCCESS; }
	private static Component categoryLabel(Category value) {
		return Component.translatable("screen.scoutremastered.ready_slots.category_" + value.id());
	}
	private static String format(float value) {
		if (value == (long)value) return Long.toString((long)value);
		return String.format(Locale.ROOT, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
	}

	private enum Mode {
		TRANSFORM("screen.scoutremastered.ready_slots.mode_transform"),
		VISIBILITY("screen.scoutremastered.ready_slots.mode_lists");
		private final String key;
		Mode(String key) { this.key = key; }
		Component label() { return Component.translatable(this.key); }
	}

	private enum Scope {
		BASE("screen.scoutremastered.ready_slots.scope_base"),
		CATEGORY("screen.scoutremastered.ready_slots.scope_category"),
		ITEM("screen.scoutremastered.ready_slots.scope_item");
		private final String key;
		Scope(String key) { this.key = key; }
		Component label() { return Component.translatable(this.key); }
	}

	private enum PolicyList {
		WHITELIST("screen.scoutremastered.ready_slots.whitelist"),
		BLACKLIST("screen.scoutremastered.ready_slots.blacklist");
		private final String key;
		PolicyList(String key) { this.key = key; }
		Component label() { return Component.translatable(this.key); }
	}

	private enum TransformField {
		TRANSLATE_X("X", ReadySlotPresentationConfig.MIN_TRANSLATION, ReadySlotPresentationConfig.MAX_TRANSLATION, true) {
			@Override float read(Transform v) { return v.translateX(); }
			@Override Transform write(Transform v, float n) { return copy(v, n, v.translateY(), v.translateZ(), v.rotateX(), v.rotateY(), v.rotateZ(), v.scale()); }
		},
		TRANSLATE_Y("Y", ReadySlotPresentationConfig.MIN_TRANSLATION, ReadySlotPresentationConfig.MAX_TRANSLATION, true) {
			@Override float read(Transform v) { return v.translateY(); }
			@Override Transform write(Transform v, float n) { return copy(v, v.translateX(), n, v.translateZ(), v.rotateX(), v.rotateY(), v.rotateZ(), v.scale()); }
		},
		TRANSLATE_Z("Z", ReadySlotPresentationConfig.MIN_TRANSLATION, ReadySlotPresentationConfig.MAX_TRANSLATION, true) {
			@Override float read(Transform v) { return v.translateZ(); }
			@Override Transform write(Transform v, float n) { return copy(v, v.translateX(), v.translateY(), n, v.rotateX(), v.rotateY(), v.rotateZ(), v.scale()); }
		},
		ROTATE_X("RX", ReadySlotPresentationConfig.MIN_ROTATION, ReadySlotPresentationConfig.MAX_ROTATION, true) {
			@Override float read(Transform v) { return v.rotateX(); }
			@Override Transform write(Transform v, float n) { return copy(v, v.translateX(), v.translateY(), v.translateZ(), n, v.rotateY(), v.rotateZ(), v.scale()); }
		},
		ROTATE_Y("RY", ReadySlotPresentationConfig.MIN_ROTATION, ReadySlotPresentationConfig.MAX_ROTATION, true) {
			@Override float read(Transform v) { return v.rotateY(); }
			@Override Transform write(Transform v, float n) { return copy(v, v.translateX(), v.translateY(), v.translateZ(), v.rotateX(), n, v.rotateZ(), v.scale()); }
		},
		ROTATE_Z("RZ", ReadySlotPresentationConfig.MIN_ROTATION, ReadySlotPresentationConfig.MAX_ROTATION, true) {
			@Override float read(Transform v) { return v.rotateZ(); }
			@Override Transform write(Transform v, float n) { return copy(v, v.translateX(), v.translateY(), v.translateZ(), v.rotateX(), v.rotateY(), n, v.scale()); }
			@Override float mirrorValue(float value) {
				float next = value - 90.0F;
				return next < ReadySlotPresentationConfig.MIN_ROTATION ? next + 720.0F : next;
			}
		},
		SCALE("Scale", ReadySlotPresentationConfig.MIN_SCALE, ReadySlotPresentationConfig.MAX_SCALE, false) {
			@Override float read(Transform v) { return v.scale(); }
			@Override Transform write(Transform v, float n) { return copy(v, v.translateX(), v.translateY(), v.translateZ(), v.rotateX(), v.rotateY(), v.rotateZ(), n); }
		};
		private final Component label;
		private final float minimum;
		private final float maximum;
		private final boolean mirrorable;
		TransformField(String label, float minimum, float maximum, boolean mirrorable) {
			this.label = Component.literal(label); this.minimum = minimum; this.maximum = maximum; this.mirrorable = mirrorable;
		}
		Component label() { return this.label; }
		boolean mirrorable() { return this.mirrorable; }
		boolean inBounds(float value) { return Float.isFinite(value) && value >= this.minimum && value <= this.maximum; }
		float mirrorValue(float value) { return -value; }
		abstract float read(Transform value);
		abstract Transform write(Transform value, float next);
		private static Transform copy(Transform ignored, float x, float y, float z, float rx, float ry, float rz, float scale) {
			return new Transform(x, y, z, rx, ry, rz, scale);
		}
	}

	private static final class BoundedSlider extends AbstractSliderButton {
		private final TransformField field;
		private final Consumer<Float> listener;
		BoundedSlider(int x, int y, int width, int height, TransformField field, float value, Consumer<Float> listener) {
			super(x, y, width, height, Component.empty(), normalize(field, value));
			this.field = field; this.listener = listener; updateMessage();
		}
		void setActualValue(float actual) { setValue(normalize(this.field, actual)); }
		@Override protected void updateMessage() { setMessage(Component.literal(format(actualValue()))); }
		@Override protected void applyValue() { this.listener.accept(actualValue()); }
		private float actualValue() { return (float)(this.field.minimum + this.value * (this.field.maximum - this.field.minimum)); }
		private static double normalize(TransformField field, float value) { return (value - field.minimum) / (field.maximum - field.minimum); }
	}

	private final class PolicyListWidget extends ContainerObjectSelectionList<PolicyEntry> {
		private final int rowWidth;
		PolicyListWidget(int x, int height, int y, int width) {
			super(ReadySlotConfigScreen.this.minecraft, width, height, y, 20);
			this.rowWidth = width - 12;
			setX(x);
			rebuildEntries();
		}
		void rebuildEntries() {
			clearEntries();
			if (ReadySlotConfigScreen.this.policyView == PolicyList.WHITELIST) {
				ReadySlotConfigScreen.this.draft.itemWhitelist().keySet().stream().sorted().forEach(id -> addEntry(new PolicyEntry(id)));
			} else {
				ReadySlotConfigScreen.this.draft.itemBlacklist().stream().sorted().forEach(id -> addEntry(new PolicyEntry(id)));
			}
		}
		@Override public int getRowWidth() { return this.rowWidth; }
	}

	private final class PolicyEntry extends ContainerObjectSelectionList.Entry<PolicyEntry> {
		private final String entryId;
		private final Button removeButton;
		PolicyEntry(String entryId) {
			this.entryId = entryId;
			this.removeButton = Button.builder(Component.literal("-"), button -> removePolicyEntry(this.entryId))
				.bounds(0, 0, 18, 16).build();
			this.removeButton.setTooltip(Tooltip.create(Component.translatable("screen.scoutremastered.ready_slots.remove_item", entryId)));
		}
		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			String label = this.entryId;
			if (ReadySlotConfigScreen.this.policyView == PolicyList.WHITELIST) {
				Category assigned = ReadySlotConfigScreen.this.draft.whitelistedCategory(this.entryId).orElse(Category.SWORD);
				label += "  [" + assigned.id() + "]";
			}
			graphics.text(ReadySlotConfigScreen.this.font, label, getX() + 2, getY() + 6, TEXT, false);
			this.removeButton.setPosition(getX() + getWidth() - 20, getY() + 2);
			this.removeButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
		}
		@Override public List<? extends GuiEventListener> children() { return List.of(this.removeButton); }
		@Override public List<? extends NarratableEntry> narratables() { return List.of(this.removeButton); }
	}
}
