package com.example.nkbtradesphere.ui.product;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.data.model.Product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.nkbtradesphere.util.RatingFormat;
import com.example.nkbtradesphere.util.WishlistAnimator;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ProductDetailBottomSheet extends BottomSheetDialogFragment {

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }

    // Key used to pass the Product into this fragment
    private static final String ARG_PRODUCT_NAME       = "name";
    private static final String ARG_PRODUCT_PRICE      = "price";
    private static final String ARG_PRODUCT_LOCATION   = "location";
    private static final String ARG_PRODUCT_IMAGE      = "imageUrl";
    private static final String ARG_PRODUCT_DESC       = "description";
    private static final String ARG_PRODUCT_SELLER     = "sellerName";
    private static final String ARG_PRODUCT_RATING     = "sellerRating";
    private static final String ARG_PRODUCT_CONDITION  = "condition";
    private static final String ARG_PRODUCT_CATEGORY   = "category";

    // ── Factory method: call this to create the sheet with a product ──
    public static ProductDetailBottomSheet newInstance(Product product) {
        ProductDetailBottomSheet sheet = new ProductDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT_NAME,      product.getName());
        args.putString(ARG_PRODUCT_PRICE,     product.getPrice());
        args.putString(ARG_PRODUCT_LOCATION,  product.getLocation());
        args.putString(ARG_PRODUCT_IMAGE,     product.getImageUrl());
        args.putString(ARG_PRODUCT_DESC,      product.getDescription());
        args.putString(ARG_PRODUCT_SELLER,    product.getSellerName());
        args.putString(ARG_PRODUCT_RATING,    product.getSellerRating());
        args.putString(ARG_PRODUCT_CONDITION, product.getCondition());
        args.putString(ARG_PRODUCT_CATEGORY,  product.getCategory());
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_product_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── Read arguments ──
        Bundle args        = getArguments();
        String name        = args.getString(ARG_PRODUCT_NAME);
        String price       = args.getString(ARG_PRODUCT_PRICE);
        String location    = args.getString(ARG_PRODUCT_LOCATION);
        String imageUrl    = args.getString(ARG_PRODUCT_IMAGE);
        String description = args.getString(ARG_PRODUCT_DESC);
        String sellerName  = args.getString(ARG_PRODUCT_SELLER);
        String rating      = args.getString(ARG_PRODUCT_RATING);
        String condition   = args.getString(ARG_PRODUCT_CONDITION);
        String category    = args.getString(ARG_PRODUCT_CATEGORY);

        // ── Bind views ──
        ImageView ivImage       = view.findViewById(R.id.ivSheetImage);
        ImageView ivClose       = view.findViewById(R.id.ivSheetClose);
        ImageView ivSave        = view.findViewById(R.id.ivSheetSave);
        TextView  tvPrice       = view.findViewById(R.id.tvSheetPrice);
        TextView  tvName        = view.findViewById(R.id.tvSheetName);
        TextView  tvLocation    = view.findViewById(R.id.tvSheetLocation);
        TextView  tvDesc        = view.findViewById(R.id.tvSheetDesc);
        TextView  tvCondition   = view.findViewById(R.id.tvCondition);
        TextView  tvCategory    = view.findViewById(R.id.tvCategory);
        TextView  tvSellerAv    = view.findViewById(R.id.tvSellerAvatar);
        TextView  tvSellerName  = view.findViewById(R.id.tvSellerName);
        TextView  tvRating      = view.findViewById(R.id.tvSellerRating);
        Button    btnChat       = view.findViewById(R.id.btnChat);

        // ── Populate data ──
        tvPrice.setText(price);
        tvName.setText(name);
        tvLocation.setText("📍 " + location);
        tvDesc.setText(description);
        tvCondition.setText(condition);
        tvCategory.setText(category);
        tvSellerName.setText(sellerName);
        tvRating.setText(RatingFormat.valueOnly(rating));

        // Seller avatar initial
        if (sellerName != null && !sellerName.isEmpty()) {
            tvSellerAv.setText(String.valueOf(sellerName.charAt(0)));
        }

        // Load product image with Glide
        Glide.with(requireContext())
                .load(imageUrl)
                .centerCrop()
                .into(ivImage);

        // ── Close button ──
        ivClose.setOnClickListener(v -> dismiss());

        // ── Save / Wishlist toggle ──
        final boolean[] saved = {false};
        WishlistAnimator.applyBottomSheet(ivSave, saved[0]);

        ivSave.setOnClickListener(v -> {
            saved[0] = !saved[0];
            WishlistAnimator.applyBottomSheet(ivSave, saved[0]);
            WishlistAnimator.playToggleCelebration(ivSave, saved[0]);
            Toast.makeText(requireContext(),
                    saved[0] ? "Saved to wishlist ❤️" : "Removed from wishlist",
                    Toast.LENGTH_SHORT).show();
        });

        // ── Chat button ──
        btnChat.setOnClickListener(v -> {
            // Animate button press
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90)
                    .withEndAction(() ->
                            v.animate().scaleX(1f).scaleY(1f).setDuration(140).start())
                    .start();
            Toast.makeText(requireContext(),
                    "Opening chat with " + sellerName + "…",
                    Toast.LENGTH_SHORT).show();
            // TODO: navigate to MessagesFragment and pass sellerName
        });
    }

    // ── Make the sheet taller: peek at 70% of screen height ──
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int screenHeight = requireContext().getResources()
                    .getDisplayMetrics().heightPixels;
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (screenHeight * 0.90)
            );
        }
    }
}
