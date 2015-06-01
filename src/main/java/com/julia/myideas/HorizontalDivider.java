package com.julia.myideas;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.View;

public class HorizontalDivider extends View {
	Paint paint = new Paint();
	public HorizontalDivider(Context context) {
		super(context);
		paint.setColor(Color.LTGRAY);
		paint.setStrokeWidth(strokeWidth);
		paint.setStyle(Style.STROKE);
	}

	int padding = 200;
	int strokeWidth = 3;
	
	protected void onDraw(android.graphics.Canvas canvas) {
		canvas.drawLine(padding, canvas.getHeight() / 2, canvas.getWidth() - padding, canvas.getHeight() / 2, paint);
	};
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		setMeasuredDimension(parentWidth, 30);
	}
}
