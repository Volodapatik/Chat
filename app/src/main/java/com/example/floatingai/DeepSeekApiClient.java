package com.example.floatingai;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeepSeekApiClient {

    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface ResponseCallback {
        void onSuccess(String assistantMessage);

        void onError(String errorMessage);
    }

    private final OkHttpClient httpClient;
    private final Context context;

    public DeepSeekApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public void sendMessage(List<ChatWindowManager.ChatMessage> history, ResponseCallback callback) {
        String apiKey = ApiKeyManager.getApiKey(context);
        if (apiKey.isEmpty() || "YOUR_API_KEY_HERE".equals(apiKey)) {
            callback.onError("Missing API key. Update assets/api_key.txt or set DEEPSEEK_API_KEY.");
            return;
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("model", "deepseek-chat");
            JSONArray messagesArray = new JSONArray();
            for (ChatWindowManager.ChatMessage message : history) {
                JSONObject msg = new JSONObject();
                msg.put("role", message.role);
                msg.put("content", message.content);
                messagesArray.put(msg);
            }
            payload.put("messages", messagesArray);

            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Network error");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError("HTTP " + response.code());
                        return;
                    }
                    String responseBody = response.body() != null ? response.body().string() : "";
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        JSONArray choices = json.optJSONArray("choices");
                        if (choices == null || choices.length() == 0) {
                            callback.onError("Empty response from API");
                            return;
                        }
                        JSONObject first = choices.getJSONObject(0);
                        JSONObject message = first.getJSONObject("message");
                        String content = message.optString("content", "");
                        if (content.isEmpty()) {
                            callback.onError("No content in response");
                        } else {
                            callback.onSuccess(content.trim());
                        }
                    } catch (JSONException e) {
                        callback.onError("Parse error");
                    }
                }
            });
        } catch (JSONException e) {
            callback.onError("Failed to build request");
        }
    }
}
