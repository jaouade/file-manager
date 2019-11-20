package com.filemanagement.app.utils.db;


import com.filemanagement.app.exception.FileManagementException;
import com.filemanagement.app.exception.InvalidDBConnectionParamsException;
import com.filemanagement.app.exception.InvalidDBURLException;
import com.filemanagement.app.services.EmailService;
import com.filemanagement.app.validators.Validator;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zeroturnaround.zip.ZipUtil;

import javax.mail.MessagingException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.filemanagement.app.utils.db.Constants.*;

/**
 * @author Jaouad El Aoud
 */
@Component
public class MysqlExportService {
    private Statement stmt;
    private String dbUrl;
    private String databaseName;
    private String sql = "";
    private Logger logger = LoggerFactory.getLogger(getClass());
    private final String LOG_PREFIX = "Exporter : ";
    private String dirName = "java-mysql-exporter-temp";
    private String sqlFileName = "";
    private String zipFileName = "";
    private boolean sendViaEmail;
    private StringBuilder report;
    private Map<String, String> properties;
    private File generatedZipFile;
    private boolean withFileAttached;
    private MysqlBaseService mysqlBaseService;
    private boolean toBeZipped;
    private String path;
    private String reportFileName;
    @Autowired
    EmailService emailService;

    public String getReportFileName() {
        return reportFileName;
    }

    @Autowired
    public MysqlExportService(MysqlBaseService mysqlBaseService) {
        this.mysqlBaseService = mysqlBaseService;
    }


    MysqlExportService setTmpDir(File directory) {
        if (this.properties == null) this.properties = new HashMap<>();
        this.properties.put(TEMP_DIR, handleNullValue(directory.getAbsolutePath()));
        return this;
    }

    public String asString() {
        return sql;
    }

    public File asZip() throws IOException {
        if (reportFileName != null) {
            FileOutputStream outputStream = new FileOutputStream(path + SLASH + reportFileName);
            outputStream.write(report.append("Export finished successfully\n").toString().getBytes());
            outputStream.close();
            clearTempDir(true);
        }
        return generatedZipFile;
    }

    public String report() {
        return this.report.toString();
    }

    MysqlExportService sendViaEmail(boolean sendViaEmail) {
        this.sendViaEmail = sendViaEmail;
        return this;
    }


    MysqlExportService dropExistingTables(boolean value) {
        if (this.properties == null) this.properties = new HashMap<>();
        this.properties.put(DROP_TABLES, String.valueOf(value));
        return this;
    }

    MysqlExportService deleteExistingData(boolean value) {
        if (this.properties == null) this.properties = new HashMap<>();
        this.properties.put(DELETE_EXISTING_DATA, String.valueOf(value));
        return this;
    }

    MysqlExportService addIfNotExists(boolean value) {
        if (this.properties == null) this.properties = new HashMap<>();
        this.properties.put(ADD_IF_NOT_EXISTS, String.valueOf(value));
        return this;
    }

    MysqlExportService setDBProperties(String url, String user, String password, String driverClassName) {
        if (this.properties == null) this.properties = new HashMap<>();
        this.properties.put(DB_URL, handleNullValue(url));
        this.properties.put(DB_PASSWORD, handleNullValue(password));
        this.properties.put(DB_USERNAME, handleNullValue(user));
        this.properties.put(JDBC_DRIVER_NAME, handleNullValue(driverClassName));
        return this;
    }

    private String handleNullValue(String value) {
        return value == null ? "" : value;
    }

    private String getTableInsertStatement(String table) throws SQLException {

        StringBuilder sql = new StringBuilder();
        ResultSet rs;
        boolean addIfNotExists = Boolean.parseBoolean(properties.containsKey(ADD_IF_NOT_EXISTS) ? properties.get(ADD_IF_NOT_EXISTS) : "true");
        boolean dropTable = Boolean.parseBoolean(properties.containsKey(DROP_TABLES) ? properties.get(DROP_TABLES) : "false");

        if (table != null && !table.isEmpty()) {
            rs = stmt.executeQuery("SHOW CREATE TABLE `" + databaseName + "`.`" + table + "`;");
            while (rs.next()) {
                String createTableQuery = rs.getString(1);
                String query = rs.getString(2);
                sql.append("\n\n--");
                sql.append(NEW_LINE).append(SQL_START_PATTERN).append("  table dump : ").append(createTableQuery);
                sql.append("\n--\n\n");

                if (addIfNotExists) {
                    query = query.trim().replace("CREATE TABLE", "CREATE TABLE IF NOT EXISTS ");
                }

                if (dropTable) {
                    sql.append("DROP TABLE IF EXISTS `").append(databaseName).append("`.`").append(table).append("`;\n");
                }
                sql.append(query).append(";\n\n");
            }
        }

        sql.append("\n\n--");
        sql.append(NEW_LINE).append(SQL_END_PATTERN).append("  table dump : ").append(table);
        sql.append("\n--\n\n");

        return sql.toString();
    }

