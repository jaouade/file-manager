package com.filemanagement.app.utils.db;

public class InvalidDBConnectionParams extends Exception {
    public InvalidDBConnectionParams(String message) {
        super(message);
    }
}
