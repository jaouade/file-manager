package com.filemanagement.app.controllers;

import com.filemanagement.app.models.Directory;
import com.filemanagement.app.utils.db.DBUtils;
import com.filemanagement.app.utils.ErrorHandler;
import com.filemanagement.app.utils.db.MysqlExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/")
public class HomeController {
    private static final Logger LOGGER= LoggerFactory.getLogger(DBUtils.class);
    private final DBUtils dbUtils;

    @Autowired
    public HomeController(DBUtils dbUtils) {
        this.dbUtils = dbUtils;
    }

    @GetMapping
    public String home(Model model) {
        model.addAttribute("directory", new Directory());
        return "home";
    }
    @GetMapping("/dump")
    public String dump(Model model) {
        MysqlExportService dump = dbUtils.dump();
        File file = dump.getDumpFile();
        System.out.println(file.getAbsolutePath());
        LOGGER.info("dump report : "+dump.report());
        return "dump";
    }

    @PostMapping("/directory")
    public String browse(RedirectAttributes redirectAttributes, @ModelAttribute Directory directory) {
        if (directory != null && directory.getPath() != null && !directory.getPath().isEmpty()) {
            String rootPath = directory.getPath();
            File root =  new File(rootPath);
            if (root.exists() && root.isDirectory()){
                try {
                    return "redirect:/open/dir/?path="+ URLEncoder.encode(rootPath, StandardCharsets.UTF_8.toString());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }else {
                return ErrorHandler.error(redirectAttributes,"The path you gave us is not a directory, You kidding us.");
            }
        }
        return ErrorHandler.error(redirectAttributes,"Please give us a dir before you can browse it :( .");
    }


}
