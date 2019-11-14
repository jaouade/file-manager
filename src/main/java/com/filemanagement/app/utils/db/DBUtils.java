package com.filemanagement.app.utils.db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

@Component
public class DBUtils {
    private final Environment environment;


    @Autowired
    private MysqlExportService mysqlExportService;

    public DBUtils(Environment environment) {
        this.environment = environment;
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
        } catch (IOException | SQLException | ClassNotFoundException | InvalidDBURLException | InvalidDBConnectionParamsException | InvalidEmailParamsException e) {
            e.printStackTrace();
        }
        return mysqlExportService;
    }
}
