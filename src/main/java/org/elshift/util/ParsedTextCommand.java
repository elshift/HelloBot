package org.elshift.util;

import org.elshift.config.Config;

public class ParsedTextCommand {
    private final String rawText;
    private final boolean hasPrefix;
    private String cmdName = null;
    private String cmdArgs = null;

    public ParsedTextCommand(String text) {
        String textPrefix = Config.get().getTextPrefix();

        rawText = text;
        hasPrefix = textPrefix != null && text.startsWith(textPrefix);

        int cmdNameStart = getTrimmedPos(text, hasPrefix ? textPrefix.length() : 0);
        if (cmdNameStart < 0)
            return;

        int cmdNameEnd = getWhitespaceOrEnd(text, cmdNameStart + 1);
        cmdName = text.substring(cmdNameStart, cmdNameEnd);

        int cmdArgsStart = getTrimmedPos(text, cmdNameEnd);

        if (cmdArgsStart >= 0)
            cmdArgs = text.substring(cmdArgsStart, getTrimmedEndPos(text));
    }

    public boolean hasPrefix() { return hasPrefix; }
    public String getRawText() { return rawText; }
    public String getCmdName() { return cmdName; }
    public String getCmdArgs() { return cmdArgs; }

    private static int getTrimmedPos(String text, int pos) {
        for (; pos < text.length(); ++pos) {
            if (!Character.isWhitespace(text.charAt(pos)))
                return pos;
        }
        return -1;
    }

    private static int getWhitespaceOrEnd(String text, int pos) {
        for (; pos < text.length(); ++pos) {
            if (Character.isWhitespace(text.charAt(pos)))
                return pos;
        }
        return text.length();
    }

    private static int getTrimmedEndPos(String text) {
        int i = text.length();
        while(i >= 1 && Character.isWhitespace(text.charAt(i - 1)))
            --i;
        return i;
    }
}
