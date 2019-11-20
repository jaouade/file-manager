package com.filemanagement.app.models;

import lombok.Data;
@Data
public class BackUpProperties {
    private String dbUrl;
    private String dbPassword;
    private String path;
    private String dbUser;
    private boolean dropTables;
    private boolean deleteExistingData;
    private boolean addIfNotExist;
    private boolean sendViaEmail;
}
