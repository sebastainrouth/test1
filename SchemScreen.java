package com.schembuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.List;
import java.util.function.DoubleConsumer;

/**
 * Opened with Right Shift. Everything the /schem chat commands do, as buttons,
 * sliders and switches, plus rotation and mirroring.
 */
public class SchemScreen extends Screen {
    private static final int PER_PAGE = 6;
    private static final int COL_W = 200;
    private static final int STEP = 22;
    private static final String[] ROT_LABELS = {"0°", "90°", "180°", "270°"};

    private final SchematicBuilder builder;
    private List<String> files = List.of();
    private int page = 0;
    private String feedback = "";
    private Button rotationButton;

    public SchemScreen(SchematicBuilder builder) {
        super(Component.literal("Schem Builder"));
        this.builder = builder;
    }

    // ---------------------------------------------------------------- layout

    @Override
    protected void init() {
        files = listFiles();
        int pages = Math.max(1, (files.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);

        int left = this.width / 2 - COL_W - 5;
        int right = this.width / 2 + 5;
        int top = 26;

        // ---------- left column: schematic list (= /schem load <file>)
        int first = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = first + i;
            if (idx >= files.size()) break;
            String name = files.get(idx);
            boolean selected = name.equals(GuiSettings.selectedFile);
            addRenderableWidget(Button.builder(
                    Component.literal((selected ? "> " : "") + name),
                    b -> loadFile(name))
                .bounds(left, top + i * STEP, COL_W, 20).build());
        }

        // paging row
        int pageRow = top + PER_PAGE * STEP;
        Button prev = Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); })
            .bounds(left, pageRow, 60, 20).build();
        prev.active = page > 0;
        addRenderableWidget(prev);
        Button label = Button.builder(Component.literal((page + 1) + " / " + pages), b -> {})
            .bounds(left + 64, pageRow, 72, 20).build();
        label.active = false;
        addRenderableWidget(label);
        Button next = Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); })
            .bounds(left + 140, pageRow, 60, 20).build();
        next.active = page < pages - 1;
        addRenderableWidget(next);

        // action row (= /schem build, stop, status)
        int actionRow = pageRow + STEP;
        addRenderableWidget(Button.builder(Component.literal("Build"), b -> build())
            .bounds(left, actionRow, 64, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Stop"), b -> {
                builder.stop();
                report("Stopped. " + builder.status());
            }).bounds(left + 68, actionRow, 64, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Status"), b -> report(builder.status()))
            .bounds(left + 136, actionRow, 64, 20).build());

        // ---------- right column
        int y = top;

        // Rotation (new)
        rotationButton = Button.builder(rotationText(), b -> {
                GuiSettings.rotation = (GuiSettings.rotation + 1) & 3;
                b.setMessage(rotationText());
            }).bounds(right, y, COL_W, 20).build();
        addRenderableWidget(rotationButton);
        y += STEP;

        // Mirror switches (new)
        addRenderableWidget(CycleButton.onOffBuilder(GuiSettings.mirrorX)
            .create(right, y, 98, 20, Component.literal("Mirror X"),
                (b, v) -> GuiSettings.mirrorX = v));
        addRenderableWidget(CycleButton.onOffBuilder(GuiSettings.mirrorZ)
            .create(right + 102, y, 98, 20, Component.literal("Mirror Z"),
                (b, v) -> GuiSettings.mirrorZ = v));
        y += STEP;

        // Origin X / Y / Z (= /schem build <x> <y> <z>)
        addRenderableWidget(coordBox(right, y, "X", GuiSettings.originX, v -> GuiSettings.originX = v));
        addRenderableWidget(coordBox(right + 68, y, "Y", GuiSettings.originY, v -> GuiSettings.originY = v));
        addRenderableWidget(coordBox(right + 136, y, "Z", GuiSettings.originZ, v -> GuiSettings.originZ = v));
        y += STEP;

        addRenderableWidget(Button.builder(Component.literal("Use my position"), b -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return;
                BlockPos p = mc.player.blockPosition();
                GuiSettings.originX = p.getX();
                GuiSettings.originY = p.getY();
                GuiSettings.originZ = p.getZ();
                rebuildWidgets();
            }).bounds(right, y, COL_W, 20).build());
        y += STEP;

        // Reach slider (= /schem reach <blocks>)
        double maxReach = Config.exploit ? Config.EXPLOIT_MAX_REACH : Config.LEGIT_MAX_REACH;
        addRenderableWidget(new ValueSlider(right, y, COL_W, 20, "Reach", 1.0, maxReach,
            Math.min(Config.reach, maxReach), false, v -> Config.reach = v));
        y += STEP;

        // Speed slider (= /schem speed <blocksPerTick>)
        addRenderableWidget(new ValueSlider(right, y, COL_W, 20, "Blocks per tick", 1, 20,
            Config.blocksPerTick, true, v -> Config.blocksPerTick = (int) v));
        y += STEP;

        // Exploit switch (= /schem exploit <on|off>)
        addRenderableWidget(CycleButton.onOffBuilder(Config.exploit)
            .create(right, y, COL_W, 20, Component.literal("Exploit mode"), (b, on) -> {
                Config.exploit = on;
                if (on) {
                    Config.reach = Math.max(Config.reach, Config.EXPLOIT_DEFAULT_REACH);
                    report("Exploit mode ON: no line-of-sight check, reach up to "
                        + Config.EXPLOIT_MAX_REACH + ". Anti-cheat on public servers will likely flag this.");
                } else {
                    Config.reach = Math.min(Config.reach, Config.LEGIT_MAX_REACH);
                    report("Exploit mode OFF: line of sight required, reach " + Config.reach + ".");
                }
                rebuildWidgets(); // reach slider range depends on this switch
            }));
    }

    // --------------------------------------------------------------- actions

    private void loadFile(String name) {
        Path p = schematicDir().resolve(name).normalize();
        try {
            report(builder.load(p));
            GuiSettings.selectedFile = name;
            rebuildWidgets();
        } catch (Exception e) {
            report("Could not load: " + e.getMessage());
        }
    }

    private void build() {
        if (!builder.hasSchematic()) {
            report("Load a schematic first (click one in the list).");
            return;
        }
        BlockPos origin = new BlockPos(GuiSettings.originX, GuiSettings.originY, GuiSettings.originZ);
        report(builder.start(origin));
        if (builder.isRunning()) onClose(); // builder pauses while a screen is open, so hand control back
    }

    /** Shows a message in the GUI and in chat (same as the commands did). */
    private void report(String msg) {
        feedback = msg == null ? "" : msg;
        if (!feedback.isEmpty()) Chat.say(feedback);
    }

    private Component rotationText() {
        return Component.literal("Rotation: " + ROT_LABELS[GuiSettings.rotation & 3] + " (click to rotate)");
    }

    // ----------------------------------------------------------------- input

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) { // same key closes it
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    /** Don't pause the game while the GUI is open (note: the builder itself waits until the GUI is closed). */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

        int left = this.width / 2 - COL_W - 5;
        int textY = 26 + PER_PAGE * STEP + 2 * STEP + 4;
        int textW = COL_W * 2 + 10;
        g.drawString(this.font, Component.literal(builder.status()), left, textY, 0xFFFFFFFF);
        if (!feedback.isEmpty()) {
            g.drawWordWrap(this.font, Component.literal(feedback), left, textY + 12, textW, 0xFFAAAAAA);
        }
        if (files.isEmpty()) {
            g.drawString(this.font, Component.literal("No .schem/.litematic files in " + schematicDir()),
                left, 30, 0xFFFF7777);
        }
    }

    // --------------------------------------------------------------- helpers

    private EditBox coordBox(int x, int y, String name, int value, java.util.function.IntConsumer onChange) {
        EditBox box = new EditBox(this.font, x, y, 64, 20, Component.literal(name));
        box.setHint(Component.literal(name));
        box.setFilter(s -> s.matches("-?\\d{0,9}"));
        box.setValue(Integer.toString(value));
        box.setResponder(s -> {
            try { onChange.accept(Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
        });
        return box;
    }

    private static Path schematicDir() {
        return SchemCommands.schematicDir();
    }

    private static List<String> listFiles() {
        return SchemCommands.listFiles();
    }

    /** Slider that shows "Label: value" and reports the real value on change. */
    private static final class ValueSlider extends AbstractSliderButton {
        private final String label;
        private final double min, max;
        private final boolean integer;
        private final DoubleConsumer onChange;

        ValueSlider(int x, int y, int w, int h, String label, double min, double max,
                    double current, boolean integer, DoubleConsumer onChange) {
            super(x, y, w, h, Component.empty(), (clamp(current, min, max) - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.integer = integer;
            this.onChange = onChange;
            updateMessage();
        }

        private static double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        private double current() {
            double v = min + this.value * (max - min);
            return integer ? Math.round(v) : Math.round(v * 10.0) / 10.0;
        }

        @Override
        protected void updateMessage() {
            if (label == null) return; // super constructor may call this early
            double v = current();
            setMessage(Component.literal(label + ": " + (integer ? Long.toString((long) v) : Double.toString(v))));
        }

        @Override
        protected void applyValue() {
            onChange.accept(current());
        }
    }
}
