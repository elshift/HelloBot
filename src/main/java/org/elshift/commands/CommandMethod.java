package org.elshift.commands;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import org.elshift.commands.annotations.CommandGroup;
import org.elshift.commands.annotations.RunMode;
import org.elshift.modules.Module;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * A command that can be invoked, given a matching event
 */
public abstract class CommandMethod {
    private final String name;
    private final Module module;
    private final @NotNull Method method;
    private final RunMode.Mode runMode;
    private final @Nullable
    CommandGroup group;

    protected CommandMethod(
            @NotNull String name,
            @NotNull Module module,
            @NotNull Method method,
            @Nullable CommandGroup group,
            @Nullable RunMode runMode
    ) {
        this.name = name;
        this.module = module;
        this.method = method;
        this.group = group;
        this.runMode = runMode == null ? RunMode.Mode.Sync : runMode.value();
    }

    /**
     * Invokes the command with the specified context
     *
     * @param ctx Original JDA Discord event to provide as context
     * @throws IllegalArgumentException     'ctx' was not the expected event type for this command
     * @throws ReflectiveOperationException Method could not get expected arg-type/arg-count from the event
     */
    public abstract void invoke(@NotNull Event ctx) throws IllegalArgumentException, ReflectiveOperationException;

    /**
     * Decides if a JDA interaction/event is referring to this method
     *
     * @param event A JDA object such as {@link CommandInteractionPayload} or events ({@link MessageReceivedEvent})
     * @return Whether a JDA interaction/event is referring to this method
     */
    public abstract boolean matchesEvent(@NotNull Object event);

    public String getName() {
        return name;
    }

    /**
     * @return The module that holds this command
     */
    public Module getModule() {
        return module;
    }

    /**
     * @return The group this command is a part of
     */
    @Nullable
    public CommandGroup getGroup() {
        return group;
    }

    /**
     * @return The mode of execution for this command
     */
    public RunMode.Mode getRunMode() {
        return runMode;
    }

    protected @NotNull Method getMethod() {
        return method;
    }
}
