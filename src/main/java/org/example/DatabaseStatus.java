package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseStatus {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseStatus.class);

    public static void showStatus() {
        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Получение текущей версии
            ResultSet rs = stmt.executeQuery("SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank DESC LIMIT 1");
            if (rs.next()) {
                String currentVersion = rs.getString("version");
                System.out.println("Current database version: " + currentVersion);
            } else {
                System.out.println("No migrations applied yet.");
            }

            // Вывод всех примененных миграций
            rs = stmt.executeQuery("SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank");
            System.out.println("\nApplied migrations:");
            System.out.printf("%-10s %-30s %-20s %-10s%n", "Version", "Description", "Installed On", "Success");
            while (rs.next()) {
                System.out.printf("%-10s %-30s %-20s %-10s%n",
                        rs.getString("version"),
                        rs.getString("description"),
                        rs.getString("installed_on"),
                        rs.getBoolean("success"));
            }

        } catch (Exception e) {
            logger.error("Error retrieving database status: {}", e.getMessage(), e);
        }
    }
}

