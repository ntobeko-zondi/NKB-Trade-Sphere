package com.example.nkbtradesphere.database;

import android.content.Context;
import android.os.Looper;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;

import com.example.nkbtradesphere.network.ApiConfig;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Compatibility helper for older screens that still call DatabaseHelper.
 *
 * This is NOT the old local SQLite database helper. It forwards those calls to
 * the hosted PHP/PostgreSQL backend so existing Java files can compile and keep
 * working while the app uses the online database.
 */
public class DatabaseHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String API_URL = ApiConfig.API_URL;

    private static DatabaseHelper instance;
    private final Context appContext;

    public static final int RESET_OK = 0;
    public static final int RESET_EMAIL_NOT_FOUND = 1;
    public static final int RESET_DETAILS_MISSING = 2;
    public static final int RESET_ID_MISMATCH = 3;
    public static final int RESET_PHONE_MISMATCH = 4;

    private DatabaseHelper(Context context) {
        appContext = context.getApplicationContext();
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }
        return instance;
    }

    // ==================== USER METHODS ====================

    public boolean registerUser(String email, String password, String fullName, String idNumber, String phone) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user_id", normalizeEmail(email));
        params.put("email", normalizeEmail(email));
        params.put("password", password == null ? "" : password);
        params.put("full_name", safe(fullName));
        params.put("id_number", idNumber == null ? "" : idNumber.replaceAll("\\D+", ""));
        params.put("phone", normalizePhone(phone));
        JSONObject response = request("registerUser", params);
        return isSuccess(response);
    }

    public boolean registerUser(String email, String password, String fullName, String phone) {
        return registerUser(email, password, fullName, "", phone);
    }

    public boolean authenticateUser(String email, String password) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user_id", normalizeEmail(email));
        params.put("password", password == null ? "" : password);
        JSONObject response = request("authenticateUser", params);
        return isSuccess(response);
    }

    @Nullable
    public UserData getUserByEmail(String email) {
        if (TextUtils.isEmpty(email)) return null;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user_id", normalizeEmail(email));
        JSONObject response = request("getUserDetails", params);
        if (!isSuccess(response)) return null;
        return parseUser(response.optJSONObject("user"));
    }

    public int checkResetDetails(String email, String idNumber, String phone) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("email", normalizeEmail(email));
        params.put("id_number", idNumber == null ? "" : idNumber.replaceAll("\\D+", ""));
        params.put("phone", normalizePhone(phone));
        JSONObject response = request("verifyResetDetails", params);
        if (isSuccess(response)) return RESET_OK;

        String error = response == null ? "" : response.optString("error", "").toLowerCase(Locale.US);
        if (error.contains("no account") || error.contains("not found")) return RESET_EMAIL_NOT_FOUND;
        if (error.contains("missing")) return RESET_DETAILS_MISSING;
        if (error.contains("id")) return RESET_ID_MISMATCH;
        if (error.contains("phone")) return RESET_PHONE_MISMATCH;
        return RESET_DETAILS_MISSING;
    }

    public boolean verifyResetDetails(String email, String idNumber, String phone) {
        return checkResetDetails(email, idNumber, phone) == RESET_OK;
    }

    public boolean updateUserPassword(String email, String newPassword) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("email", normalizeEmail(email));
        params.put("password", newPassword == null ? "" : newPassword);
        JSONObject response = request("updateUserPassword", params);
        return isSuccess(response);
    }

    public boolean updateUserProfile(String email, String fullName, String phone) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user_id", normalizeEmail(email));
        params.put("email", normalizeEmail(email));
        params.put("full_name", safe(fullName));
        params.put("phone", normalizePhone(phone));
        JSONObject response = request("updateUserProfile", params);
        return isSuccess(response);
    }

    public boolean deleteUserAccount(String email) {
        if (TextUtils.isEmpty(email)) return false;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user_id", normalizeEmail(email));
        JSONObject response = request("deleteUserAccount", params);
        return isSuccess(response);
    }

    // ==================== LISTING METHODS ====================

    public long createListing(String sellerId, String title, String category, String condition,
                              String description, String price, int quantity) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("seller_id", normalizeEmail(sellerId));
        params.put("title", safe(title));
        params.put("category", safe(category));
        params.put("condition", safe(condition));
        params.put("description", safe(description));
        params.put("price", safe(price));
        params.put("image_url", "");
        params.put("quantity", String.valueOf(Math.max(0, quantity)));
        JSONObject response = request("createListing", params);
        if (!isSuccess(response)) return -1;
        return response.optLong("listing_id", -1);
    }

    public boolean addListingImage(long listingId, String imagePath, boolean isPrimary) {
        if (listingId <= 0 || TextUtils.isEmpty(imagePath)) return false;
        ListingData listing = getListingById(listingId);
        if (listing == null) return false;
        List<String> images = new ArrayList<>(listing.imageGallery == null ? new ArrayList<String>() : listing.imageGallery);
        if (isPrimary) {
            images.add(0, imagePath);
        } else {
            images.add(imagePath);
        }
        return updateListingWithImages(listing, images);
    }

    public boolean replaceListingImages(long listingId, List<String> imagePaths) {
        ListingData listing = getListingById(listingId);
        if (listing == null) return false;
        return updateListingWithImages(listing, imagePaths == null ? new ArrayList<String>() : imagePaths);
    }

    public List<ListingData> getAllListings() {
        JSONObject response = request("getAllListings", new LinkedHashMap<String, String>());
        return parseListings(response == null ? null : response.optJSONArray("listings"));
    }

    @Nullable
    public ListingData getListingById(long listingId) {
        if (listingId <= 0) return null;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("listing_id", String.valueOf(listingId));
        JSONObject response = request("getListing", params);
        if (!isSuccess(response)) return null;
        return parseListing(response.optJSONObject("listing"));
    }

    public List<ListingData> getListingsBySeller(String sellerId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("seller_id", normalizeEmail(sellerId));
        JSONObject response = request("getSellerListings", params);
        return parseListings(response == null ? null : response.optJSONArray("listings"));
    }

    public List<ListingData> getListingsByCategory(String category) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("category", safe(category));
        JSONObject response = request("getListingsByCategory", params);
        return parseListings(response == null ? null : response.optJSONArray("listings"));
    }

    public List<String> getListingImages(long listingId) {
        ListingData listing = getListingById(listingId);
        if (listing == null || listing.imageGallery == null) return new ArrayList<>();
        return new ArrayList<>(listing.imageGallery);
    }

    public boolean updateListingQuantity(long listingId, int newQuantity) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("listing_id", String.valueOf(listingId));
        params.put("quantity", String.valueOf(Math.max(0, newQuantity)));
        JSONObject response = request("updateListingQuantity", params);
        return isSuccess(response);
    }

    public boolean updateListing(long listingId, String sellerId, String title, String category,
                                 String condition, String description, String price, int quantity) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("listing_id", String.valueOf(listingId));
        params.put("seller_id", normalizeEmail(sellerId));
        params.put("title", safe(title));
        params.put("category", safe(category));
        params.put("condition", safe(condition));
        params.put("description", safe(description));
        params.put("price", safe(price));
        params.put("image_url", ""); // api.php keeps the existing image when this is empty.
        params.put("quantity", String.valueOf(Math.max(0, quantity)));
        JSONObject response = request("updateListing", params);
        return isSuccess(response);
    }

    public boolean purchaseListing(long listingId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("listing_id", String.valueOf(listingId));
        JSONObject response = request("purchaseListing", params);
        return isSuccess(response);
    }

    public boolean rateListing(String buyerId, long listingId, float rating, String comment) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("buyer_id", normalizeEmail(buyerId));
        params.put("listing_id", String.valueOf(listingId));
        params.put("rating", String.valueOf(rating));
        params.put("comment", safe(comment));
        JSONObject response = request("rateListing", params);
        return isSuccess(response);
    }

    public boolean deleteListing(long listingId) {
        ListingData listing = getListingById(listingId);
        if (listing == null) return false;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("listing_id", String.valueOf(listingId));
        params.put("seller_id", normalizeEmail(listing.sellerId));
        JSONObject response = request("deleteListing", params);
        return isSuccess(response);
    }

    public int getSellerListingCount(String sellerId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("seller_id", normalizeEmail(sellerId));
        JSONObject response = request("getSellerListingCount", params);
        return isSuccess(response) ? response.optInt("count", 0) : 0;
    }

    // ==================== SAVED ITEM METHODS ====================

    public boolean isListingOwnedBy(long listingId, String userId) {
        ListingData listing = getListingById(listingId);
        return listing != null && normalizeEmail(userId).equals(normalizeEmail(listing.sellerId));
    }

    public boolean saveItem(String userId, long listingId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user_id", normalizeEmail(userId));
        params.put("listing_id", String.valueOf(listingId));
        JSONObject response = request("saveItem", params);
        return isSuccess(response);
    }

    public boolean removeSavedItem(String userId, long listingId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user_id", normalizeEmail(userId));
        params.put("listing_id", String.valueOf(listingId));
        JSONObject response = request("removeSavedItem", params);
        return isSuccess(response);
    }

    public boolean isSaved(String userId, long listingId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user_id", normalizeEmail(userId));
        params.put("listing_id", String.valueOf(listingId));
        JSONObject response = request("isSaved", params);
        return isSuccess(response) && response.optBoolean("is_saved", false);
    }

    public List<ListingData> getSavedItems(String userId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user_id", normalizeEmail(userId));
        JSONObject response = request("getSavedItems", params);
        return parseListings(response == null ? null : response.optJSONArray("listings"));
    }

    public int getSavedItemsCount(String userId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user_id", normalizeEmail(userId));
        JSONObject response = request("getSavedItemsCount", params);
        return isSuccess(response) ? response.optInt("count", 0) : 0;
    }

    // ==================== MESSAGE METHODS ====================

    public boolean sendLocalMessage(String senderId, String receiverId, long listingId, String messageText) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("sender_id", normalizeEmail(senderId));
        params.put("receiver_id", normalizeEmail(receiverId));
        params.put("listing_id", String.valueOf(listingId));
        params.put("message_text", safe(messageText));
        JSONObject response = request("sendMessage", params);
        return isSuccess(response);
    }

    public List<MessageData> getConversationMessages(String userId, String otherUserId, long listingId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user_id_1", normalizeEmail(userId));
        params.put("user_id_2", normalizeEmail(otherUserId));
        params.put("listing_id", String.valueOf(listingId));
        JSONObject response = request("getConversation", params);
        JSONArray arr = response == null ? null : response.optJSONArray("messages");
        List<MessageData> messages = new ArrayList<>();
        if (arr == null) return messages;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item != null) messages.add(parseMessage(item));
        }
        return messages;
    }

    public int getUnreadMessageCount(String userId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user_id", normalizeEmail(userId));
        JSONObject response = request("getUnreadMessageCount", params);
        return isSuccess(response) ? response.optInt("count", 0) : 0;
    }

    public int markConversationAsRead(String userId, String otherUserId, long listingId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user_id", normalizeEmail(userId));
        params.put("other_user_id", normalizeEmail(otherUserId));
        params.put("listing_id", String.valueOf(listingId));
        JSONObject response = request("markConversationAsRead", params);
        return isSuccess(response) ? response.optInt("rows", 0) : 0;
    }

    public List<ConversationData> getUserConversations(String userId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("user_id", normalizeEmail(userId));
        JSONObject response = request("getUserConversations", params);
        JSONArray arr = response == null ? null : response.optJSONArray("conversations");
        List<ConversationData> conversations = new ArrayList<>();
        if (arr == null) return conversations;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) continue;
            ConversationData conversation = parseConversation(item);
            if (!TextUtils.isEmpty(conversation.otherUserId) && conversation.listingId > 0) {
                conversations.add(conversation);
            }
        }
        return conversations;
    }

    @Nullable
    public ConversationData getLatestUnreadConversation(String userId) {
        List<ConversationData> conversations = getUserConversations(userId);
        for (ConversationData conversation : conversations) {
            if (conversation.unreadCount > 0) return conversation;
        }
        return null;
    }

    public String getDisplayNameForEmail(String email) {
        UserData user = getUserByEmail(email);
        return user != null && !TextUtils.isEmpty(user.fullName) ? user.fullName : safe(email);
    }

    // ==================== INTERNAL HTTP + PARSING ====================

    @Nullable
    private JSONObject request(String action, Map<String, String> params) {
        StrictMode.ThreadPolicy oldPolicy = null;
        boolean changedPolicy = Looper.getMainLooper() == Looper.myLooper();
        if (changedPolicy) {
            oldPolicy = StrictMode.getThreadPolicy();
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(oldPolicy).permitNetwork().build());
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(API_URL + "?action=" + URLEncoder.encode(action, "UTF-8"));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            Map<String, String> postParams = new LinkedHashMap<>();
            postParams.put("action", action);
            if (params != null) {
                postParams.putAll(params);
            }
            String postBody = buildPostBody(postParams);
            OutputStream os = connection.getOutputStream();
            os.write(postBody.getBytes("UTF-8"));
            os.flush();
            os.close();

            int code = connection.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 400 ? connection.getInputStream() : connection.getErrorStream(),
                    "UTF-8"));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            reader.close();
            JSONObject json = new JSONObject(body.toString());
            if (!json.optBoolean("success", false)) {
                Log.w(TAG, "API returned error for " + action + ": " + json.optString("error", "unknown"));
            }
            return json;
        } catch (Exception e) {
            Log.e(TAG, "API request failed: " + action, e);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
            if (changedPolicy && oldPolicy != null) {
                StrictMode.setThreadPolicy(oldPolicy);
            }
        }
    }

    private static String buildPostBody(Map<String, String> params) throws Exception {
        StringBuilder body = new StringBuilder();
        if (params == null) return "";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (body.length() > 0) body.append('&');
            body.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            body.append('=');
            body.append(URLEncoder.encode(entry.getValue() == null ? "" : entry.getValue(), "UTF-8"));
        }
        return body.toString();
    }

    private boolean updateListingWithImages(ListingData listing, List<String> images) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("listing_id", String.valueOf(listing.listingId));
        params.put("seller_id", normalizeEmail(listing.sellerId));
        params.put("title", safe(listing.title));
        params.put("category", safe(listing.category));
        params.put("condition", safe(listing.condition));
        params.put("description", safe(listing.description));
        params.put("price", safe(listing.price));
        params.put("image_url", joinImages(images));
        params.put("quantity", String.valueOf(Math.max(0, listing.quantity)));
        JSONObject response = request("updateListing", params);
        return isSuccess(response);
    }

    private static boolean isSuccess(@Nullable JSONObject response) {
        return response != null && response.optBoolean("success", false);
    }

    @Nullable
    private static UserData parseUser(@Nullable JSONObject user) {
        if (user == null) return null;
        return new UserData(
                user.optInt("id", 0),
                user.optString("email", user.optString("user_id", "")),
                user.optString("full_name", ""),
                user.optString("phone", ""),
                (float) user.optDouble("average_rating", 0.0)
        );
    }

    private static List<ListingData> parseListings(@Nullable JSONArray arr) {
        List<ListingData> listings = new ArrayList<>();
        if (arr == null) return listings;
        for (int i = 0; i < arr.length(); i++) {
            ListingData listing = parseListing(arr.optJSONObject(i));
            if (listing != null) listings.add(listing);
        }
        return listings;
    }

    @Nullable
    private static ListingData parseListing(@Nullable JSONObject listing) {
        if (listing == null) return null;
        String rawImages = listing.optString("image_url", "");
        List<String> gallery = splitImages(rawImages);
        String primary = gallery.isEmpty() ? normalizeImageData(rawImages) : gallery.get(0);
        return new ListingData(
                listing.optInt("listing_id", 0),
                listing.optString("seller_id", ""),
                listing.optString("title", ""),
                listing.optString("category", ""),
                listing.optString("condition", ""),
                listing.optString("description", ""),
                listing.optString("price", ""),
                listing.optInt("quantity", 0),
                listing.optString("status", "active"),
                (float) listing.optDouble("average_rating", 0.0),
                primary,
                gallery
        );
    }

    private static MessageData parseMessage(JSONObject item) {
        return new MessageData(
                item.optInt("message_id", 0),
                item.optString("sender_id", ""),
                item.optString("receiver_id", ""),
                item.optInt("listing_id", 0),
                item.optString("message_text", ""),
                parseTimestamp(item.optString("created_at", ""))
        );
    }

    private static ConversationData parseConversation(JSONObject item) {
        return new ConversationData(
                item.optString("other_user_id", ""),
                item.optString("other_user_name", item.optString("other_user_id", "")),
                item.optInt("listing_id", 0),
                item.optString("last_message", ""),
                parseTimestamp(item.optString("last_message_time", item.optString("created_at", ""))),
                item.optInt("unread_count", 0)
        );
    }

    private static long parseTimestamp(String raw) {
        if (TextUtils.isEmpty(raw)) return System.currentTimeMillis();
        String value = raw.trim();
        String[] patterns = new String[]{
                "yyyy-MM-dd HH:mm:ss.SSSSSS",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                if (pattern.contains("'Z'")) sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(value);
                if (d != null) return d.getTime();
            } catch (ParseException ignored) {
            }
        }
        return System.currentTimeMillis();
    }

    private static List<String> splitImages(String raw) {
        List<String> out = new ArrayList<>();
        if (TextUtils.isEmpty(raw)) return out;
        String[] parts = raw.split("\\|");
        for (String part : parts) {
            String clean = normalizeImageData(part == null ? "" : part.trim());
            if (!clean.isEmpty()) out.add(clean);
        }
        return out;
    }

    private static String normalizeImageData(String rawImage) {
        if (TextUtils.isEmpty(rawImage)) return "";
        String image = rawImage.trim();
        if (image.startsWith("http://") || image.startsWith("https://") || image.startsWith("data:image")) {
            return image;
        }
        // Listings saved from Android are often stored as raw base64. Glide needs the data URI prefix.
        if (image.length() > 120) {
            return "data:image/jpeg;base64," + image;
        }
        return image;
    }

    private static String joinImages(List<String> images) {
        if (images == null || images.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (String image : images) {
            if (TextUtils.isEmpty(image)) continue;
            if (out.length() > 0) out.append('|');
            out.append(image.trim());
        }
        return out.toString();
    }

    private static String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    private static String normalizePhone(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static class UserData {
        public int id;
        public String email;
        public String fullName;
        public String phone;
        public float rating;

        public UserData(int id, String email, String fullName, String phone, float rating) {
            this.id = id;
            this.email = email;
            this.fullName = fullName;
            this.phone = phone;
            this.rating = rating;
        }
    }

    public static class MessageData {
        public int messageId;
        public String senderId;
        public String receiverId;
        public int listingId;
        public String messageText;
        public long createdAt;

        public MessageData(int messageId, String senderId, String receiverId, int listingId,
                           String messageText, long createdAt) {
            this.messageId = messageId;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.listingId = listingId;
            this.messageText = messageText;
            this.createdAt = createdAt;
        }
    }

    public static class ConversationData {
        public String otherUserId;
        public String otherUserName;
        public int listingId;
        public String lastMessage;
        public long lastMessageTime;
        public int unreadCount;

        public ConversationData(String otherUserId, String otherUserName, int listingId,
                                String lastMessage, long lastMessageTime, int unreadCount) {
            this.otherUserId = otherUserId;
            this.otherUserName = otherUserName;
            this.listingId = listingId;
            this.lastMessage = lastMessage;
            this.lastMessageTime = lastMessageTime;
            this.unreadCount = unreadCount;
        }
    }

    public static class ListingData {
        public int listingId;
        public String sellerId;
        public String title;
        public String category;
        public String condition;
        public String description;
        public String price;
        public int quantity;
        public String status;
        public float rating;
        public String imageUrl;
        public List<String> imageGallery;

        public ListingData(int listingId, String sellerId, String title, String category,
                           String condition, String description, String price, int quantity,
                           String status, float rating, String imageUrl, List<String> imageGallery) {
            this.listingId = listingId;
            this.sellerId = sellerId;
            this.title = title;
            this.category = category;
            this.condition = condition;
            this.description = description;
            this.price = price;
            this.quantity = quantity;
            this.status = status;
            this.rating = rating;
            this.imageUrl = imageUrl;
            this.imageGallery = imageGallery;
        }
    }
}
