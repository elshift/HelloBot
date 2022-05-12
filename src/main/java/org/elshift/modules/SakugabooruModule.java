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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@CommandModule
public class SakugabooruModule {
    // Can be any server running Moebooru (and probably works for most Danbooru instances too)
    private static final String MOEBOORU_API = "https://sakugabooru.com";
    private static final Logger logger = LoggerFactory.getLogger(SakugabooruModule.class);

    private SakugabooruPost[] getPosts(Set<String> tags) throws IOException {
        String tagsParameter = URLEncoder.encode(String.join(" ", tags), StandardCharsets.UTF_8);
        URL url = new URL("%s/post.json?limit=1&tags=%s".formatted(MOEBOORU_API, tagsParameter));
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        try (InputStream stream = conn.getInputStream()) {
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                return new Gson().fromJson(reader, SakugabooruPost[].class);
            }
        }
    }

    @SlashCommand(name = "sakuga", description = "Search Sakugabooru with tags")
    @RunMode(RunMode.Mode.Async)
    public void searchSakuga(CommandContext context, @Option(name = "tags") String search) {
        context.event().deferReply().queue();

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
            SakugabooruPost[] posts = getPosts(tags);

            if (posts.length <= 0) {
                context.hook()
                        .sendMessage("No results found! Try a different set of tags.")
                        .setEphemeral(true).queue();
                return;
            }

            SakugabooruPost post = posts[0];

            String postUrl = "%s/post/show/%d".formatted(MOEBOORU_API, post.getId());

            String message = """
                    **URL**: <%s>
                    **Search**: %s
                    **Tags**: %s
                    %s
                    """.formatted(postUrl, String.join(" ", tags), post.getTags(), post.getFileUrl());
            message = message.replace("@", "\\@");

            context.hook().sendMessage(message).queue();
        } catch (Exception e) {
            logger.error("Failed to search %s".formatted(MOEBOORU_API), e);
            context.hook()
                    .sendMessage("Failed to search: " + e.getMessage())
                    .setEphemeral(true).queue();
        }
    }
}
