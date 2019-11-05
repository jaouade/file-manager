package com.filemanagement.app.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Builder
@AllArgsConstructor
public class File {
    private String name;
    private String content;
    private String path;
    private String deletePath;
    private String renamePath;
    private String editPath;
    private boolean archived;
    private String unzipPath;


    public File(boolean archived,String unzipPath,String editPath,String name, String path, String deletePath, String renamePath) {
        this.name = name;
        this.path = path;
        this.deletePath = deletePath;
        this.renamePath = renamePath;
        this.editPath = editPath;
        this.unzipPath = unzipPath;
        this.archived = archived;
    }

    public File() {
    }
}

