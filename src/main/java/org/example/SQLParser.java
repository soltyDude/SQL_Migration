package org.example;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SQLParser {

    public static List<String> parseSQL(File sqlFile) throws IOException {
        List<String> commands = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(sqlFile))) {
            StringBuilder command = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue; // Пропускаем пустые строки и комментарии
                }
                command.append(line).append(" ");
                if (line.endsWith(";")) { // Конец команды
                    commands.add(command.toString().trim());
                    command.setLength(0); // Сбрасываем команду
                }
            }
        }
        return commands;
    }
}
