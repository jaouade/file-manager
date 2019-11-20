package com.filemanagement.app.controllers;

import com.filemanagement.app.exception.FileManagementException;
import com.filemanagement.app.models.BackUpProperties;
import com.filemanagement.app.models.Directory;
import com.filemanagement.app.services.EmailService;
import com.filemanagement.app.utils.ErrorHandler;
import com.filemanagement.app.utils.ZipUtils;
import com.filemanagement.app.utils.db.Constants;
import com.filemanagement.app.utils.db.DBUtils;
import com.filemanagement.app.utils.db.MysqlExportService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.mail.MessagingException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static com.filemanagement.app.utils.Constants.SLASH;
import static com.filemanagement.app.utils.Constants.ZIP;
import static com.filemanagement.app.utils.FileUtils.*;
import static com.filemanagement.app.utils.PathUtils.decodePath;
import static com.filemanagement.app.utils.PathUtils.encodePath;

@Controller

public class DirectoryManagerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBUtils.class);

    @Autowired
    EmailService emailService;
    private final ServletContext servletContext;
    private final DBUtils dbUtils;

    public DirectoryManagerController(ServletContext servletContext, DBUtils dbUtils) {
        this.servletContext = servletContext;
        this.dbUtils = dbUtils;
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

    @GetMapping("/backup/db/")
    public String backUpPage(Model model, @RequestParam String path) throws IOException {
        model.addAttribute("properties", new BackUpProperties());
        model.addAttribute("path", "/backup/db/?path=" + encodePath(decodePath(path)));
        return "backup-db";
    }

    @PostMapping("/backup/db/")
    public String backUp(RedirectAttributes redirectAttributes, @RequestParam String path, @ModelAttribute BackUpProperties properties) throws IOException {

        new Thread(() -> {
            MysqlExportService service = dbUtils.dump(properties);
            try {
                service.export().asZip();
            } catch (IOException | SQLException | ClassNotFoundException | FileManagementException e) {
                String reportFileName = service.getReportFileName();
                if (reportFileName != null) {
                    FileOutputStream outputStream = null;
                    try {
                        outputStream = new FileOutputStream(decodePath(path) + Constants.SLASH + reportFileName);
                        outputStream.write((service.report() + "Export finished exceptionally \n with :"
                                + e.getMessage() + "\n" + Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString)).getBytes());
                    } catch (IOException ex) {
                        LOGGER.error("An error occurred while writing report to file");
                    } finally {
                        try {
                            if (outputStream != null) outputStream.close();
                        } catch (IOException ex) {
                            LOGGER.error("An error occurred while closing stream");
                        }
                    }

                }
                LOGGER.error("Something went wrong during the dump, caused by : ", e);
            }
            //LOGGER.info("\ndump report : " + service.report());
        }).start();
        redirectAttributes.addFlashAttribute("backupMsg", "Your backup has been launched successfully.We'll notify you by email once it's done.");
        return "redirect:/open/dir/?path=" + encodePath(decodePath(path));
    }


    @GetMapping("zip/dir/")
    public String zip(@RequestParam String path, RedirectAttributes redirectAttributes) throws IOException {
        new Thread(() -> {
            try {
                File file = new File(decodePath(path));
                String zipName = FilenameUtils.getBaseName(file.getName()) + ZIP;
                ZipUtils.compressDirectory(file, new File(file.getParent() + SLASH + zipName));
                emailService.send("Zip process of directory " + file.getName() + " has finished.", "Please check this directory to find your zip " + Constants.EMAIL_NEW_LINE + file.getParent());
            } catch (IOException | MessagingException e) {
                LOGGER.error("An error has occurred during zip process", e);
            }

        }).start();
        redirectAttributes.addFlashAttribute("zipMsg", "Zipping process has been launched successfully, We'll notify you once it's done.");
        return "redirect:/open/dir/?path=" + URLEncoder.encode(URLDecoder.decode(new File(decodePath(path)).getParent(), StandardCharsets.UTF_8.toString()), StandardCharsets.UTF_8.toString());
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
        return "redirect:/open/dir/?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.toString());

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

    @GetMapping("/confirmed/delete/")
    public String delete(RedirectAttributes redirectAttributes, @RequestParam String path) {

        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
            File file = new File(decodedPath);
            File parent = file.getParentFile();
            if (parent != null) {
                if (file.isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(file);
                        return "redirect:/open/dir/?path=" + encodePath(parent);
                    } catch (IOException ex) {
                        redirectAttributes.addFlashAttribute("error", "We could'nt delete folder " + file.getName());
                        return "redirect:/open/dir/?path=" + encodePath(file);

                    }
                } else {
                    if (file.delete())
                        return "redirect:/open/dir/?path=" + encodePath(parent);
                    else {
                        redirectAttributes.addFlashAttribute("error", "We could'nt delete folder " + file.getName());
                        return "redirect:/open/dir/?path=" + encodePath(file);

                    }
                }
            } else return "redirect:/";
        } catch (UnsupportedEncodingException e) {
            return "redirect:/";
        }

    }

    @GetMapping("delete/")
    public String delete(Model model, @RequestParam String path, HttpServletRequest request) {
        try {
            model.addAttribute("next", "/confirmed/delete/?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.toString()));
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
                    List<Directory> directories = foldersOf(directory);
                    List<com.filemanagement.app.models.File> files = filesOf(directory);
                    model.addAttribute("dirs", directories.size() > 0 ? directories : null);
                    model.addAttribute("files", files.size() > 0 ? files : null);
                    model.addAttribute("deletePath", "/delete/?path=" + encodePath(directory));
                    model.addAttribute("uploadPath", "/upload/file/?path=" + encodePath(directory));
                    model.addAttribute("createPath", "/create/dir/?path=" + encodePath(directory));
                    model.addAttribute("createFilePath", "/create/file/?path=" + encodePath(directory));
                    model.addAttribute("backupPath", "/backup/db/?path=" + encodePath(directory));
                    model.addAttribute("breadcrumb", directoryAsBreadCrumb(directory, ""));
                    return "dir";

                } else {
                    return ErrorHandler.error(redirectAttributes, "The path you gave us is not a directory).");

                }
            } else {
                return ErrorHandler.error(redirectAttributes, "The path you gave us is not a directory).");

            }
        } catch (UnsupportedEncodingException e) {
            return ErrorHandler.error(redirectAttributes, "The path you gave us is not well decoded).");
        }

    }

    @PostMapping("open/dir/")
    public String browse(RedirectAttributes redirectAttributes, @ModelAttribute Directory directory) {
        if (directory != null && directory.getPath() != null && !directory.getPath().isEmpty()) {
            String rootPath = directory.getPath();
            File root = new File(rootPath);
            if (root.exists() && root.isDirectory()) {
                try {
                    return "redirect:/open/dir/?path=" + URLEncoder.encode(rootPath, StandardCharsets.UTF_8.toString());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {
                return ErrorHandler.error(redirectAttributes, "The path you gave us is not a directory, You kidding us.");
            }
        }
        return ErrorHandler.error(redirectAttributes, "Please give us a dir before you can browse it :( .");
    }


}
