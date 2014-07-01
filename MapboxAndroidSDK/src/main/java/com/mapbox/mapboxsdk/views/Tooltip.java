package com.mapbox.mapboxsdk.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mapbox.mapboxsdk.R;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.util.constants.MapViewConstants;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.view.ViewHelper;


/**
 * A tooltip view
 */
public class Tooltip {

    public interface TooltipInfoView {
        public void setText(final String title);
        public void setDescription(final String snippet);
        public void setSubDescription(final String subDesc);
    }

    private Marker boundMarker;

    private MapView mMapView;
    private boolean mIsVisible;
    private ViewGroup mTooltipView;
    private TooltipInfoView mInfoView;
    private Marker.HotspotPlace mHostpotPlace = Marker.HotspotPlace.TOP_CENTER;
    private int mLayoutAnchor = MapView.LayoutParams.BOTTOM_CENTER;

    public void setLayoutAnchor(int anchor) {
        mLayoutAnchor = anchor;
    }
    public Marker.HotspotPlace getHotspot() {
        return mHostpotPlace;
    }

    public class TooltipView extends RelativeLayout {
        private int mBackgroundColor = Color.WHITE;
        private Paint mPaint;
        private Path mPath = new Path();
        private int mArrowHeight;
        private int mBorderRadius;

        public TooltipView(Context context) {
            super(context);
            setWillNotDraw(false);
            setArrowHeightDp(10);
            setBorderRadiusDp(4);
            this.mPaint = new Paint();
            this.mPaint.setColor(mBackgroundColor);
            this.mPaint.setAntiAlias(true);
            this.mPaint.setStrokeWidth(0.0f);
            this.mPaint.setStyle(Paint.Style.FILL);
        }

        private void setScale(final float scale) {
            ViewHelper.setScaleX(this, scale);
            ViewHelper.setScaleY(this, scale);
        }

