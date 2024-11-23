# SQL Migration Manager

## Описание
SQL Migration Manager — это инструмент для управления миграциями и откатами базы данных. Он позволяет:
- Применять новые SQL-скрипты миграций к базе данных.
- Откатывать миграции до определенной версии.
- Логировать успешные и неудачные миграции.
- Управлять блокировками для предотвращения одновременного применения миграций.

Поддерживает работу как с локальными базами данных (SQLite), так и с удаленными (PostgreSQL, MySQL и др.).

---

## Функционал
### Основные возможности
1. **Применение миграций:**
   - Автоматический поиск SQL-файлов миграций в указанной директории.
   - Применение только новых файлов, которые еще не были зарегистрированы в истории миграций.
   - Логирование результатов в таблицу `flyway_schema_history`.

2. **Откат миграций:**
   - Автоматический откат миграций до указанной версии.
   - Применение файлов отката (`R`-файлы) в обратном порядке.

3. **Управление блокировками:**
   - Защита от одновременного выполнения миграций с помощью блокировок на уровне базы данных.

4. **Логирование:**
   - Запись результатов выполнения в таблицу `flyway_schema_history`, включая:
     - Версию миграции.
     - Время выполнения.
     - Статус (успешно/неуспешно).
     - Пользователя, выполнившего миграцию.

---

## Установка
1. Склонируйте репозиторий:
   ```bash
   git clone https://github.com/soltyDude/SQL_Migration.git
   cd sql-migration-manager
   ```

2. Убедитесь, что у вас установлен Maven:
   ```bash
   mvn -version
   ```

3. Установите зависимости:
   ```bash
   mvn clean install
   ```

---

## Настройка
1. В файле `src/main/resources/application.properties` укажите параметры подключения к базе данных:
   ```properties
   db.url=jdbc:postgresql://<REMOTE_HOST>:5432/<DATABASE_NAME>
   db.user=<USERNAME>
   db.password=<PASSWORD>
   ```

2. Убедитесь, что база данных доступна, а указанный пользователь имеет необходимые права.

3. Добавьте SQL-скрипты в директорию `src/main/resources`. Пример именования файлов:
   ```
   V1__init.sql
   V2__add_users.sql
   R2__rollback_add_users.sql
   ```
   - `V` — файлы миграций.
   - `R` — файлы откатов.

---

## Использование
### Применение миграций
Чтобы применить все доступные миграции, вызовите метод:
```java
MigrationManager.migrateFiles();
```

### Применение миграций из определенной директории
```java
MigrationManager.migrateFiles("path_to_migrations");
```
### Применение миграций по умолчанию из ресурсов 
```java
MigrationManager.migrateFiles();
```

### Откат миграций если они в ресурсах 
Чтобы откатить миграции до определенной версии, используйте:
```java
MigrationManager.migrateRollbacks("V1");
```

### Откат миграций если они в любой другой дериктории 
Чтобы откатить миграции до определенной версии, используйте:
```java
MigrationManager.migrateRollbacks("path_to_migrations", "V1");
```

Откат будет выполнен с применением файлов `R` (rollback).

---

## Пример SQL-скрипта
**Файл миграции (`V1__init.sql`)**
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100)
);
```

**Файл отката (`R1__rollback_init.sql`)**
```sql
DROP TABLE IF EXISTS users;
```

---

## Таблица истории миграций
Инструмент автоматически создает таблицу `flyway_schema_history`, если она не существует. В ней хранятся данные о всех примененных миграциях:
- **version**: Версия миграции.
- **description**: Описание.
- **type**: Тип (миграция или откат).
- **script**: Имя файла скрипта.
- **execution_time**: Время выполнения (мс).
- **success**: Статус выполнения.
- **installed_by**: Пользователь, выполнивший миграцию.
- **installed_on**: Дата и время применения.

---

## Требования
- **Java**: версия 11 или выше.
- **Maven**: для сборки и управления зависимостями.
- **База данных**: PostgreSQL, MySQL, SQLite или другая поддерживаемая.

---

## Отладка
1. Проверьте настройки в `application.properties`.
2. Убедитесь, что SQL-файлы корректно названы.
3. Проверяйте логи в консоли или в таблице `flyway_schema_history`.

---

## Лицензия
Этот проект распространяется под лицензией MIT.


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

