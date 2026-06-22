package com.example.nkbtradesphere;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import com.example.nkbtradesphere.util.AvatarUtils;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private List<ConversationItem> conversations;
    private Context context;
    private OnConversationClickListener clickListener;

    public interface OnConversationClickListener {
        void onConversationClick(ConversationItem conversation);
    }

    public static class ConversationItem {
        public String otherUserName;
        public String lastMessage;
        public String timestamp;
        public String otherUserId;
        public int listingId;

        public ConversationItem(String otherUserName, String lastMessage, String timestamp, 
                                String otherUserId, int listingId) {
            this.otherUserName = otherUserName;
            this.lastMessage = lastMessage;
            this.timestamp = timestamp;
            this.otherUserId = otherUserId;
            this.listingId = listingId;
        }
    }

    public ConversationAdapter(Context context, List<ConversationItem> conversations, OnConversationClickListener listener) {
        this.context = context;
        this.conversations = conversations;
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        ConversationItem item = conversations.get(position);
        
        holder.tvName.setText(item.otherUserName);
        holder.tvLastMessage.setText(item.lastMessage);
        holder.tvTimestamp.setText(item.timestamp);
        
        // Set avatar
        holder.imgAvatar.setImageDrawable(
                AvatarUtils.createInitialsAvatar(context, item.otherUserName, 48)
        );
        
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onConversationClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void updateItems(List<ConversationItem> newConversations) {
        conversations.clear();
        conversations.addAll(newConversations);
        notifyDataSetChanged();
    }

    public static class ConversationViewHolder extends RecyclerView.ViewHolder {
        public ShapeableImageView imgAvatar;
        public TextView tvName;
        public TextView tvLastMessage;
        public TextView tvTimestamp;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
        }
    }
}
