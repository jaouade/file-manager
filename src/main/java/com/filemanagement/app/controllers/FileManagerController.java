package com.filemanagement.app.controllers;

import com.filemanagement.app.models.Directory;
import com.filemanagement.app.utils.DateUtils;
import com.filemanagement.app.utils.ErrorHandler;
import com.filemanagement.app.utils.MediaTypeUtils;
import com.filemanagement.app.utils.ZipUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.filemanagement.app.utils.Constants.SLASH;
import static com.filemanagement.app.utils.Constants.ZIP;
import static java.net.URLEncoder.encode;

@Controller

public class FileManagerController {
    private final ServletContext servletContext;

    public FileManagerController(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @GetMapping("/download/dir/")
    @ResponseBody
    public ResponseEntity<ByteArrayResource> downloadFile(@RequestParam String path) throws IOException {

        File file = new File(decodePath(path));

        File zipFile = new File(FilenameUtils.getBaseName(file.getName()) + ZIP);
        zipFile.createNewFile();
        ZipUtils.compressDirectory(file, zipFile);
        return downloadFile(zipFile);
    }

    private ResponseEntity<ByteArrayResource> downloadFile(File fileToBeDownloaded) throws IOException {
        byte[] data = Files.readAllBytes(fileToBeDownloaded.toPath());
        ByteArrayResource resource = new ByteArrayResource(data);
        MediaType mediaType = MediaTypeUtils.getMediaTypeForFileName(this.servletContext, fileToBeDownloaded.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileToBeDownloaded.getName())
                .contentType(mediaType)
                .contentLength(data.length)
                .body(resource);
    }

    @GetMapping("/download/file/")
    @ResponseBody
    public ResponseEntity<ByteArrayResource> downloadDir(@RequestParam String path) throws IOException {
        File toDownload = new File(decodePath(path));
        return downloadFile(toDownload);
    }

    @PostMapping("edit/file/")
    public String saveEdit(RedirectAttributes redirectAttributes,@ModelAttribute com.filemanagement.app.models.File file, @RequestParam String path) throws UnsupportedEncodingException {
        File fileToSave = new File(URLDecoder.decode(path, StandardCharsets.UTF_8.toString()));

        try {
            FileUtils.writeStringToFile(
                    fileToSave,
                    file.getContent(),false);
            redirectAttributes.addFlashAttribute("success","File saved.");
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error","File not saved.");
        }
        return "redirect:/edit/file/?path=" + URLEncoder.encode(URLDecoder.decode(path, StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString());
    }
    @GetMapping("unzip/file/")
    public String unzip(@RequestParam String path) throws IOException {
        String decodedPath = decodePath(path);
        File file = new File(decodedPath);
        ZipUtils.extractArchive(new File(file.getParent()+SLASH+FilenameUtils.getBaseName(file.getName())),file);
        return "redirect:/open/dir/?path=" + URLEncoder.encode(URLDecoder.decode(file.getParent(), StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString());
    }

    private String decodePath(@RequestParam String path) throws UnsupportedEncodingException {
        return URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
    }

    @GetMapping("zip/dir/")
    public String zip(@RequestParam String path) throws IOException {
        String decodedPath = decodePath(path);
        File file = new File(decodedPath);
        ZipUtils.compressDirectory(file, new File(file.getParent() + SLASH + FilenameUtils.getBaseName(file.getName()) + ZIP));
        return "redirect:/open/dir/?path=" + URLEncoder.encode(URLDecoder.decode(file.getParent(), StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString());
    }
    @GetMapping("edit/file/")
    public String edit(Model model, @RequestParam String path) throws IOException {
        String decodedPath = decodePath(path);
        File file = new File(decodedPath);
        model.addAttribute("file", com.filemanagement.app.models.File.builder().content(FileUtils.readFileToString(file)).build());
        try {
            model.addAttribute("savePath","/edit/file/?path="+URLEncoder.encode(decodedPath,StandardCharsets.UTF_8.toString()));
            model.addAttribute("name",file.getName());
            model.addAttribute("backPath","/open/dir/?path=" + URLEncoder.encode(URLDecoder.decode(file.getParent(), StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  "edit";
    }
    @PostMapping("upload/file/")
    public String upload(RedirectAttributes redirectAttributes, @RequestParam("file") MultipartFile file, @RequestParam String path) throws UnsupportedEncodingException {
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Invalid uploaded file");
            return "redirect:/open/dir/?path=" + URLEncoder.encode(URLDecoder.decode(path, StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString());

        }
        String decodedPath;
        try {

            decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
            file.transferTo(new File(decodedPath + SLASH + file.getOriginalFilename()));
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "We could'nt upload file " + file.getName());
        }
        return "redirect:/open/dir/?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.toString());

    }

    @PostMapping("create/dir/")
    public String create(RedirectAttributes redirectAttributes, @RequestParam String path, @RequestParam String dirName) throws UnsupportedEncodingException {
        if (dirName == null || dirName.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "invalid dir name : " + dirName);
            return "redirect:/open/dir/?path=" + URLEncoder.encode(URLDecoder.decode(path, StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString());

        }
        String decodedPath;
        try {

            decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
            File file = new File(decodedPath + SLASH + dirName);
            file.mkdirs();
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "We could'nt create dir " + dirName);
        }
        return "redirect:/open/dir/?path=" + URLEncoder.encode(path + SLASH + dirName, StandardCharsets.UTF_8.toString());

    }

    @PostMapping("create/file/")
    public String createFile(RedirectAttributes redirectAttributes, @RequestParam String path, @RequestParam String fileName) throws UnsupportedEncodingException {
        if (fileName == null || fileName.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "invalid file name : " + fileName);
            return "redirect:/open/dir/?path=" + URLEncoder.encode(URLDecoder.decode(path, StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString());

        }
        String decodedPath;
        try {

            decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
            File file = new File(decodedPath + SLASH + fileName);
            file.createNewFile();
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "We could'nt create file " + fileName);
        }
        return "redirect:/open/dir/?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.toString());

    }

    @PostMapping("rename/file/")
    public String renameFile(RedirectAttributes redirectAttributes, @RequestParam String path, @RequestParam String name) throws UnsupportedEncodingException {
        String decodedPath;
        decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
        File file = new File(decodedPath);

        if (name == null || name.isEmpty()) {

            redirectAttributes.addFlashAttribute("error", "invalid file name : " + name);
            return "redirect:/open/dir/?path=" + URLEncoder.encode(file.getParent(), StandardCharsets.UTF_8.toString());

        }
        if (name.equals(file.getName())) return "redirect:/open/dir/?path=" + URLEncoder.encode(file.getParent(), StandardCharsets.UTF_8.toString());
        try {
            FileUtils.moveFile(
                    FileUtils.getFile(decodedPath),
                    FileUtils.getFile(file.getParent() + SLASH + name));
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "We could'nt change file name to " + name);
        }
        return "redirect:/open/dir/?path=" + URLEncoder.encode(file.getParent(), StandardCharsets.UTF_8.toString());

    }

    @PostMapping("rename/dir/")
    public String renameDir(RedirectAttributes redirectAttributes, @RequestParam String path, @RequestParam String name) throws UnsupportedEncodingException {
        String decodedPath;
        decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
        File file = new File(decodedPath);
        if (name == null || name.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "invalid dir name : " + name);
            return "redirect:/open/dir/?path=" + URLEncoder.encode(file.getParent(), StandardCharsets.UTF_8.toString());

        }
        if (name.equals(file.getName()))
            return "redirect:/open/dir/?path=" + URLEncoder.encode(file.getParent(), StandardCharsets.UTF_8.toString());


        try {
            FileUtils.moveDirectory(
                    FileUtils.getFile(decodedPath),
                    FileUtils.getFile(file.getParent() + SLASH + name));
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "We could'nt change dir name to " + name);
        }
        return "redirect:/open/dir/?path=" + URLEncoder.encode(file.getParent(), StandardCharsets.UTF_8.toString());

    }

    public static String makeUrl(HttpServletRequest request) {
        return request.getRequestURL().toString() + "?" + request.getQueryString();
    }

    @GetMapping("/confirmed/delete/dir/")
    public String delete(RedirectAttributes redirectAttributes, @RequestParam String path) {

        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
            File directory = new File(decodedPath);
            File parent = directory.getParentFile();
            if (parent != null) {
                if (directory.isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(directory);
                        return "redirect:/open/dir/?path=" + encodePath(parent);
                    } catch (IOException ex) {
                        redirectAttributes.addFlashAttribute("error", "We could'nt delete folder " + directory.getName());
                        return "redirect:/open/dir/?path=" + encodePath(directory);

                    }
                } else {
                    if (directory.delete())
                        return "redirect:/open/dir/?path=" + encodePath(parent);
                    else {
                        redirectAttributes.addFlashAttribute("error", "We could'nt delete folder " + directory.getName());
                        return "redirect:/open/dir/?path=" + encodePath(directory);

                    }
                }
            } else return "redirect:/";
        } catch (UnsupportedEncodingException e) {
            return "redirect:/";
        }

    }

