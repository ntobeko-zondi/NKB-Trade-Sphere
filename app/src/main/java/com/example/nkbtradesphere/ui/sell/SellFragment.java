package com.example.nkbtradesphere.ui.sell;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.core.AppPreferences;
import com.example.nkbtradesphere.network.ApiClient;
import com.example.nkbtradesphere.ui.home.CategoryUtils;
import com.example.nkbtradesphere.ui.main.MainActivity;

import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SellFragment extends Fragment {

    private EditText etTitle;
    private EditText etPrice;
    private EditText etDescription;
    private EditText etQuantity;

    private Spinner spinnerCategory;
    private Spinner spinnerCondition;

    private ImageView ivProductImage;
    private View imagePromptLayout;
    private RecyclerView rvImageThumbs;
    private TextView tvImageCount;
    private Button btnPickImage;
    private MaterialButton btnSubmitListing;
    private ProgressBar progressBar;

    private static final int MAX_LISTING_IMAGE_DIMENSION = 900;
    private static final int THUMBNAIL_IMAGE_DIMENSION = 280;
    private static final int LISTING_IMAGE_JPEG_QUALITY = 70;

    private final List<String> selectedImagesBase64 = new ArrayList<>();

    private final List<Bitmap> selectedThumbBitmaps = new ArrayList<>();
    private ActivityResultLauncher<Intent> pickMultipleImagesLauncher;
    private ThumbAdapter thumbAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickMultipleImagesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) {
                        return;
                    }
                    List<Uri> uris = extractUrisFromResult(result.getData());
                    if (!uris.isEmpty()) loadImagesFromUris(uris);
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sell, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etTitle = view.findViewById(R.id.et_title);
        etPrice = view.findViewById(R.id.et_price);
        etDescription = view.findViewById(R.id.et_description);
        etQuantity = view.findViewById(R.id.et_quantity);
        etQuantity.setText("0");

        spinnerCategory = view.findViewById(R.id.spinner_category);
        spinnerCondition = view.findViewById(R.id.spinner_condition);

        ivProductImage = view.findViewById(R.id.iv_product_image);
        imagePromptLayout = view.findViewById(R.id.layout_pick_image);
        rvImageThumbs = view.findViewById(R.id.rv_image_thumbs);
        tvImageCount = view.findViewById(R.id.tv_image_count);
        btnPickImage = view.findViewById(R.id.btn_pick_image);
        btnSubmitListing = view.findViewById(R.id.btn_submit_listing);
        progressBar = view.findViewById(R.id.progress_bar);

        setupCategorySpinner();
        setupConditionSpinner();
        setupThumbRecycler();

        btnPickImage.setOnClickListener(v -> pickImages());
        btnSubmitListing.setOnClickListener(v -> submitListing());

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).selectHomeTab();
            }
        });
    }

    private void setupCategorySpinner() {
        String[] categories = CategoryUtils.categoryTitlesForSellSpinner();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupConditionSpinner() {
        String[] conditions = {
                "Like New",
                "Good",
                "Fair",
                "Used",
                "Excellent"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                conditions
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCondition.setAdapter(adapter);
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickMultipleImagesLauncher.launch(Intent.createChooser(intent, "Select photos"));
    }

    @NonNull
    private List<Uri> extractUrisFromResult(@NonNull Intent data) {
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

    private void setupThumbRecycler() {
        thumbAdapter = new ThumbAdapter();
        rvImageThumbs.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvImageThumbs.setAdapter(thumbAdapter);

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT | ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false;
                Collections.swap(selectedImagesBase64, from, to);
                Collections.swap(selectedThumbBitmaps, from, to);
                thumbAdapter.notifyItemMoved(from, to);
                if (!selectedThumbBitmaps.isEmpty()) {
                    ivProductImage.setImageBitmap(selectedThumbBitmaps.get(0));
                }
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        });
        helper.attachToRecyclerView(rvImageThumbs);
    }

    private void loadImagesFromUris(@NonNull List<Uri> uris) {
        selectedImagesBase64.clear();
        selectedThumbBitmaps.clear();

        try {
            for (Uri imageUri : uris) {
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(
                            imageUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (SecurityException ignored) {
                    // Some providers only grant temporary read access.
                }
                Bitmap bitmap;

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    ImageDecoder.Source source =
                            ImageDecoder.createSource(requireContext().getContentResolver(), imageUri);
                    bitmap = ImageDecoder.decodeBitmap(source);
                } else {
                    InputStream in = requireContext().getContentResolver().openInputStream(imageUri);

                    if (in == null) {
                        throw new IOException("Unable to open selected image");
                    }

                    try {
                        bitmap = BitmapFactory.decodeStream(in);
                    } finally {
                        in.close();
                    }
                }

                if (bitmap == null) {
                    throw new IOException("Image decode failed");
                }

                Bitmap listingBitmap = scaleBitmapIfNeeded(bitmap, MAX_LISTING_IMAGE_DIMENSION);
                Bitmap thumbBitmap = scaleBitmapIfNeeded(listingBitmap, THUMBNAIL_IMAGE_DIMENSION);

                selectedImagesBase64.add(bitmapToBase64(listingBitmap));
                selectedThumbBitmaps.add(thumbBitmap);

                if (selectedImagesBase64.size() == 1) {
                    ivProductImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivProductImage.setImageBitmap(thumbBitmap);
                }
            }

            imagePromptLayout.setVisibility(View.GONE);
            tvImageCount.setText(selectedImagesBase64.size() + " photos selected");
            thumbAdapter.notifyDataSetChanged();

            Toast.makeText(
                    requireContext(),
                    selectedImagesBase64.size() + " image(s) selected",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    private Bitmap scaleBitmapIfNeeded(@NonNull Bitmap source, int maxDimension) {
        int width = source.getWidth();
        int height = source.getHeight();
        int largestSide = Math.max(width, height);
        if (largestSide <= maxDimension) {
            return source;
        }

        float scale = maxDimension / (float) largestSide;
        int newWidth = Math.max(1, Math.round(width * scale));
        int newHeight = Math.max(1, Math.round(height * scale));
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true);
    }

    private String bitmapToBase64(@NonNull Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                LISTING_IMAGE_JPEG_QUALITY,
                byteArrayOutputStream
        );

        byte[] byteArray = byteArrayOutputStream.toByteArray();

        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private void submitListing() {
        String title = etTitle.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String condition = spinnerCondition.getSelectedItem().toString();
        String description = etDescription.getText().toString().trim();
        String quantityText = etQuantity.getText().toString().trim();

        int quantity;

        try {
            quantity = TextUtils.isEmpty(quantityText)
                    ? 0
                    : Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            Toast.makeText(
                    requireContext(),
                    "Quantity must be a valid number",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(
                    requireContext(),
                    "Please enter item title",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (TextUtils.isEmpty(price)) {
            Toast.makeText(
                    requireContext(),
                    "Please enter price",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (selectedImagesBase64.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "Please select an image",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (quantity <= 0) {
            Toast.makeText(
                    requireContext(),
                    "Quantity must be at least 1 to create a listing",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmitListing.setEnabled(false);

        SharedPreferences prefs = requireContext().getSharedPreferences(
                AppPreferences.PREFS_NKB,
                0
        );

        String userId = prefs.getString(AppPreferences.KEY_USER_EMAIL, "");

        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(
                    requireContext(),
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();

            progressBar.setVisibility(View.GONE);
            btnSubmitListing.setEnabled(true);
            return;
        }

        String imagePayload = TextUtils.join("|", selectedImagesBase64);
        ApiClient.initialize(requireContext());
        ApiClient.createListing(userId, title, category, condition, description, price, imagePayload, quantity,
                new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(org.json.JSONObject response) {
                        Toast.makeText(requireContext(), "Listing created successfully!", Toast.LENGTH_SHORT).show();
                        clearForm();
                        progressBar.setVisibility(View.GONE);
                        btnSubmitListing.setEnabled(true);
                        if (requireActivity() instanceof MainActivity) {
                            ((MainActivity) requireActivity()).selectHomeTab();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(requireContext(), "Failed to create listing: " + error, Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        btnSubmitListing.setEnabled(true);
                    }
                });
    }

    private void clearForm() {
        etTitle.setText("");
        etPrice.setText("");
        etDescription.setText("");
        etQuantity.setText("0");

        ivProductImage.setImageDrawable(null);
        imagePromptLayout.setVisibility(View.VISIBLE);
        selectedThumbBitmaps.clear();
        if (thumbAdapter != null) thumbAdapter.notifyDataSetChanged();
        tvImageCount.setText("0 photos selected");

        selectedImagesBase64.clear();

        spinnerCategory.setSelection(0);
        spinnerCondition.setSelection(0);
    }

    private class ThumbAdapter extends RecyclerView.Adapter<ThumbAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView thumb = new ImageView(parent.getContext());
            int size = (int) (64 * getResources().getDisplayMetrics().density);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(size, size);
            params.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
            thumb.setLayoutParams(params);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setBackgroundResource(R.drawable.rounded_input_bg);
            thumb.setPadding(2, 2, 2, 2);
            return new VH(thumb);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.thumb.setImageBitmap(selectedThumbBitmaps.get(position));
            holder.thumb.setOnClickListener(v -> {
                ivProductImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ivProductImage.setImageBitmap(selectedThumbBitmaps.get(position));
            });
        }

        @Override
        public int getItemCount() {
            return selectedThumbBitmaps.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView thumb;
            VH(@NonNull View itemView) {
                super(itemView);
                thumb = (ImageView) itemView;
            }
        }
    }
}