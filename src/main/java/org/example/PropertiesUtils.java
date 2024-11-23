package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesUtils {
    private static final String CONFIG_PATH = "src/main/resources/application.properties";
    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtils.class);

    private static Properties properties;

    // Load properties only once to improve performance
    static {
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_PATH)) {
            properties.load(fis);
            logger.info("Properties loaded successfully.");

        } catch (IOException e) {
            logger.error("Failed to load properties file.", e);
            throw new RuntimeException("Failed to load properties file.", e);
        }
    }

    // Generic method to get a property by key
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    // Method to fetch DB URL specifically
    public static String getDbUrl() {
        return getProperty("db.url");
    }
}
