package modules;

import commands.CommandContext;
import commands.annotations.Option;
import commands.annotations.RunMode;
import commands.annotations.SlashCommand;
import config.Config;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.TemporaryFileUploader;

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

public class DownloadModule {
    private static final Logger logger = LoggerFactory.getLogger(DownloadModule.class);

    // Matches an Instagram, Twitter, Reddit, or TikTok post url
    private static final Pattern validUrlPatterns
            = Pattern.compile("(https://)?(www.)?" +
            "(instagram.com((/p/)?(/tv/)?(/reel/)?)[a-zA-Z\\d-]{11})?" +
            "(twitter.com/[a-zA-Z\\d]/status/\\d{19})?" +
            "(reddit.com/r/[a-zA-Z\\d_/-]+)?" +
            "(tiktok.com/@[a-zA-Z\\d]+/video/\\d{19})?" +
            "/?");

    private static final File downloadsDir = new File(Config.get().downloadDir());
    private static final Path YT_DLP_BIN = Path.of(downloadsDir.toString(), "yt-dlp");
    private static final String YT_DLP_BIN_LINUX = "/bin/yt-dlp";

    private static final ConcurrentLinkedQueue<File> removalQueue = new ConcurrentLinkedQueue<>();

    static {
        downloadsDir.mkdirs();

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
                if(!file.isDirectory() && !file.canExecute() && Instant.now().toEpochMilli() - file.lastModified() > (120 * 1000))
                    file.delete();
            }

            removalQueue.forEach(File::delete);
            removalQueue.clear();
        } catch (Exception e) {
            // ignored
        }
    }

    /**
     * Tries to find a yt-dlp install.
     */
    private static void checkYTDLP() throws IOException {
        if (Files.exists(YT_DLP_BIN))
            return;

        String os = System.getProperty("os.name");
        if(os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            if (Files.exists(Path.of(YT_DLP_BIN_LINUX))) {
                logger.info("Found yt-dlp: {}", YT_DLP_BIN);
                Files.createSymbolicLink(YT_DLP_BIN, Path.of(YT_DLP_BIN_LINUX));
            } else
                logger.error("Cannot find a yt-dlp install! Download module will not work. " +
                        "You can install it via your package manager.");
        } else {
            logger.error("You need to download the latest version of yt-dlp and place it in %s".formatted(YT_DLP_BIN));
        }
    }

    /**
     * Attempts to download media from given url and attempts to save the result to destination.
     * @param destination
     *  Destination path for downloaded media
     * @param url
     *  URL to fetch media from
     * @return
     *  The downloaded media
     */
    private static File download(@NotNull Path destination, String url) throws IOException, InterruptedException {
        purgeOldFiles();

        ProcessBuilder builder = new ProcessBuilder(YT_DLP_BIN.toString(), "-o%s".formatted(destination), url);
        builder.start().waitFor(10, TimeUnit.SECONDS);

        File file = new File(destination.toString());

        // yt-dlp may append an extension by itself for whatever reason, just find any file where the id is present
        if(!file.exists()) {
            File[] files = downloadsDir.listFiles((dir, name) -> name.contains(destination.getFileName().toString()));
            if(files == null || files.length == 0)
                throw new IOException("Failed to find file");
            file = files[0];
        }

        return file;
    }

    @SlashCommand(name = "download", description = "Download a video from Twitter, Instagram, TikTok, or Reddit")
    @RunMode(RunMode.Mode.Async)
    public void downloadVideo(CommandContext context, @Option(name = "url", description = "post url") String url) {
        Matcher matcher = validUrlPatterns.matcher(url);

        if (!matcher.matches()) {
            context.replyEphemeral("Invalid URL. Try removing any extra parameters.");
            return;
        }

        url = matcher.group(0);

        logger.debug("Attempting to download: {}", url);

        context.event().deferReply().queue();
        try {
            Path destination = Path.of(Config.get().downloadDir(), "%s.mp4".formatted(Instant.now().toEpochMilli()));
            File downloadedFile = download(destination, url);

            long fileSize = downloadedFile.length();
            long maxFileSize = 1024 * 1024 * 8;

            if(context.event().isFromGuild())
                maxFileSize = Objects.requireNonNull(context.event().getGuild()).getMaxFileSize();

            // File is small enough to upload directly to Discord
            if (fileSize < maxFileSize) {
                context.event().getHook().sendFile(downloadedFile).queue();
                return;
            }

            String remoteUrl = TemporaryFileUploader.uploadAndGetURL(downloadedFile);
            context.event().getHook().sendMessage(remoteUrl).queue(m -> removalQueue.add(downloadedFile));
        } catch(InterruptedException e) {
            context.event().getHook()
                    .sendMessage("Your video took too long to download!")
                    .setEphemeral(true).queue();
        } catch (Exception e) {
            logger.warn("Failed to download video", e);
            context.event().getHook()
                    .sendMessage("Failed to download video (image only posts aren't supported on all platforms).")
                    .setEphemeral(true).queue();
        }
    }
}
