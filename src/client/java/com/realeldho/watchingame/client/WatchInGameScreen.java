package com.realeldho.watchingame.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.realeldho.watchingame.mixin.client.GuiGraphicsInvoker;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WatchInGameScreen extends Screen {

    private static final int MAX_VIDEO_WIDTH = 560;
    private static final int MAX_VIDEO_HEIGHT = 315;

    private static final int MIN_VIDEO_WIDTH = 240;
    private static final int MIN_VIDEO_HEIGHT = 135;

    /*
     * Keeps the WatchInGame session alive when the screen is closed.
     * The MCEF browser itself is also kept alive by McefTest.
     */
    private static boolean videoSessionActive = false;
    private EditBox urlBox;

    private Button watchButton;
    private Button closeButton;

    private Button rewindButton;
    private Button playPauseButton;
    private Button forwardButton;
    private Button muteButton;
    private Button changeButton;

    private boolean videoStarted = false;
    private boolean playing = true;
    private boolean muted = false;

    private int videoWidth;
    private int videoHeight;

    private double currentTime = 0;
    private double duration = 0;

    private boolean draggingTimeline = false;

    public WatchInGameScreen() {
        super(Component.literal("WatchInGame"));

        /*
         * If a video was already started before this screen was closed,
         * immediately reopen the video instead of showing the URL screen.
         */
        this.videoStarted = videoSessionActive;
    }

    @Override
    protected void init() {

        WatchInGameWebServer.start();
        McefTest.initialize();

        calculateVideoSize();

        if (videoStarted) {
            setupVideoControls();
        } else {
            setupUrlScreen();
        }
    }

    private void calculateVideoSize() {

        int availableWidth =
                this.width - 40;

        int availableHeight =
                this.height - 120;

        int width =
                Math.min(
                        MAX_VIDEO_WIDTH,
                        availableWidth
                );

        int height =
                width * 9 / 16;

        if (height > availableHeight) {

            height =
                    Math.min(
                            MAX_VIDEO_HEIGHT,
                            availableHeight
                    );

            width =
                    height * 16 / 9;
        }

        width =
                Math.max(
                        MIN_VIDEO_WIDTH,
                        width
                );

        height =
                Math.max(
                        MIN_VIDEO_HEIGHT,
                        height
                );

        if (width > availableWidth) {

            width = availableWidth;

            height =
                    width * 9 / 16;
        }

        if (height > availableHeight) {

            height = availableHeight;

            width =
                    height * 16 / 9;
        }

        videoWidth = width;
        videoHeight = height;

        MCEFBrowser browser =
                McefTest.getBrowser();

        if (browser != null) {

            browser.resize(
                    videoWidth,
                    videoHeight
            );
        }
    }

    public static void stopVideoSession() {
        videoSessionActive = false;

        MCEFBrowser browser = McefTest.getBrowser();

        if (browser != null) {
            browser.setFocus(false);
            browser.getCefBrowser().stopLoad();
            browser.getCefBrowser().loadURL("about:blank");
        }
    }

    private void setupUrlScreen() {

        int centerX =
                this.width / 2;

        int centerY =
                this.height / 2;

        urlBox =
                new EditBox(
                        this.font,
                        centerX - 160,
                        centerY - 10,
                        320,
                        20,
                        Component.literal("YouTube URL")
                );

        urlBox.setHint(
                Component.literal(
                        "https://www.youtube.com/watch?v=..."
                )
        );

        this.addRenderableWidget(
                urlBox
        );

        watchButton =
                Button.builder(
                        Component.literal("Watch"),
                        button -> startVideo()
                ).bounds(
                        centerX - 130,
                        centerY + 20,
                        120,
                        20
                ).build();

        this.addRenderableWidget(
                watchButton
        );

        closeButton =
                Button.builder(
                        Component.literal("Close"),
                        button -> this.onClose()
                ).bounds(
                        centerX + 10,
                        centerY + 20,
                        120,
                        20
                ).build();

        this.addRenderableWidget(
                closeButton
        );
    }

    private void setupVideoControls() {

        int centerX =
                this.width / 2;

        int videoY =
                (this.height - videoHeight) / 2 - 10;

        int timelineY =
                videoY + videoHeight + 7;

        int controlsY =
                timelineY + 13;

        rewindButton =
                Button.builder(
                        Component.literal("⏪ 10s"),
                        button -> executeJavaScript(
                                "if(window.player) player.seekTo(" +
                                        "Math.max(0,player.getCurrentTime()-10),true);"
                        )
                ).bounds(
                        centerX - 180,
                        controlsY,
                        70,
                        20
                ).build();

        this.addRenderableWidget(
                rewindButton
        );

        playPauseButton =
                Button.builder(
                        Component.literal(
                                playing ? "Pause" : "Play"
                        ),
                        button -> togglePlayPause()
                ).bounds(
                        centerX - 105,
                        controlsY,
                        70,
                        20
                ).build();

        this.addRenderableWidget(
                playPauseButton
        );

        forwardButton =
                Button.builder(
                        Component.literal("10s ⏩"),
                        button -> executeJavaScript(
                                "if(window.player) player.seekTo(" +
                                        "Math.min(player.getDuration()," +
                                        "player.getCurrentTime()+10),true);"
                        )
                ).bounds(
                        centerX - 30,
                        controlsY,
                        70,
                        20
                ).build();

        this.addRenderableWidget(
                forwardButton
        );

        muteButton =
                Button.builder(
                        Component.literal(
                                muted ? "🔇" : "🔊"
                        ),
                        button -> toggleMute()
                ).bounds(
                        centerX + 45,
                        controlsY,
                        45,
                        20
                ).build();

        this.addRenderableWidget(
                muteButton
        );

        changeButton =
                Button.builder(
                        Component.literal("Change"),
                        button -> changeVideo()
                ).bounds(
                        centerX + 95,
                        controlsY,
                        75,
                        20
                ).build();

        this.addRenderableWidget(
                changeButton
        );
    }

    @Override
    public void renderBackground(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        // Keep Minecraft world clear.
    }

    private void startVideo() {

        String url =
                urlBox.getValue().trim();

        String videoId =
                extractVideoId(url);

        if (videoId == null) {
            return;
        }

        MCEFBrowser browser =
                McefTest.getBrowser();

        if (browser == null) {
            return;
        }

        String playerUrl =
                "http://127.0.0.1:18765/player?video="
                        + videoId;

        browser.getCefBrowser().loadURL(
                playerUrl
        );

        browser.setFocus(true);

        videoStarted = true;

        /*
         * Remember that a video session now exists.
         */
        videoSessionActive = true;

        playing = true;
        muted = false;

        currentTime = 0;
        duration = 0;
        draggingTimeline = false;

        browser.resize(
                videoWidth,
                videoHeight
        );

        this.removeWidget(urlBox);
        this.removeWidget(watchButton);
        this.removeWidget(closeButton);

        setupVideoControls();
    }

    private void togglePlayPause() {

        if (playing) {

            executeJavaScript(
                    "if(window.player) player.pauseVideo();"
            );

            playing = false;

            playPauseButton.setMessage(
                    Component.literal("Play")
            );

        } else {

            executeJavaScript(
                    "if(window.player) player.playVideo();"
            );

            playing = true;

            playPauseButton.setMessage(
                    Component.literal("Pause")
            );
        }
    }

    private void toggleMute() {

        if (muted) {

            executeJavaScript(
                    "if(window.player) player.unMute();"
            );

            muted = false;

            muteButton.setMessage(
                    Component.literal("🔊")
            );

        } else {

            executeJavaScript(
                    "if(window.player) player.mute();"
            );

            muted = true;

            muteButton.setMessage(
                    Component.literal("🔇")
            );
        }
    }

    private void changeVideo() {

        /*
         * Completely end the current video session.
         */
        videoSessionActive = false;
        videoStarted = false;

        currentTime = 0;
        duration = 0;
        draggingTimeline = false;

        MCEFBrowser browser =
                McefTest.getBrowser();

        if (browser != null) {

            browser.setFocus(false);

            browser.getCefBrowser().stopLoad();

            browser.getCefBrowser().loadURL(
                    "about:blank"
            );
        }

        /*
         * Remove all current controls.
         */
        clearWidgets();

        urlBox = null;
        watchButton = null;
        closeButton = null;
        rewindButton = null;
        playPauseButton = null;
        forwardButton = null;
        muteButton = null;
        changeButton = null;

        /*
         * Show the URL entry screen for the new video.
         */
        setupUrlScreen();
    }

    private void executeJavaScript(
            String javascript
    ) {

        MCEFBrowser browser =
                McefTest.getBrowser();

        if (browser == null) {
            return;
        }

        browser.getCefBrowser().executeJavaScript(
                javascript,
                "http://127.0.0.1:18765/",
                0
        );
    }

    private void updatePlaybackState() {

        if (!videoStarted) {
            return;
        }

        MCEFBrowser browser =
                McefTest.getBrowser();

        if (browser == null) {
            return;
        }

        String url =
                browser.getCefBrowser().getURL();

        if (url == null) {
            return;
        }

        Matcher timeMatcher =
                Pattern.compile(
                        "[?&]t=([0-9.]+)"
                ).matcher(url);

        Matcher durationMatcher =
                Pattern.compile(
                        "[?&]d=([0-9.]+)"
                ).matcher(url);

        if (timeMatcher.find()) {

            try {

                currentTime =
                        Double.parseDouble(
                                timeMatcher.group(1)
                        );

            } catch (NumberFormatException ignored) {
            }
        }

        if (durationMatcher.find()) {

            try {

                duration =
                        Double.parseDouble(
                                durationMatcher.group(1)
                        );

            } catch (NumberFormatException ignored) {
            }
        }
    }

    private int getTimelineX() {

        return (this.width - videoWidth) / 2;
    }

    private int getTimelineWidth() {

        return Math.min(
                videoWidth,
                this.width - 40
        );
    }

    private int getTimelineY() {

        int videoY =
                (this.height - videoHeight) / 2 - 10;

        return videoY + videoHeight + 7;
    }

    private void seekFromMouse(
            double mouseX
    ) {

        int x =
                getTimelineX();

        int width =
                getTimelineWidth();

        double percentage =
                (mouseX - x)
                        / (double) width;

        percentage =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                percentage
                        )
                );

        if (duration > 0) {

            double newTime =
                    duration * percentage;

            currentTime =
                    newTime;

            executeJavaScript(
                    "if(window.player) player.seekTo("
                            + newTime
                            + ",true);"
            );
        }
    }

    private String formatTime(
            double seconds
    ) {

        int total =
                Math.max(
                        0,
                        (int) seconds
                );

        int minutes =
                total / 60;

        int secs =
                total % 60;

        return String.format(
                "%d:%02d",
                minutes,
                secs
        );
    }

    private String extractVideoId(
            String url
    ) {

        Pattern[] patterns = {

                Pattern.compile(
                        "youtu\\.be/([A-Za-z0-9_-]{11})"
                ),

                Pattern.compile(
                        "youtube\\.com/watch\\?v=([A-Za-z0-9_-]{11})"
                ),

                Pattern.compile(
                        "youtube\\.com/shorts/([A-Za-z0-9_-]{11})"
                ),

                Pattern.compile(
                        "youtube\\.com/embed/([A-Za-z0-9_-]{11})"
                )
        };

        for (Pattern pattern : patterns) {

            Matcher matcher =
                    pattern.matcher(url);

            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    @Override
    public void tick() {

        super.tick();

        updatePlaybackState();
    }

    @Override
    public boolean mouseClicked(
            MouseButtonEvent event,
            boolean doubleClick
    ) {

        double mouseX = event.x();
        double mouseY = event.y();

        if (videoStarted
                && event.button() == 0
                && isOverTimeline(mouseX, mouseY)) {

            draggingTimeline = true;

            seekFromMouse(mouseX);

            return true;
        }

        return super.mouseClicked(
                event,
                doubleClick
        );
    }

    @Override
    public boolean mouseDragged(
            MouseButtonEvent event,
            double deltaX,
            double deltaY
    ) {

        if (videoStarted
                && draggingTimeline
                && event.button() == 0) {

            seekFromMouse(
                    event.x()
            );

            return true;
        }

        return super.mouseDragged(
                event,
                deltaX,
                deltaY
        );
    }

    @Override
    public boolean mouseReleased(
            MouseButtonEvent event
    ) {

        if (event.button() == 0) {
            draggingTimeline = false;
        }

        return super.mouseReleased(
                event
        );
    }

    private boolean isOverTimeline(
            double mouseX,
            double mouseY
    ) {

        int x =
                getTimelineX();

        int width =
                getTimelineWidth();

        int y =
                getTimelineY();

        return mouseX >= x
                && mouseX <= x + width
                && mouseY >= y - 4
                && mouseY <= y + 8;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {

        if (videoStarted) {

            MCEFBrowser browser =
                    McefTest.getBrowser();

            if (browser != null) {

                GpuTextureView texture =
                        browser.getTextureView();

                if (texture != null) {

                    int videoX =
                            (this.width - videoWidth) / 2;

                    int videoY =
                            (this.height - videoHeight) / 2 - 10;

                    int framePadding = 5;

                    // Outer frame
                    graphics.fill(
                            videoX - framePadding,
                            videoY - framePadding,
                            videoX + videoWidth + framePadding,
                            videoY + videoHeight + framePadding,
                            0xFF202020
                    );

                    // Inner border
                    graphics.fill(
                            videoX - 2,
                            videoY - 2,
                            videoX + videoWidth + 2,
                            videoY + videoHeight + 2,
                            0xFF808080
                    );

                    GpuSampler sampler =
                            RenderSystem
                                    .getSamplerCache()
                                    .getClampToEdge(
                                            com.mojang.blaze3d.textures.FilterMode.LINEAR
                                    );

                    GuiGraphicsInvoker invoker =
                            (GuiGraphicsInvoker)
                                    (Object) graphics;

                    invoker.watchingame$submitBlit(
                            RenderPipelines.GUI_TEXTURED,
                            texture,
                            sampler,
                            videoX,
                            videoY,
                            videoX + videoWidth,
                            videoY + videoHeight,
                            0.0f,
                            1.0f,
                            0.0f,
                            1.0f,
                            0xFFFFFFFF
                    );
                }
            }
        }

        super.render(
                graphics,
                mouseX,
                mouseY,
                delta
        );

        if (videoStarted) {

            drawTimeline(
                    graphics
            );

        } else {

            graphics.drawCenteredString(
                    this.font,
                    Component.literal(
                            "Enter YouTube video URL"
                    ),
                    this.width / 2,
                    (this.height / 2) - 40,
                    0xFFFFFFFF
            );
        }
    }

    private void drawTimeline(
            GuiGraphics graphics
    ) {

        int x =
                getTimelineX();

        int width =
                getTimelineWidth();

        int y =
                getTimelineY();

        /*
         * Background.
         */
        graphics.fill(
                x,
                y,
                x + width,
                y + 3,
                0xFF555555
        );

        /*
         * Progress.
         */
        double progress =
                duration > 0
                        ? currentTime / duration
                        : 0;

        progress =
                Math.max(
                        0,
                        Math.min(
                                1,
                                progress
                        )
                );

        int progressWidth =
                (int) (
                        width * progress
                );

        graphics.fill(
                x,
                y,
                x + progressWidth,
                y + 3,
                0xFFFFFFFF
        );

        /*
         * Handle.
         */
        int knobX =
                x + progressWidth - 3;

        graphics.fill(
                knobX,
                y - 2,
                knobX + 6,
                y + 5,
                0xFFFFFFFF
        );

        /*
         * Time labels.
         */
        graphics.drawString(
                this.font,
                formatTime(currentTime),
                x,
                y + 6,
                0xFFFFFFFF
        );

        String durationText =
                formatTime(duration);

        graphics.drawString(
                this.font,
                durationText,
                x + width
                        - this.font.width(
                        durationText
                ),
                y + 6,
                0xFFFFFFFF
        );
    }

    @Override
    public void onClose() {

        MCEFBrowser browser =
                McefTest.getBrowser();

        if (browser != null) {
            browser.setFocus(false);
        }

        /*
         * IMPORTANT:
         * Do NOT reset videoSessionActive here.
         *
         * The MCEF browser stays alive, so pressing Y again
         * will reopen the same video at its current position.
         */
        super.onClose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
