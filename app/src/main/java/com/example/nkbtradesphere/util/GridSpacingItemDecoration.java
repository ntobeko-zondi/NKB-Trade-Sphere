package com.example.nkbtradesphere.util;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Gaps between cells for {@link GridLayoutManager}; reads span count from the attached manager
 * so it stays correct when span changes (e.g. rotation).
 */
public final class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private final int spacingPx;

    public GridSpacingItemDecoration(int spacingPx) {
        this.spacingPx = spacingPx;
    }

    @Override
    public void getItemOffsets(
            @NonNull Rect outRect,
            @NonNull View view,
            @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state) {
        RecyclerView.LayoutManager lm = parent.getLayoutManager();
        if (!(lm instanceof GridLayoutManager)) {
            return;
        }
        int spanCount = ((GridLayoutManager) lm).getSpanCount();
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) {
            return;
        }

        outRect.bottom = spacingPx;

        if (spanCount <= 1) {
            outRect.left = 0;
            outRect.right = 0;
            if (position == 0) {
                outRect.top = spacingPx;
            }
            return;
        }

        int column = position % spanCount;
        outRect.left = column * spacingPx / spanCount;
        outRect.right = spacingPx - (column + 1) * spacingPx / spanCount;
        if (position < spanCount) {
            outRect.top = spacingPx;
        }
    }
}
