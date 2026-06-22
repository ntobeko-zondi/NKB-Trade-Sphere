package com.example.nkbtradesphere.ui.messages;

import com.example.nkbtradesphere.ConversationAdapter;
import com.example.nkbtradesphere.ConversationDetailActivity;
import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.network.ApiClient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessagesFragment extends Fragment {

    private RecyclerView rvConversations;
    private LinearLayout emptyState;
    private ConversationAdapter adapter;
    private String currentUserId;
    private boolean isLoading = false;
    private boolean hasLoadedOnce = false;
    private long lastLoadAt = 0L;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<ConversationAdapter.ConversationItem> cachedConversations = new ArrayList<>();
    private static final long REFRESH_INTERVAL_MS = 10_000L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvConversations = view.findViewById(R.id.rv_conversations);
        emptyState = view.findViewById(R.id.empty_state);

        ApiClient.initialize(requireContext());
        SharedPreferences prefs = requireContext().getSharedPreferences(AppPreferences.PREFS_NKB, 0);
        currentUserId = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");

        rvConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ConversationAdapter(requireContext(), new ArrayList<>(), this::openConversation);
        rvConversations.setAdapter(adapter);
        loadConversations(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadConversations(false);
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadConversations(false);
        }
    }

    private void loadConversations(boolean force) {
        loadConversations(force, 0);
    }

    private void loadConversations(boolean force, int attempt) {
        if (!isAdded() || isLoading) return;

        long now = System.currentTimeMillis();
        if (!force && hasLoadedOnce && now - lastLoadAt < REFRESH_INTERVAL_MS) {
            return;
        }

        if (TextUtils.isEmpty(currentUserId)) {
            showEmpty();
            return;
        }

        isLoading = true;
        ApiClient.getUserConversations(currentUserId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray arr = response.optJSONArray("conversations");
                List<ConversationAdapter.ConversationItem> conversations = new ArrayList<>();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject c = arr.optJSONObject(i);
                        if (c == null) continue;
                        String otherUserId = c.optString("other_user_id", c.optString("other_user", ""));
                        int listingId = c.optInt("listing_id", 0);
                        if (TextUtils.isEmpty(otherUserId) || listingId <= 0) continue;
                        String otherUserName = c.optString("other_user_name", otherUserId);
                        conversations.add(new ConversationAdapter.ConversationItem(
                                otherUserName,
                                c.optString("last_message", ""),
                                formatTimestamp(parseTimestamp(c.optString("last_message_time", ""))),
                                otherUserId,
                                listingId
                        ));
                    }
                }
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    isLoading = false;
                    hasLoadedOnce = true;
                    lastLoadAt = System.currentTimeMillis();
                    cachedConversations.clear();
                    cachedConversations.addAll(conversations);
                    updateConversationAdapter(conversations);
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    isLoading = false;
                    if (attempt == 0) {
                        mainHandler.postDelayed(() -> loadConversations(true, 1), 700);
                        return;
                    }
                    if (!cachedConversations.isEmpty()) {
                        updateConversationAdapter(new ArrayList<>(cachedConversations));
                    } else if (!hasLoadedOnce) {
                        showEmpty();
                    }
                    // Do not show a noisy error toast for a temporary refresh failure.
                    // The next tab open/resume will refresh again.
                });
            }
        });
    }

    private void updateConversationAdapter(List<ConversationAdapter.ConversationItem> conversations) {
        if (!isAdded()) return;

        if (conversations.isEmpty()) {
            showEmpty();
        } else {
            emptyState.setVisibility(View.GONE);
            rvConversations.setVisibility(View.VISIBLE);
            if (adapter != null) {
                adapter.updateItems(conversations);
            }
        }
    }

    private void showEmpty() {
        emptyState.setVisibility(View.VISIBLE);
        rvConversations.setVisibility(View.GONE);
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private long parseTimestamp(String raw) {
        if (TextUtils.isEmpty(raw)) return 0;
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
        return 0;
    }

    private void openConversation(ConversationAdapter.ConversationItem conversation) {
        Intent intent = new Intent(requireContext(), ConversationDetailActivity.class);
        intent.putExtra("otherUserName", conversation.otherUserName);
        intent.putExtra("otherUserId", conversation.otherUserId);
        intent.putExtra("listingId", conversation.listingId);
        startActivity(intent);
    }
}
