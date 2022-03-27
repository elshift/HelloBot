package modules;

import commands.CommandContext;
import commands.annotations.Option;
import commands.annotations.SlashCommand;
import config.Config;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.TemporaryFileUploader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadModule {
    private static final Logger logger = LoggerFactory.getLogger(DownloadModule.class);

    // Matches an Instagram, Twitter, Reddit, or TikTok post url
    private static final Pattern validUrlPatterns
            = Pattern.compile("(https://)?(www.)?" +
            "(instagram.com((/p/)?(/tv/)?)[a-zA-Z\\\\d-]{11})?" +
            "(twitter.com/[a-zA-Z\\d]/status/\\d{19})?" +
            "(reddit.com/r/[a-zA-Z\\d_/-]+)?" +
            "(tiktok.com/@[a-zA-Z\\d]+/video/\\d{19})?");

    private static final File downloadsDir = new File(Config.get().downloadDir());
    private static final String YT_DLP_BIN = "/usr/bin/yt-dlp";

    static {
        assert downloadsDir.mkdirs();
        purgeOldFiles();
    }

    /**
     * Purges any non-directory files in the download directory.
     */
    private static void purgeOldFiles() {
        try {
            for (File file : Objects.requireNonNull(downloadsDir.listFiles())) {
                if(!file.isDirectory())
                    file.delete();
            }
        } catch (Exception e) {
            // ignored
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

        ProcessBuilder builder = new ProcessBuilder(YT_DLP_BIN, "-o" + destination, url);

        builder.redirectOutput(new File("output.txt"));
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
    public void downloadVideo(CommandContext context, @Option(name = "url", description = "post url") String url) {
        Matcher matcher = validUrlPatterns.matcher(url);

        if (!matcher.matches()) {
            context.replyEphemeral("Invalid URL.");
            return;
        }

        url = matcher.group(0);

        logger.debug("Attempting to download: {}", url);

        context.event().deferReply().queue();
        try {
            Path destination = Path.of(Config.get().downloadDir(), context.event().getUser().getId() + ".mp4");
            File downloadedFile = download(destination, url);

            long fileSizeKB = downloadedFile.length() / 1024;

            // File is small enough to upload directly to Discord
            if (fileSizeKB < 8000) {
                context.event().getHook().sendFile(downloadedFile).queue();
                return;
            }

            String remoteUrl = TemporaryFileUploader.uploadAndGetURL(downloadedFile);
            context.event().getHook().sendMessage(remoteUrl).queue();
        } catch(InterruptedException e) {
            context.event().getHook()
                    .sendMessage("Your video took too long to download!")
                    .setEphemeral(true).queue();
        } catch (Exception e) {
            context.event().getHook()
                    .sendMessage("Failed to download video (image only posts aren't supported on all platforms).")
                    .setEphemeral(true).queue();
        }
    }
}
