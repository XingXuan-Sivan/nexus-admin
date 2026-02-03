package com.nexusadmin.plugin.exception;

public class PluginLoadException extends PluginException {
    public PluginLoadException(String message) {
        super(message);
    }

    public PluginLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
