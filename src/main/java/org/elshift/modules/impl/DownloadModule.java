package org.elshift.modules.impl;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.elshift.commands.annotations.Option;
import org.elshift.commands.annotations.RunMode;
import org.elshift.commands.annotations.SlashCommand;
import org.elshift.config.Config;
import org.elshift.modules.Module;
import org.elshift.util.TemporaryFileUploader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadModule extends ListenerAdapter implements Module {
    private static final Logger logger = LoggerFactory.getLogger(DownloadModule.class);

    // Matches a YouTube, Instagram, Twitter, Reddit, or TikTok post url
    private static final Pattern validUrlPatterns
            = Pattern.compile("(http://)?(https://)?(www.)?" +
            "(instagram.com((/p/)?(/tv/)?(/reel/)?)[a-zA-Z\\d-]{11})?" +
            "(twitter.com/[a-zA-Z_\\d]+/status/\\d{19})?" +
            "(reddit.com/r/[a-zA-Z\\d_/-]+)?" +
            "(tiktok.com/@[a-zA-Z\\d]+/video/\\d{19})?" +
            "(youtu(?:be\\.com/watch\\?v=|\\.be/)([\\w\\-_]*)(&(amp;)?\u200C\u200B[\\w?\u200C\u200B=]*)?)?" +
            "(?:[a-zA-Z\\d&=?]+)?/?");// Accept &, =, ? and standard letters/numbers for URL parameters, and trailing slashes.

    private static final File downloadsDir = new File(Config.get().downloadDir());
    private static Path YT_DLP_BIN = Path.of(downloadsDir.toString(), "yt-dlp");
    private static final Path YT_DLP_BIN_LINUX = Path.of("/bin/yt-dlp");
    private static final Path YT_DLP_BIN_WINDOWS = Path.of(downloadsDir.toString(), "yt-dlp.exe");
    private static boolean hasRequiredDependencies = false;

    private static final ConcurrentLinkedQueue<File> removalQueue = new ConcurrentLinkedQueue<>();

    static {
        if (!downloadsDir.mkdirs() && !downloadsDir.exists()) {
            logger.error("Failed to create download directory.");
        }

        purgeOldFiles();
        try {
            checkYTDLP();
        } catch (Exception e) {
            logger.error("Failed to find yt-dlp; download module will not work", e);
        }
    }

    /**
     * Purges any non-directory and non-executable files in the download directory.
     */
    private static void purgeOldFiles() {
        try {
            for (File file : Objects.requireNonNull(downloadsDir.listFiles())) {
                if (!file.isDirectory() && !file.canExecute()
                        && Instant.now().toEpochMilli() - file.lastModified() > (120 * 1000))
                    if (!file.delete() && file.exists()) {
                        logger.error("Failed to delete old file {}.", file.getName());
                    }
            }

            removalQueue.forEach((file) -> {
                if (!file.delete() && file.exists()) {
                    logger.error("Failed to delete file {} in removal queue.", file.getName());
                }
            });
            removalQueue.clear();
        } catch (Exception e) {
            // ignored
        }
    }

    /**
     * Tries to find a yt-dlp install.
     */
    private static void checkYTDLP() throws IOException {
        if (Files.exists(YT_DLP_BIN)) {
            hasRequiredDependencies = true;
            return;
        }

        String osName = System.getProperty("os.name").toLowerCase();
        boolean isLinux = osName.contains("nix") || osName.contains("nux") || osName.contains("aix");
        boolean isWindows = osName.contains("win");

        Path installBin = isLinux ? YT_DLP_BIN_LINUX : isWindows ? YT_DLP_BIN_WINDOWS : null;
        if (installBin == null) {
            logger.error("Make sure both ffmpeg and yt-dlp are installed. " +
                    "You need to symlink the path to your yt-dlp install to %s".formatted(YT_DLP_BIN));
            return;
        }

        if (Files.exists(installBin)) {
            logger.info("Found yt-dlp: {}", installBin);
            if (isLinux)
                Files.createSymbolicLink(YT_DLP_BIN, YT_DLP_BIN_LINUX);
            else // Windows
                YT_DLP_BIN = YT_DLP_BIN_WINDOWS;
            hasRequiredDependencies = true;
        } else {
            logger.error("Cannot find a yt-dlp install! " +
                    "You can install it with your package manager or the GitHub page.");
        }
    }

    /**
     * Attempts to download media from given url and attempts to save the result to destination.
     *
     * @param destination Destination path for downloaded media
     * @param url         URL to fetch media from
     * @return The downloaded media
     */
    private static File download(@NotNull Path destination, String url) throws IOException, InterruptedException {
        purgeOldFiles();

        ProcessBuilder builder = new ProcessBuilder(YT_DLP_BIN.toString(), "-o%s".formatted(destination), url);
        builder.start().waitFor(10, TimeUnit.SECONDS);

        File file = new File(destination.toString());

        // Just in case
        if (!file.exists()) {
            String fileName = destination.getFileName().toString();
            int periodIdx = fileName.lastIndexOf('.');
            String fileNameNoExt = fileName.substring(0, periodIdx);
            String extension = fileName.substring(periodIdx);

            File[] files = downloadsDir.listFiles((dir, name) ->
                    name.contains(fileNameNoExt) && name.contains(extension));
            if (files == null || files.length == 0)
                throw new IOException("Failed to find file");

            file = files[0];
        }

        return file;
    }

    @SlashCommand(name = "download", description = "Download a video from YouTube, Twitter, Instagram, TikTok, or Reddit")
    @RunMode(RunMode.Mode.Async)
    public void downloadVideo(SlashCommandInteractionEvent event, @Option(name = "url", description = "post url") String url) {
        if (!hasRequiredDependencies) {
            event.reply("Missing dependencies: ffmpeg + yt-dlp. Check log for more info.")
                    .setEphemeral(true).queue();
            return;
        }

        Matcher matcher = validUrlPatterns.matcher(url);

        if (!matcher.matches()) {
            event.reply("Invalid URL.\n\nSupported platforms:\nYouTube, Twitter, Instagram, TikTok and Reddit")
                    .setEphemeral(true).queue();
            return;
        }

        url = matcher.group(0);

        logger.debug("Attempting to download: {}", url);

        event.deferReply().queue();

        try {
            Path destination = Path.of(Config.get().downloadDir(), "%s.mp4".formatted(Instant.now().toEpochMilli()));
            File downloadedFile = download(destination, url);

            long fileSize = downloadedFile.length();
            long maxFileSize = 1024 * 1024 * 8;

            if (event.isFromGuild())
                maxFileSize = Objects.requireNonNull(event.getGuild()).getMaxFileSize();

            // File is small enough to upload directly to Discord
            if (fileSize < maxFileSize) {
                event.getHook().sendFile(downloadedFile).queue();
                return;
            }

            String remoteUrl = TemporaryFileUploader.uploadAndGetURL(downloadedFile);
            event.getHook().sendMessage(remoteUrl).queue(m -> removalQueue.add(downloadedFile));
        } catch (InterruptedException e) {
            event.getHook()
                    .sendMessage("Your video took too long to download!")
                    .setEphemeral(true).queue();
        } catch (Exception e) {
            logger.warn("Failed to download video", e);
            event.getHook()
                    .sendMessage("Failed to download video (image only posts aren't supported on all platforms).")
                    .setEphemeral(true).queue();
        }
    }

    @Override
    public String getName() {
        return "download";
    }

    @Override
    public String getHelpMessage() {
        return "Downloads videos from social media websites";
    }
}
