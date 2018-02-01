package com.gc.triprec;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class AutoFitTextureView extends TextureView {
    private int m_ratioW = 0;
    private int m_ratioH = 0;

    public AutoFitTextureView(Context context) {
        super(context);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setAspectRatio(int width, int height) {

        if ((0 > width) || (0 > height))
            throw new IllegalArgumentException("size cannot be negative!");

        if ((m_ratioW == width) && (m_ratioH == height))
            return;

        m_ratioW = width;
        m_ratioH = height;

        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width   = MeasureSpec.getSize(widthMeasureSpec);
        int height  = MeasureSpec.getSize(heightMeasureSpec);

        if ((0 == m_ratioW) || (0 == m_ratioH)) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * m_ratioW / m_ratioH) {
                setMeasuredDimension(width, width * m_ratioH / m_ratioW);
            } else {
                setMeasuredDimension(height * m_ratioW / m_ratioH, width);
            }
        }
    }
}
