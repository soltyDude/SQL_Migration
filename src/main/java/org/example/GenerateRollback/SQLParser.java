package org.example.GenerateRollback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SQLParser {

    private static final Logger logger = LoggerFactory.getLogger(SQLParser.class); // Logger instance

    /**
     * Parses an SQL file into individual SQL commands.
     *
     * @param sqlFile The SQL file to parse.
     * @return A list of SQL commands.
     * @throws IOException If there is an error reading the file.
     */
    public static List<String> parseSQL(File sqlFile) throws IOException {
        List<String> commands = new ArrayList<>();

        if (!sqlFile.exists() || !sqlFile.isFile()) {
            logger.error("SQL file does not exist or is not a file: {}", sqlFile.getAbsolutePath());
            return commands;
        }

        logger.info("Parsing SQL file: {}", sqlFile.getAbsolutePath());

        try (BufferedReader reader = new BufferedReader(new FileReader(sqlFile))) {
            StringBuilder command = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }

                command.append(line).append(" ");
                if (line.endsWith(";")) { // End of command
                    commands.add(command.toString().trim());
                    logger.debug("Parsed SQL command: {}", command.toString().trim());
                    command.setLength(0); // Reset command
                }
            }

            if (command.length() > 0) {
                logger.warn("SQL file ended with an incomplete command: {}", command.toString().trim());
            }
        } catch (IOException e) {
            logger.error("Error reading SQL file: {}", sqlFile.getAbsolutePath(), e);
            throw e;
        }

        logger.info("Finished parsing SQL file: {}. Total commands parsed: {}", sqlFile.getAbsolutePath(), commands.size());
        return commands;
    }
}
