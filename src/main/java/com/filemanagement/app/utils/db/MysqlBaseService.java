package com.filemanagement.app.utils.db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.filemanagement.app.utils.db.Constants.*;

/**
 * @author Jaouad El Aoud
 */
@Component
public class MysqlBaseService {


    private final Environment environment;

    @Autowired
    public MysqlBaseService(Environment environment) {
        this.environment = environment;
    }

    Connection connect(String username, String password, String database, String driverName) throws ClassNotFoundException, SQLException {
        String url = environment.getProperty(SPRING_DATASOURCE_DEFAULT_URL, "jdbc:mysql://localhost:3306/") + database;
        String driver = (Objects.isNull(driverName) || driverName.isEmpty())
                ? environment.getProperty(SPRING_DATASOURCE_DEFAULT_DRIVER_NAME, "com.mysql.jdbc.Driver") : driverName;
        return doConnect(driver, url, username, password);
    }

    Connection connectWithURL(String username, String password, String jdbcURL, String driverName) throws ClassNotFoundException, SQLException {
        String driver = (Objects.isNull(driverName) || driverName.isEmpty()) ? environment.getProperty(SPRING_DATASOURCE_DEFAULT_DRIVER_NAME, "com.mysql.jdbc.Driver") : driverName;
        return doConnect(driver, jdbcURL, username, password);
    }
    boolean disconnect(Connection connection) throws SQLException {
        connection.close();
        return connection.isClosed();
    }


    private Connection doConnect(String driver, String url, String username, String password) throws SQLException, ClassNotFoundException {
        Class.forName(driver);
        return DriverManager.getConnection(url, username, password);
    }


    List<String> tablesFrom(String database, Statement stmt) throws SQLException {
        List<String> table = new ArrayList<>();
        ResultSet rs;
        rs = stmt.executeQuery("SHOW TABLE STATUS FROM `" + database + "`;");
        while (rs.next()) {
            table.add(rs.getString(COLUMN_LABEL_NAME));
        }
        return table;
    }

    String emptyTableSQL(String database, String table) {
        return "\n" + SQL_START_PATTERN + "\n" +
                "DELETE FROM `" + database + "`.`" + table + "`;\n" +
                "\n" + SQL_END_PATTERN + "\n";
    }

}
