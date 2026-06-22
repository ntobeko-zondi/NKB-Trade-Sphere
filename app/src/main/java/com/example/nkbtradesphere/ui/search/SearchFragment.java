package com.example.nkbtradesphere.ui.search;

import com.example.nkbtradesphere.R;
import com.example.nkbtradesphere.data.model.Product;
import com.example.nkbtradesphere.ui.home.HomeSearchOverlay;
import com.example.nkbtradesphere.data.repository.NetworkRepository;
import com.example.nkbtradesphere.ui.main.MainActivity;
import com.example.nkbtradesphere.ui.product.ProductDetailActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.ArrayList;

public class SearchFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Runnable goHome = () -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectHomeTab();
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        goHome.run();
                    }
                });

        HomeSearchOverlay overlay = view.findViewById(R.id.homeSearchOverlay);
        overlay.setProductListener(product ->
                startActivity(ProductDetailActivity.newIntent(requireContext(), product)));
        overlay.configureForPrimaryTab(goHome);
        overlay.showImmediate();

        Context appContext = requireContext().getApplicationContext();
        // Search uses the same hosted PostgreSQL API as the Home and Sell pages.
        new Thread(() -> {
            List<Product> products = new NetworkRepository(appContext).getAllProducts();
            if (isAdded() && getView() != null) {
                getView().post(() -> {
                    if (isAdded()) {
                        overlay.bindCatalog(products);
                    }
                });
            }
        }).start();
    }
}
