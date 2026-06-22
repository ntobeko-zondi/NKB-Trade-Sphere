package com.example.nkbtradesphere.network;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * API Client for connecting to the hosted PHP backend.
 */
public class ApiClient {

    // IMPORTANT: This must be the real public URL where api.php was uploaded in the sgroup2729 file manager.
    // Test it in a browser first: BASE_URL + "?action=ping" must return JSON.
    private static final String BASE_URL = ApiConfig.API_URL;
    private static RequestQueue requestQueue;

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public static void initialize(Context context) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }
    
    private static void makeRequest(String action, @Nullable JSONObject params, final ApiCallback callback) {
        if (requestQueue == null) {
            callback.onError("ApiClient not initialized. App cannot make HTTP requests yet.");
            return;
        }

        String url = BASE_URL + "?action=" + action;

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                responseString -> {
                    try {
                        JSONObject response = new JSONObject(responseString);
                        if (response.optBoolean("success", false)) {
                            callback.onSuccess(response);
                        } else {
                            String error = response.optString("error", "Server returned success=false");
                            if (response.has("detail")) {
                                error = error + ": " + response.optString("detail");
                            }
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        String preview = responseString == null ? "empty response" : responseString.trim();
                        if (preview.length() > 300) {
                            preview = preview.substring(0, 300);
                        }
                        callback.onError("Error parsing server response: " + preview);
                    }
                },
                error -> {
                    String message = null;

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String body = "";
                        if (error.networkResponse.data != null) {
                            body = new String(error.networkResponse.data, StandardCharsets.UTF_8).trim();
                        }

                        if (!body.isEmpty()) {
                            try {
                                JSONObject errorJson = new JSONObject(body);
                                if (errorJson.has("error")) {
                                    message = errorJson.getString("error") + " (HTTP " + statusCode + ")";
                                }
                            } catch (JSONException ignored) {
                                message = "HTTP " + statusCode + ": " + body;
                            }
                        }

                        if (message == null) {
                            message = "HTTP " + statusCode + " from " + BASE_URL;
                        }
                    }

                    if (message == null && error.getMessage() != null) {
                        message = error.getMessage();
                    }

                    if (message == null) {
                        message = "Cannot reach API. Test this URL in your browser: " + BASE_URL + "?action=ping";
                    }

                    callback.onError("Network error: " + message);
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> post = new HashMap<>();
                // Send the action in both the query string and POST body. Some student hosting
                // setups strip query strings during redirects, so this makes requests more robust.
                post.put("action", action);
                if (params == null) {
                    return post;
                }
                Iterator<String> keys = params.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    Object v = params.opt(k);
                    if (v != null) {
                        post.put(k, String.valueOf(v));
                    }
                }
                return post;
            }
        };

        request.setShouldCache(false);

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    // ==================== TEST METHODS ====================

    public static void ping(ApiCallback callback) {
        makeRequest("ping", new JSONObject(), callback);
    }

    public static void dbTest(ApiCallback callback) {
        makeRequest("dbTest", new JSONObject(), callback);
    }

    // ==================== USER METHODS ====================

    public static void registerUser(String userId, String password, String email, String fullName, String phone, String idNumber, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
            params.put("password", password);
            params.put("email", email);
            params.put("full_name", fullName);
            params.put("phone", phone);
            params.put("id_number", idNumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("registerUser", params, callback);
    }

    public static void registerUser(String userId, String password, String email, String fullName, String phone, ApiCallback callback) {
        registerUser(userId, password, email, fullName, phone, "", callback);
    }

    public static void authenticateUser(String userId, String password, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
            params.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("authenticateUser", params, callback);
    }

    public static void getUserDetails(String userId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("getUserDetails", params, callback);
    }

    public static void updateUserProfile(String userId, String email, String fullName, String phone, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
            params.put("email", email);
            params.put("full_name", fullName);
            params.put("phone", phone);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("updateUserProfile", params, callback);
    }

    public static void getUserFullName(String userId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("getUserFullName", params, callback);
    }

    public static void verifyResetDetails(String email, String idNumber, String phone, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("email", email);
            params.put("id_number", idNumber);
            params.put("phone", phone);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("verifyResetDetails", params, callback);
    }

    public static void updateUserPassword(String email, String password, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("email", email);
            params.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("updateUserPassword", params, callback);
    }

    public static void deleteUserAccount(String userId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("deleteUserAccount", params, callback);
    }

    // ==================== LISTINGS METHODS ====================

    public static void createListing(String sellerId, String title, String category, String condition, String description, String price, String imageUrl, int quantity, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("seller_id", sellerId);
            params.put("title", title);
            params.put("category", category);
            params.put("condition", condition);
            params.put("description", description);
            params.put("price", price);
            params.put("image_url", imageUrl);
            params.put("quantity", quantity);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("createListing", params, callback);
    }

    public static void getAllListings(ApiCallback callback) {
        makeRequest("getAllListings", null, callback);
    }

    public static void getListingsByCategory(String category, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("category", category);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("getListingsByCategory", params, callback);
    }

    public static void getSellerListings(String sellerId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("seller_id", sellerId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("getSellerListings", params, callback);
    }

    public static void getListing(int listingId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("listing_id", listingId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("getListing", params, callback);
    }

    public static void updateListing(int listingId, String sellerId, String title, String category, String condition, String description, String price, String imageUrl, int quantity, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("listing_id", listingId);
            params.put("seller_id", sellerId);
            params.put("title", title);
            params.put("category", category);
            params.put("condition", condition);
            params.put("description", description);
            params.put("price", price);
            params.put("image_url", imageUrl);
            params.put("quantity", quantity);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("updateListing", params, callback);
    }

    public static void deleteListing(int listingId, String sellerId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("listing_id", listingId);
            params.put("seller_id", sellerId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("deleteListing", params, callback);
    }

    public static void updateListingQuantity(int listingId, int quantity, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("listing_id", listingId);
            params.put("quantity", quantity);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("updateListingQuantity", params, callback);
    }

    public static void purchaseListing(int listingId, ApiCallback callback) {
        purchaseListing(listingId, "", callback);
    }

    public static void purchaseListing(int listingId, String buyerId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("listing_id", listingId);
            params.put("buyer_id", buyerId == null ? "" : buyerId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("purchaseListing", params, callback);
    }

    public static void getSellerListingCount(String sellerId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("seller_id", sellerId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("getSellerListingCount", params, callback);
    }

    // ==================== MESSAGES METHODS ====================

    public static void sendMessage(String senderId, String receiverId, int listingId, String messageText, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("sender_id", senderId);
            params.put("receiver_id", receiverId);
            params.put("listing_id", listingId);
            params.put("message_text", messageText);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("sendMessage", params, callback);
    }

    public static void getConversation(String userId1, String userId2, int listingId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id_1", userId1);
            params.put("user_id_2", userId2);
            params.put("listing_id", listingId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("getConversation", params, callback);
    }

    public static void getUserConversations(String userId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("getUserConversations", params, callback);
    }

    public static void markConversationAsRead(String userId, String otherUserId, int listingId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
            params.put("other_user_id", otherUserId);
            params.put("listing_id", listingId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("markConversationAsRead", params, callback);
    }

    public static void getUnreadMessageCount(String userId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("getUnreadMessageCount", params, callback);
    }

    // ==================== SAVED ITEMS METHODS ====================

    public static void saveItem(String userId, int listingId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
            params.put("listing_id", listingId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("saveItem", params, callback);
    }

    public static void removeSavedItem(String userId, int listingId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
            params.put("listing_id", listingId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("removeSavedItem", params, callback);
    }

    public static void isSaved(String userId, int listingId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
            params.put("listing_id", listingId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("isSaved", params, callback);
    }

    public static void getSavedItems(String userId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("getSavedItems", params, callback);
    }

    public static void getSavedItemsCount(String userId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("getSavedItemsCount", params, callback);
    }

    // ==================== RATINGS METHODS ====================

    public static void addRating(String raterId, String ratedId, float rating, String comment, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("rater_id", raterId);
            params.put("rated_id", ratedId);
            params.put("rating", rating);
            params.put("comment", comment);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("addRating", params, callback);
    }

    public static void getAverageRating(String userId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("getAverageRating", params, callback);
    }

    public static void getUserRatings(String userId, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("getUserRatings", params, callback);
    }

    public static void rateListing(String buyerId, int listingId, float rating, String comment, ApiCallback callback) {
        JSONObject params = new JSONObject();
        try {
            params.put("buyer_id", buyerId);
            params.put("listing_id", listingId);
            params.put("rating", rating);
            params.put("comment", comment);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        makeRequest("rateListing", params, callback);
    }
}
