package com.filemanagement.app.controllers;

import com.filemanagement.app.services.EmailService;
import com.filemanagement.app.utils.ZipUtils;
import com.filemanagement.app.utils.db.DBUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.filemanagement.app.utils.Constants.SLASH;
import static com.filemanagement.app.utils.FileUtils.downloadFile;
import static com.filemanagement.app.utils.PathUtils.decodePath;

@Controller

public class FileManagerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBUtils.class);

    @Autowired
    EmailService emailService;
    private final ServletContext servletContext;
    private final DBUtils dbUtils;

    public FileManagerController(ServletContext servletContext, DBUtils dbUtils) {
        this.servletContext = servletContext;
        this.dbUtils = dbUtils;
    }


    @GetMapping("/download/file/")
    @ResponseBody
    public ResponseEntity<ByteArrayResource> downloadDir(@RequestParam String path) throws IOException {
        File toDownload = new File(decodePath(path));
        return downloadFile(toDownload, servletContext);
    }

    @PostMapping("edit/file/")
    public String saveEdit(RedirectAttributes redirectAttributes, @ModelAttribute com.filemanagement.app.models.File file, @RequestParam String path) throws UnsupportedEncodingException {
        File fileToSave = new File(URLDecoder.decode(path, StandardCharsets.UTF_8.toString()));

        try {
            FileUtils.writeStringToFile(
                    fileToSave,
                    file.getContent(), false);
            redirectAttributes.addFlashAttribute("success", "File saved.");
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "File not saved.");
        }
        return "redirect:/edit/file/?path=" + URLEncoder.encode(URLDecoder.decode(path, StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString());
    }

    @GetMapping("unzip/file/")
    public String unzip(@RequestParam String path) throws IOException {
        File file = new File(decodePath(path));
        ZipUtils.extractArchive(new File(file.getParent() + SLASH + FilenameUtils.getBaseName(file.getName())), file);
        return "redirect:/open/dir/?path=" + URLEncoder.encode(URLDecoder.decode(file.getParent(), StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString());
    }


    @GetMapping("edit/file/")
    public String edit(Model model, @RequestParam String path) throws IOException {
        String decodedPath = decodePath(path);
        File file = new File(decodedPath);
        model.addAttribute("file", com.filemanagement.app.models.File.builder().content(FileUtils.readFileToString(file)).build());
        try {
            model.addAttribute("savePath", "/edit/file/?path=" + URLEncoder.encode(decodedPath, StandardCharsets.UTF_8.toString()));
            model.addAttribute("name", file.getName());
            model.addAttribute("backPath", "/open/dir/?path=" + URLEncoder.encode(URLDecoder.decode(file.getParent(), StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "edit";
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
        if (name.equals(file.getName()))
            return "redirect:/open/dir/?path=" + URLEncoder.encode(file.getParent(), StandardCharsets.UTF_8.toString());
        try {
            FileUtils.moveFile(
                    FileUtils.getFile(decodedPath),
                    FileUtils.getFile(file.getParent() + SLASH + name));
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "We could'nt change file name to " + name);
        }
        return "redirect:/open/dir/?path=" + URLEncoder.encode(file.getParent(), StandardCharsets.UTF_8.toString());

    }


}
