package utils;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class VlcJavaFXPlayer {

    private MediaPlayerFactory factory;
    private EmbeddedMediaPlayer mediaPlayer;
    private WritableImage writableImage;
    private PixelWriter pixelWriter;
    private int videoWidth = 1280;
    private int videoHeight = 720;
    private Timer progressTimer;

    // ── Construire le lecteur dans un VBox JavaFX ────────────────────────────
    public VBox buildPlayer(String filePath) {

        factory = new MediaPlayerFactory("--no-xlib", "--quiet");
        mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();

        // ── ImageView pour afficher les frames ────────────────────────────────
        writableImage = new WritableImage(videoWidth, videoHeight);
        pixelWriter = writableImage.getPixelWriter();
        ImageView videoView = new ImageView(writableImage);
        videoView.setPreserveRatio(true);
        videoView.setFitWidth(850);
        videoView.setFitHeight(480);

        StackPane videoPane = new StackPane(videoView);
        videoPane.setStyle("-fx-background-color: black;");
        videoPane.setPrefHeight(480);
        VBox.setVgrow(videoPane, Priority.ALWAYS);

        // ── Surface callback (pixel buffer) ───────────────────────────────────
        uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurface surface = factory.videoSurfaces().newVideoSurface(
                new BufferFormatCallback() {
                    @Override
                    public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
                        videoWidth = sourceWidth;
                        videoHeight = sourceHeight;
                        Platform.runLater(() -> {
                            writableImage = new WritableImage(videoWidth, videoHeight);
                            pixelWriter = writableImage.getPixelWriter();
                            videoView.setImage(writableImage);
                        });
                        return new RV32BufferFormat(sourceWidth, sourceHeight);
                    }

                    @Override
                    public void allocatedBuffers(ByteBuffer[] buffers) {
                    }
                },
                new RenderCallback() {
                    @Override
                    public void display(MediaPlayer mp, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
                        ByteBuffer buffer = nativeBuffers[0];
                        int w = bufferFormat.getWidth();
                        int h = bufferFormat.getHeight();

                        // Copier les pixels dans l'image JavaFX
                        int[] pixels = new int[w * h];
                        buffer.asIntBuffer().get(pixels);

                        Platform.runLater(() -> {
                            if (pixelWriter != null) {
                                pixelWriter.setPixels(0, 0, w, h,
                                        PixelFormat.getIntArgbPreInstance(),
                                        pixels, 0, w);
                            }
                        });
                    }
                },
                true);

        mediaPlayer.videoSurface().set(surface);

        // ── Contrôles ─────────────────────────────────────────────────────────
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setStyle("-fx-background-color: #0F1923; -fx-padding: 10 15;");

        Button playBtn = new Button("⏸");
        playBtn.setStyle("-fx-background-color: #00C9A7; -fx-text-fill: #0F1923; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 7 16; -fx-cursor: hand;");

        Button muteBtn = new Button("🔊");
        muteBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-padding: 7 12; -fx-cursor: hand;");

        Slider progressSlider = new Slider(0, 100, 0);
        progressSlider.setStyle("-fx-accent: #00C9A7;");
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        Label timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 11px; "
                + "-fx-font-family: 'Courier New';");

        Slider volumeSlider = new Slider(0, 100, 80);
        volumeSlider.setPrefWidth(90);
        volumeSlider.setStyle("-fx-accent: #00C9A7;");

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 7 14; -fx-cursor: hand;");

        controls.getChildren().addAll(
                playBtn, muteBtn, progressSlider, timeLabel, volumeSlider, closeBtn);

        // ── Actions ───────────────────────────────────────────────────────────
        playBtn.setOnAction(e -> {
            if (mediaPlayer.status().isPlaying()) {
                mediaPlayer.controls().pause();
                playBtn.setText("▶");
            } else {
                mediaPlayer.controls().play();
                playBtn.setText("⏸");
            }
        });

        muteBtn.setOnAction(e -> {
            boolean muted = mediaPlayer.audio().isMute();
            mediaPlayer.audio().setMute(!muted);
            muteBtn.setText(muted ? "🔊" : "🔇");
        });

        volumeSlider.valueProperty().addListener((obs, o, n) -> mediaPlayer.audio().setVolume(n.intValue()));

        progressSlider.setOnMouseReleased(e -> mediaPlayer.controls().setPosition(
                (float) (progressSlider.getValue() / 100.0)));

        closeBtn.setOnAction(e -> cleanup());

        // ── Timer progression ─────────────────────────────────────────────────
        progressTimer = new Timer(true);
        progressTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    float pos = mediaPlayer.status().position();
                    long cur = mediaPlayer.status().time();
                    long tot = mediaPlayer.media().info() != null
                            ? mediaPlayer.media().info().duration()
                            : 0;
                    Platform.runLater(() -> {
                        if (!progressSlider.isValueChanging())
                            progressSlider.setValue(pos * 100);
                        timeLabel.setText(fmt(cur) + " / " + fmt(tot));
                    });
                } catch (Exception ignored) {
                }
            }
        }, 500, 500);

        // ── Lancer la vidéo ───────────────────────────────────────────────────
        mediaPlayer.media().play(filePath);
        mediaPlayer.audio().setVolume(80);

        // ── Layout final ──────────────────────────────────────────────────────
        VBox player = new VBox(0);
        player.getChildren().addAll(videoPane, controls);
        return player;
    }

    private String fmt(long millis) {
        if (millis <= 0)
            return "00:00";
        long s = millis / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    public void cleanup() {
        if (progressTimer != null)
            progressTimer.cancel();
        if (mediaPlayer != null) {
            mediaPlayer.controls().stop();
            mediaPlayer.release();
        }
        if (factory != null)
            factory.release();
    }
}
