package com.schembuilder;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Client-only entrypoint. Everything here runs on your own game, so it works on any server you can join. */
public class SchemClient implements ClientModInitializer {
    public static final SchematicBuilder BUILDER = new SchematicBuilder();

    /** Set by "/schem gui"; the screen opens on the next tick once no other screen (chat) is showing. */
    static volatile boolean openRequested;

    /** Right Shift by default; can be rebound in Options > Controls > Schem Builder. */
    private static KeyMapping openGui;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("schembuilder", "main"));
        openGui = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.schembuilder.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BUILDER.tick(client);

            boolean pressed = false;
            while (openGui.consumeClick()) pressed = true;

            if ((pressed || openRequested) && client.screen == null && client.player != null) {
                openRequested = false;
                client.setScreen(new SchemScreen(BUILDER));
            }
        });
        SchemCommands.register(BUILDER);
    }
}
