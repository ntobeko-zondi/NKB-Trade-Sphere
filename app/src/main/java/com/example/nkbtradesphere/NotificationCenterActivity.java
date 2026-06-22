package com.example.nkbtradesphere;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.database.DatabaseHelper;
import com.example.nkbtradesphere.util.AvatarUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationCenterActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private View layoutArchiveEmpty;
    private TextView tvTabActivity;
    private TextView tvTabArchive;

    private DatabaseHelper db;
    private String currentUserId;
    private boolean showingActivityTab = true;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_ITEM = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);

        db = DatabaseHelper.getInstance(this);
        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, MODE_PRIVATE);
        currentUserId = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");

        rvNotifications = findViewById(R.id.rvNotifications);
        layoutArchiveEmpty = findViewById(R.id.layoutArchiveEmpty);
        tvTabActivity = findViewById(R.id.tvTabActivity);
        tvTabArchive = findViewById(R.id.tvTabArchive);
        ImageButton btnBack = findViewById(R.id.btnNotificationBack);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        tvTabActivity.setOnClickListener(v -> showActivityTab());
        tvTabArchive.setOnClickListener(v -> showArchiveTab());

        showActivityTab();
    }

    private void showActivityTab() {
        showingActivityTab = true;
        tvTabActivity.setBackgroundResource(R.drawable.bg_notification_segment_active);
        tvTabArchive.setBackground(null);
        tvTabActivity.setTextColor(ContextCompat.getColor(this, R.color.white));
        tvTabArchive.setTextColor(ContextCompat.getColor(this, R.color.orange_dark));
        rvNotifications.setVisibility(View.VISIBLE);
        layoutArchiveEmpty.setVisibility(View.GONE);
        loadActivityRowsAsync();
    }

    private void showArchiveTab() {
        showingActivityTab = false;
        tvTabArchive.setBackgroundResource(R.drawable.bg_notification_segment_active);
        tvTabActivity.setBackground(null);
        tvTabArchive.setTextColor(ContextCompat.getColor(this, R.color.white));
        tvTabActivity.setTextColor(ContextCompat.getColor(this, R.color.orange_dark));
        rvNotifications.setVisibility(View.GONE);
        layoutArchiveEmpty.setVisibility(View.VISIBLE);
    }

    private void loadActivityRowsAsync() {
        final String userIdSnapshot = currentUserId;
        new Thread(() -> {
            List<FeedRow> rows = buildFeedRows(userIdSnapshot);
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed() || !showingActivityTab) return;
                if (rows.isEmpty()) {
                    rvNotifications.setVisibility(View.GONE);
                    layoutArchiveEmpty.setVisibility(View.VISIBLE);
                } else {
                    rvNotifications.setVisibility(View.VISIBLE);
                    layoutArchiveEmpty.setVisibility(View.GONE);
                    rvNotifications.setAdapter(new NotificationAdapter(rows));
                }
            });
        }).start();
    }

    @NonNull
    private List<FeedRow> buildFeedRows(@Nullable String userId) {
        List<FeedRow> rows = new ArrayList<>();
        if (TextUtils.isEmpty(userId)) return rows;

        List<DatabaseHelper.ConversationData> conversations = db.getUserConversations(userId);
        List<DatabaseHelper.ConversationData> todayItems = new ArrayList<>();
        List<DatabaseHelper.ConversationData> earlierItems = new ArrayList<>();

        long now = System.currentTimeMillis();
        long thirtyDaysMs = 30L * 24 * 60 * 60 * 1000;

        for (DatabaseHelper.ConversationData conversation : conversations) {
            long age = now - conversation.lastMessageTime;
            if (isSameDay(conversation.lastMessageTime, now)) {
                todayItems.add(conversation);
            } else if (age <= thirtyDaysMs) {
                earlierItems.add(conversation);
            }
        }

        if (!todayItems.isEmpty()) {
            rows.add(FeedRow.section("Today"));
            for (DatabaseHelper.ConversationData item : todayItems) {
                rows.add(FeedRow.item(item));
            }
        }
        if (!earlierItems.isEmpty()) {
            rows.add(FeedRow.section("Earlier"));
            for (DatabaseHelper.ConversationData item : earlierItems) {
                rows.add(FeedRow.item(item));
            }
        }
        return rows;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (showingActivityTab) {
            loadActivityRowsAsync();
        }
    }

    private static class FeedRow {
        final int type;
        final String sectionTitle;
        final DatabaseHelper.ConversationData item;

        private FeedRow(int type, @Nullable String sectionTitle, @Nullable DatabaseHelper.ConversationData item) {
            this.type = type;
            this.sectionTitle = sectionTitle;
            this.item = item;
        }

        static FeedRow section(String title) {
            return new FeedRow(TYPE_SECTION, title, null);
        }

        static FeedRow item(DatabaseHelper.ConversationData item) {
            return new FeedRow(TYPE_ITEM, null, item);
        }
    }

    private class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<FeedRow> rows;

        NotificationAdapter(List<FeedRow> rows) {
            this.rows = rows;
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position).type;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_SECTION) {
                View v = inflater.inflate(R.layout.item_notification_section, parent, false);
                return new SectionVH(v);
            }
            View v = inflater.inflate(R.layout.item_notification_row, parent, false);
            return new ItemVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            FeedRow row = rows.get(position);
            if (holder instanceof SectionVH) {
                ((SectionVH) holder).tvSectionTitle.setText(row.sectionTitle);
                return;
            }
            ItemVH vh = (ItemVH) holder;
            if (row.item == null) return;
            DatabaseHelper.ConversationData item = row.item;
            vh.tvNotifText.setText(item.otherUserName + " • " + item.lastMessage);
            vh.tvNotifTime.setText(formatTime(item.lastMessageTime));
            vh.ivNotifAvatar.setImageDrawable(
                    AvatarUtils.createInitialsAvatar(NotificationCenterActivity.this, item.otherUserName, 44));
            if (item.unreadCount > 0) {
                vh.tvUnreadCount.setVisibility(View.VISIBLE);
                vh.tvUnreadCount.setText(item.unreadCount > 99 ? "99+" : String.valueOf(item.unreadCount));
            } else {
                vh.tvUnreadCount.setVisibility(View.GONE);
            }

            View.OnClickListener openConversation = v -> {
                Intent intent = new Intent(NotificationCenterActivity.this, ConversationDetailActivity.class);
                intent.putExtra("otherUserId", item.otherUserId);
                intent.putExtra("otherUserName", item.otherUserName);
                intent.putExtra("listingId", item.listingId);
                startActivity(intent);
            };
            vh.itemView.setOnClickListener(openConversation);
            vh.btnNotifView.setOnClickListener(openConversation);
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }

    private static class SectionVH extends RecyclerView.ViewHolder {
        final TextView tvSectionTitle;

        SectionVH(@NonNull View itemView) {
            super(itemView);
            tvSectionTitle = itemView.findViewById(R.id.tvSectionTitle);
        }
    }

    private static class ItemVH extends RecyclerView.ViewHolder {
        final ImageView ivNotifAvatar;
        final TextView tvNotifText;
        final TextView tvNotifTime;
        final TextView tvUnreadCount;
        final TextView btnNotifView;

        ItemVH(@NonNull View itemView) {
            super(itemView);
            ivNotifAvatar = itemView.findViewById(R.id.ivNotifAvatar);
            tvNotifText = itemView.findViewById(R.id.tvNotifText);
            tvNotifTime = itemView.findViewById(R.id.tvNotifTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            btnNotifView = itemView.findViewById(R.id.btnNotifView);
        }
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "";
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(timestamp);

        boolean sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR);
        if (sameDay) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        }
        return new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date(timestamp));
    }

    private boolean isSameDay(long first, long second) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(first);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(second);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}
