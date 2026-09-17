package com.realeldho.watchingame.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public final class WatchInGameWebServer {

    private static HttpServer server;

    private static final int PORT = 18765;

    private WatchInGameWebServer() {
    }

    public static synchronized void start() {

        if (server != null) {
            return;
        }

        try {

            server = HttpServer.create(
                    new InetSocketAddress(
                            "127.0.0.1",
                            PORT
                    ),
                    0
            );

            server.createContext(
                    "/player",
                    WatchInGameWebServer::handlePlayer
            );

            server.setExecutor(
                    Executors.newCachedThreadPool()
            );

            server.start();

            System.out.println(
                    "[WatchInGame] Local web server started on port "
                            + PORT
            );

        } catch (IOException error) {

            System.err.println(
                    "[WatchInGame] Could not start local web server."
            );

            error.printStackTrace();
        }
    }

    private static void handlePlayer(
            HttpExchange exchange
    ) throws IOException {

        String query =
                exchange.getRequestURI().getRawQuery();

        String videoId = "";

        if (query != null) {

            for (String parameter : query.split("&")) {

                String[] pair =
                        parameter.split("=", 2);

                if (pair.length == 2
                        && pair[0].equals("video")) {

                    videoId =
                            URLDecoder.decode(
                                    pair[1],
                                    StandardCharsets.UTF_8
                            );

                    break;
                }
            }
        }

        if (!videoId.matches("[A-Za-z0-9_-]{11}")) {

            send(
                    exchange,
                    400,
                    "Invalid YouTube video ID."
            );

            return;
        }

        String html =
                createPlayerPage(videoId);

        byte[] bytes =
                html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/html; charset=UTF-8"
        );

        exchange.getResponseHeaders().set(
                "Cache-Control",
                "no-store"
        );

        exchange.sendResponseHeaders(
                200,
                bytes.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(bytes);
        }
    }

    private static void send(
            HttpExchange exchange,
            int status,
            String message
    ) throws IOException {

        byte[] bytes =
                message.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(
                status,
                bytes.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(bytes);
        }
    }

    private static String createPlayerPage(
            String videoId
    ) {

        return """
                <!DOCTYPE html>
                <html>
                <head>

                    <meta charset="UTF-8">

                    <meta
                        name="referrer"
                        content="strict-origin-when-cross-origin"
                    >

                    <meta
                        name="viewport"
                        content="width=device-width, initial-scale=1.0"
                    >

                    <style>

                        html, body {
                            margin: 0;
                            padding: 0;
                            width: 100%%;
                            height: 100%%;
                            overflow: hidden;
                            background: #000;
                        }

                        #player {
                            width: 100%%;
                            height: 100%%;
                        }

                    </style>

                </head>

                <body>

                    <div id="player"></div>

                    <script>

                        var player;

                        var videoId = "%s";

                        var tag =
                            document.createElement("script");

                        tag.src =
                            "https://www.youtube.com/iframe_api";

                        var firstScript =
                            document.getElementsByTagName(
                                "script"
                            )[0];

                        firstScript.parentNode.insertBefore(
                            tag,
                            firstScript
                        );

                        function updateState() {

                            if (!player) {
                                return;
                            }

                            var duration =
                                player.getDuration();

                            var current =
                                player.getCurrentTime();

                            if (!duration || duration <= 0) {
                                return;
                            }

                            /*
                             * Store playback state in the URL
                             * without reloading the page.
                             */
                            history.replaceState(
                                null,
                                "",
                                "/player?video="
                                + videoId
                                + "&t="
                                + current.toFixed(2)
                                + "&d="
                                + duration.toFixed(2)
                            );
                        }

                        function onYouTubeIframeAPIReady() {

                            player =
                                new YT.Player(
                                    "player",
                                    {

                                        width: "100%%",
                                        height: "100%%",

                                        videoId: videoId,

                                        playerVars: {

                                            autoplay: 1,

                                            controls: 0,

                                            rel: 0,

                                            enablejsapi: 1,

                                            /*
                                             * Do not force captions.
                                             */
                                            cc_load_policy: 0,

                                            origin:
                                                "http://127.0.0.1:%d"
                                        },

                                        events: {

                                            onReady:
                                                function(event) {

                                                    event.target.playVideo();

                                                    setInterval(
                                                        updateState,
                                                        250
                                                    );
                                                }
                                        }
                                    }
                                );
                        }

                    </script>

                </body>
                </html>
                """.formatted(
                videoId,
                PORT
        );
    }
}