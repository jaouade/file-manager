package com.filemanagement.app.models;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Directory {
    private String path;
    private String deletePath;
    private String renamePath;
    private String zipPath;
    private String name;
    private String modifiedAt;
    private String createdAt;

    public Directory(String zipPath,String renamePath,String path,String deletePath, String name) {
        this.path = path;
        this.deletePath = deletePath;
        this.renamePath = renamePath;
        this.name = name;
        this.zipPath = zipPath;
    }

    public Directory() {
    }
}
