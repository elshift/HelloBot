package org.elshift.commands.context.impl;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.elshift.commands.context.CommandContext;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Consumer;

public class TextCommandContext extends CommandContext<MessageReceivedEvent> {
    public TextCommandContext(MessageReceivedEvent event) {
        super(event);
    }

    @Override
    public void deferReply() {
        super.deferReply();
        event.getChannel().sendTyping().queue();
    }

    private void inDM(Consumer<PrivateChannel> channel) {
        if(event.getMember() == null)
            throw new RuntimeException("member is null");

        event.getMember().getUser().openPrivateChannel().queue(channel);
    }

    @Override
    public void reply(String text) {
        if(isEphemeral) {
            inDM(ch -> ch.sendMessage(text).queue());
            return;
        }
        event.getMessage().reply(text).queue();
    }

    @Override
    public void sendFile(File file) {
        if(isEphemeral) {
            inDM(ch -> ch.sendFile(file).queue());
            return;
        }
        event.getMessage().reply(file).queue();
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
