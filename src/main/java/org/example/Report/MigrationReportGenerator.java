package org.example.Report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public class MigrationReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MigrationReportGenerator.class);


    /**
     * Generates a migration report in JSON format.
     *
     * @param filePath the path to save the JSON file
     */
    public static void generateJsonReport(String filePath) {
        List<MigrationRecord> records = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM flyway_schema_history")) {

            while (rs.next()) {
                MigrationRecord record = new MigrationRecord(
                        rs.getString("version"),
                        rs.getString("description"),
                        rs.getString("type"),
                        rs.getString("script"),
                        rs.getInt("execution_time"),
                        rs.getBoolean("success"),
                        rs.getString("installed_by"),
                        rs.getString("installed_on")
                );
                records.add(record);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new FileWriter(filePath), records);

            logger.info("JSON report generated successfully at {}", filePath);
        } catch (Exception e) {
            logger.error("Error generating JSON report: {}", e.getMessage(), e);
        }
    }
}
