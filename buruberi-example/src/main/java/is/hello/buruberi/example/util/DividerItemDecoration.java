package is.hello.buruberi.example.util;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import is.hello.buruberi.example.R;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {
    private final Paint dividerPaint = new Paint();
    private final int dividerHeight;
    private final int leftInset;

    public DividerItemDecoration(@NonNull Resources resources, boolean includeInset) {
        this.dividerHeight = resources.getDimensionPixelSize(R.dimen.divider_size);
        if (includeInset) {
            this.leftInset = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        } else {
            this.leftInset = 0;
        }

        @SuppressWarnings("deprecation") // ResourcesCompat hasn't caught up yet
        final @ColorInt int color = resources.getColor(R.color.divider);
        dividerPaint.setColor(color);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        final int lastPosition = parent.getAdapter().getItemCount() - 1;
        final int adapterPosition = parent.getChildAdapterPosition(view);
        if (adapterPosition < lastPosition) {
            outRect.bottom += dividerHeight;
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int lastPosition = parent.getAdapter().getItemCount() - 1;
        for (int i = 0, count = parent.getChildCount(); i < count; i++) {
            final View child = parent.getChildAt(i);
            final int adapterPosition = parent.getChildAdapterPosition(child);
            if (adapterPosition >= lastPosition) {
                break;
            }

            dividerPaint.setAlpha(Math.round(255f * child.getAlpha()));
            c.drawRect(child.getLeft() + leftInset, child.getBottom(),
                       child.getRight(), child.getBottom() + dividerHeight,
                       dividerPaint);
        }
    }
}