        private float getScale() {
            return ViewHelper.getScaleX(this);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (Tooltip.this.boundMarker.isAnimated() == false) return;
            //make sure the view is measured first (almost never the case)
            int widthMeasureSpec = MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED);
            int heightMeasureSpec = MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED);
            measure(widthMeasureSpec, heightMeasureSpec);
            final int w = getMeasuredWidth();
            final int h = getMeasuredHeight();
            ViewHelper.setPivotX(this, w / 2);
            ViewHelper.setPivotY(this, h);
            ObjectAnimator anim = ObjectAnimator.ofFloat(this, "scale", 0.01f, 1.0f);
            anim.setInterpolator(new OvershootInterpolator());
            anim.setDuration(200);
            anim.start();
        }

        public void closeMeAnimated() {
            ObjectAnimator anim = ObjectAnimator.ofFloat(this, "scale", 1.0f, 0.01f);
            anim.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    ((ViewGroup) getParent()).removeView(TooltipView.this);
                    super.onAnimationEnd(animation);
                }
            });
            anim.setDuration(100);
            anim.start();
        }

        @Override
        protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            final int w = getMeasuredWidth();
            final int h = getMeasuredHeight();
            final int w_2 = w /2;
            final int height = h - mArrowHeight;

            mPath.rewind();
            if (mBorderRadius == 0) {
                mPath.addRect(0, 0, w, height, Path.Direction.CW);
            }
            else {
                mPath.addRoundRect(new RectF(0, 0, w, height), mBorderRadius, mBorderRadius, Path.Direction.CW);
            }
            mPath.moveTo(w_2 - mArrowHeight, height);
            mPath.lineTo(w_2, h);
            mPath.lineTo(w_2 + mArrowHeight, height);
            mPath.lineTo(w_2 - mArrowHeight, height);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawPath(mPath, this.mPaint);
        }

        public void setBackgroundColor(final int color) {
            mBackgroundColor = color;
            invalidate();
        }

        public void setArrowHeightDp(final float height) {
            final float scale = getResources().getDisplayMetrics().density;
            mArrowHeight = (int) (height * scale);
            setPadding(0, 0, 0, mArrowHeight);
        }

        public void setBorderRadiusDp(final float radius) {
            final float scale = getResources().getDisplayMetrics().density;
            mBorderRadius = (int) (radius * scale);
            requestLayout();
        }
    }


    public static class InfoView extends LinearLayout implements TooltipInfoView {
        int mTitleId = 0;
        int mDescriptionId = 0;
        int mSubDescriptionId = 0;
        int mImageId = 0;

        public InfoView(Context context) {
            super(context);
            if (mTitleId == 0) {
                setResIds(context);
            }
        }

        public InfoView(Context context, AttributeSet attrs) {
            super(context, attrs);
            if (mTitleId == 0) {
                setResIds(context);
            }
        }

        /**
         * Given a context, set the resource ids for the layout
         * of the Tooltip.
         * @param context
         */
        private void setResIds(Context context) {
            String packageName = context.getPackageName(); //get application package name
            mTitleId = context.getResources().getIdentifier("id/tooltip_title", null, packageName);
            mDescriptionId =
                    context.getResources().getIdentifier("id/tooltip_description", null, packageName);
            mSubDescriptionId = context.getResources()
                    .getIdentifier("id/tooltip_subdescription", null, packageName);
            mImageId = context.getResources().getIdentifier("id/tooltip_image", null, packageName);
        }


        public void setText(final String title) {
            ((TextView) findViewById(mTitleId /*R.id.title*/)).setText(title);
        }

        public void setDescription(final String snippet) {
            ((TextView) findViewById(mDescriptionId /*R.id.description*/)).setText(snippet);
        }

        public void setSubDescription(final String subDesc) {
            TextView subDescText = (TextView) findViewById(mSubDescriptionId);
            if ("".equals(subDesc)) {
                subDescText.setVisibility(View.GONE);
            } else {
                subDescText.setText(subDesc);
                subDescText.setVisibility(View.VISIBLE);
            }
        }
    }


    protected ViewGroup createTooltipView(final Context context) {
        TooltipView view = new TooltipView(context);
        return view;
    }

    protected TooltipInfoView createInfoView(final Context context) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        TooltipInfoView view = (TooltipInfoView) inflater.inflate(R.layout.tooltip, null);
        return view;
    }


    public Tooltip(final Context context) {
        mIsVisible = false;

    }

    /**
     * open the window at the specified position.
     *
     * @param object the graphical object on which is hooked the view
     * @param position to place the window on the map
     * @param offsetX (&offsetY) the offset of the view to the position, in pixels.
     * This allows to offset the view from the object position.
     * @return this infowindow
     */
    public Tooltip open(Marker object, LatLng position, int offsetX, int offsetY) {
        setBoundMarker(object);
        if (mTooltipView == null) {
            mTooltipView = createTooltipView(mMapView.getContext());
        }

        if (mInfoView == null) {
            mInfoView = createInfoView(mMapView.getContext());
            // default behavior: close it when clicking on the tooltip:
//            mInfoView.setOnTouchListener(new View.OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent e) {
//                    if (e.getAction() == MotionEvent.ACTION_UP) {
//                        close();
//                    }
//                    return true;
//                }
//            });
        }
        if (((View) mInfoView).getParent() != mTooltipView) {
            mTooltipView.addView((View) mInfoView);
        }

        willOpen(object);
        MapView.LayoutParams lp = new MapView.LayoutParams(MapView.LayoutParams.WRAP_CONTENT,
                MapView.LayoutParams.WRAP_CONTENT, position, mLayoutAnchor,
                offsetX, offsetY);
        close(); //if it was already opened
        mMapView.addTooltipView(mTooltipView, lp);
        mIsVisible = true;
        return this;
    }

    /**
     * Close this Tooltip if it is visible, otherwise don't do anything.
     * @return this info window
     */
    public Tooltip close() {
        if (mIsVisible) {
            mIsVisible = false;
            if (this.boundMarker.isAnimated() && mTooltipView instanceof TooltipView) {
                ((TooltipView)mTooltipView).closeMeAnimated();
            }
            else {
                ((ViewGroup) mTooltipView.getParent()).removeView(mTooltipView);
            }
            this.boundMarker.blur();
            setBoundMarker(null);
            onClose();
        }
        return this;
    }

    /**
     * Returns the Android view. This allows to set its content.
     *
     * @return the Android view
     */
    public TooltipInfoView getInfoView() {return mInfoView;}
    public ViewGroup getTooltipView() {
        return mTooltipView;
    }

    /**
     * Returns the mapView this Tooltip is bound to
     *
     * @return the mapView
     */
    public MapView getMapView() {
        return mMapView;
    }

    public void setMapView(final MapView mapView)  {
        mMapView = mapView;
    }



    /**
     * Constructs the view that is displayed when the Tooltip opens.
     * This retrieves data from overlayItem and shows it in the tooltip.
     *
     * @param overlayItem the tapped overlay item
     */
    public void willOpen(Marker overlayItem) {
        mInfoView.setText(overlayItem.getTitle());
        mInfoView.setDescription(overlayItem.getDescription());
        mInfoView.setSubDescription(overlayItem.getSubDescription());
    }

    public void onClose() {
        //by default, do nothing
    }

    public Tooltip setBoundMarker(Marker aBoundMarker) {
        this.boundMarker = aBoundMarker;
        return this;
    }

    public Marker getBoundMarker() {
        return boundMarker;
    }
}