    private String getDataInsertStatement(String table) throws SQLException {

        StringBuilder sql = new StringBuilder();

        ResultSet rs = stmt.executeQuery("SELECT * FROM `" + databaseName + "`.`" + table + "`;");
        rs.last();
        int rowCount = rs.getRow();

        //there are no records just return empty string
        if (rowCount <= 0) {
            return sql.toString();
        }

        sql.append("\n--").append("\n-- Inserts of ").append(table).append("\n--\n\n");

        //temporarily disable foreign key constraint
        sql.append("\n/*!40000 ALTER TABLE `").append(table).append("` DISABLE KEYS */;\n");

        boolean deleteExistingData = Boolean.parseBoolean(properties.containsKey(DELETE_EXISTING_DATA) ? properties.get(DELETE_EXISTING_DATA) : "false");

        if (deleteExistingData) {
            sql.append(mysqlBaseService.emptyTableSQL(databaseName, table));
        }

        sql.append("\n--\n")
                .append(SQL_START_PATTERN).append(" table insert : ").append(table)
                .append("\n--\n");

        sql.append("INSERT INTO `").append(table).append("`(");

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 0; i < columnCount; i++) {
            sql.append("`")
                    .append(metaData.getColumnName(i + 1))
                    .append("`, ");
        }

        //remove the last whitespace and comma
        sql.deleteCharAt(sql.length() - 1).deleteCharAt(sql.length() - 1).append(") VALUES \n");

        //build the values
        rs.beforeFirst();
        while (rs.next()) {
            sql.append("(");
            for (int i = 0; i < columnCount; i++) {

                int columnType = metaData.getColumnType(i + 1);
                int columnIndex = i + 1;

                if (columnType == Types.INTEGER || columnType == Types.TINYINT || columnType == Types.BIT) {
                    sql.append(rs.getInt(columnIndex)).append(", ");
                } else {
                    String val = rs.getString(columnIndex) != null ? rs.getString(columnIndex) : "";
                    val = val.replace("'", "\\'");
                    sql.append("'").append(val).append("', ");
                }
            }

            //now that we're done with a row

            //let's remove the last whitespace and comma
            sql.deleteCharAt(sql.length() - 1).deleteCharAt(sql.length() - 1);

            if (rs.isLast()) {
                sql.append(")");
            } else {
                sql.append("),\n");
            }
        }

        //now that we are done processing the entire row
        //let's add the terminator
        sql.append(";");

        sql.append("\n--\n")
                .append(SQL_END_PATTERN).append(" table insert : ").append(table)
                .append("\n--\n");

        //enable FK constraint
        sql.append("\n/*!40000 ALTER TABLE `").append(table).append("` ENABLE KEYS */;\n");

