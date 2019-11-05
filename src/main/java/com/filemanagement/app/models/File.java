package com.filemanagement.app.models;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class File {
    private String name;
    private String path;
    private String deletePath;
    private String renamePath;

    public File(String name, String path, String deletePath, String renamePath) {
        this.name = name;
        this.path = path;
        this.deletePath = deletePath;
        this.renamePath = renamePath;
    }
}
