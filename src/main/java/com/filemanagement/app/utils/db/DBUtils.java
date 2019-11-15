package com.filemanagement.app.utils.db;

import com.filemanagement.app.exception.FileManagementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
/**
 * @author Jaouad El Aoud
 */
@Component
public class DBUtils {
    private final Environment environment;
    private static final Logger LOGGER= LoggerFactory.getLogger(DBUtils.class);

    private final MysqlExportService mysqlExportService;

    @Autowired
    public DBUtils(Environment environment, MysqlExportService mysqlExportService) {
        this.environment = environment;
        this.mysqlExportService = mysqlExportService;
    }

    public MysqlExportService dump() {
        String host = environment.getProperty("mail.smtp.server");
        String port = environment.getProperty("mail.smtp.port");
        String username = environment.getProperty("mail.smtp.user");
        String passwordE = environment.getProperty("mail.smtp.password");
        String from = environment.getProperty("mail.smtp.from");
        String to = "jaouadelaoud@gmail.com";
        String url = environment.getProperty("spring.datasource.name");
        String user = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");
        String driverClassName = environment.getProperty("spring.datasource.driver-class-name");

        mysqlExportService
                .setEmailProperties(host, port, username, passwordE, from, to,"TEST")
                .sendViaEmail(true)
                .setDBProperties(url, user, password, driverClassName)
                .setTmpDir(new File("external"))
                .sendWithFileAttached(true)
                .keepZip(true)
                .dropExistingTables(true)
                .deleteExistingData(true)
                .addIfNotExists(true);
        try {
            mysqlExportService.export();
        } catch (IOException | SQLException | ClassNotFoundException | FileManagementException e) {
            LOGGER.error("Something went wrong during the dump, caused by : ",e);
        }
        return mysqlExportService;
    }
}
