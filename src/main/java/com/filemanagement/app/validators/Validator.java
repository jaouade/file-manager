package com.filemanagement.app.validators;

import java.util.Map;

import static com.filemanagement.app.utils.db.Constants.*;

public class Validator {
    public static boolean validDBProperties(Map<String,String> properties){
        return properties != null &&
                properties.containsKey(DB_USERNAME) &&
                properties.containsKey(DB_PASSWORD) &&
                properties.containsKey(DB_URL);
    }
    public static boolean validEmailProperties(Map<String,String> properties){
        return properties != null &&
                properties.containsKey(EMAIL_HOST) &&
                properties.containsKey(EMAIL_PORT) &&
                properties.containsKey(EMAIL_USERNAME) &&
                properties.containsKey(EMAIL_PASSWORD) &&
                properties.containsKey(EMAIL_FROM) &&
                properties.containsKey(EMAIL_TO);
    }

}