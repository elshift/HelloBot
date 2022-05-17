package org.elshift.commands.context.impl;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.elshift.commands.context.CommandContext;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class SlashCommandContext extends CommandContext<SlashCommandInteractionEvent> {
    public SlashCommandContext(SlashCommandInteractionEvent event) {
        super(event);
    }

    @Override
    public void deferReply() {
        super.deferReply();
        event.deferReply().queue();
    }

    private InteractionHook hook() {
        return event.getHook().setEphemeral(isEphemeral);
    }

    @Override
    public void reply(String text) {
        if(isReplyDeferred) {
            hook().sendMessage(text).queue();
            return;
        }

        event.reply(text).queue();
    }

    @Override
    public void sendFile(File file) {
        if(isReplyDeferred) {
            hook().sendFile(file).queue();
            return;
        }

        event.replyFile(file).setEphemeral(isEphemeral).queue();
    }

    @Nullable
    @Override
    public Guild getGuild() {
        return event.getGuild();
    }

    @Override
    public JDA getJDA() {
        return event.getJDA();
    }
}
