package com.filemanagement.app.utils.db;


import com.filemanagement.app.exception.FileManagementException;
import com.filemanagement.app.exception.InvalidDBConnectionParamsException;
import com.filemanagement.app.exception.InvalidDBURLException;
import com.filemanagement.app.exception.InvalidEmailParamsException;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import static com.filemanagement.app.utils.db.Constants.*;

/**
 * @author Jaouad El Aoud
 */
@Component
@Data
@Builder
public class MysqlExportService {


    private Statement stmt;
    private String database;
    private String databaseCleanName;
    private String sql = "";
    private Logger logger = LoggerFactory.getLogger(getClass());
    private final String LOG_PREFIX = "java-mysql-exporter : ";
    private String dirName = "java-mysql-exporter-temp";
    private String sqlFileName = "";
    private String zipFileName = "";
    private boolean sendViaEmail;
    private StringBuilder report;
    private Properties properties;
    private File generatedZipFile;


    private boolean withFileAttached;
    @Autowired
    private MysqlBaseService mysqlBaseService;

    public MysqlExportService setEmailProperties(String host, String port, String username, String password, String from, String to, String msg) {
        if (this.properties == null) this.properties = new Properties();
        this.properties.setProperty(EMAIL_HOST, host);
        this.properties.setProperty(EMAIL_PORT, port);
        this.properties.setProperty(EMAIL_USERNAME, username);
        this.properties.setProperty(EMAIL_PASSWORD, password);
        this.properties.setProperty(EMAIL_FROM, from);
        this.properties.setProperty(EMAIL_TO, to);
        this.properties.setProperty(EMAIL_MESSAGE, msg);
        return this;
    }

    public MysqlExportService setTmpDir(File directory) {
        if (this.properties == null) this.properties = new Properties();
        this.properties.setProperty(TEMP_DIR, directory.getAbsolutePath());
        return this;
    }

    public String report() {
        return this.report.toString();
    }

    public MysqlExportService sendViaEmail(boolean sendViaEmail) {
        this.sendViaEmail = sendViaEmail;
        return this;
    }

    public MysqlExportService keepZip(boolean value) {
        if (this.properties == null) this.properties = new Properties();
        this.properties.setProperty(PRESERVE_GENERATED_ZIP, String.valueOf(value));
        return this;
    }

    public MysqlExportService dropExistingTables(boolean value) {
        if (this.properties == null) this.properties = new Properties();
        this.properties.setProperty(DROP_TABLES, String.valueOf(value));
        return this;
    }

    public MysqlExportService deleteExistingData(boolean value) {
        if (this.properties == null) this.properties = new Properties();
        this.properties.setProperty(DELETE_EXISTING_DATA, String.valueOf(value));
        return this;
    }

    public MysqlExportService addIfNotExists(boolean value) {
        if (this.properties == null) this.properties = new Properties();
        this.properties.setProperty(ADD_IF_NOT_EXISTS, String.valueOf(value));
        return this;
    }

    public MysqlExportService setDBProperties(String url, String user, String password, String driverClassName) {
        if (this.properties == null) this.properties = new Properties();
        this.properties.setProperty(DB_URL, url);
        this.properties.setProperty(DB_PASSWORD, password);
        this.properties.setProperty(DB_USERNAME, user);
        this.properties.setProperty(JDBC_DRIVER_NAME, driverClassName);
        return this;
    }

    private boolean validateProperties() {
        return properties != null &&
                properties.containsKey(DB_USERNAME) &&
                properties.containsKey(DB_PASSWORD) &&
                (properties.containsKey(DB_NAME) || properties.containsKey(JDBC_CONNECTION_STRING));
    }

    private boolean isEmailPropertiesValid() {
        return properties != null &&
                properties.containsKey(EMAIL_HOST) &&
                properties.containsKey(EMAIL_PORT) &&
                properties.containsKey(EMAIL_USERNAME) &&
                properties.containsKey(EMAIL_PASSWORD) &&
                properties.containsKey(EMAIL_FROM) &&
                properties.containsKey(EMAIL_TO);
    }

