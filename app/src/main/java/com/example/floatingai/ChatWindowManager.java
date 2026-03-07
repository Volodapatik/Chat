package com.example.floatingai;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import android.view.ViewGroup;

public class ChatWindowManager {

    private final Context context;
    private final WindowManager windowManager;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final DeepSeekApiClient apiClient;

    private View chatView;
    private boolean isShowing = false;
    private ChatAdapter adapter;
    private ProgressBar loadingIndicator;

    public ChatWindowManager(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.apiClient = new DeepSeekApiClient(this.context);
    }

    public void show() {
        if (isShowing) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        chatView = inflater.inflate(R.layout.chat_window, null);

        RecyclerView recyclerView = chatView.findViewById(R.id.chatRecycler);
        EditText input = chatView.findViewById(R.id.messageInput);
        ImageButton closeButton = chatView.findViewById(R.id.closeButton);
        loadingIndicator = chatView.findViewById(R.id.loadingIndicator);

        adapter = new ChatAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);

        closeButton.setOnClickListener(v -> dismiss());

        chatView.findViewById(R.id.sendButton).setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                return;
            }
            input.setText("");
            addMessage(new ChatMessage("user", text));
            showLoading(true);

            apiClient.sendMessage(messages, new DeepSeekApiClient.ResponseCallback() {
                @Override
                public void onSuccess(String assistantMessage) {
                    mainHandler.post(() -> {
                        showLoading(false);
                        addMessage(new ChatMessage("assistant", assistantMessage));
                        recyclerView.scrollToPosition(messages.size() - 1);
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    mainHandler.post(() -> {
                        showLoading(false);
                        addMessage(new ChatMessage("assistant", "Error: " + errorMessage));
                        recyclerView.scrollToPosition(messages.size() - 1);
                    });
                }
            });
        });

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 40;
        params.y = 200;

        windowManager.addView(chatView, params);
        isShowing = true;
    }

    public void dismiss() {
        if (!isShowing || chatView == null) {
            return;
        }
        windowManager.removeView(chatView);
        isShowing = false;
        chatView = null;
    }

    private void addMessage(ChatMessage message) {
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
    }

    private void showLoading(boolean show) {
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public static class ChatMessage {
        public final String role;
        public final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatViewHolder> {

        private final List<ChatMessage> items;

        ChatAdapter(List<ChatMessage> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView messageView = new TextView(parent.getContext());
            int padding = (int) (10 * parent.getResources().getDisplayMetrics().density);
            messageView.setPadding(padding, padding, padding, padding);
            messageView.setTextSize(15);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.WRAP_CONTENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 8, 8, 8);
            messageView.setLayoutParams(params);
            return new ChatViewHolder(messageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = items.get(position);
            holder.messageView.setText(message.content);
            if ("user".equals(message.role)) {
                holder.messageView.setBackgroundResource(R.drawable.message_user_bg);
                holder.messageView.setTextColor(holder.messageView.getResources().getColor(R.color.on_primary));
                ((RecyclerView.LayoutParams) holder.messageView.getLayoutParams()).setMargins(80, 8, 8, 8);
            } else {
                holder.messageView.setBackgroundResource(R.drawable.message_assistant_bg);
                holder.messageView.setTextColor(holder.messageView.getResources().getColor(R.color.on_surface));
                ((RecyclerView.LayoutParams) holder.messageView.getLayoutParams()).setMargins(8, 8, 80, 8);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class ChatViewHolder extends RecyclerView.ViewHolder {
        final TextView messageView;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            messageView = (TextView) itemView;
        }
    }
}
