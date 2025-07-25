package com.ra.base_spring_boot.Configuration;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DatabaseInitConfig {

    private final DataSource dataSource;

    @PostConstruct
    public void init() {
        if (checkDatabaseConnection()) {
            System.out.println("Database connection established.");
            addUtf8mb4();
            addFullTextSearch();
        } else {
            System.out.println("Cannot connect to the database. FullText index not created.");
        }


    }

    private boolean checkDatabaseConnection() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    private void addUtf8mb4(){
        String[] tables = {"songs", "users","playlists", "albums"};
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()){
            for (String table : tables) {
                String sql = String.format("ALTER TABLE %s CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci", table);
                try{
                    stmt.executeUpdate(sql);
                    System.out.println("Converted table " + table + " to utf8mb4 successfully.");
                }catch (SQLException e){
                    System.err.println("Error converting table " + table + " to utf8mb4.");
                    e.printStackTrace();
                }
            }

        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    private void addFullTextSearch() {
        String[][] targets = {
                {"songs", "title,lyrics"}, // FULLTEXT trên cả 2 cột (giải quyết mọi truy vấn MATCH(title, lyrics))
                {"songs", "title"},
                {"songs", "lyrics"},
                {"users", "first_name,last_name"},
                {"playlists", "description"},
                {"albums", "title"}
        };
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String[] target : targets) {
                String table = target[0];
                String[] columns = target[1].split(",");
                if (!isFullTextIndexOnColumns(conn, table, Arrays.asList(columns))) {
                    String sql = String.format("ALTER TABLE %s ADD FULLTEXT(%s)", table, target[1]);
                    try {
                        stmt.executeUpdate(sql);
                        System.out.println("Executed: " + sql);
                    } catch (SQLException e) {
                        System.err.println("Error executing: " + sql);
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("FullText index already exists for " + table + " on column(s): " + target[1]);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean isFullTextIndexOnColumns(Connection conn, String tableName, List<String> columns) throws SQLException {
        String sql = "SHOW INDEX FROM " + tableName + " WHERE Index_type='FULLTEXT'";
        Map<String, List<String>> indexColumns = new HashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String keyName = rs.getString("Key_name");
                String colName = rs.getString("Column_name");
                indexColumns.computeIfAbsent(keyName, k -> new ArrayList<>()).add(colName);
            }
        }
        for (List<String> cols : indexColumns.values()) {
            if (cols.size() == columns.size() && cols.containsAll(columns)) {
                return true;
            }
        }
        return false;
    }
}
