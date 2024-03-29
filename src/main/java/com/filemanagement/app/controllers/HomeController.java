package com.filemanagement.app.controllers;

import com.filemanagement.app.models.Directory;
import com.filemanagement.app.utils.ErrorHandler;
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
    @GetMapping
    public String home(Model model) {
        model.addAttribute("directory", new Directory());
        return "home";
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
