package commands;

import commands.annotations.CommandGroup;
import commands.annotations.Name;
import commands.annotations.Option;
import commands.annotations.SlashCommand;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to make adding slash commands easier.
 */
public class CommandBuilder {
    private final List<CommandInfo> commandInfoList = new ArrayList<>();

    private static final Map<Class<?>, OptionType> typeMap = new HashMap<>() {{
        put(String.class, OptionType.STRING);
        put(Integer.class, OptionType.INTEGER);
        put(Boolean.class, OptionType.BOOLEAN);
        put(User.class, OptionType.USER);
        put(Channel.class, OptionType.CHANNEL);
        put(GuildChannel.class, OptionType.CHANNEL);
        put(VoiceChannel.class, OptionType.CHANNEL);
        put(IMentionable.class, OptionType.MENTIONABLE);
        put(Number.class, OptionType.NUMBER);
        put(Role.class, OptionType.ROLE);
        put(Message.Attachment.class, OptionType.ATTACHMENT);
    }};

    private OptionData getParameterOptionData(Parameter parameter) {
        if(!typeMap.containsKey(parameter.getType()))
            throw new InvalidParameterException("Unknown parameter type: " + parameter.getType().getName());

        OptionType optionType = typeMap.get(parameter.getType());
        Option option = parameter.getAnnotation(Option.class);

        if(option != null) {
            String name = option.name();
            String description = option.description().isEmpty() ? name : option.description();
            boolean required = option.required();

            return new OptionData(optionType, name, description, required);
        } else {
            Name name = parameter.getAnnotation(Name.class);
            if(name == null)
                throw new InvalidParameterException("Can't extract name from command parameter. It must either be annotated with @Option or @Name");
            return new OptionData(optionType, name.value(), name.value(), true);
        }
    }

    /**
     * @return
     *  Whether type is primitive
     */
    private boolean isPrimitive(Class<?> type) {
        return type.equals(byte.class) ||
                type.equals(short.class) ||
                type.equals(int.class) ||
                type.equals(long.class) ||
                type.equals(float.class) ||
                type.equals(double.class) ||
                type.equals(boolean.class) ||
                type.equals(char.class);
    }

    /**
     * Adds a slash command method.
     * @param method
     *  The method to add
     * @param group
     *  Optional group
     * @return
     *  Self
     */
    public CommandBuilder addMethod(@NotNull Method method, @Nullable CommandGroup group) {
        SlashCommand slashCommand = method.getAnnotation(SlashCommand.class);

        // Method isn't a command
        if (slashCommand == null)
            return this;

        List<OptionData> options = new ArrayList<>();

        boolean doesHaveCommandContext = false;
        for(int i = 0; i < method.getParameterCount(); i++) {
            Parameter parameter = method.getParameters()[i];

            if(parameter.isVarArgs())
                throw new InvalidParameterException("Parameter cannot be varargs");

            if(isPrimitive(parameter.getType()))
                throw new InvalidParameterException("Parameter cannot be a primitive type");

            boolean isCommandContext = CommandContext.class.equals(parameter.getType());

            if(i == 0 && !isCommandContext)
                throw new InvalidParameterException("First parameter must be " + CommandContext.class.getName());
            else if (isCommandContext) {
                doesHaveCommandContext = true;
                continue;
            }

            options.add(getParameterOptionData(parameter));
        }

        if(!doesHaveCommandContext)
            throw new InvalidParameterException("Method provided must have " + CommandContext.class.getName() + "as first parameter");

        commandInfoList.add(new CommandInfo(slashCommand, method.getDeclaringClass(), method, group, options));
        return this;
    }

    /**
     * Adds all eligible slash commands in a class.
     * @param klass
     *  Class to search in
     * @return
     *  Self
     */
    public CommandBuilder addModule(@NotNull Class<?> klass) {
        CommandGroup commandGroup = klass.getAnnotation(CommandGroup.class);

        for (Method method : klass.getDeclaredMethods()) {
            addMethod(method, commandGroup);
        }

        return this;
    }

    /**
     * @return
     *  The command info list.
     */
    public List<CommandInfo> build() {
        return commandInfoList;
    }
}
