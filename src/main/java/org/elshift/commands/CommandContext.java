package org.elshift.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

public record CommandContext(SlashCommandInteractionEvent event) {
    public void replyEphemeral(String message) {
        event.reply(message).setEphemeral(true).queue();
    }
    public InteractionHook hook() { return event.getHook(); }
}
