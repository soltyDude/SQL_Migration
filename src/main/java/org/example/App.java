package org.example;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.example.COnection.*;

//пофиксить то что он не хочет накатывать когда там фолс
//сорт версий
public class App {
    public static void main(String[] args) {
        //MigrationRunner.migrateFiles("data");
        //COnection.clean();

        String migrationFilePath = "data/V0_0_01__aboba.sql";
        RollbackGenerator.generateRollback(migrationFilePath);
    }
}
