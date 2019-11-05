package com.filemanagement.app.utils;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public class ErrorHandler {
    public static String error(RedirectAttributes redirectAttributes, String msg) {
        redirectAttributes.addFlashAttribute("error",msg );
        return "redirect:/";
    }
}
