package com.imooc.wzy.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.imooc.wzy.R;

import java.util.List;

public class ViewPagerIndicator extends LinearLayout {
    private static final float RADIO_TRIANGLE_WIDTH = 1 / 6F;
    private static final int DEFAULT_TABLE_VISIBLE_COUNT = 4;

    private static final int COLOR_TEXT_NORNAL = Color.parseColor("#FFFFFF");
    private static final int COLOR_TEXT_HIGHLIGHT = Color.parseColor("#FF4CDA0F");


    private Paint mPaint;

    private Path mPath;

    /**
     * 三角下标的宽度
     */
    private int mTriangleWidth;

    /**
     * 三角下标的初始X轴偏移量
     */
    private int mInitTranslationX;

    /**
     * Tab标签页可见标签个数
     */
    private int mTableVisibleCount;

    /**
     * 屏幕宽度(单位:px)
     */
    private int mScreenWidth;

    /**
     * 动态添加Tab Indicator
     */
    private List<String> mTabTitles;

    /**
     * 与自定义控件联动的ViewPager
     */
    private ViewPager mViewPager;

    public ViewPagerIndicator(Context context) {
        this(context, null);
    }

    public ViewPagerIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewPagerIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ViewPagerIndicator);
        for (int i = 0; i < ta.length(); i++) {
            int index = ta.getIndex(i);
            switch (index) {
                case R.styleable.ViewPagerIndicator_visible_tab_count:
                    mTableVisibleCount = ta.getInteger(index, DEFAULT_TABLE_VISIBLE_COUNT);
                    break;
                default:
                    break;
            }
        }
        ta.recycle();

        if (mTableVisibleCount <= 0) {
            mTableVisibleCount = DEFAULT_TABLE_VISIBLE_COUNT;
        }

        // 初始化画笔
        initPaint();

        initData();
    }

    private void initData() {
        // 初始化屏幕宽度
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mScreenWidth = metrics.widthPixels;
    }

    private void initPaint() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setPathEffect(new CornerPathEffect(3));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mTriangleWidth = (int) (w / mTableVisibleCount * RADIO_TRIANGLE_WIDTH);

        mInitTranslationX = (w / mTableVisibleCount - mTriangleWidth) / 2;

        initTriangle();
    }

    /**
     * 初始化Triangle
     */
    private void initTriangle() {
        // 将三角形角度设置为30度
        int mTriagnelHeight = (int) (mTriangleWidth / 2 * Math.tan(Math.PI / 6));

        mPath = new Path();
        mPath.moveTo(0, 0);
        mPath.lineTo(mTriangleWidth, 0);
        mPath.lineTo(mTriangleWidth / 2, mTriagnelHeight * -1);
        mPath.close();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // canvas平移到指定位置
        canvas.save();

        canvas.translate(mInitTranslationX, getHeight());
        canvas.drawPath(mPath, mPaint);

        canvas.restore();

        super.dispatchDraw(canvas);
    }

    public void scroll(int position, float positionOffset) {
        // tabWidth * positionOffset + position * tabWidth + originalOffset

        int tabWidth = getWidth() / mTableVisibleCount;
        mInitTranslationX = (int) (position * tabWidth
                + positionOffset * tabWidth
                + (tabWidth - mTriangleWidth) / 2);

        // 容器移动，当tab处于可见最后一个position时
        //Log.e("TAG", "tabWidth=" + tabWidth + ", visibleCount=" + mTableVisibleCount + ", position=" + position);
        if (getChildCount() > mTableVisibleCount && positionOffset > 0
                && position >= mTableVisibleCount - 1) {
            Log.e("TAG", "scroll by x=" + (tabWidth * positionOffset));
            this.scrollTo((int) ((position + 1 - mTableVisibleCount) * tabWidth + tabWidth * positionOffset)
                    , 0);
        }

        invalidate();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        int tabCount = getChildCount();

        if (tabCount == 0) {
            return;
        }

        for (int i = 0; i < tabCount; i++) {
            View view = getChildAt(i);
            LinearLayout.LayoutParams params = (LayoutParams) view.getLayoutParams();
            params.weight = 0;
            params.width = mScreenWidth / mTableVisibleCount;
            view.setLayoutParams(params);
        }
    }

    /**
     * 动态添加Tab栏标签
     */
    public void setTabItemTitles(List<String> titles) {
        if (titles != null && titles.size() > 0) {
            this.removeAllViews();
            mTabTitles = titles;
            for (int i = 0; i < titles.size(); i ++) {
                addView(generateTitleTextView(titles.get(i), i));
            }
        }
    }

    private View generateTitleTextView(String title, int pos) {
        TextView textView = new TextView(getContext());

        LinearLayout.LayoutParams params = new LayoutParams(mScreenWidth / mTableVisibleCount,
                LayoutParams.MATCH_PARENT);
        textView.setLayoutParams(params);

        textView.setText(title);
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(COLOR_TEXT_NORNAL);
        textView.setTextSize(16);
        textView.setOnClickListener(setTitleItemClickEvent(pos));
        return textView;
    }

    private OnClickListener setTitleItemClickEvent(final int pos) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mViewPager != null) {
                    mViewPager.setCurrentItem(pos);
                }
            }
        };
    }

    /**
     * 关联ViewPager，将ViewPager的滑动在自定义控件内部进行监听
     */
    public void setViewPager(ViewPager viewPager, int position) {
        mViewPager = viewPager;
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                scroll(position, positionOffset);
                if (mPageChangeListener != null) {
                    mPageChangeListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
                }
            }

            @Override
            public void onPageSelected(int position) {
                highLightTabTitle(position);

                if (mPageChangeListener != null) {
                    mPageChangeListener.onPageSelected(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (mPageChangeListener != null) {
                    mPageChangeListener.onPageScrollStateChanged(state);
                }
            }
        });


        mViewPager.setCurrentItem(position);
        highLightTabTitle(position);
    }

    /**
     * 高亮当前Title
     */
    private void highLightTabTitle(int pos) {
        for (int i = 0; i < getChildCount(); i ++) {
            TextView tv = (TextView) getChildAt(i);
            if (i == pos) {
                tv.setTextColor(COLOR_TEXT_HIGHLIGHT);
            } else {
                tv.setTextColor(COLOR_TEXT_NORNAL);
            }
        }
    }

    private OnPageChangeListener mPageChangeListener;

    @SuppressWarnings("unused")
    public void setPageChangeListener(OnPageChangeListener pageChangeListener) {
        this.mPageChangeListener = pageChangeListener;
    }

    public interface OnPageChangeListener {
        void onPageScrolled(int position, float positionOffset, int positionOffsetPixels);

        void onPageSelected(int position);

        void onPageScrollStateChanged(int state);
    }
}
