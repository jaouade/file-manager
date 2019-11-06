package com.filemanagement.app.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    public  static  String getFormattedDate(Long date){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-YY  HH:mm:ss");
        return simpleDateFormat.format(new Date(date));
    }
}
