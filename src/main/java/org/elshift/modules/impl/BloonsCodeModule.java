package org.elshift.modules.impl;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.elshift.modules.Module;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BloonsCodeModule extends ListenerAdapter implements Module {
    private static final Pattern BLOONS_CODE_PATTERN
            = Pattern.compile("https://join\\.btd6\\.com/Coop/(?<code>[a-zA-Z].....)");

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild())
            return;

        GuildChannel channel = (GuildChannel) event.getChannel();

        Matcher matcher = BLOONS_CODE_PATTERN.matcher(event.getMessage().getContentStripped());

        String code;
        try {
            code = matcher.group("code");
        } catch (Exception ignored) {
            return;
        }

        if (code != null) {
            // Only delete the message if we have the permissions to do so
            if (event.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE)) {
                event.getMessage().delete().queue();
            }

            event.getChannel().sendMessage("%s's bloons code: `%s`"
                    .formatted(event.getAuthor().getAsMention(), code)).queue();
        }
    }

    @Override
    public String getName() {
        return "bloons-code";
    }

    @Override
    public String getHelpMessage() {
        return "Automatically replaces BTD6 invite links with the invite code";
    }
}
