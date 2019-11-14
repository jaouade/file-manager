package com.filemanagement.app.utils.db;


import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static com.filemanagement.app.utils.db.Constants.*;
/**
 * @author Jaouad El Aoud
 */
@Data
@Builder
public class MysqlImportService {
    private final MysqlBaseService mysqlBaseService;
    private String database;
    private String username;
    private String password;
    private String sql;
    private String jdbcConnString;
    private String jdbcDriver;
    private boolean deleteExisting;
    private boolean dropExisting;
    private List<String> tables;
    private StringBuilder report;
    Statement stmt;

    public String report(){
        if (report!=null) return report.toString();
        else return "";
    }
    @Autowired
    public MysqlImportService(MysqlBaseService mysqlBaseService) {
        this.deleteExisting = false;
        this.dropExisting = false;
        this.tables = new ArrayList<>();
        this.mysqlBaseService = mysqlBaseService;
        this.report = new StringBuilder();
    }

    public boolean importDB() throws SQLException, ClassNotFoundException, InvalidDBConnectionParamsException {

        if (!this.assertValidParams()) {
            throw new InvalidDBConnectionParamsException("Required Parameters not set or empty \n" +
                    "Ensure database, username, password, sql params are configured \n" +
                    "using their respective setters");
        }
        Connection connection;
        if (jdbcConnString == null || jdbcConnString.isEmpty()) {
            connection = mysqlBaseService.connect(username, password,
                    database, jdbcDriver);
        } else {
            database = jdbcConnString.substring(jdbcConnString.lastIndexOf("/") + 1);
            connection = mysqlBaseService.connectWithURL(username, username,
                    jdbcConnString, jdbcDriver);
        }

        stmt = connection.createStatement();

        //disable foreign key check
        stmt.addBatch("SET FOREIGN_KEY_CHECKS = 0");


        if (deleteExisting || dropExisting) {
            tables = mysqlBaseService.getAllTables(database, stmt);
            for (String table : tables) {
                if (deleteExisting && !dropExisting) clearTable(table);
                if (dropExisting) dropTable(table);
            }
        }

        while (sql.contains(SQL_START_PATTERN)) {
            int startIndex = sql.indexOf(SQL_START_PATTERN);
            int endIndex = sql.indexOf(SQL_END_PATTERN);

            String executable = sql.substring(startIndex, endIndex);
            stmt.addBatch(executable);
            sql = sql.substring(endIndex + 1);

        }

        stmt.addBatch("SET FOREIGN_KEY_CHECKS = 1");

        //now execute the batch
        long[] result = stmt.executeLargeBatch();

        final String[] resultString = {""};
        Arrays.stream(result).forEach(i -> resultString[0] = resultString[0].concat(i + " "));
        report.append(result.length + " queries were executed in batch for provided SQL String with the following result : \n" + resultString[0]);

        stmt.close();
        connection.close();

        return true;
    }

    private void clearTable(String table) throws SQLException {
        String delQ = "DELETE FROM `" + database + "`.`" + table + "`";
        stmt.addBatch(delQ);
    }

    private void dropTable(String table) throws SQLException {

        String dropQ = "DROP TABLE `" + database + "`.`" + table + "`";
        stmt.addBatch(dropQ);
    }

    private boolean assertValidParams() {
        return username != null && !this.username.isEmpty() &&
                password != null && !this.password.isEmpty() &&
                sql != null && !this.sql.isEmpty() &&
                ((database != null && !this.database.isEmpty()) || (jdbcConnString != null && !jdbcConnString.isEmpty()));
    }


}
