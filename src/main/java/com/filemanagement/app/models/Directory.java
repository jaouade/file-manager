package com.filemanagement.app.models;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Directory {
    private String path;
    private String deletePath;
    private String renamePath;
    private String name;

    public Directory(String renamePath,String path,String deletePath, String name) {
        this.path = path;
        this.deletePath = deletePath;
        this.renamePath = renamePath;
        this.name = name;
    }

    public Directory() {
    }
}
