package org.elshift.modules.impl.sakugabooru;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.elshift.commands.annotations.Option;
import org.elshift.commands.annotations.SlashCommand;
import org.elshift.commands.annotations.TextCommand;
import org.elshift.config.Config;
import org.elshift.db.Database;
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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class SakugabooruModule implements Module {
    // Can be any server running Moebooru (and probably works for most Danbooru instances too)
    private static final String MOEBOORU_API = "https://sakugabooru.com";
    private static final int DB_UPDATE_BATCH_SIZE = 500;
    private static final long LONG_OPERATION_NOTIFY_TIME_MS = 3_000;
    private static final long TAG_UPDATE_COOLDOWN_MS = 60_000;
    private static final Logger logger = LoggerFactory.getLogger(SakugabooruModule.class);

    private static long lastTagUpdateMs = 0;

    static {
        Database db = Config.get().sqlDatabase();
        if (db.isConnected()) {
            try {
                db.seed(SakugabooruTag.class);
                updateLatestTags();
            } catch (SQLException e) {
                logger.error("Failed to seed db", e);
            }
        }
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

            event.reply(formatPost(posts[0], event.getUser(), tags)).queue();
        } catch (Exception e) {
            logger.error("Failed to search %s".formatted(MOEBOORU_API), e);
            event.reply("Failed to search: " + e.getMessage()).setEphemeral(true).queue();
        }
    }

    @TextCommand(name = "sakuga", description = "Search Sakugabooru", aliases = {"s"})
    public void textSearchSakuga(@NotNull MessageReceivedEvent event, ParsedTextCommand parsedText) {
        String args = parsedText.getCmdArgs();
        if (args != null && args.startsWith("tags:"))
            args = args.substring("tags:".length()).trim();

        Set<String> tags = createSimplifiedTags(args);
        try {
            SakugabooruPost[] posts = fetchPosts(tags);
            String msg;
            if (posts.length > 0)
                msg = formatPost(posts[0], event.getAuthor(), tags);
            else msg = formatEmptySearchResults(tags);
            event.getMessage().reply(msg).queue();
        } catch (Exception e) {
            if (!(e instanceof InsufficientPermissionException)) { // Ignore permission errors. Not our problem.
                logger.error("Failed to search %s".formatted(MOEBOORU_API), e);
                event.getMessage().reply("Failed to search: " + e.getMessage()).queue();
            }
        }
    }

    private String formatPost(SakugabooruPost post, User searchAuthor, Set<String> searchedTags) {
        List<String> allTags = Arrays.stream(post.getTags().split(" ")).toList();

        if (shouldUpdateTags())
            updateLatestTags();

        String search = formatSearch(searchAuthor, searchedTags);
        String postUrl = ":link: **Post**:       <%s/post/show/%d>".formatted(MOEBOORU_API, post.getId());
        String tags = formatTags(allTags);
        String artists = formatArtists(allTags);
        String postFile = post.getFileUrl();
        if (post.getRating().equalsIgnoreCase("e"))
            postFile = ":warning: **EXPLICIT**: || " + post.getFileUrl() + " ||";

        return joinNonEmptyStrings("\n", search, postUrl, tags, artists, postFile);
    }

    private String formatPost(SakugabooruPost post) {
        return formatPost(post, null, null);
    }

    private String formatTags(Collection<String> tagNames) {
        String joined = "*(none)*";
        if (tagNames != null && !tagNames.isEmpty())
            joined = sanitize(String.join(" ", tagNames));
        return ":file_folder: **Tags**:      " + joined;
    }

    private String formatArtists(Collection<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty())
            return null;

        String[] artistList = tagNames.stream().filter(SakugabooruModule::isTagArtist).toArray(String[]::new);
        if (artistList.length == 0)
            return null;

        return ":artist: **Artists**:  " + sanitize(String.join(" ", artistList));
    }

    private String formatSearch(User user, Collection<String> searchedTags) {
        if (user == null || searchedTags == null || searchedTags.isEmpty())
            return null;
        return "*%s: \"%s\"*".formatted(
                user.getAsMention(),
                sanitize(String.join(" ", searchedTags))
        );
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
            while (end > 0 && goodTags[end - 1].count <= 0)
                --end;

            // Careful not to recommend the exact tag they just used!
            // (Ex: Used a bad filter like "spencer_wan score:>9999999", leading to 0 results yet no typos)
            if (end == 1 && goodTags[0].name.equals(nonFilterTag))
                return msg;

            if (end > 0) {
                StringBuilder buildMsg = new StringBuilder("No results found! Maybe you meant: ");
                for (int i = 0; i < end; ++i) {
                    if (i > 0) {
                        buildMsg.append(", ");
                        if (i == end - 1)
                            buildMsg.append("or ");
                    }

                    buildMsg.append("`%s`".formatted(goodTags[i].name));
                }
                msg = buildMsg.toString();
            }
        } catch (Exception ignored) {
        }

        return msg;
    }

    private Set<String> createSimplifiedTags(String SpacedTags) {
        Set<String> tags;
        if (SpacedTags == null) {
            tags = new HashSet<>();
        } else { // Place all tags in set to avoid duplicates
            tags = Arrays.stream(
                    SpacedTags.toLowerCase(Locale.ROOT).split(" ")
            ).collect(Collectors.toSet());
        }

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

    /**
     * Fetches all tags and inserts them into the DB
     *
     * @param afterId Fetch all tags that have an id number greater than this
     * @return False if errors occurred
     */
    private static boolean updateTags(int afterId) {
        lastTagUpdateMs = System.currentTimeMillis();

        try {
            URL url = new URL("%s/tag.json?limit=0&after_id=%d&order=date".formatted(MOEBOORU_API, afterId));
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            try (InputStream stream = conn.getInputStream()) {
                Gson gson = new Gson();
                JsonReader jsonReader = gson.newJsonReader(new InputStreamReader(stream));
                Database db = Config.get().sqlDatabase();
                ArrayList<SakugabooruTag> batch = new ArrayList<>();
                long lastTime = System.currentTimeMillis();
                int total = 0;

                jsonReader.beginArray();
                while (jsonReader.hasNext()) {
                    ++total;
                    batch.add(gson.fromJson(jsonReader, SakugabooruTag.class));

                    long newTime = System.currentTimeMillis();
                    if (newTime - lastTime >= LONG_OPERATION_NOTIFY_TIME_MS) {
                        lastTime = newTime;
                        logger.info("Updating... %s tags".formatted(total));
                    }

                    if (batch.size() >= DB_UPDATE_BATCH_SIZE) {
                        if (!db.updateOrInsertMany(SakugabooruTag.class, batch))
                            return false;
                        batch.clear();
                    }
                }
                jsonReader.endArray();
                jsonReader.close();

                if (!batch.isEmpty() && !db.updateOrInsertMany(SakugabooruTag.class, batch))
                    return false;
            }
        } catch (IOException | SQLException e) {
            logger.error("Failed to update tags", e);
            return false;
        }
        return true;
    }

    /**
     * Fetches new tags and inserts them into the DB
     *
     * @return False if errors occurred
     */
    private static boolean updateLatestTags() {
        try {
            Database db = Config.get().sqlDatabase();
            Integer maxId = db.querySimple(Integer.class, "SELECT MAX(id) FROM SakugabooruTag");
            return updateTags(maxId);
        } catch (SQLException e) {
            logger.error("db error", e);
            return false;
        }
    }

    private static boolean shouldUpdateTags() {
        Database db = Config.get().sqlDatabase();
        if (!db.isConnected())
            return false;
        return System.currentTimeMillis() - lastTagUpdateMs >= TAG_UPDATE_COOLDOWN_MS;
    }

    private static boolean isTagArtist(String tagName) {
        Database db = Config.get().sqlDatabase();
        if (!db.isConnected())
            return false;

        try {
            try (PreparedStatement stmt = db.preparedStatement("SELECT type FROM SakugabooruTag WHERE name = ?")) {
                stmt.setString(1, tagName);
                Integer tagType = db.querySimple(Integer.class, stmt.executeQuery());
                return tagType == SakugabooruTag.Type.ARTIST.intValue;
            }
        } catch (SQLException e) {
            logger.error("isTagArtist failed", e);
            return false;
        }
    }

    private static String joinNonEmptyStrings(String delimiter, String... strings) {
        return String.join(delimiter, Arrays.stream(strings).filter(
                s -> s != null && !s.isEmpty()
        ).toArray(String[]::new));
    }

    private static String sanitize(String discordMsg) {
        return discordMsg
                .replace("@", "\\@")
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("`", "\\`");
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
                You can also use `$s` or `/s` as a shortcut
                """;
    }
}
