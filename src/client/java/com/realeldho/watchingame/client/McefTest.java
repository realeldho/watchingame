package com.realeldho.watchingame.client;

import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;

public final class McefTest {

    private static MCEFBrowser browser;
    private static boolean initializing = false;
    private static boolean initialized = false;

    private McefTest() {
    }

    public static synchronized void initialize() {
        if (initialized || initializing) {
            return;
        }

        initializing = true;

        System.out.println("[WatchInGame] Starting MCEF initialization...");

        MCEFApi.Initialization initialization = MCEFApi.initialize();

        initialization.getFuture().thenAccept(api -> {
            System.out.println(
                    "[WatchInGame] MCEF initialization complete."
            );

            browser = api.createBrowser(
                    "https://www.youtube.com",
                    true
            );

            browser.resize(560, 315);

            initialized = true;
            initializing = false;

            System.out.println(
                    "[WatchInGame] MCEF browser created."
            );

        }).exceptionally(error -> {

            initializing = false;

            System.err.println(
                    "[WatchInGame] MCEF initialization failed:"
            );

            error.printStackTrace();

            return null;
        });
    }

    public static MCEFBrowser getBrowser() {
        return browser;
    }

    public static boolean isReady() {
        return browser != null;
    }

    public static boolean isInitializing() {
        return initializing;
    }

    public static void close() {
        if (browser != null) {
            browser.close();
            browser = null;
        }

        initialized = false;
        initializing = false;
    }
}