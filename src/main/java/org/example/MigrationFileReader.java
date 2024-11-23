package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MigrationFileReader {

    private static final Logger logger = LoggerFactory.getLogger(MigrationFileReader.class); // Logger instance
    private static final Pattern VERSION_PATTERN = Pattern.compile("^[VBR](\\d+(?:_\\d+)*)(_{1,2}.*)?\\.sql$");
//
    /**
     * Finds all migration files in the specified directory that have not yet been applied to the database.
     *
     * @param dir           The directory to scan for migration files.
     * @param lastDbVersion The last applied migration version from the database.
     * @return A sorted list of migration files with versions higher than the last database version.
     */
    public static List<File> findMigrations(String dir, String lastDbVersion, char startChar) {
        List<File> migrationFiles = new ArrayList<>();
        File folder = new File(dir);

        if (!folder.exists() || !folder.isDirectory()) {
            logger.error("Invalid directory: {}", dir);
            return migrationFiles;
        }

        File[] files = folder.listFiles((f, name) -> name.endsWith(".sql"));
        if (files == null || files.length == 0) {
            logger.warn("No SQL files found in directory: {}", dir);
            return migrationFiles;
        }

        for (File file : files) {
            String fileName = file.getName();

            // Check if the file starts with the specified character
            if (fileName.charAt(0) != startChar) {
                logger.debug("File {} does not start with character '{}'.", fileName, startChar);
                continue;
            }

            Matcher matcher = VERSION_PATTERN.matcher(fileName);
            logger.debug("Checking file: {} - Matches: {}", fileName, matcher.matches());

            if (matcher.matches()) {
                try {
                    String fileVersion = matcher.group(1);

                    // Compare file version with the last database version
                    if (compareVersions(fileVersion, lastDbVersion) > 0) {
                        migrationFiles.add(file);
                        logger.info("File added for migration: {}", fileName);
                    }
                } catch (IndexOutOfBoundsException ex) {
                    logger.error("File {} has no proper version. Error: {}", fileName, ex.getMessage());
                }
            } else {
                logger.debug("File {} does not match the version pattern.", fileName);
            }
        }

        // Sort migration files by version
        migrationFiles.sort(Comparator.comparing(file -> extractVersion(file.getName())));
        logger.info("Total migration files to apply: {}", migrationFiles.size());

        return migrationFiles;
    }

    /**
     * Compares two version strings to determine their order.
     *
     * @param version1 The first version string.
     * @param version2 The second version string.
     * @return A negative integer, zero, or a positive integer as the first version
     *         is less than, equal to, or greater than the second.
     */
    private static int compareVersions(String version1, String version2) {
        String[] v1Parts = version1.split("_");
        String[] v2Parts = version2.split("_");

        for (int i = 0; i < Math.max(v1Parts.length, v2Parts.length); i++) {
            int v1 = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2 = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;

            if (v1 != v2) {
                return Integer.compare(v1, v2);
            }
        }

        return 0;
    }

    /**
     * Extracts the version from a file name using the version pattern.
     *
     * @param fileName The name of the file to extract the version from.
     * @return The extracted version as a string, or "0" if no valid version is found.
     */
    private static String extractVersion(String fileName) {
        Matcher matcher = VERSION_PATTERN.matcher(fileName);

        if (matcher.matches()) {
            return matcher.group(1);
        }

        logger.warn("Could not extract version from file: {}", fileName);
        return "0";
    }
}