    private String getTableInsertStatement(String table) throws SQLException {

        StringBuilder sql = new StringBuilder();
        ResultSet rs;
        boolean addIfNotExists = Boolean.parseBoolean(properties.containsKey(ADD_IF_NOT_EXISTS) ? properties.getProperty(ADD_IF_NOT_EXISTS, "true") : "true");
        boolean dropTable = Boolean.parseBoolean(properties.containsKey(DROP_TABLES) ? properties.getProperty(DROP_TABLES, "false") : "false");

        if (table != null && !table.isEmpty()) {
            rs = stmt.executeQuery("SHOW CREATE TABLE `" + databaseCleanName + "`.`" + table + "`;");
            while (rs.next()) {
                String qtbl = rs.getString(1);
                String query = rs.getString(2);
                sql.append("\n\n--");
                sql.append(NEW_LINE).append(SQL_START_PATTERN).append("  table dump : ").append(qtbl);
                sql.append("\n--\n\n");

                if (addIfNotExists) {
                    query = query.trim().replace("CREATE TABLE", "CREATE TABLE IF NOT EXISTS ");
                }

                if (dropTable) {
                    sql.append("DROP TABLE IF EXISTS `").append(databaseCleanName).append("`.`").append(table).append("`;\n");
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

        ResultSet rs = stmt.executeQuery("SELECT * FROM `" + databaseCleanName + "`.`" + table + "`;");
        rs.last();
        int rowCount = rs.getRow();

        //there are no records just return empty string
        if (rowCount <= 0) {
            return sql.toString();
        }

        sql.append("\n--").append("\n-- Inserts of ").append(table).append("\n--\n\n");

        //temporarily disable foreign key constraint
        sql.append("\n/*!40000 ALTER TABLE `").append(table).append("` DISABLE KEYS */;\n");

        boolean deleteExistingData = Boolean.parseBoolean(properties.containsKey(DELETE_EXISTING_DATA) ? properties.getProperty(DELETE_EXISTING_DATA, "false") : "false");

        if (deleteExistingData) {
            sql.append(mysqlBaseService.getEmptyTableSQL(databaseCleanName, table));
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
        sql.append("\n-- Generated by java-mysql-exporter");
        sql.append("\n-- Date: ").append(new SimpleDateFormat("d-M-Y H:m:s").format(new Date()));
        sql.append("\n--");

        //these declarations are extracted from HeidiSQL
        sql.append("\n\n/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;")
                .append("\n/*!40101 SET NAMES utf8 */;")
                .append("\n/*!50503 SET NAMES utf8mb4 */;")
                .append("\n/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;")
                .append("\n/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;");


        //get the tables
        List<String> tables = mysqlBaseService.getAllTables(databaseCleanName, stmt);

        //get the table insert statement for each table
        for (String s : tables) {
            try {
                sql.append(getTableInsertStatement(s.trim()));
                sql.append(getDataInsertStatement(s.trim()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        sql.append("\n/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;")
                .append("\n/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;")
                .append("\n/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;");

        return sql.toString();
    }

    public void export() throws IOException, SQLException, ClassNotFoundException, FileManagementException {

        report = new StringBuilder();
        //check if properties is set or not
        if (!validateProperties()) {
            throw new InvalidDBConnectionParamsException("Invalid config properties: The config properties is missing important parameters: DB_NAME, DB_USERNAME and DB_PASSWORD");
        }

        //connect to the database
        database = properties.getProperty(DB_NAME);
        if (database.contains(INTERROGATION_POINT) && database.contains(SLASH))
            databaseCleanName = database.substring(database.lastIndexOf(SLASH) + 1, database.lastIndexOf(INTERROGATION_POINT));
        else if (!database.contains(INTERROGATION_POINT))
            databaseCleanName = database.substring(database.lastIndexOf(SLASH) + 1);
        else throw new InvalidDBURLException("Invalid db url : " + database);

        String jdbcURL = properties.getProperty(JDBC_CONNECTION_STRING, "");
        String driverName = properties.getProperty(JDBC_DRIVER_NAME, "");

        Connection connection;

        if (jdbcURL.isEmpty()) {
            connection = mysqlBaseService.connect(properties.getProperty(DB_USERNAME), properties.getProperty(DB_PASSWORD),
                    database, driverName);
        } else {
            database = jdbcURL.substring(jdbcURL.lastIndexOf(SLASH) + 1);
            this.report.append("database name extracted from connection string: " + database).append(NEW_LINE);
            connection = mysqlBaseService.connectWithURL(properties.getProperty(DB_USERNAME), properties.getProperty(DB_PASSWORD),
                    jdbcURL, driverName);
        }

        stmt = connection.createStatement();

        //generate the final SQL
        sql = exportToSql();

        //create a temp dir
        dirName = properties.getProperty(TEMP_DIR, dirName);
        File file = new File(dirName);
        if (!file.exists()) {
            boolean res = file.mkdir();
            if (!res) {
                throw new IOException(LOG_PREFIX + ": Unable to create temp dir: " + file.getAbsolutePath());
            }
        }

        //write the sql file out
        File sqlFolder = new File(dirName + "/sql");
        if (!sqlFolder.exists())
            sqlFolder.mkdir();
        sqlFileName = new SimpleDateFormat("d_M_Y_H_mm_ss").format(new Date()) + "_" + databaseCleanName + "_database_dump.sql";
        FileOutputStream outputStream = new FileOutputStream(sqlFolder + SLASH + sqlFileName);
        outputStream.write(sql.getBytes());
        outputStream.close();

        //zip the file
        zipFileName = dirName + SLASH + sqlFileName.replace(SQL, ZIP);
        generatedZipFile = new File(zipFileName);
        ZipUtil.pack(sqlFolder, generatedZipFile);
        if (sendViaEmail) {
            send();
        }

        //clear the generated temp files
        clearTempFiles(Boolean.parseBoolean(properties.getProperty(PRESERVE_GENERATED_ZIP, Boolean.FALSE.toString())));

    }

    private void send() throws InvalidEmailParamsException {
        if (isEmailPropertiesValid()) {
            String messages = properties.getProperty(EMAIL_MESSAGE, "Please find attached database backup of " + databaseCleanName);
            EmailService emailService = EmailService.builder()
                    .setHost(properties.getProperty(EMAIL_HOST))
                    .setPort(Integer.valueOf(properties.getProperty(EMAIL_PORT)))
                    .setToAddress(properties.getProperty(EMAIL_TO))
                    .setFromAddress(properties.getProperty(EMAIL_FROM))
                    .setUsername(properties.getProperty(EMAIL_USERNAME))
                    .setPassword(properties.getProperty(EMAIL_PASSWORD))
                    .setSubject(properties.getProperty(EMAIL_SUBJECT, sqlFileName.replace(SQL, "")));
            if (withFileAttached) {
                emailService.setMessage(messages)
                        .setAttachments(new File[]{new File(zipFileName)});
            } else {
                emailService.setMessage(messages + NEW_LINE + "Backup path : " + new File(zipFileName).getAbsolutePath());
            }
            boolean sent = emailService
                    .sendMail(withFileAttached);

            if (sent) {
                addReportLine("Backup file was sent to " + properties.getProperty(EMAIL_TO) + " successfully.");
            } else {
                addReportLine(" Unable to send zipped file as attachment to email. See log debug for more info");
            }
        } else {
            throw new InvalidEmailParamsException("the mailing properties are not valid.");
        }
    }

    private void addReportLine(String msg) {
        this.report.append(LOG_PREFIX + msg).append(NEW_LINE);
    }

    public void clearTempFiles(boolean preserveZipFile) {

        //delete the temp sql file
        File sqlFile = new File(dirName + SQL_DIR + sqlFileName);
        if (sqlFile.exists()) {
            boolean res = sqlFile.delete();
            addReportLine(" " + sqlFile.getAbsolutePath() + " deleted successfully? " + (res ? " TRUE " : " FALSE "));
        } else {
            addReportLine(sqlFile.getAbsolutePath() + " DOES NOT EXIST while clearing Temp Files");
        }

        File sqlFolder = new File(dirName + SQL_DIR);
        if (sqlFolder.exists()) {
            boolean res = sqlFolder.delete();
            addReportLine(" " + sqlFile.getAbsolutePath() + " deleted successfully? " + (res ? " TRUE " : " FALSE "));
        } else {
            addReportLine(sqlFolder.getAbsolutePath() + " DOES NOT EXIST while clearing Temp Files");
        }


        if (!preserveZipFile) {

            //delete the zipFile
            File zipFile = new File(zipFileName);
            if (zipFile.exists()) {
                boolean res = zipFile.delete();
                addReportLine(" " + sqlFile.getAbsolutePath() + " deleted successfully? " + (res ? " TRUE " : " FALSE "));
            } else {
                addReportLine(zipFile.getAbsolutePath() + " DOES NOT EXIST while clearing Temp Files");
            }

            //delete the temp folder
            File folder = new File(dirName);
            if (folder.exists()) {
                boolean res = folder.delete();
                addReportLine(" " + sqlFile.getAbsolutePath() + " deleted successfully? " + (res ? " TRUE " : " FALSE "));
            } else {
                addReportLine(folder.getAbsolutePath() + " DOES NOT EXIST while clearing Temp Files");
            }
        }

        addReportLine(" generated temp files cleared successfully");
    }


    public String getSql() {
        return sql;
    }

    public File getDumpFile() {
        if (generatedZipFile != null && generatedZipFile.exists()) {
            return generatedZipFile;
        }
        return null;
    }

    public MysqlExportService sendWithFileAttached(boolean withFileAttached) {
        this.withFileAttached = withFileAttached;
        return null;
    }
}
