package com.example.nkbtradesphere;

import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nkbtradesphere.database.DatabaseHelper;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.ui.home.CategoryUtils;
import com.bumptech.glide.Glide;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyListingsActivity extends AppCompatActivity {

    private RecyclerView rvListings;
    private LinearLayout emptyState;
    private String currentUserId;
    private final List<DatabaseHelper.ListingData> userListings = new ArrayList<>();
    private ListingsAdapter listingsAdapter;
    private long pendingPhotoEditListingId = -1;
    private ActivityResultLauncher<Intent> pickMultipleImagesLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_listings);
        pickMultipleImagesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null || pendingPhotoEditListingId <= 0) {
                        return;
                    }
                    List<Uri> uris = extractUrisFromResult(result.getData());
                    if (uris.isEmpty()) return;
                    replaceListingPhotos(pendingPhotoEditListingId, uris);
                    pendingPhotoEditListingId = -1;
                }
        );
        
        // Get current user
        SharedPreferences prefs = getSharedPreferences(AppPreferences.PREFS_NKB, 0);
        currentUserId = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");

        // Initialize views
        rvListings = findViewById(R.id.rvListings);
        emptyState = findViewById(R.id.empty_state);
        ImageButton btnBack = findViewById(R.id.btnBack);
        rvListings.setLayoutManager(new LinearLayoutManager(this));
        listingsAdapter = new ListingsAdapter();
        rvListings.setAdapter(listingsAdapter);

        // Set click listener
        btnBack.setOnClickListener(v -> onBackPressed());

        // Load listings
        loadListings();
    }

    private void loadListings() {
        if (TextUtils.isEmpty(currentUserId)) {
            emptyState.setVisibility(View.VISIBLE);
            rvListings.setVisibility(View.GONE);
            return;
        }

        // Load from database in background thread
        new Thread(() -> {
            try {
                DatabaseHelper db = DatabaseHelper.getInstance(MyListingsActivity.this);
                List<DatabaseHelper.ListingData> listings = db.getListingsBySeller(currentUserId);
                userListings.clear();
                
                for (DatabaseHelper.ListingData listing : listings) {
                    userListings.add(listing);
                }
                
                runOnUiThread(() -> {
                    if (userListings.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        rvListings.setVisibility(View.GONE);
                        Toast.makeText(MyListingsActivity.this, "No listings yet", Toast.LENGTH_SHORT).show();
                    } else {
                        listingsAdapter.notifyDataSetChanged();
                        emptyState.setVisibility(View.GONE);
                        rvListings.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                Log.e("MyListingsActivity", "Error loading listings: " + e.getMessage());
            }
        }).start();
    }

    private void showEditListingDialog(DatabaseHelper.ListingData listing) {
        if (listing == null || listing.listingId <= 0) return;

        View form = LayoutInflater.from(this).inflate(R.layout.dialog_edit_listing, null, false);
        EditText titleInput = form.findViewById(R.id.etDialogTitle);
        Spinner categorySpinner = form.findViewById(R.id.spinnerDialogCategory);
        Spinner conditionSpinner = form.findViewById(R.id.spinnerDialogCondition);
        EditText descriptionInput = form.findViewById(R.id.etDialogDescription);
        EditText priceInput = form.findViewById(R.id.etDialogPrice);
        EditText quantityInput = form.findViewById(R.id.etDialogQuantity);

        titleInput.setText(listing.title);
        descriptionInput.setText(listing.description);
        priceInput.setText(listing.price);
        quantityInput.setText(String.valueOf(Math.max(listing.quantity, 0)));

        String[] categories = CategoryUtils.categoryTitlesForSellSpinner();
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(getResources().getColor(R.color.text_primary, null));
                }
                return view;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(getResources().getColor(R.color.text_primary, null));
                    view.setBackground(null);
                }
                return view;
            }
        };
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
        int categoryIndex = 0;
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equalsIgnoreCase(listing.category)) {
                categoryIndex = i;
                break;
            }
        }
        categorySpinner.setSelection(categoryIndex);

        String[] conditions = {"Like New", "Excellent", "Good", "Fair", "Used"};
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, conditions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(getResources().getColor(R.color.text_primary, null));
                }
                return view;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(getResources().getColor(R.color.text_primary, null));
                    view.setBackground(null);
                }
                return view;
            }
        };
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        conditionSpinner.setAdapter(conditionAdapter);
        int conditionIndex = 0;
        for (int i = 0; i < conditions.length; i++) {
            if (conditions[i].equalsIgnoreCase(listing.condition)) {
                conditionIndex = i;
                break;
            }
        }
        conditionSpinner.setSelection(conditionIndex);

        new AlertDialog.Builder(this)
                .setTitle("Edit listing")
                .setMessage("Update or delete this item")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete", (dialog, which) -> showDeleteConfirmDialog(listing))
                .setPositiveButton("Save changes", (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String category = String.valueOf(categorySpinner.getSelectedItem());
                    String condition = String.valueOf(conditionSpinner.getSelectedItem());
                    String description = descriptionInput.getText().toString().trim();
                    String price = priceInput.getText().toString().trim();
                    String qtyValue = quantityInput.getText().toString().trim();

                    if (TextUtils.isEmpty(title) || TextUtils.isEmpty(category) || TextUtils.isEmpty(price)) {
                        Toast.makeText(this, "Title, category and price are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int quantity;
                    try {
                        quantity = Integer.parseInt(qtyValue);
                    } catch (Exception e) {
                        Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (quantity < 0) {
                        Toast.makeText(this, "Quantity cannot be negative", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    DatabaseHelper db = DatabaseHelper.getInstance(MyListingsActivity.this);
                    if (db.updateListing(listing.listingId, currentUserId, title, category, condition, description, price, quantity)) {
                        Toast.makeText(MyListingsActivity.this, "Listing updated", Toast.LENGTH_SHORT).show();
                        loadListings();
                    } else {
                        Toast.makeText(MyListingsActivity.this, "Could not update listing", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void startPhotoEdit(DatabaseHelper.ListingData listing) {
        pendingPhotoEditListingId = listing.listingId;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickMultipleImagesLauncher.launch(Intent.createChooser(intent, "Select listing photos"));
    }

    private List<Uri> extractUrisFromResult(Intent data) {
        List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                if (uri != null) uris.add(uri);
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        return uris;
    }

    private void replaceListingPhotos(long listingId, List<Uri> uris) {
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        List<String> newImagesBase64 = new ArrayList<>();
        try {
            for (Uri imageUri : uris) {
                try {
                    getContentResolver().takePersistableUriPermission(
                            imageUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (SecurityException ignored) {
                    // Some providers grant one-time read permission only.
                }
                Bitmap bitmap;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageUri);
                    bitmap = ImageDecoder.decodeBitmap(source);
                } else {
                    InputStream in = getContentResolver().openInputStream(imageUri);
                    if (in == null) throw new IOException("Unable to open selected image");
                    try {
                        bitmap = BitmapFactory.decodeStream(in);
                    } finally {
                        in.close();
                    }
                }
                if (bitmap != null) {
                    String encodedImage = bitmapToBase64(bitmap);
                    if (!TextUtils.isEmpty(encodedImage)) {
                        newImagesBase64.add(encodedImage);
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load selected images", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newImagesBase64.isEmpty()) {
            Toast.makeText(this, "No valid images selected", Toast.LENGTH_SHORT).show();
            return;
        }
        if (db.replaceListingImages(listingId, newImagesBase64)) {
            Toast.makeText(this, "Listing photos updated (" + newImagesBase64.size() + ")", Toast.LENGTH_SHORT).show();
            loadListings();
        } else {
            Toast.makeText(this, "Could not update photos", Toast.LENGTH_SHORT).show();
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    private void showDeleteConfirmDialog(DatabaseHelper.ListingData listing) {
        new AlertDialog.Builder(this)
                .setTitle("Delete listing")
                .setMessage("Delete \"" + listing.title + "\"? This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    DatabaseHelper db = DatabaseHelper.getInstance(MyListingsActivity.this);
                    if (db.deleteListing(listing.listingId)) {
                        Toast.makeText(MyListingsActivity.this, "Listing deleted", Toast.LENGTH_SHORT).show();
                        loadListings();
                    } else {
                        Toast.makeText(MyListingsActivity.this, "Could not delete listing", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private class ListingsAdapter extends RecyclerView.Adapter<ListingsAdapter.VH> {
        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_listing, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DatabaseHelper.ListingData listing = userListings.get(position);
            String price = listing.quantity <= 0
                    ? "Out of stock"
                    : (listing.price.startsWith("R") ? listing.price : "R" + listing.price);
            String meta = price + "  •  Qty " + listing.quantity + "  •  " + listing.category
                    + "  •  " + listing.condition + "  •  ★" + String.format(Locale.US, "%.1f", listing.rating);
            holder.tvTitle.setText(listing.title);
            holder.tvMeta.setText(meta);
            int imageCount = listing.imageGallery == null ? 0 : listing.imageGallery.size();
            holder.tvPhotoCount.setText(imageCount + " photos");
            String thumbnail = !TextUtils.isEmpty(listing.imageUrl)
                    ? listing.imageUrl
                    : (imageCount > 0 ? listing.imageGallery.get(0) : "");
            if (!TextUtils.isEmpty(thumbnail)) {
                Glide.with(holder.ivThumb)
                        .load(thumbnail)
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(holder.ivThumb);
            } else {
                holder.ivThumb.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            holder.btnEditDetails.setOnClickListener(v -> showEditListingDialog(listing));
            holder.btnEditPhotos.setOnClickListener(v -> startPhotoEdit(listing));
            holder.btnDelete.setOnClickListener(v -> showDeleteConfirmDialog(listing));
        }

        @Override
        public int getItemCount() {
            return userListings.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMeta, tvPhotoCount;
            android.widget.ImageView ivThumb;
            View btnEditDetails, btnEditPhotos, btnDelete;

            VH(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvListingTitle);
                tvMeta = itemView.findViewById(R.id.tvListingMeta);
                tvPhotoCount = itemView.findViewById(R.id.tvPhotoCount);
                ivThumb = itemView.findViewById(R.id.ivListingThumb);
                btnEditDetails = itemView.findViewById(R.id.btnEditDetails);
                btnEditPhotos = itemView.findViewById(R.id.btnEditPhotos);
                btnDelete = itemView.findViewById(R.id.btnDeleteListing);
            }
        }
    }
}