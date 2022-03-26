package modules;

import commands.CommandContext;
import commands.annotations.Option;
import commands.annotations.SlashCommand;
import config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadModule {
    private static final Logger logger = LoggerFactory.getLogger(DownloadModule.class);

    // Matches an Instagram or Twitter post url
    private static final Pattern validUrlPatterns
            = Pattern.compile("(https://)?(www.)?(instagram.com((/p/)?(/tv/)?)[a-zA-Z\\\\d-]{11})?(twitter.com/.+/status/\\d{19})?");

    private static final File downloadsDir = new File(Config.get().downloadDir());
    private static final String YT_DLP_BIN = "/usr/bin/yt-dlp";

    static {
        assert downloadsDir.mkdirs();
    }

    private void purgeOldFiles() {
        try {
            for (File file : downloadsDir.listFiles())
                if(!file.isDirectory())
                    file.delete();
        } catch (Exception e) {
            // ignored
        }
    }

    @SlashCommand(name = "download", description = "Download a video from Twitter or Instagram")
    public void DownloadVideo(CommandContext context, @Option(name = "url", description = "post url") String url) {
        purgeOldFiles();

        Matcher matcher = validUrlPatterns.matcher(url);

        if(!matcher.matches()) {
            context.replyEphemeral("Invalid URL.");
            return;
        }

        url = matcher.group(0);

        logger.debug("Attempting to download: {}", url);

        try {
            String path = "downloads/" + context.event().getUser().getId() + ".mp4";

            ProcessBuilder builder = new ProcessBuilder(YT_DLP_BIN, "-o" + path, url);
            builder.start().waitFor(10, TimeUnit.SECONDS);

            File downloadedFile = new File(path);

            context.event().replyFile(downloadedFile).queue();
        } catch (Exception e) {
            if(url.contains("twitter"))
                context.replyEphemeral("Failed to download video (image only posts aren't supported)");
            else
                context.replyEphemeral("Failed to download video.");
        }
    }
}
