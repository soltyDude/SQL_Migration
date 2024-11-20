package org.example;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MigrationFileReader {

    /**
     * Scans the given directory for SQL migration files with versions higher than the last version in the database.
     *
     * @param dir       The directory to scan for migration files.
     * @param lastDbVersion The last version present in the database.
     * @return A sorted list of SQL file paths with versions higher than the last database version.
     */
    public static List<File> findUnPushed(String dir, String lastDbVersion) {
        List<File> migrationFiles = new ArrayList<>();
        File folder = new File(dir);
        Pattern versionPattern = Pattern.compile("^[VB](\\d+(?:_\\d+)*)(_{1,2}.*)?\\.sql$");

        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("Invalid directory: " + dir);
            return migrationFiles;
        }

        File[] files = folder.listFiles((f, name) -> name.endsWith(".sql"));

        if (files == null) {
            System.err.println("No SQL files found in directory: " + dir);
            return migrationFiles;
        }

        for (File file : files) {
            String fileName = file.getName();
            Matcher matcher = versionPattern.matcher(fileName);
            System.out.println(fileName + " " + matcher.matches());

            if (matcher.matches()) {
                try {
                    String fileVersion = matcher.group(1);

                    // Compare file version with the last database version
                    if (compareVersions(fileVersion, lastDbVersion) > 0) {
                        migrationFiles.add(file);
                    }
                }catch(IndexOutOfBoundsException ex){
                    System.out.println(fileName + " has no proper version");
                }
            }
        }

        // Sort migration files by version
        migrationFiles.sort(Comparator.comparing(file -> extractVersion(file.getName())));

        return migrationFiles;
    }

    /**
     * Compares two version strings.
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
     * Extracts the version from the file name.
     *
     * @param fileName The name of the file.
     * @return The extracted version string.
     */
    private static String extractVersion(String fileName) {
        Pattern versionPattern = Pattern.compile("^[VB](\\d+(?:_\\d+)*)(__.*)?\\.sql$");
        Matcher matcher = versionPattern.matcher(fileName);

        if (matcher.matches()) {
            return matcher.group(1);
        }

        return "0";
    }
}