        return sql.toString();
    }

    private String exportToSql() throws SQLException {

        StringBuilder sql = new StringBuilder();
        sql.append("--");
        sql.append("\n-- Generated by java-mysql-exporter __ author EL AOUD JAOUAD");
        sql.append("\n-- Date: ").append(new SimpleDateFormat("d-M-Y H:m:s").format(new Date()));
        sql.append("\n--");

        //these declarations are extracted from HeidiSQL
        sql.append("\n\n/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;")
                .append("\n/*!40101 SET NAMES utf8 */;")
                .append("\n/*!50503 SET NAMES utf8mb4 */;")
                .append("\n/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;")
                .append("\n/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;");


        //get the tables
        List<String> tables = mysqlBaseService.tablesFrom(databaseName, stmt);

        //get the table insert statement for each table
        tables.forEach(s -> {
            try {
                sql.append(getTableInsertStatement(s.trim()));
                sql.append(getDataInsertStatement(s.trim()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        sql.append("\n/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;")
                .append("\n/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;")
                .append("\n/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;");

        return sql.toString();
    }

    public MysqlExportService export() throws IOException, SQLException, ClassNotFoundException, FileManagementException {

        report = new StringBuilder();
        addReportLine("Export started...");
        //check if properties is set or not
        if (!Validator.validDBProperties(this.properties)) {
            throw new InvalidDBConnectionParamsException("Invalid config properties: The config properties is missing important parameters: DB_NAME, DB_USERNAME and DB_PASSWORD");
        }

        //connect to the database
        dbUrl = properties.get(DB_URL);
        if (dbUrl.contains(INTERROGATION_POINT) && dbUrl.contains(SLASH)) {
            databaseName = dbUrl.substring(dbUrl.lastIndexOf(SLASH) + 1, dbUrl.lastIndexOf(INTERROGATION_POINT));
        } else if (!dbUrl.contains(INTERROGATION_POINT)) {
            databaseName = dbUrl.substring(dbUrl.lastIndexOf(SLASH) + 1);
        } else throw new InvalidDBURLException("Invalid db url : " + dbUrl);
        reportFileName = new SimpleDateFormat(DATE_FORMAT).format(new Date()) + UNDERSCORE + databaseName + "_database_report.txt";
        String driverName = properties.get(JDBC_DRIVER_NAME);

        Connection connection = null;

        String username = properties.get(DB_USERNAME);
        String password = properties.get(DB_PASSWORD);
        if (!dbUrl.isEmpty()) {
            this.report.append("database name extracted from connection string: " + databaseName).append(NEW_LINE);
            connection = mysqlBaseService.connectWithURL(username, password, dbUrl, driverName);
        }
        if (connection == null) throw new FileManagementException("Could'nt open connection to DB.");

        addReportLine("Established connection to db...");
        stmt = connection.createStatement();

        //generate the final SQL
        sql = exportToSql();
        addReportLine("generated sql string from db content.");
        //create a temp dir


        if (mysqlBaseService.disconnect(connection)) {
            this.addReportLine("Connection to DB Closed.");
        }
        if (toBeZipped) {
            addReportLine("zipping backup file started...");
            zipExportedSQL();
            addReportLine("zipping backup file ended...");
        }
        addReportLine("finished export...");

        return this;

    }

    private void zipExportedSQL() throws IOException {
        dirName = properties.getOrDefault(TEMP_DIR, dirName);
        File file = new File(dirName);
        if (!file.exists()) {
            boolean res = file.mkdir();
            if (!res) {
                throw new IOException(LOG_PREFIX + ": Unable to create temp dir: " + file.getAbsolutePath());
            }
        }
        clearTempDir(false);
        //write the sql file out
        File sqlFolder = new File(dirName + "/sql");
        boolean folderExists = sqlFolder.exists();
        if (!folderExists)
            if (sqlFolder.mkdir()) {
                folderExists = true;
            }

        if (folderExists) {
            sqlFileName = new SimpleDateFormat(DATE_FORMAT).format(new Date()) + UNDERSCORE + databaseName + "_database_dump.sql";
            FileOutputStream outputStream = new FileOutputStream(sqlFolder + SLASH + sqlFileName);
            outputStream.write(sql.getBytes());
            outputStream.close();

            //zip the file
            zipFileName = path + SLASH + sqlFileName.replace(SQL, com.filemanagement.app.utils.Constants.ZIP);
            generatedZipFile = new File(zipFileName);
            addReportLine("started zipping...");
            ZipUtil.pack(sqlFolder, generatedZipFile);
            addReportLine("finished zipping...");
            if (sendViaEmail) {
                addReportLine("started sending email with attachment to ..." + properties.getOrDefault(EMAIL_TO, ""));
                sendBackUpFile();
                addReportLine("finished sending email with attachment to ..." + properties.getOrDefault(EMAIL_TO, ""));
            }
            addReportLine("started sending notification email to ..." + properties.getOrDefault(EMAIL_TO, ""));
            notifyUser();
            addReportLine("finished sending notification email to ..." + properties.getOrDefault(EMAIL_TO, ""));


        }
    }

    private void sendBackUpFile() {
        String messages = "Please find attached database backup of " + databaseName;
        try {
            emailService.send("Your backup of " + databaseName + " is ready.", messages + EMAIL_NEW_LINE + "Backup path : " + new File(zipFileName).getAbsolutePath(), new File(zipFileName));
        } catch (MessagingException | IOException e) {
            addReportLine("We've encountered a problem while sending email. error : " + e.getMessage());
        }
    }

    private void notifyUser() {

        try {
            emailService.send("Your backup of " + databaseName + " is ready.", "Your backup is ready at : " + new File(zipFileName).getAbsolutePath());
        } catch (MessagingException | IOException e) {
            addReportLine("We've encountered a problem while sending email. error : " + e.getMessage());
        }

    }

    private void addReportLine(String msg) {
        this.report.append(LOG_PREFIX + msg).append(NEW_LINE);
    }

    public void clearTempDir(boolean log) throws IOException {

        //delete the temp sql file
        File dir = new File(dirName);
        if (dir.exists()) {
            FileUtils.cleanDirectory(dir);
            if (log) addReportLine(" " + dir.getAbsolutePath() + " deleted successfully.");
        } else {
            if (log) addReportLine(dir.getAbsolutePath() + " DOES NOT EXIST while clearing Temp Files");
        }
        if (log) addReportLine(" generated temp files cleared successfully");
    }


    MysqlExportService sendWithFileAttached(boolean withFileAttached) {
        this.withFileAttached = withFileAttached;
        return this;
    }

    public MysqlExportService zipIt(boolean value) {
        this.toBeZipped = value;
        return this;
    }

    public MysqlExportService backupPath(String path) {
        this.path = path;
        return this;
    }
}
