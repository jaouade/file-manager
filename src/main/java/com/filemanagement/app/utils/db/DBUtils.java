package com.filemanagement.app.utils.db;

import com.filemanagement.app.models.BackUpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author Jaouad El Aoud
 */
@Component
public class DBUtils {
    private final Environment environment;
    private static final Logger LOGGER = LoggerFactory.getLogger(DBUtils.class);

    private final MysqlExportService mysqlExportService;

    @Autowired
    public DBUtils(Environment environment, MysqlExportService mysqlExportService) {
        this.environment = environment;
        this.mysqlExportService = mysqlExportService;
    }

    public MysqlExportService dump(BackUpProperties backUpProperties) {
        String url = backUpProperties.getDbUrl() == null || backUpProperties.getDbUrl().isEmpty()
                ? environment.getProperty("spring.datasource.url") : backUpProperties.getDbUrl();
        String user = backUpProperties.getDbUser() == null || backUpProperties.getDbUser().isEmpty()
                ? environment.getProperty("spring.datasource.username") : backUpProperties.getDbUser();
        String password = backUpProperties.getDbPassword() == null || backUpProperties.getDbPassword().isEmpty()
                ? environment.getProperty("spring.datasource.password") : backUpProperties.getDbPassword();
        String driverClassName = environment.getProperty("spring.datasource.driver-class-name");

        mysqlExportService
                .sendViaEmail(backUpProperties.isSendViaEmail())
                .setDBProperties(url, user, password, driverClassName)
                .setTmpDir(new File("external"))
                .backupPath(backUpProperties.getPath())
                .zipIt(true)
                .dropExistingTables(backUpProperties.isDropTables())
                .deleteExistingData(backUpProperties.isDeleteExistingData())
                .addIfNotExists(backUpProperties.isAddIfNotExist());
        return mysqlExportService;
    }
}
