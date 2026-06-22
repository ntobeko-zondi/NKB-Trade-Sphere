package com.example.nkbtradesphere.ui.home;

import com.example.nkbtradesphere.data.model.Product;
import com.example.nkbtradesphere.database.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Converts API listing rows into Product objects used by Home/Search/Product Detail UI. */
public final class ListingProductMapper {

    private ListingProductMapper() {}

    public static Product fromListing(DatabaseHelper db, DatabaseHelper.ListingData listing) {
        DatabaseHelper.UserData seller = db.getUserByEmail(listing.sellerId);
        String sellerName = seller != null && seller.fullName != null && !seller.fullName.trim().isEmpty()
                ? seller.fullName
                : listing.sellerId;
        String sellerRating = String.format(Locale.US, "%.1f", seller != null ? seller.rating : 0f);

        return new Product(
                listing.listingId,
                listing.sellerId,
                listing.title,
                listing.price,
                "Local",
                listing.imageUrl,
                listing.description,
                sellerName,
                sellerRating,
                listing.condition,
                listing.category,
                null,
                String.format(Locale.US, "%.1f", listing.rating),
                "",
                "",
                listing.imageGallery,
                listing.quantity,
                listing.status
        );
    }

    public static List<Product> fromListings(DatabaseHelper db, List<DatabaseHelper.ListingData> listings) {
        List<Product> products = new ArrayList<>();
        if (listings == null) {
            return products;
        }
        for (DatabaseHelper.ListingData listing : listings) {
            products.add(fromListing(db, listing));
        }
        return products;
    }
}
