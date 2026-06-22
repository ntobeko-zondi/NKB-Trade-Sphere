package com.example.nkbtradesphere;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.network.ApiClient;
import com.example.nkbtradesphere.util.AvatarUtils;
import com.example.nkbtradesphere.util.MessageNotificationHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationDetailActivity extends AppCompatActivity {

    private ListView lvMessages;
    private EditText etMessageInput;
    private TextView tvOtherUserName;
    private ImageButton btnSendMessage;

    private String otherUserName;
    private String otherUserId;
    private int listingId;
    private String currentUserId;
    private boolean isSendingMessage = false;
    private boolean isLoadingMessages = false;
    private final List<MessageRow> cachedMessages = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_detail);

        Intent intent = getIntent();
        otherUserName = intent.getStringExtra("otherUserName");
        otherUserId = intent.getStringExtra("otherUserId");
        listingId = intent.getIntExtra("listingId", 0);

        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, 0);
        currentUserId = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");

        ApiClient.initialize(this);

        if (TextUtils.isEmpty(otherUserName)) {
            otherUserName = TextUtils.isEmpty(otherUserId) ? "User" : otherUserId;
        }

        tvOtherUserName = findViewById(R.id.tvOtherUserName);
        lvMessages = findViewById(R.id.lvMessages);
        etMessageInput = findViewById(R.id.etMessageInput);
        ImageView ivHeaderAvatar = findViewById(R.id.ivHeaderAvatar);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        ImageButton btnBack = findViewById(R.id.btnBack);

        tvOtherUserName.setText(otherUserName);
        ivHeaderAvatar.setImageDrawable(AvatarUtils.createInitialsAvatar(this, otherUserName, 36));
        btnSendMessage.setOnClickListener(v -> sendMessage());
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        ensureConversationReadyThenLoad();
    }

    private void ensureConversationReadyThenLoad() {
        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(otherUserId) && listingId > 0) {
            loadMessages();
            return;
        }

        if (listingId > 0) {
            // Recover seller details if the screen was opened with only a listing id.
            // This protects buyer-to-seller messaging from missing Intent extras.
            ApiClient.getListing(listingId, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    JSONObject listing = response.optJSONObject("listing");
                    if (listing != null) {
                        otherUserId = listing.optString("seller_id", otherUserId);
                        otherUserName = listing.optString("seller_name", otherUserName);
                        if (TextUtils.isEmpty(otherUserName)) {
                            otherUserName = TextUtils.isEmpty(otherUserId) ? "Seller" : otherUserId;
                        }
                    }
                    mainHandler.post(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        tvOtherUserName.setText(TextUtils.isEmpty(otherUserName) ? "Seller" : otherUserName);
                        loadMessages();
                    });
                }

                @Override
                public void onError(String error) {
                    mainHandler.post(() -> Toast.makeText(
                            ConversationDetailActivity.this,
                            "Seller conversation is unavailable: " + error,
                            Toast.LENGTH_SHORT).show());
                }
            });
            return;
        }

        Toast.makeText(this, "This conversation is unavailable", Toast.LENGTH_SHORT).show();
    }

    private void loadMessages() {
        loadMessages(0);
    }

    private void loadMessages(int attempt) {
        if (isLoadingMessages) return;
        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(otherUserId) || listingId <= 0) {
            bindMessages(new ArrayList<>());
            return;
        }

        isLoadingMessages = true;

        ApiClient.markConversationAsRead(currentUserId, otherUserId, listingId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                ApiClient.getUnreadMessageCount(currentUserId, new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject unreadResponse) {
                        if (unreadResponse.optInt("count", 0) == 0) {
                            MessageNotificationHelper.cancelUnreadMessagesNotification(ConversationDetailActivity.this);
                        }
                    }

                    @Override
                    public void onError(String error) { }
                });
            }

            @Override
            public void onError(String error) { }
        });

        ApiClient.getConversation(currentUserId, otherUserId, listingId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                List<MessageRow> rows = new ArrayList<>();
                JSONArray messages = response.optJSONArray("messages");
                if (messages != null) {
                    for (int i = 0; i < messages.length(); i++) {
                        JSONObject m = messages.optJSONObject(i);
                        if (m == null) continue;
                        rows.add(new MessageRow(
                                m.optInt("message_id", 0),
                                m.optString("sender_id", ""),
                                m.optString("receiver_id", ""),
                                m.optInt("listing_id", listingId),
                                m.optString("message_text", ""),
                                parseTimestamp(m.optString("created_at", m.optString("timestamp", "")))
                        ));
                    }
                }
                mainHandler.post(() -> {
                    isLoadingMessages = false;
                    if (isFinishing() || isDestroyed()) return;
                    cachedMessages.clear();
                    cachedMessages.addAll(rows);
                    bindMessages(rows);
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    isLoadingMessages = false;
                    if (isFinishing() || isDestroyed()) return;
                    if (attempt == 0) {
                        mainHandler.postDelayed(() -> loadMessages(1), 700);
                        return;
                    }
                    if (!cachedMessages.isEmpty()) {
                        bindMessages(new ArrayList<>(cachedMessages));
                    } else {
                        bindMessages(new ArrayList<>());
                    }
                    Toast.makeText(ConversationDetailActivity.this,
                            "Messages could not refresh. Please check your connection and try again.",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void bindMessages(List<MessageRow> messages) {
        ArrayAdapter<MessageRow> adapter = new ArrayAdapter<MessageRow>(
                this, R.layout.item_message_bubble, messages) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View row = convertView;
                if (row == null) {
                    row = getLayoutInflater().inflate(R.layout.item_message_bubble, parent, false);
                }
                MessageRow message = getItem(position);
                if (message == null) return row;

                View otherLayout = row.findViewById(R.id.layoutOther);
                View myLayout = row.findViewById(R.id.layoutMe);
                TextView tvOtherMessage = row.findViewById(R.id.tvOtherMessage);
                TextView tvOtherTime = row.findViewById(R.id.tvOtherTime);
                TextView tvMyMessage = row.findViewById(R.id.tvMyMessage);
                TextView tvMyTime = row.findViewById(R.id.tvMyTime);
                TextView tvMySeenState = row.findViewById(R.id.tvMySeenState);
                TextView tvDateSeparator = row.findViewById(R.id.tvDateSeparator);

                if (shouldShowDateSeparator(position, messages)) {
                    tvDateSeparator.setVisibility(View.VISIBLE);
                    tvDateSeparator.setText(formatDateHeader(message.createdAt));
                } else {
                    tvDateSeparator.setVisibility(View.GONE);
                }

                boolean mine = currentUserId.equalsIgnoreCase(message.senderId);
                if (mine) {
                    myLayout.setVisibility(View.VISIBLE);
                    otherLayout.setVisibility(View.GONE);
                    tvMyMessage.setText(message.messageText);
                    tvMyTime.setText(formatTimestamp(message.createdAt));
                    tvMySeenState.setText(buildSeenState(position, messages));
                } else {
                    otherLayout.setVisibility(View.VISIBLE);
                    myLayout.setVisibility(View.GONE);
                    tvOtherMessage.setText(message.messageText);
                    tvOtherTime.setText(formatTimestamp(message.createdAt));
                }

                if (convertView == null) {
                    row.setAlpha(0f);
                    row.setTranslationY(16f);
                    row.animate().alpha(1f).translationY(0f).setDuration(180).start();
                }
                return row;
            }
        };
        lvMessages.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            lvMessages.setSelection(adapter.getCount() - 1);
        }
    }

    private void sendMessage() {
        String messageText = etMessageInput.getText().toString().trim();

        if (isSendingMessage) {
            return;
        }
        if (TextUtils.isEmpty(messageText)) {
            etMessageInput.setError("Please enter a message");
            return;
        }
        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(otherUserId) || listingId <= 0) {
            Toast.makeText(this, "Cannot send message for this item", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUserId.equalsIgnoreCase(otherUserId)) {
            Toast.makeText(this, "You cannot message yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        isSendingMessage = true;
        if (btnSendMessage != null) btnSendMessage.setEnabled(false);

        ApiClient.sendMessage(currentUserId, otherUserId, listingId, messageText, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                mainHandler.post(() -> {
                    isSendingMessage = false;
                    if (btnSendMessage != null) btnSendMessage.setEnabled(true);
                    etMessageInput.setText("");
                    loadMessages();
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    isSendingMessage = false;
                    if (btnSendMessage != null) btnSendMessage.setEnabled(true);
                    Toast.makeText(ConversationDetailActivity.this, "Failed to send message: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private boolean shouldShowDateSeparator(int position, List<MessageRow> messages) {
        if (position == 0) return true;
        if (position < 0 || position >= messages.size()) return false;
        long previous = messages.get(position - 1).createdAt;
        long current = messages.get(position).createdAt;
        return !isSameDay(previous, current);
    }

    private boolean isSameDay(long first, long second) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(first);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(second);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private String formatDateHeader(long timestamp) {
        Calendar now = Calendar.getInstance();
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timestamp);

        if (isSameDay(now.getTimeInMillis(), timestamp)) {
            return "Today";
        }
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(now.getTimeInMillis(), timestamp)) {
            return "Yesterday";
        }
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(timestamp));
    }

    private String buildSeenState(int position, List<MessageRow> messages) {
        boolean hasReplyAfter = false;
        for (int i = position + 1; i < messages.size(); i++) {
            MessageRow next = messages.get(i);
            if (!currentUserId.equalsIgnoreCase(next.senderId)) {
                hasReplyAfter = true;
                break;
            }
        }
        return hasReplyAfter ? "✓✓ Seen" : "✓ Sent";
    }

    private long parseTimestamp(String raw) {
        if (TextUtils.isEmpty(raw)) return System.currentTimeMillis();
        String cleaned = raw.replace("T", " ").replace("Z", "");
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss.SSSSSS",
                "yyyy-MM-dd HH:mm:ss.SSS",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern, Locale.US).parse(cleaned).getTime();
            } catch (Exception ignored) { }
        }
        return System.currentTimeMillis();
    }

    private static final class MessageRow {
        final int messageId;
        final String senderId;
        final String receiverId;
        final int listingId;
        final String messageText;
        final long createdAt;

        MessageRow(int messageId, String senderId, String receiverId, int listingId,
                   String messageText, long createdAt) {
            this.messageId = messageId;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.listingId = listingId;
            this.messageText = messageText;
            this.createdAt = createdAt;
        }
    }
}
