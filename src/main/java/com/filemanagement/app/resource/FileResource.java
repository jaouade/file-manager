package com.filemanagement.app.resource;

import com.filemanagement.app.models.Directory;
import com.filemanagement.app.resource.models.ListDir;
import com.filemanagement.app.utils.DateUtils;
import com.filemanagement.app.utils.FileUtils;
import com.filemanagement.app.utils.ZipUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static com.filemanagement.app.utils.Constants.SLASH;
import static com.filemanagement.app.utils.Constants.ZIP;
import static com.filemanagement.app.utils.FileUtils.*;
import static com.filemanagement.app.utils.PathUtils.decodePath;
import static com.filemanagement.app.utils.PathUtils.encodePath;

@RestController
@RequestMapping("/api/")
public class FileResource {
    @Autowired
    ServletContext servletContext;

    @GetMapping("open/dir/")
    public ResponseEntity listDir(@RequestParam String path) throws UnsupportedEncodingException {
        String decodedPath = decodePath(path);
        File directory = new File(decodedPath);
        if (directory.exists()) {
            if (directory.isDirectory()) {
                return ResponseEntity.ok().body(ListDir.builder().current(new Directory("/api/zip/dir/?path=" + encodePath(directory)
                        , "/api/rename/dir/?path=" + encodePath(directory)
                        , "/api/open/dir/?path=" + encodePath(directory)
                        , "/api/delete/dir/?path=" + encodePath(directory)
                        , directory.getName()
                ).setModifiedAt(DateUtils.getFormattedDate(directory.lastModified()))
                        .setDownloadPath("/api/download/dir/?path=" + encodePath(directory))).files(filesOf(directory, "/api")).directories(foldersOf(directory, "/api")).build());
            } else {
                return ResponseEntity.badRequest().body("The root path is not a dir.");
            }
        } else {
            return ResponseEntity.badRequest().body("The root path does not exist.");

        }
    }

    @GetMapping("breadcrumb/dir/")
    public ResponseEntity breadCrumb(@RequestParam String path) throws UnsupportedEncodingException {
        String decodedPath = decodePath(path);
        File directory = new File(decodedPath);
        if (directory.exists()) {
            if (directory.isDirectory()) {

                return ResponseEntity.ok().body(FileUtils.directoryAsBreadCrumb(directory, "/api"));
            } else {
                return ResponseEntity.badRequest().body("The root path is not a dir.");
            }
        } else {
            return ResponseEntity.badRequest().body("The root path does not exist.");

        }
    }

