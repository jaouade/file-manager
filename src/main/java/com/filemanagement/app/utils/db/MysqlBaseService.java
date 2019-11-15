package com.filemanagement.app.utils.db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.filemanagement.app.utils.Constants.*;
import static com.filemanagement.app.utils.db.Constants.SQL_END_PATTERN;
import static com.filemanagement.app.utils.db.Constants.SQL_START_PATTERN;
/**
 * @author Jaouad El Aoud
 */
@Component
public class MysqlBaseService {


    @Autowired
    Environment environment;

    public Connection connect(String username, String password, String database, String driverName) throws ClassNotFoundException, SQLException {
        String url = environment.getProperty(SPRING_DATASOURCE_DEFAULT_URL, "jdbc:mysql://localhost:3306/") + database;
        String driver = (Objects.isNull(driverName) || driverName.isEmpty())
                ? environment.getProperty(SPRING_DATASOURCE_DEFAULT_DRIVER_NAME, "com.mysql.jdbc.Driver") : driverName;
        return doConnect(driver, url, username, password);
    }

    public Connection connectWithURL(String username, String password, String jdbcURL, String driverName) throws ClassNotFoundException, SQLException {
        String driver = (Objects.isNull(driverName) || driverName.isEmpty()) ? environment.getProperty(SPRING_DATASOURCE_DEFAULT_DRIVER_NAME, "com.mysql.jdbc.Driver") : driverName;
        return doConnect(driver, jdbcURL, username, password);
    }


    private Connection doConnect(String driver, String url, String username, String password) throws SQLException, ClassNotFoundException {
        Class.forName(driver);
        return DriverManager.getConnection(url, username, password);
    }


    public List<String> getAllTables(String database, Statement stmt) throws SQLException {
        List<String> table = new ArrayList<>();
        ResultSet rs;
        rs = stmt.executeQuery("SHOW TABLE STATUS FROM `" + database + "`;");
        while (rs.next()) {
            table.add(rs.getString(COLUMN_LABEL_NAME));
        }
        return table;
    }

    public String getEmptyTableSQL(String database, String table) {
        return "\n" + SQL_START_PATTERN + "\n" +
                "DELETE FROM `" + database + "`.`" + table + "`;\n" +
                "\n" + SQL_END_PATTERN + "\n";
    }

}
