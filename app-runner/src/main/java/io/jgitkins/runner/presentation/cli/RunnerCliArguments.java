package io.jgitkins.runner.presentation.cli;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RunnerCliArguments {

    public enum Mode {
        START,
        ACTIVATE
    }

    private final Mode mode;
    private final String token;
    private final String server;

    private RunnerCliArguments(Mode mode, String token, String server) {
        this.mode = mode;
        this.token = token;
        this.server = server;
    }

    public static RunnerCliArguments parse(String[] args) {
        if (args == null || args.length == 0) {
            return new RunnerCliArguments(Mode.START, null, null);
        }

        int index = 0;
        Mode mode = Mode.START;
        if (!args[0].startsWith("-")) {
            if ("activate".equalsIgnoreCase(args[0])) {
                mode = Mode.ACTIVATE;
                index = 1;
            } else if ("start".equalsIgnoreCase(args[0])) {
                mode = Mode.START;
                index = 1;
            } else {
                throw new IllegalArgumentException("Unknown command: " + args[0]);
            }
        }

        Map<String, String> options = parseOptions(args, index);
        String token = options.get("token");
        String server = options.get("server");

        if (mode == Mode.ACTIVATE) {
            if (token == null || token.isBlank() || server == null || server.isBlank()) {
                throw new IllegalArgumentException("activate requires --token and --server");
            }
        }

        return new RunnerCliArguments(mode, token, server);
    }

    public boolean isActivate() {
        return mode == Mode.ACTIVATE;
    }

    public Map<String, Object> toSpringProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("runner.mode", mode == Mode.ACTIVATE ? "activate" : "start");
        if (mode == Mode.ACTIVATE) {
            properties.put("runner.activate.token", token);
            properties.put("runner.activate.server", server);
            properties.put("spring.task.scheduling.enabled", "false");
        }
        return Collections.unmodifiableMap(properties);
    }

    public static String usage() {
        return String.join(System.lineSeparator(),
            "Usage:",
            "  java -jar runner.jar start",
            "  java -jar runner.jar activate --token=<token> --server=<serverBaseUrl>"
        );
    }

    private static Map<String, String> parseOptions(String[] args, int index) {
        Map<String, String> options = new HashMap<>();
        for (int i = index; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                continue;
            }
            String key;
            String value;
            int separator = arg.indexOf('=');
            if (separator > 2) {
                key = arg.substring(2, separator);
                value = arg.substring(separator + 1);
            } else {
                key = arg.substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    value = args[++i];
                } else {
                    value = "";
                }
            }
            options.put(key, value);
        }
        return options;
    }
}
