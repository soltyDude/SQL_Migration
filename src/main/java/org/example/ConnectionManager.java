package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager { ;

    public static Connection getConnection() throws SQLException {
        String url = PropertiesUtils.getProperty("db.url");
        String user = PropertiesUtils.getProperty("db.user");
        String password = PropertiesUtils.getProperty("db.password");
        return DriverManager.getConnection(url, user, password);
    }
}
