package org.elshift.commands.context;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.File;

public abstract class CommandContext<T> {
    protected final T event;
    protected boolean isReplyDeferred;
    protected boolean isEphemeral;

    public CommandContext(T event) {
        this.event = event;
    }

    public final CommandContext<T> setEphemeral(boolean ephemeral) {
        this.isEphemeral = ephemeral;
        return this;
    }

    @OverridingMethodsMustInvokeSuper
    public void deferReply() {
        isReplyDeferred = true;
    }

    public final T event() {
        return event;
    }

    @Nullable public abstract Guild getGuild();

    public abstract void reply(String text);

    public abstract void sendFile(File file);

    public abstract JDA getJDA();
}
