package org.elshift.modules.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO: Merge ListenerModule and CommandModule as flags in a single "xxxModule" annotation
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleProperties {
    /**
     * Display name (used in the help menu)
     */
    String name();
    boolean useSlashCommands() default false;
    boolean listenAllMessages() default false;
}
