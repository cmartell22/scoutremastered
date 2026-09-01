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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/** RS7A client-only, live-preview editor for bounded Ready Slots presentation settings. */
final class ReadySlotConfigScreen extends Screen {
	private static final int PANEL = 0xCC171717;
	private static final int PANEL_BORDER = 0xFF707070;
	private static final int TEXT = 0xFFE0E0E0;
	private static final int MUTED = 0xFFA0A0A0;
	private static final int ERROR = 0xFFFF6060;
	private static final int SUCCESS = 0xFF60FF80;
	private static final int ROW_HEIGHT = 13;
	private static final int ROW_GAP = 1;
	private static final String DEFAULT_ITEM_ID = "minecraft:diamond_sword";

	private final ReadySlotPresentationConfig openingConfig;
	private ReadySlotPresentationConfig draft;
	private Mode mode = Mode.TRANSFORM;
	private Scope scope = Scope.BASE;
	private Position position = Position.LEFT_HIP;
	private Category category = Category.HANDHELD;
	private String itemId = DEFAULT_ITEM_ID;
	private Transform copiedTransform;
	private boolean finished;
	private boolean syncingWidgets;
	private Component status = Component.translatable("screen.scoutremastered.ready_slots.unsaved");
	private int statusColor = MUTED;
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
			buildVisibilityControls(controlsX, controlsWidth, half);
		}

		int bottom = this.height - 24;
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.save"), button -> save())
			.bounds(controlsX, bottom, half, 20)
			.build());
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.cancel"), button -> cancel())
			.bounds(controlsX + half + 4, bottom, controlsWidth - half - 4, 20)
			.build());
	}

	private void buildTransformControls(int x, int width, int half) {
		addRenderableWidget(CycleButton.builder(Scope::label, this.scope)
			.withValues(List.of(Scope.values()))
			.create(x, 47, half, 20, Component.translatable("screen.scoutremastered.ready_slots.scope"), (button, value) -> {
				this.scope = value;
				rebuildWidgets();
			}));
		addRenderableWidget(CycleButton.builder(value -> Component.literal(value.id()), this.position)
			.withValues(List.of(Position.values()))
			.create(x + half + 4, 47, width - half - 4, 20, Component.translatable("screen.scoutremastered.ready_slots.position"), (button, value) -> {
				this.position = value;
				rebuildWidgets();
			}));

		if (this.scope != Scope.BASE) {
			addRenderableWidget(CycleButton.builder(value -> Component.literal(value.id()), this.category)
				.withValues(List.of(Category.values()))
				.create(x, 69, half, 20, Component.translatable("screen.scoutremastered.ready_slots.category"), (button, value) -> {
					this.category = value;
					rebuildWidgets();
				}));
		}
		if (this.scope == Scope.ITEM) {
			EditBox itemBox = itemIdBox(x + half + 4, 69, width - half - 4);
			addRenderableWidget(itemBox);
		}

		int firstRow = this.scope == Scope.BASE ? 71 : 91;
		int labelWidth = 36;
		int valueWidth = 54;
		int sliderWidth = width - labelWidth - valueWidth - 6;
		Transform transform = selectedTransform();
		for (TransformField field : TransformField.values()) {
			int rowY = firstRow + field.ordinal() * (ROW_HEIGHT + ROW_GAP);
			BoundedSlider slider = new BoundedSlider(
				x + labelWidth,
				rowY,
				sliderWidth,
				ROW_HEIGHT,
				field,
				field.read(transform),
				value -> applyField(field, value)
			);
			this.sliders.put(field, slider);
			addRenderableWidget(slider);

			EditBox numeric = new EditBox(
				this.font,
				x + labelWidth + sliderWidth + 6,
				rowY,
				valueWidth,
				ROW_HEIGHT,
				field.label()
			);
			numeric.setMaxLength(12);
			numeric.setValue(format(field.read(transform)));
			numeric.setResponder(value -> applyNumeric(field, value));
			this.numericBoxes.put(field, numeric);
			addRenderableWidget(numeric);
		}

		int actionsY = Math.min(this.height - 46, firstRow + TransformField.values().length * (ROW_HEIGHT + ROW_GAP) + 1);
		int buttonWidth = (width - 12) / 4;
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.reset"), button -> resetTransform())
			.bounds(x, actionsY, buttonWidth, 18).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.copy"), button -> copyTransform())
			.bounds(x + buttonWidth + 4, actionsY, buttonWidth, 18).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.paste"), button -> pasteTransform())
			.bounds(x + (buttonWidth + 4) * 2, actionsY, buttonWidth, 18).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.mirror"), button -> mirrorTransform())
			.bounds(x + (buttonWidth + 4) * 3, actionsY, width - (buttonWidth + 4) * 3, 18).build());
	}

	private void buildVisibilityControls(int x, int width, int half) {
		addRenderableWidget(CycleButton.builder(value -> Component.literal(value.id()), this.category)
			.withValues(List.of(Category.values()))
			.create(x, 51, half, 20, Component.translatable("screen.scoutremastered.ready_slots.category"), (button, value) -> {
				this.category = value;
				rebuildWidgets();
			}));
		addRenderableWidget(Checkbox.builder(Component.translatable("screen.scoutremastered.ready_slots.category_enabled"), this.font)
			.pos(x + half + 8, 53)
			.maxWidth(width - half - 8)
			.selected(this.draft.categoryEnabled(this.category))
			.onValueChange((checkbox, enabled) -> updateDraft(this.draft.withCategoryEnabled(this.category, enabled)))
			.build());

		EditBox itemBox = itemIdBox(x, 82, width);
		addRenderableWidget(itemBox);
		int buttonWidth = (width - 8) / 3;
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.whitelist"), button -> whitelistItem())
			.bounds(x, 106, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.blacklist"), button -> blacklistItem())
			.bounds(x + buttonWidth + 4, 106, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.scoutremastered.ready_slots.clear_policy"), button -> clearItemPolicy())
			.bounds(x + (buttonWidth + 4) * 2, 106, width - (buttonWidth + 4) * 2, 20).build());
	}

	private EditBox itemIdBox(int x, int y, int width) {
		EditBox itemBox = new EditBox(
			this.font,
			x,
			y,
			width,
			20,
			Component.translatable("screen.scoutremastered.ready_slots.item_id")
		);
		itemBox.setMaxLength(128);
		itemBox.setHint(Component.translatable("screen.scoutremastered.ready_slots.item_id_hint"));
		itemBox.setValue(this.itemId);
		itemBox.setResponder(value -> {
			this.itemId = value.trim();
			if (this.mode == Mode.TRANSFORM && this.scope == Scope.ITEM && ReadySlotPresentationConfig.isValidItemId(this.itemId)) {
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
			InventoryScreen.extractEntityInInventoryFollowsMouse(
				graphics,
				12,
				28,
				previewRight - 4,
				this.height - 32,
				Math.min(65, Math.max(30, (previewRight - 16) / 2)),
				0.0625F,
				mouseX,
				mouseY,
				this.minecraft.player
			);
		}
		graphics.text(this.font, this.title, 10, 8, TEXT, false);
		graphics.text(this.font, Component.translatable("screen.scoutremastered.ready_slots.preview"), 14, 24, MUTED, false);
		if (this.mode == Mode.TRANSFORM) {
			int firstRow = this.scope == Scope.BASE ? 71 : 91;
			for (TransformField field : TransformField.values()) {
				int rowY = firstRow + field.ordinal() * (ROW_HEIGHT + ROW_GAP) + 5;
				graphics.text(this.font, field.label(), controlsX(), rowY, TEXT, false);
			}
		}
		graphics.text(this.font, this.status, 12, this.height - 19, this.statusColor, false);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private int controlsX() {
		return Math.min(180, Math.max(132, this.width / 3));
	}

	private Transform selectedTransform() {
		return switch (this.scope) {
			case BASE -> this.draft.baseTransform(this.position);
			case CATEGORY -> this.draft.resolveCategory(this.position, this.category);
			case ITEM -> ReadySlotPresentationConfig.isValidItemId(this.itemId)
				? this.draft.resolve(this.position, this.category, this.itemId)
				: this.draft.resolveCategory(this.position, this.category);
		};
	}

	private void applyField(TransformField field, float value) {
		if (this.syncingWidgets) {
			return;
		}
		applySelectedTransform(field.write(selectedTransform(), value));
		synchronizeTransformWidgets();
	}

	private void applyNumeric(TransformField field, String text) {
		if (this.syncingWidgets || text.isBlank() || text.equals("-") || text.equals(".")) {
			return;
		}
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
		try {
			ReadySlotPresentationConfig updated = switch (this.scope) {
				case BASE -> this.draft.withBaseTransform(this.position, transform);
				case CATEGORY -> this.draft.withCategoryTransform(this.category, this.position, transform);
				case ITEM -> {
					if (!ReadySlotPresentationConfig.isValidItemId(this.itemId)) {
						throw new IllegalArgumentException("Invalid item id");
					}
					yield this.draft.withItemTransform(this.itemId, this.position, transform);
				}
			};
			updateDraft(updated);
			return true;
		} catch (IllegalArgumentException exception) {
			setError(Component.translatable("screen.scoutremastered.ready_slots.invalid_item_id"));
			return false;
		}
	}

	private void synchronizeTransformWidgets() {
		if (this.sliders.isEmpty()) {
			return;
		}
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
			case BASE -> updated = this.draft.withBaseTransform(
				this.position,
				ReadySlotConfig.bundledBaseline().baseTransform(this.position)
			);
			case CATEGORY -> updated = this.draft.withoutCategoryOverride(this.category, this.position);
			case ITEM -> {
				if (!requireValidItemId()) {
					return;
				}
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

	private void mirrorTransform() {
		if (this.position == Position.BACK) {
			setError(Component.translatable("screen.scoutremastered.ready_slots.back_not_mirrored"));
			return;
		}
		Position targetPosition = this.position == Position.LEFT_HIP ? Position.RIGHT_HIP : Position.LEFT_HIP;
		Transform source = selectedTransform();
		Transform mirrored = new Transform(
			-source.translateX(),
			source.translateY(),
			source.translateZ(),
			source.rotateX(),
			-source.rotateY(),
			-source.rotateZ(),
			source.scale()
		);
		Position sourcePosition = this.position;
		this.position = targetPosition;
		boolean applied = applySelectedTransform(mirrored);
		this.position = sourcePosition;
		if (applied) {
			setSuccess(Component.translatable("screen.scoutremastered.ready_slots.mirrored", targetPosition.id()));
		}
	}

	private void whitelistItem() {
		if (!requireValidItemId()) {
			return;
		}
		updateDraft(this.draft.withoutBlacklistedItem(this.itemId).withWhitelistedItem(this.itemId, this.category));
		setSuccess(Component.translatable("screen.scoutremastered.ready_slots.whitelisted", this.itemId));
	}

	private void blacklistItem() {
		if (!requireValidItemId()) {
			return;
		}
		updateDraft(this.draft.withoutWhitelistedItem(this.itemId).withBlacklistedItem(this.itemId));
		setSuccess(Component.translatable("screen.scoutremastered.ready_slots.blacklisted", this.itemId));
	}

	private void clearItemPolicy() {
		if (!requireValidItemId()) {
			return;
		}
		updateDraft(this.draft.withoutWhitelistedItem(this.itemId).withoutBlacklistedItem(this.itemId));
		setSuccess(Component.translatable("screen.scoutremastered.ready_slots.policy_cleared", this.itemId));
	}

	private boolean requireValidItemId() {
		if (!ReadySlotPresentationConfig.isValidItemId(this.itemId)) {
			setError(Component.translatable("screen.scoutremastered.ready_slots.invalid_item_id"));
			return false;
		}
		return true;
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

	@Override
	public void onClose() {
		cancel();
	}

	@Override
	public void removed() {
		if (!this.finished) {
			ReadySlotConfig.restore(this.openingConfig);
		}
	}

	private void setError(Component message) {
		this.status = message;
		this.statusColor = ERROR;
	}

	private void setSuccess(Component message) {
		this.status = message;
		this.statusColor = SUCCESS;
	}

	private static String format(float value) {
		if (value == (long) value) {
			return Long.toString((long) value);
		}
		return String.format(Locale.ROOT, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
	}

	private enum Mode {
		TRANSFORM("screen.scoutremastered.ready_slots.mode_transform"),
		VISIBILITY("screen.scoutremastered.ready_slots.mode_visibility");

		private final String key;

		Mode(String key) {
			this.key = key;
		}

		Component label() {
			return Component.translatable(this.key);
		}
	}

	private enum Scope {
		BASE("screen.scoutremastered.ready_slots.scope_base"),
		CATEGORY("screen.scoutremastered.ready_slots.scope_category"),
		ITEM("screen.scoutremastered.ready_slots.scope_item");

		private final String key;

		Scope(String key) {
			this.key = key;
		}

		Component label() {
			return Component.translatable(this.key);
		}
	}

	private enum TransformField {
		TRANSLATE_X("X", ReadySlotPresentationConfig.MIN_TRANSLATION, ReadySlotPresentationConfig.MAX_TRANSLATION) {
			@Override float read(Transform value) { return value.translateX(); }
			@Override Transform write(Transform value, float next) { return copy(value, next, value.translateY(), value.translateZ(), value.rotateX(), value.rotateY(), value.rotateZ(), value.scale()); }
		},
		TRANSLATE_Y("Y", ReadySlotPresentationConfig.MIN_TRANSLATION, ReadySlotPresentationConfig.MAX_TRANSLATION) {
			@Override float read(Transform value) { return value.translateY(); }
			@Override Transform write(Transform value, float next) { return copy(value, value.translateX(), next, value.translateZ(), value.rotateX(), value.rotateY(), value.rotateZ(), value.scale()); }
		},
		TRANSLATE_Z("Z", ReadySlotPresentationConfig.MIN_TRANSLATION, ReadySlotPresentationConfig.MAX_TRANSLATION) {
			@Override float read(Transform value) { return value.translateZ(); }
			@Override Transform write(Transform value, float next) { return copy(value, value.translateX(), value.translateY(), next, value.rotateX(), value.rotateY(), value.rotateZ(), value.scale()); }
		},
		ROTATE_X("RX", ReadySlotPresentationConfig.MIN_ROTATION, ReadySlotPresentationConfig.MAX_ROTATION) {
			@Override float read(Transform value) { return value.rotateX(); }
			@Override Transform write(Transform value, float next) { return copy(value, value.translateX(), value.translateY(), value.translateZ(), next, value.rotateY(), value.rotateZ(), value.scale()); }
		},
		ROTATE_Y("RY", ReadySlotPresentationConfig.MIN_ROTATION, ReadySlotPresentationConfig.MAX_ROTATION) {
			@Override float read(Transform value) { return value.rotateY(); }
			@Override Transform write(Transform value, float next) { return copy(value, value.translateX(), value.translateY(), value.translateZ(), value.rotateX(), next, value.rotateZ(), value.scale()); }
		},
		ROTATE_Z("RZ", ReadySlotPresentationConfig.MIN_ROTATION, ReadySlotPresentationConfig.MAX_ROTATION) {
			@Override float read(Transform value) { return value.rotateZ(); }
			@Override Transform write(Transform value, float next) { return copy(value, value.translateX(), value.translateY(), value.translateZ(), value.rotateX(), value.rotateY(), next, value.scale()); }
		},
		SCALE("S", ReadySlotPresentationConfig.MIN_SCALE, ReadySlotPresentationConfig.MAX_SCALE) {
			@Override float read(Transform value) { return value.scale(); }
			@Override Transform write(Transform value, float next) { return copy(value, value.translateX(), value.translateY(), value.translateZ(), value.rotateX(), value.rotateY(), value.rotateZ(), next); }
		};

		private final Component label;
		private final float minimum;
		private final float maximum;

		TransformField(String label, float minimum, float maximum) {
			this.label = Component.literal(label);
			this.minimum = minimum;
			this.maximum = maximum;
		}

		Component label() {
			return this.label;
		}

		boolean inBounds(float value) {
			return Float.isFinite(value) && value >= this.minimum && value <= this.maximum;
		}

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
			this.field = field;
			this.listener = listener;
			updateMessage();
		}

		void setActualValue(float actual) {
			setValue(normalize(this.field, actual));
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(format(actualValue())));
		}

		@Override
		protected void applyValue() {
			this.listener.accept(actualValue());
		}

		private float actualValue() {
			return (float) (this.field.minimum + this.value * (this.field.maximum - this.field.minimum));
		}

		private static double normalize(TransformField field, float value) {
			return (value - field.minimum) / (field.maximum - field.minimum);
		}
	}
}
