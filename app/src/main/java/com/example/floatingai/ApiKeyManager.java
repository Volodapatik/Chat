package com.example.floatingai;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ApiKeyManager {

    private static final String ENV_KEY = "DEEPSEEK_API_KEY";

    public static String getApiKey(Context context) {
        String envKey = System.getenv(ENV_KEY);
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }
        return readFromAssets(context);
    }

    private static String readFromAssets(Context context) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("api_key.txt")))) {
            String line = reader.readLine();
            return line != null ? line.trim() : "";
        } catch (IOException e) {
            return "";
        }
    }
}
