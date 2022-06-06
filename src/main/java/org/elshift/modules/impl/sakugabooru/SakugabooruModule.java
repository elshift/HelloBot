package org.elshift.modules.impl.sakugabooru;

import com.google.gson.Gson;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.elshift.commands.annotations.Option;
import org.elshift.commands.annotations.SlashCommand;
import org.elshift.commands.annotations.TextCommand;
import org.elshift.modules.Module;
import org.elshift.util.ParsedTextCommand;
import org.jetbrains.annotations.NotNull;
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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SakugabooruModule extends ListenerAdapter implements Module {
    // Can be any server running Moebooru (and probably works for most Danbooru instances too)
    private static final String MOEBOORU_API = "https://sakugabooru.com";
    private static final Logger logger = LoggerFactory.getLogger(SakugabooruModule.class);

    @Override
    public boolean usesSlashCommands() {
        return true;
    }

    @SlashCommand(name = "sakuga", description = "Search Sakugabooru")
    public void slashSearchSakuga(SlashCommandInteractionEvent event, @Option(name = "tags", required = false) String rawTags) {
        Set<String> tags = createSimplifiedTags(rawTags);
        try {
            SakugabooruPost[] posts = fetchPosts(tags);
            if (posts.length <= 0) {
                event.reply(formatEmptySearchResults(tags)).setEphemeral(true).queue();
                return;
            }

            event.reply(formatSearchedPost(posts[0], tags)).queue();
        } catch (Exception e) {
            logger.error("Failed to search %s".formatted(MOEBOORU_API), e);
            event.reply("Failed to search: " + e.getMessage()).setEphemeral(true).queue();
        }
    }

    @TextCommand(name = "sakuga", description = "Search Sakugabooru", aliases = {"s"})
    public void textSearchSakuga(@NotNull MessageReceivedEvent event, ParsedTextCommand parsedText) {
        Set<String> tags = createSimplifiedTags(parsedText.getCmdArgs());
        try {
            SakugabooruPost[] posts = fetchPosts(tags);
            String msg = (posts.length > 0) ? formatSearchedPost(posts[0], tags) : formatEmptySearchResults(tags);
            event.getMessage().reply(msg).queue();
        } catch (Exception e) {
            if (!(e instanceof InsufficientPermissionException)) { // Ignore permission errors. Not our problem.
                logger.error("Failed to search %s".formatted(MOEBOORU_API), e);
                event.getMessage().reply("Failed to search: " + e.getMessage()).queue();
            }
        }
    }

    private String formatPost(SakugabooruPost Post) {
        String postUrl = "%s/post/show/%d".formatted(MOEBOORU_API, Post.getId());
        String message = """
                **Post URL**: <%s>
                **Tags**: %s
                %s
                """.formatted(postUrl, Post.getTags(), Post.getFileUrl());
        return message.replace("@", "\\@");
    }

    private String formatSearchedPost(SakugabooruPost Post, Set<String> SearchedTags) {
        String postUrl = "%s/post/show/%d".formatted(MOEBOORU_API, Post.getId());
        String message = """
                **Post URL**: <%s>
                **Search**: %s
                **Tags**: %s
                %s
                """.formatted(postUrl, String.join(" ", SearchedTags), Post.getTags(), Post.getFileUrl());
        return message.replace("@", "\\@");
    }

    private String formatEmptySearchResults(Set<String> SearchedTags) {
        String msg = "No results found! Try a different set of tags or use `/help sakuga`";
        if (SearchedTags == null || SearchedTags.isEmpty())
            return msg;

        // Find how many (non-filter) tags were used
        int nonFilterCount = 0;
        String nonFilterTag = null;
        for (String searchedTag : SearchedTags) {
            if (!searchedTag.contains(":")) {
                ++nonFilterCount;
                nonFilterTag = searchedTag;
            }
        }

        // Recommend similar (non-filter) tags, if only one was used.
        if (nonFilterCount != 1)
            return msg;

        try {
            SakugabooruTag[] goodTags = fetchTags(nonFilterTag, 6, SakugabooruTag.Order.COUNT);
            int end = goodTags.length;
            while (end > 0 && goodTags[end - 1].getCount() <= 0)
                --end;

            // Careful not to recommend the exact tag they just used!
            // (Ex: Used a bad filter like "spencer_wan score:>9999999", leading to 0 results yet no typos)
            if (end == 1 && goodTags[0].getName().equals(nonFilterTag))
                return msg;

            if (end > 0) {
                StringBuilder buildMsg = new StringBuilder("No results found! Maybe you meant: ");
                for (int i = 0; i < end; ++i) {
                    if (i > 0) {
                        buildMsg.append(", ");
                        if (i == end - 1)
                            buildMsg.append("or ");
                    }

                    buildMsg.append("`%s`".formatted(goodTags[i].getName()));
                }
                msg = buildMsg.toString();
            }
        } catch (Exception ignored) {
        }

        return msg;
    }

    private Set<String> createSimplifiedTags(String SpacedTags) {
        Set<String> tags;
        if (SpacedTags == null)
            tags = new HashSet<>();
        else // Place all tags in set to avoid duplicates
            tags = Arrays.stream(SpacedTags.split(" ")).collect(Collectors.toSet());

        boolean hasOrder = false; // Did the user explicitly set a sort/order?
        for (String tag : tags) {
            if (tag.startsWith("order:")) {
                hasOrder = true;
                break;
            }
        }

        if (!hasOrder) // If unspecified, prefer randomly sorted results
            tags.add("order:random");

        return tags;
    }

    private SakugabooruPost[] fetchPosts(Set<String> tags) throws IOException {
        return fetchPosts(tags, 1);
    }

    private SakugabooruPost[] fetchPosts(Set<String> tags, int limit) throws IOException {
        String tagsParameter = URLEncoder.encode(String.join(" ", tags), StandardCharsets.UTF_8);
        return fetchJsonObject(
                "%s/post.json?limit=%d&tags=%s".formatted(MOEBOORU_API, limit, tagsParameter),
                SakugabooruPost[].class
        );
    }

    private static SakugabooruTag[] fetchTags(String name, int limit, SakugabooruTag.Order order) throws IOException {
        return fetchJsonObject(
                "%s/tag.json?limit=%d&order=%s&name=%s".formatted(MOEBOORU_API, limit, order.urlValue, name),
                SakugabooruTag[].class
        );
    }

    private static <T> T fetchJsonObject(String urlString, Class<T> classOfT) throws IOException {
        URL url = new URL(urlString);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        try (InputStream stream = conn.getInputStream()) {
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                return new Gson().fromJson(reader, classOfT);
            }
        }
    }

    @Override
    public String getName() {
        return "sakuga";
    }

    @Override
    public String getHelpMessage() {
        return """
                Sakugabooru tips!
                                
                **Basic searching:**
                - Find a random post: `/sakuga`
                - Posts with Spencer Wan: `/sakuga spencer_wan`
                - Must have Spencer Wan __*and*__ character acting: `/sakuga spencer_wan character_acting`
                - May have Spencer Wan __*and/or*__ character acting: `/sakuga ~spencer_wan ~character_acting`
                :grey_exclamation: Tags cannot have spaces! Use underscores (`spencer wan` -> `spencer_wan`)
                                
                **Advanced searching:**
                - The highest-scoring post: `/sakuga order:score`
                - Posts with a score over 100: `/sakuga score:>100`
                - Posts of Boruto episode #217: `/sakuga boruto:_naruto_next_generations source:#217`
                                
                :speech_left: **Did you know...**
                You can also use `$s` or `$sakuga` as a shortcut (and it works in my DMs!)
                """;
    }
}
