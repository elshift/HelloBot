package org.elshift.commands;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.elshift.commands.annotations.CommandGroup;
import org.elshift.commands.annotations.RunMode;
import org.elshift.commands.annotations.TextCommand;
import org.elshift.modules.Module;
import org.elshift.util.ParsedTextCommand;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class TextCommandMethod extends CommandMethod {
    private final @NotNull TextCommand command;

    public TextCommandMethod(
            @NotNull TextCommand command,
            @NotNull Module module,
            @NotNull Method method,
            @Nullable CommandGroup group,
            @Nullable RunMode runMode
    ) {
        super(command.name(), module, method, group, runMode);
        this.command = command;
    }

    @Override
    public void invoke(@NotNull Event ctx) throws InvocationTargetException, IllegalAccessException {
        if (!(ctx instanceof MessageReceivedEvent msgEvent))
            throw new IllegalArgumentException("Expected MessageReceivedEvent");

        getMethod().invoke(getModule(), msgEvent, new ParsedTextCommand(msgEvent.getMessage().getContentRaw()));
    }

    /**
     * @return Whether the {@link MessageReceivedEvent} matches this command
     */
    public boolean matchesEvent(@NotNull Object event) {
        if (!(event instanceof MessageReceivedEvent msgEvent))
            return false;

        ParsedTextCommand cmd = new ParsedTextCommand(msgEvent.getMessage().getContentStripped());

        String cmdName = cmd.getCmdName();
        if (cmdName == null)
            return false;

        return cmdName.equals(command.name()) || Arrays.asList(command.aliases()).contains(cmdName);
    }
}
