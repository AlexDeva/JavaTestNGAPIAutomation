package com.example.apiautomation.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/** Loads framework properties with JVM system properties and environment variables taking precedence. */
public final class ConfigManager {
    private static final String CONFIG_FILE = "config.properties";
    private static final Properties PROPERTIES = loadProperties();

    private ConfigManager() {
    }

    /** Returns the API base URI, resolved from system property, environment variable, then config file. */
    public static String getBaseUri() {
        return getRequiredProperty("api.base.uri");
    }

    /** Returns the selected environment name. */
    public static String getEnvironment() {
        return getRequiredProperty("api.environment");
    }

    /** Returns the HTTP connection and socket timeout in milliseconds. */
    public static int getTimeoutMillis() {
        String value = getRequiredProperty("api.timeout.millis");
        try {
            int timeout = Integer.parseInt(value);
            if (timeout <= 0) {
                throw new IllegalArgumentException("api.timeout.millis must be greater than zero");
            }
            return timeout;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("api.timeout.millis must be a positive integer", exception);
        }
    }

    /** Returns whether the deterministic local WireMock API should be started for sample tests. */
    public static boolean isTestServerEnabled() {
        String value = getRequiredProperty("api.test.server.enabled");
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("api.test.server.enabled must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    private static String getRequiredProperty(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            String environmentValue = System.getenv(toEnvironmentName(key));
            value = environmentValue == null || environmentValue.isBlank()
                    ? PROPERTIES.getProperty(key)
                    : environmentValue;
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration property: " + key);
        }
        return value.trim();
    }

    private static String toEnvironmentName(String key) {
        return key.replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IllegalStateException("Configuration file not found on the classpath: " + CONFIG_FILE);
            }
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load " + CONFIG_FILE, exception);
        }
    }
}
