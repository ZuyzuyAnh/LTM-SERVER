package org.example.dto;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

// Parser la util class giup chuyen doi Message thanh dang JSON string
public class Parser {
    private static final Gson gson = new Gson();

    // Chuyen thanh json
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    // Chuyen thanh Message
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return gson.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
}
