идеи реализации:

проблема: выполнение только половины миграции и в результате инконстистенс датабызыы
решения: 
1.драй ран(тестовый) +надёжный -в два раза больше запросов 
2.объеденения всех инсертов в один +быстрота -работает тоько с инсёртами
3.Использовать SQL-скрипты с транзакциями +++++ -модификация исходных sql запросов 
4.

private static void validateTransaction(String sqlScript) {
    if (!sqlScript.contains("BEGIN TRANSACTION") || !sqlScript.contains("COMMIT")) {
        throw new IllegalArgumentException("SQL script must contain transaction boundaries.");
    }
}

try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE_PATH);
     InputStream in = new FileInputStream("path/to/migration.sql")) {

    // Validate script
    String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    validateTransaction(script);

    // Execute script
    importSQL(conn, new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)));
}

