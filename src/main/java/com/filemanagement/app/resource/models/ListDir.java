package com.filemanagement.app.resource.models;

import com.filemanagement.app.models.Directory;
import com.filemanagement.app.models.File;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ListDir {
    Directory current;
    List<File> files;
    List<Directory> directories;

}
