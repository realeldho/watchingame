package com.realeldho.watchingame.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class WatchInGameClient implements ClientModInitializer {

	private static final KeyMapping.Category WATCHINGAME_CATEGORY =
			KeyMapping.Category.register(
					Identifier.fromNamespaceAndPath(
							"watchingame",
							"controls"
					)
			);

	private static final KeyMapping OPEN_WATCHINGAME =
			KeyBindingHelper.registerKeyBinding(
					new KeyMapping(
							"key.watchingame.open",
							InputConstants.Type.KEYSYM,
							InputConstants.KEY_Y,
							WATCHINGAME_CATEGORY
					)
			);

	@Override
	public void onInitializeClient() {

		// Initialize MCEF / Chromium.
		McefTest.initialize();

		// Y key opens WatchInGame.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {

			while (OPEN_WATCHINGAME.consumeClick()) {

				client.setScreen(
						new WatchInGameScreen()
				);
			}
		});

		// Add WatchInGame to the pause menu.
		ScreenEvents.AFTER_INIT.register(
				(client, screen, scaledWidth, scaledHeight) -> {

					if (!(screen instanceof PauseScreen)) {
						return;
					}

					List<AbstractWidget> widgets =
							Screens.getButtons(screen);

					/*
					 * Find a normal two-column vanilla button.
					 *
					 * Advancements / Statistics use this size.
					 */
					AbstractWidget referenceButton = null;

					for (int i = 0; i < widgets.size(); i++) {

						AbstractWidget first =
								widgets.get(i);

						for (int j = i + 1; j < widgets.size(); j++) {

							AbstractWidget second =
									widgets.get(j);

							if (first.getY() == second.getY()
									&& first.getWidth() == second.getWidth()
									&& first.getHeight() == second.getHeight()
									&& first.getX() != second.getX()) {

								referenceButton = first;
								break;
							}
						}

						if (referenceButton != null) {
							break;
						}
					}

					/*
					 * Safety fallback.
					 */
					if (referenceButton == null) {
						return;
					}

					int buttonWidth =
							referenceButton.getWidth();

					int buttonHeight =
							referenceButton.getHeight();

					int margin = 8;
					int spacing = 4;

					/*
					 * Start at the top-right.
					 */
					int x =
							scaledWidth
									- buttonWidth
									- margin;

					int y =
							margin;

					/*
					 * Avoid overlapping other buttons.
					 */
					while (isOverlappingExistingWidget(
							x,
							y,
							buttonWidth,
							buttonHeight,
							widgets
					)) {

						y +=
								buttonHeight
										+ spacing;

						/*
						 * If the top-right area is full,
						 * move toward the middle-right.
						 */
						if (y + buttonHeight
								> scaledHeight - margin) {

							y =
									(scaledHeight
											- buttonHeight)
											/ 2;

							while (isOverlappingExistingWidget(
									x,
									y,
									buttonWidth,
									buttonHeight,
									widgets
							)) {

								y +=
										buttonHeight
												+ spacing;

								if (y + buttonHeight
										> scaledHeight - margin) {

									return;
								}
							}

							break;
						}
					}

					Button watchButton =
							Button.builder(
									Component.literal(
											"WatchInGame"
									),
									button ->
											client.setScreen(
													new WatchInGameScreen()
											)
							).bounds(
									x,
									y,
									buttonWidth,
									buttonHeight
							).build();

					widgets.add(watchButton);
				}
		);

		/*
		 * When the player disconnects from a Minecraft
		 * world/server, completely stop the WatchInGame
		 * video session.
		 *
		 * ESC -> Y is unaffected because this only runs
		 * when the play connection disconnects.
		 */
		ClientPlayConnectionEvents.DISCONNECT.register(
				(handler, client) -> {

					WatchInGameScreen.stopVideoSession();
				}
		);
	}

	private static boolean isOverlappingExistingWidget(
			int x,
			int y,
			int width,
			int height,
			List<AbstractWidget> widgets
	) {

		for (AbstractWidget widget : widgets) {

			int widgetX =
					widget.getX();

			int widgetY =
					widget.getY();

			int widgetWidth =
					widget.getWidth();

			int widgetHeight =
					widget.getHeight();

			boolean overlaps =
					x < widgetX + widgetWidth
							&& x + width > widgetX
							&& y < widgetY + widgetHeight
							&& y + height > widgetY;

			if (overlaps) {
				return true;
			}
		}

		return false;
	}
}