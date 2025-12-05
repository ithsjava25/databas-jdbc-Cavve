package com.example;

import java.sql.*;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    static void main(String[] args) {
        if (isDevMode(args)) {
            DevDatabaseInitializer.start();
        }
        new Main().run();
    }

    public void run() {
        // Resolve DB settings with precedence: System properties -> Environment variables
        String jdbcUrl = resolveConfig("APP_JDBC_URL", "APP_JDBC_URL");
        String dbUser = resolveConfig("APP_DB_USER", "APP_DB_USER");
        String dbPass = resolveConfig("APP_DB_PASS", "APP_DB_PASS");

        if (jdbcUrl == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException(
                    "Missing DB configuration. Provide APP_JDBC_URL, APP_DB_USER, APP_DB_PASS " +
                            "as system properties (-Dkey=value) or environment variables.");
        }

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
            Scanner sc = new Scanner(System.in);
            System.out.println("Username: ");
            String username = sc.nextLine();

            System.out.println("Password: ");
            String password = sc.nextLine();

            boolean valid = false;

            String sql = "SELECT * FROM account WHERE name = ? AND password = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, username);
                statement.setString(2, password);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        System.out.println("Invalid username or password.");
                        return;
                    }
                }
            }

            //nytt resultset varje gång för varje test
            //behöver inte nytt objekt varje gång men = statement.executeQuery behövs
            //kan återanvända connection och statement
            //måste ställa ny fråga varje gång också

            System.out.println("Login Sucessful");

            int choice = -1;
            while (choice != 0) {
            System.out.println("\nThese are the menu options: ");
            System.out.println("1) List moon missions (prints spacecraft names from `moon_mission`)");
            System.out.println("2) Get a moon mission by mission_id (prints details for that mission)");
            System.out.println("3) Count missions for a given year (prompts: year; prints the number of missions launched that year)");
            System.out.println("4) Create an account (prompts: first name, last name, ssn, password; prints confirmation)");
            System.out.println("5) Update an account password (prompts: user_id, new password; prints confirmation).");
            System.out.println("6) Delete an account (prompts: user_id; prints confirmation).");
            System.out.println("0) Exit.");

            System.out.print("Enter choice: ");
            choice = sc.nextInt();

            switch (choice) {
            case 1 -> listSpacecrafts(connection);
            case 2 -> moonMission(connection, sc);
            case 0 -> System.out.println("Exiting...");
            default -> System.out.println("Invalid choice, try again");

            }
       }


        //Todo: Starting point for your code
        // showMenu(sc, connection)
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        }


    public void listSpacecrafts(Connection connection) throws SQLException {
        String sql = "SELECT spacecraft FROM moon_mission";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                System.out.println(rs.getString("spacecraft"));
            }
        }
    }

    public void moonMission(Connection connection, Scanner sc) throws SQLException {
        System.out.println("Enter Mission ID: ");
        int id = sc.nextInt();

        String sql = "SELECT mission_id FROM moon_mission";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
             stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Spacecraft: " + rs.getString("spacecraft"));
                    System.out.println("Launch Year: " + rs.getInt("launch_year"));
                } else {
                    System.out.println("No mission found with that ID.");
                }
            }
        }
    }

    /**
     * Determines if the application is running in development mode based on system properties,
     * environment variables, or command-line arguments.
     *
     * @param args an array of command-line arguments
     * @return {@code true} if the application is in development mode; {@code false} otherwise
     */
    private static boolean isDevMode(String[] args) {
        if (Boolean.getBoolean("devMode"))  //Add VM option -DdevMode=true
            return true;
        if ("true".equalsIgnoreCase(System.getenv("DEV_MODE")))  //Environment variable DEV_MODE=true
            return true;
        return Arrays.asList(args).contains("--dev"); //Argument --dev
    }

    /**
     * Reads configuration with precedence: Java system property first, then environment variable.
     * Returns trimmed value or null if neither source provides a non-empty value.
     */
    private static String resolveConfig(String propertyKey, String envKey) {
        String v = System.getProperty(propertyKey);
        if (v == null || v.trim().isEmpty()) {
            v = System.getenv(envKey);
        }
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }
}




//nytt resultset varje gång för varje test
//behöver inte nytt objekt varje gång men = statement.executeQuery behövs
//kan återanvända connection och statement
//måste ställa ny fråga varje gång också