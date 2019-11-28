package com.filemanagement.app.utils;

import com.filemanagement.app.models.Directory;
import com.filemanagement.app.models.File;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.filemanagement.app.utils.PathUtils.encodePath;

public class FileUtils {

    public static List<File> filesOf(java.io.File directory, String... prefix) {
        String api = getPrefix(prefix);
        Function<java.io.File, File> fileFileMapper = file -> {
            try {
                return new com.filemanagement.app.models.File(
                        FilenameUtils.getExtension(file.getName()).equals("zip"),
                        api + "/unzip/file/?path=" + encodePath(file),
                        api + "/edit/file/?path=" + encodePath(file), file.getName(),
                        api + "/open/dir/?path=" + encodePath(file),
                        api + "/delete/?path=" + encodePath(file),
                        api + "/rename/file/?path=" + encodePath(file))
                        .setModifiedAt(DateUtils.getFormattedDate(file.lastModified()))
                        .setDownloadPath(api + "/download/file/?path=" + encodePath(file));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return null;
        };

        return Arrays.stream(Objects.requireNonNull(directory.listFiles(java.io.File::isFile)))
                .map(fileFileMapper).collect(Collectors.toList());
    }

    public static List<Directory> foldersOf(java.io.File directory, String... prefix) {
        String api = getPrefix(prefix);
        Function<java.io.File, Directory> fileDirectoryMapper = file -> {
            try {
                return new Directory(api + "/zip/dir/?path=" + encodePath(file)
                        , api + "/rename/dir/?path=" + encodePath(file)
                        , api + "/open/dir/?path=" + encodePath(file)
                        , api + "/delete/?path=" + encodePath(file)
                        , file.getName()
                ).setModifiedAt(DateUtils.getFormattedDate(file.lastModified()))
                        .setDownloadPath(api + "/download/dir/?path=" + encodePath(file));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return null;
        };
        return Arrays.stream(Objects.requireNonNull(directory.listFiles(java.io.File::isDirectory)))
                .map(fileDirectoryMapper).collect(Collectors.toList());
    }

    public static List<Directory> directoryAsBreadCrumb(java.io.File directory, String... prefix) throws UnsupportedEncodingException {
        java.io.File currentDir = directory;
        List<Directory> breadCrumb = new ArrayList<>();
        String api = getPrefix(prefix);
        while (currentDir != null) {
            breadCrumb.add(new Directory(api + "/zip/dir/?path=" + encodePath(currentDir), "", api + "/open/dir/?path=" + encodePath(currentDir)
                    , api + "/delete/?path=" + encodePath(currentDir)
                    , currentDir.getName().isEmpty() ? "root" : currentDir.getName())
                    .setModifiedAt(DateUtils.getFormattedDate(currentDir.lastModified())).setDownloadPath(api + "/download/dir/?path=" + encodePath(currentDir)));
            currentDir = currentDir.getParentFile();
        }
        Collections.reverse(breadCrumb);
        return breadCrumb;
    }

    private static String getPrefix(String[] prefix) {
        if (prefix == null) return "";
        else if (prefix.length == 0) return "";
        else return prefix[0];
    }

    public static ResponseEntity<ByteArrayResource> downloadFile(java.io.File fileToBeDownloaded, ServletContext servletContext) throws IOException {
        byte[] data = Files.readAllBytes(fileToBeDownloaded.toPath());
        ByteArrayResource resource = new ByteArrayResource(data);
        MediaType mediaType = MediaTypeUtils.getMediaTypeForFileName(servletContext, fileToBeDownloaded.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileToBeDownloaded.getName())
                .contentType(mediaType)
                .contentLength(data.length)
                .body(resource);
    }
}
