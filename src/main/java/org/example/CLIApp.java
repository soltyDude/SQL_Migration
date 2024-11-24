package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLIApp {
    private static final Logger logger = LoggerFactory.getLogger(CLIApp.class);

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage:");
            System.out.println("  migrate <dir>        - Apply all available migrations");
            System.out.println("  rollback <version>   - Rollback to the specified version");
            System.out.println("  status               - Show database migration status");
            return;
        }

        String command = args[0];

        try {
            switch (command) {
                case "migrate":
                    String migrateDir = args.length > 1 ? args[1] : "src/main/resources";
                    MigrationManager.migrateFiles(migrateDir);
                    break;

                case "rollback":
                    if (args.length < 2) {
                        System.err.println("Usage: rollback <version>");
                        return;
                    }
                    String rollbackVersion = args[1];
                    MigrationManager.migrateRollbacks("src/main/resources", rollbackVersion);
                    break;

                case "status":
                    DatabaseStatus.showStatus();
                    break;

                default:
                    System.err.println("Unknown command: " + command);
                    System.out.println("Available commands: migrate, rollback, status");
                    break;
            }
        } catch (Exception e) {
            logger.error("Error executing command: {}", e.getMessage(), e);
        }
    }
}