    @GetMapping("delete/dir/")
    public String delete(Model model, @RequestParam String path, HttpServletRequest request) {
        try {
            model.addAttribute("next", "/confirmed/delete/dir/?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.toString()));
            model.addAttribute("previous", request.getHeader("referer"));
            model.addAttribute("file", new File(decodePath(path)).getName());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "confirm";
    }

    @GetMapping("open/dir/")
    public String open(Model model, RedirectAttributes redirectAttributes, @RequestParam String path) {
        try {
            String decodedPath = decodePath(path);
            File directory = new File(decodedPath);
            if (directory.exists()) {

                if (directory.isDirectory()) {

                    Function<File, Directory> fileDirectoryMapper = file -> {
                        try {
                            return new Directory("/zip/dir/?path="+ encodePath(file)
                                    ,"/rename/dir/?path=" + encodePath(file)
                                    , "/open/dir/?path=" + encodePath(file)
                                    , "/delete/dir/?path=" + encodePath(file)
                                    , file.getName()
                            ).setModifiedAt(DateUtils.getFormattedDate(file.lastModified()))
                                    .setDownloadPath("/download/dir/?path=" + encodePath(file));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        return null;
                    };
                    Function<File, com.filemanagement.app.models.File> fileFileMapper = file -> {
                        try {
                            return new com.filemanagement.app.models.File(
                                    FilenameUtils.getExtension(file.getName()).equals("zip"),
                                    "/unzip/file/?path=" + encodePath(file),
                                    "/edit/file/?path=" + encodePath(file),file.getName(),
                                    "/open/dir/?path=" + encodePath(file),
                                    "/delete/dir/?path=" + encodePath(file),
                                    "/rename/file/?path=" + encodePath(file))
                                    .setModifiedAt(DateUtils.getFormattedDate(file.lastModified()))
                                    .setDownloadPath("/download/file/?path=" + encodePath(file));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        return null;
                    };
                    List<Directory> directories = Arrays.stream(Objects.requireNonNull(directory.listFiles()))
                            .filter(File::isDirectory)
                            .map(fileDirectoryMapper).collect(Collectors.toList());
                    List<com.filemanagement.app.models.File> files = Arrays.stream(Objects.requireNonNull(directory.listFiles()))
                            .filter(File::isFile)
                            .map(fileFileMapper).collect(Collectors.toList());
                    model.addAttribute("dirs", directories.size() > 0 ? directories : null);
                    model.addAttribute("files", files.size() > 0 ? files : null);
                    model.addAttribute("deletePath", "/delete/dir/?path=" + encodePath(directory));
                    model.addAttribute("uploadPath", "/upload/file/?path=" + encodePath(directory));
                    model.addAttribute("createPath", "/create/dir/?path=" + encodePath(directory));
                    model.addAttribute("createFilePath", "/create/file/?path=" + encodePath(directory));
                    model.addAttribute("breadcrumb", breadCrumbThis(directory));
                    return "dir";

                } else {
                    return ErrorHandler.error(redirectAttributes, "The path you gave us is not a directory, You kidding us.");

                }
            } else {
                return ErrorHandler.error(redirectAttributes, "The path you gave us is not a directory, You kidding us.");

            }
        } catch (UnsupportedEncodingException e) {
            return ErrorHandler.error(redirectAttributes, "The path you gave us is not well decoded, You kidding us.");
        }

    }

    private String encodePath(File file) throws UnsupportedEncodingException {
        return encode(file.getAbsolutePath(), StandardCharsets.UTF_8.toString());
    }

    private List<Directory> breadCrumbThis(File directory) throws UnsupportedEncodingException {
        File currentDir = directory;
        List<Directory> breadCrumb = new ArrayList<>();

        while (currentDir != null) {
            breadCrumb.add(new Directory("/zip/dir/?path="+ encodePath(currentDir),"", "/open/dir/?path=" + encodePath(currentDir)
                    , "/delete/dir/?path=" + encodePath(currentDir)
                    , currentDir.getName().isEmpty() ? "root" : currentDir.getName())
                    .setModifiedAt(DateUtils.getFormattedDate(currentDir.lastModified())).setDownloadPath("/download/dir/?path=" + encodePath(currentDir)));
            currentDir = currentDir.getParentFile();
        }
        Collections.reverse(breadCrumb);
        return breadCrumb;
    }
}
