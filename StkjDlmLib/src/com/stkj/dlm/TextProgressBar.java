package com.stkj.dlm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ProgressBar;

/**
 * @author jarrah
 *  文字居中的progress bar
 *  
 *  样式要使用
 *	style="@android:style/Widget.ProgressBar.Horizontal"
 * 	android:progressDrawable="@drawable/green_progress_drawable"
 */
public class TextProgressBar extends ProgressBar {
	
	private final int DEFALUT_TEXT_COLOR = Color.BLACK;
	
	private Paint mTextPaint;
	private float mTextSize;
	private int mTextColor = DEFALUT_TEXT_COLOR;

	private int mTextDrawX;
	private int mTextDrawY;

	private Rect mTextBounds;

	public TextProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		initWith(context);
	}

	public TextProgressBar(Context context) {
		super(context);
		initWith(context);
	}

	public TextProgressBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initWith(context);
	}

	private void initWith(Context context) {
		
		mTextSize = sp2px(12);//默认字体大小12sp
		
		mTextPaint = new Paint();
		mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setColor(mTextColor);
		mTextPaint.setTextSize(mTextSize);
		
		mTextBounds = new Rect();
	}

	@Override
	protected synchronized void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		//动态计算需要绘制的text的区域
		mTextPaint.getTextBounds(getProgressText(), 0, getProgressText().length(), mTextBounds);
		
		float textWidth = mTextPaint.measureText(getProgressText());
		
		//获取绘制text的坐标
		mTextDrawX = (int) (getMeasuredWidth() / 2 - textWidth / 2);
		mTextDrawY = (int) (getMeasuredHeight() / 2 - mTextBounds.centerY());
		
		canvas.drawText(getProgressText(), mTextDrawX, mTextDrawY, mTextPaint);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
	}
	
	private String getProgressText() {
		return getProgress() + "%";
	}
	
	public float sp2px(float sp){
        final float scale = getResources().getDisplayMetrics().scaledDensity;
        return sp * scale;
    }

}
