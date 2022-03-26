package commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public record CommandContext(SlashCommandInteractionEvent event) {
    public void replyEphemeral(String message) {
        event.reply(message).setEphemeral(true).queue();
    }
}
