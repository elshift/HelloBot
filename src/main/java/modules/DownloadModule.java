package modules;

import commands.CommandContext;
import commands.annotations.Option;
import commands.annotations.SlashCommand;
import config.Config;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.FileUploader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadModule {
    private static final Logger logger = LoggerFactory.getLogger(DownloadModule.class);

    // Matches an Instagram, Twitter, Reddit, or TikTok post url
    private static final Pattern validUrlPatterns
            = Pattern.compile("(https://)?(www.)?(instagram.com((/p/)?(/tv/)?)[a-zA-Z\\\\d-]{11})?(twitter.com/[a-zA-Z\\d]/status/\\d{19})?(reddit.com/r/[a-zA-Z\\d_/-]+)?(tiktok.com/@[a-zA-Z\\d]+/video/\\d{19})?");

    private static final File downloadsDir = new File(Config.get().downloadDir());
    private static final String YT_DLP_BIN = "/usr/bin/yt-dlp";

    private static final Map<String, String> mimeExt = new HashMap<>() {{
        put("image/png", "png");
        put("image/jpeg", "jpeg");
        put("video/mp4", "mp4");
    }};

    private static void purgeOldFiles() {
        try {
            for (File file : downloadsDir.listFiles())
                if(!file.isDirectory())
                    file.delete();
        } catch (Exception e) {
            // ignored
        }
    }

    static {
        assert downloadsDir.mkdirs();
        purgeOldFiles();
    }

    /**
     * Probes the file MIME type and gives it a corresponding extension.
     */
    private @NotNull File getFinalFile(@NotNull File downloaded) throws IOException {
        // TODO: probeContentType returns null, find something else
        String mimeType = Files.probeContentType(downloaded.toPath());
        String extension = mimeExt.getOrDefault(mimeType, ".mp4");
        File finalFile = new File(downloaded.getPath() + extension);

        if(!downloaded.renameTo(finalFile))
            throw new IOException("Failed to rename file.");

        return finalFile;
    }

    private boolean urlSanityCheck(@NotNull String url) {
        return !(url.contains(";") || url.contains("|") || url.contains("&") || url.contains(">") || url.contains("<"));
    }

    @SlashCommand(name = "download", description = "Download a video from Twitter, Instagram, TikTok, or Reddit")
    public void downloadVideo(CommandContext context, @Option(name = "url", description = "post url") String url) {
        purgeOldFiles();

        Matcher matcher = validUrlPatterns.matcher(url);

        if (!matcher.matches()) {
            context.replyEphemeral("Invalid URL.");
            return;
        }

        url = matcher.group(0);

        if(!urlSanityCheck(url)) {
            context.replyEphemeral("Invalid URL.");
            return;
        }

        logger.debug("Attempting to download: {}", url);

        context.event().deferReply().queue();

        try {
            String path = Config.get().downloadDir() + context.event().getUser().getId();

            ProcessBuilder builder = new ProcessBuilder(YT_DLP_BIN, "-o" + path, url);
            builder.start().waitFor(10, TimeUnit.SECONDS);

            File finalFile = getFinalFile(new File(path));
            long fileSizeKB = finalFile.length() / 1024;

            // File is small enough to upload directly to Discord
            if (fileSizeKB < 8000) {
                context.event().getHook().sendFile(finalFile).queue();
                return;
            }

            String remoteUrl = FileUploader.uploadAndGetURL(finalFile);
            context.event().getHook().sendMessage(remoteUrl).queue();
        } catch (Exception e) {
            logger.error("Failed to download video", e);
            context.event().getHook()
                    .sendMessage("Failed to download video (image only posts aren't supported on all platforms).")
                    .setEphemeral(true).queue();
        }
    }
}
