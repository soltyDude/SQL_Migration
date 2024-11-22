package org.example.now;

//пофиксить то что он не хочет накатывать когда там фолс(сделаоно)
//сорт версий
//проверка на налічіе скл хуйні
//добавление в логи ошибок с скл хуёнёй
public class App {
    public static void main(String[] args) {
        //MigrationLock.ensureLockTableExists();

        MigrationRunner.migrateFiles("data");
        //COnection.clean();

        //String migrationFilePath = "data/V0_0_01__aboba.sql";
        //RollbackGenerator.generateRollback(migrationFilePath);


    }
}
