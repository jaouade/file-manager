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

    public String getModifiedAt() {
        return modifiedAt;
    }

    public Directory setModifiedAt(String modifiedAt) {
        this.modifiedAt = modifiedAt;
        return this;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Directory setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
        return this;
    }

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