    @GetMapping("delete/dir/")
    public ResponseEntity delete(@RequestParam String path) throws IOException {

        File directory = new File(decodePath(path));

        if (directory.isDirectory()) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(directory);
                return ResponseEntity.ok().build();
            } catch (UnsupportedEncodingException e) {
                return ResponseEntity.badRequest().body("Couldn't delete directory.");
            }

        } else {
            if (directory.delete())
                return ResponseEntity.ok().build();
            else return ResponseEntity.badRequest().body("Couldn't delete file.");

        }
    }

    @PostMapping("upload/file/")
    public ResponseEntity upload(@RequestParam("file") MultipartFile file, @RequestParam String path) throws UnsupportedEncodingException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid uploaded file");

        }
        try {
            file.transferTo(new File(decodePath(path) + SLASH + file.getOriginalFilename()));
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Could'nt upload file.");
        }

    }

    @PostMapping("create/dir/")
    public ResponseEntity create(@RequestParam String path, @RequestParam String dirName) {
        if (dirName == null || dirName.isEmpty()) {
            return ResponseEntity.badRequest().body("Name of directory can't be empty");

        }
        try {
            File file = new File(decodePath(path) + SLASH + dirName);
            file.mkdirs();
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Couldn't create directory.");
        }

    }

    @PostMapping("create/file/")
    public ResponseEntity createFile(@RequestParam String path, @RequestParam String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return ResponseEntity.badRequest().body("invalid file name");

        }
        try {
            File file = new File(decodePath(path) + SLASH + fileName);
            file.createNewFile();
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Couldn't create file " + fileName);
        }

    }

    @PostMapping("rename/file/")
    public ResponseEntity renameFile(@RequestParam String path, @RequestParam String name) throws UnsupportedEncodingException {
        File file = new File(decodePath(path));

        if (name == null || name.isEmpty()) {

            return ResponseEntity.badRequest().body("invalid file name : " + name);

        }
        if (name.equals(file.getName())) return ResponseEntity.ok().build();
        try {
            org.apache.commons.io.FileUtils.moveFile(
                    org.apache.commons.io.FileUtils.getFile(decodePath(path)),
                    org.apache.commons.io.FileUtils.getFile(file.getParent() + SLASH + name));
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("We could'nt change file name to " + name);
        }

    }

    @PostMapping("rename/dir/")
    public ResponseEntity renameDir(RedirectAttributes redirectAttributes, @RequestParam String path, @RequestParam String name) throws UnsupportedEncodingException {
        File file = new File(decodePath(path));
        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest().body("invalid dir name : " + name);

        }
        if (name.equals(file.getName()))
            return ResponseEntity.ok().build();

        try {
            org.apache.commons.io.FileUtils.moveDirectory(
                    org.apache.commons.io.FileUtils.getFile(decodePath(path)),
                    org.apache.commons.io.FileUtils.getFile(file.getParent() + SLASH + name));
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(" We could'nt change dir name to " + name);
        }

    }

    @GetMapping("/download/dir/")
    @ResponseBody
    public ResponseEntity<ByteArrayResource> download(@RequestParam String path) throws IOException {

        File file = new File(decodePath(path));

        File zipFile = new File(FilenameUtils.getBaseName(file.getName()) + ZIP);
        zipFile.createNewFile();
        ZipUtils.compressDirectory(file, zipFile);
        return downloadFile(zipFile, servletContext);
    }


    @GetMapping("/download/file/")
    @ResponseBody
    public ResponseEntity<ByteArrayResource> downloadDir(@RequestParam String path) throws IOException {
        File toDownload = new File(decodePath(path));
        return downloadFile(toDownload, servletContext);
    }

    @GetMapping("unzip/file/")
    public ResponseEntity unzip(@RequestParam String path) {
        File file;
        try {
            file = new File(decodePath(path));
            ZipUtils.extractArchive(new File(file.getParent() + SLASH + FilenameUtils.getBaseName(file.getName())), file);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Couldn't archive directory.");
        }

    }

    @GetMapping("zip/dir/")
    public ResponseEntity<Object> zip(@RequestParam String path) {
        File file;
        try {
            file = new File(decodePath(path));
            ZipUtils.compressDirectory(file, new File(file.getParent() + SLASH + FilenameUtils.getBaseName(file.getName()) + ZIP));
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Couldn't archive directory");
        }
    }

    @GetMapping("edit/file/")
    public ResponseEntity edit(@RequestParam String path) {

        try {
            String decodedPath = decodePath(path);
            File file = new File(decodedPath);
            String content = org.apache.commons.io.FileUtils.readFileToString(file);
            return ResponseEntity.ok().body(com.filemanagement.app.models.File.builder().content(content).build());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("An error occurred while reading file");
        }
    }

    @PostMapping("edit/file/")
    public ResponseEntity saveEdit(RedirectAttributes redirectAttributes, @ModelAttribute com.filemanagement.app.models.File file, @RequestParam String path) throws UnsupportedEncodingException {
        File fileToSave = new File(URLDecoder.decode(path, StandardCharsets.UTF_8.toString()));

        try {
            org.apache.commons.io.FileUtils.writeStringToFile(
                    fileToSave,
                    file.getContent(), false);
            return ResponseEntity.ok().body("File saved.");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("File not saved.");
        }

    }

}


