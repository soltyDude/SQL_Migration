package org.example;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.example.SQLParser;
import java.util.List;


public class RollbackGenerator {

    public static void generateRollback(String migrationFilePath) {
        File migrationFile = new File(migrationFilePath);
        if (!migrationFile.exists() || !migrationFile.getName().endsWith(".sql")) {
            System.err.println("Invalid migration file: " + migrationFilePath);
            return;
        }

        String rollbackFileName = migrationFile.getName().replace(".sql", "__rollback.sql");
        File rollbackFile = new File(migrationFile.getParent(), rollbackFileName);

        try {
            List<String> commands = SQLParser.parseSQL(migrationFile);
            List<String> rollbackCommands = new ArrayList<>();

            for (String command : commands) {
                if (command.toUpperCase().startsWith("CREATE TABLE")) {
                    // Генерируем DROP TABLE
                    String tableName = command.split(" ")[2]; // Имя таблицы
                    rollbackCommands.add("DROP TABLE IF EXISTS " + tableName + ";");
                } else if (command.toUpperCase().startsWith("INSERT INTO")) {
                    // Генерируем DELETE
                    String tableName = command.split(" ")[2]; // Имя таблицы
                    rollbackCommands.add("DELETE FROM " + tableName + " WHERE ...; -- Add conditions manually");
                } else if (command.toUpperCase().startsWith("ALTER TABLE")) {
                    rollbackCommands.add("-- Manual rollback required for: " + command);
                } else {
                    rollbackCommands.add("-- No rollback implemented for: " + command);
                }
            }

            // Записываем rollback-скрипт
            try (FileWriter writer = new FileWriter(rollbackFile)) {
                writer.write("-- Rollback script generated for: " + migrationFile.getName() + "\n");
                for (String rollbackCommand : rollbackCommands) {
                    writer.write(rollbackCommand + "\n");
                }
            }

            System.out.println("Rollback script generated: " + rollbackFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Error generating rollback script: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
