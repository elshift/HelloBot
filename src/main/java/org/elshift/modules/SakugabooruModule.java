package org.elshift.modules;

import com.google.gson.Gson;
import org.elshift.commands.CommandContext;
import org.elshift.commands.annotations.Option;
import org.elshift.commands.annotations.RunMode;
import org.elshift.commands.annotations.SlashCommand;
import org.elshift.modules.annotations.CommandModule;
import org.elshift.modules.sakugabooru.SakugabooruPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@CommandModule
public class SakugabooruModule {
    // Can be any server running Moebooru (and probably works for most Danbooru instances too)
    private static final String MOEBOORU_API = "https://sakugabooru.com";
    private static final Logger logger = LoggerFactory.getLogger(SakugabooruModule.class);

    @SlashCommand(name = "sakuga", description = "Search Sakugabooru with tags")
    @RunMode(RunMode.Mode.Async)
    public void sakuga(CommandContext context, @Option(name = "tags") String search) {
        Set<String> tags = Arrays.stream(search.split(" ")).collect(Collectors.toSet());

        boolean hasOrder = false;
        for (String tag : tags) {
            if (tag.startsWith("order:")) {
                hasOrder = true;
                break;
            }
        }

        if (!hasOrder) // Select randomly by default
            tags.add("order:random");

        try {
            URL url = new URL("%s/post.json?limit=1&tags=%s".formatted(MOEBOORU_API, String.join("%20", tags)));

            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            InputStream is = conn.getInputStream();
            InputStreamReader sr = new InputStreamReader(is);

            Gson gson = new Gson();
            SakugabooruPost[] posts = gson.fromJson(sr, SakugabooruPost[].class);

            if (posts.length <= 0) {
                context.hook()
                        .sendMessage("No results found! Try a different set of tags.")
                        .setEphemeral(true).queue();
                return;
            }

            SakugabooruPost post = posts[0];
            String details = "Url: %s\nSearch: %s\nTags: %s\nFile: %s".formatted(
                    "%s/posts/show/%d".formatted(MOEBOORU_API, post.getId()),
                    search,
                    post.getTags(),
                    post.getFileUrl()
            );

            context.event().reply(details).queue();

            //"http://www.sakugabooru.com/post.xml?tags=spencer_wan%20order:random&limit=1";
        } catch (Exception e) {
            logger.error("Failed to search %s".formatted(MOEBOORU_API), e);
            context.hook()
                    .sendMessage("Failed to search: " + e.getMessage())
                    .setEphemeral(true).queue();
        }
    }
}
