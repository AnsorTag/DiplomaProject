package com.diplomawork.agents.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Read-only PostgreSQL connection check for the Java agent layer.
 */
public class DatabaseConnectionCheck {
    public static void main(String[] args) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                DatabaseConfig.jdbcUrl(),
                DatabaseConfig.user(),
                DatabaseConfig.password()
        )) {
            printConnectionInfo(connection);
            printApplicationTables(connection);
        }
    }

    private static void printConnectionInfo(Connection connection) throws SQLException {
        String sql = "select current_database() as database_name, current_user as user_name";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                System.out.println("Connected to database: " + resultSet.getString("database_name"));
                System.out.println("Connected as user: " + resultSet.getString("user_name"));
            }
        }
    }

    private static void printApplicationTables(Connection connection) throws SQLException {
        String sql = """
            select table_schema, table_name
            from information_schema.tables
            where table_type = 'BASE TABLE'
              and table_schema not in ('pg_catalog', 'information_schema')
            order by table_schema, table_name
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            System.out.println("Application tables:");
            boolean found = false;
            while (resultSet.next()) {
                found = true;
                System.out.println("- "
                    + resultSet.getString("table_schema")
                    + "."
                    + resultSet.getString("table_name"));
            }
            if (!found) {
                System.out.println("- none");
            }
        }
    }
}
