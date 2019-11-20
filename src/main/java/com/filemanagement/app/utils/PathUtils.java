package com.filemanagement.app.utils;

import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static java.net.URLEncoder.encode;

public class PathUtils {
    public static String encodePath(File file) throws UnsupportedEncodingException {
        return encode(file.getAbsolutePath(), StandardCharsets.UTF_8.toString());
    }
    public static String encodePath(String path) throws UnsupportedEncodingException {
        return encode(path, StandardCharsets.UTF_8.toString());
    }

    public static String decodePath(@RequestParam String path) throws UnsupportedEncodingException {
        return URLDecoder.decode(path, StandardCharsets.UTF_8.toString());
    }
}
