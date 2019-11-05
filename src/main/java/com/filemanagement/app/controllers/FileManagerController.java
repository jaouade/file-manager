package com.filemanagement.app.controllers;

import com.filemanagement.app.models.Directory;
import com.filemanagement.app.utils.ErrorHandler;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.net.URLEncoder.encode;

@Controller

public class FileManagerController {
    @PostMapping("upload/file/")
    public String upload(RedirectAttributes redirectAttributes, @RequestParam("file") MultipartFile file, @RequestParam String path) throws UnsupportedEncodingException {
        if (file==null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Invalid uploaded file");
            return "redirect:/open/dir/?path="+URLEncoder.encode(URLDecoder.decode(path,StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString());

        }
        String decodedPath;
        try {

            decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
            file.transferTo(new File(decodedPath+"/"+file.getOriginalFilename()));
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "We could'nt upload file " + file.getName());
        }
        return "redirect:/open/dir/?path="+URLEncoder.encode(path, StandardCharsets.UTF_8.toString());

    }
    @PostMapping("create/dir/")
    public String create(RedirectAttributes redirectAttributes, @RequestParam String path,@RequestParam String dirName) throws UnsupportedEncodingException {
        if (dirName==null || dirName.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "invalid dir name : " + dirName);
            return "redirect:/open/dir/?path="+URLEncoder.encode(URLDecoder.decode(path,StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString());

        }
        String decodedPath;
        try {

            decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
            File file = new File(decodedPath + "/" + dirName);
            file.mkdirs();
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "We could'nt create dir " + dirName);
        }
        return "redirect:/open/dir/?path="+URLEncoder.encode(path+"/"+dirName, StandardCharsets.UTF_8.toString());

    }
    @PostMapping("create/file/")
    public String createFile(RedirectAttributes redirectAttributes, @RequestParam String path,@RequestParam String fileName) throws UnsupportedEncodingException {
        if (fileName==null || fileName.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "invalid file name : " + fileName);
            return "redirect:/open/dir/?path="+URLEncoder.encode(URLDecoder.decode(path,StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString());

        }
        String decodedPath;
        try {

            decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
            File file = new File(decodedPath + "/" + fileName);
            file.createNewFile();
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "We could'nt create file " + fileName);
        }
        return "redirect:/open/dir/?path="+URLEncoder.encode(path, StandardCharsets.UTF_8.toString());

    }
    @PostMapping("rename/file/")
    public String renameFile(RedirectAttributes redirectAttributes, @RequestParam String path,@RequestParam String name) throws UnsupportedEncodingException {
        String decodedPath;
        decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
        File file = new File(decodedPath);
        if (name==null || name.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "invalid file name : " + name);
            return "redirect:/open/dir/?path="+URLEncoder.encode(file.getParent(), StandardCharsets.UTF_8.toString());

        }

        try {
            FileUtils.moveFile(
                    FileUtils.getFile(decodedPath),
                    FileUtils.getFile(file.getParent()+"/"+name));
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "We could'nt change file name to " + name);
        }
        return "redirect:/open/dir/?path="+URLEncoder.encode(file.getParent(), StandardCharsets.UTF_8.toString());

    }
    @PostMapping("rename/dir/")
    public String renameDir(RedirectAttributes redirectAttributes, @RequestParam String path,@RequestParam String name) throws UnsupportedEncodingException {
        String decodedPath;
        decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
        File file = new File(decodedPath);
        if (name==null || name.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "invalid dir name : " + name);
            return "redirect:/open/dir/?path="+URLEncoder.encode(file.getParent(), StandardCharsets.UTF_8.toString());

        }

        try {
            FileUtils.moveDirectory(
                    FileUtils.getFile(decodedPath),
                    FileUtils.getFile(file.getParent()+"/"+name));
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "We could'nt change dir name to " + name);
        }
        return "redirect:/open/dir/?path="+URLEncoder.encode(file.getParent(), StandardCharsets.UTF_8.toString());

    }

    @GetMapping("delete/dir/")
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
                        return "redirect:/open/dir/?path=" + encode(parent.getAbsolutePath(), StandardCharsets.UTF_8.toString());
                    } catch (IOException ex) {
                        redirectAttributes.addFlashAttribute("error", "We could'nt delete folder " + directory.getName());
                        return "redirect:/open/dir/?path=" + encode(directory.getAbsolutePath(), StandardCharsets.UTF_8.toString());

                    }
                }else {
                    if (directory.delete())
                        return "redirect:/open/dir/?path=" + encode(parent.getAbsolutePath(), StandardCharsets.UTF_8.toString());
                    else {
                        redirectAttributes.addFlashAttribute("error", "We could'nt delete folder " + directory.getName());
                        return "redirect:/open/dir/?path=" + encode(directory.getAbsolutePath(), StandardCharsets.UTF_8.toString());

                    }
                }
            } else return "redirect:/";
        } catch (UnsupportedEncodingException e) {
            return "redirect:/";
        }

    }

    @GetMapping("open/dir/")
    public String open(Model model, RedirectAttributes redirectAttributes, @RequestParam String path) {
        try {
            String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
            File directory = new File(decodedPath);
            if (directory.exists()) {

                if (directory.isDirectory()) {

                    Function<File, Directory> fileDirectoryMapper = file -> {
                        try {
                            return new Directory("/rename/dir/?path=" + encode(file.getAbsolutePath(), StandardCharsets.UTF_8.toString()),"/open/dir/?path=" + encode(file.getAbsolutePath(), StandardCharsets.UTF_8.toString()), "/delete/dir/?path=" + encode(file.getAbsolutePath(), StandardCharsets.UTF_8.toString()), file.getName());
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        return null;
                    };
                    Function<File, com.filemanagement.app.models.File> fileFileMapper = file -> {
                        try {
                            return new com.filemanagement.app.models.File(file.getName(),"/open/dir/?path=" + encode(file.getAbsolutePath(), StandardCharsets.UTF_8.toString()), "/delete/dir/?path=" + encode(file.getAbsolutePath(), StandardCharsets.UTF_8.toString()),"/rename/file/?path=" + encode(file.getAbsolutePath(), StandardCharsets.UTF_8.toString()));
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
                    model.addAttribute("deletePath", "/delete/dir/?path=" + encode(directory.getAbsolutePath(), StandardCharsets.UTF_8.toString()));
                    model.addAttribute("uploadPath", "/upload/file/?path=" + encode(directory.getAbsolutePath(), StandardCharsets.UTF_8.toString()));
                    model.addAttribute("createPath", "/create/dir/?path=" + encode(directory.getAbsolutePath(), StandardCharsets.UTF_8.toString()));
                    model.addAttribute("createFilePath", "/create/file/?path=" + encode(directory.getAbsolutePath(), StandardCharsets.UTF_8.toString()));
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

    private List<Directory> breadCrumbThis(File directory) throws UnsupportedEncodingException {
        File currentDir = directory;
        List<Directory> breadCrumb = new ArrayList<>();
        while (currentDir != null) {
            breadCrumb.add(new Directory("","/open/dir/?path=" + encode(currentDir.getAbsolutePath(), StandardCharsets.UTF_8.toString())
                    , "/delete/dir/?path=" + encode(currentDir.getAbsolutePath(), StandardCharsets.UTF_8.toString())
                    , currentDir.getName().isEmpty() ? "root" : currentDir.getName()));
            currentDir = currentDir.getParentFile();
        }
        Collections.reverse(breadCrumb);
        return breadCrumb;
    }
}
