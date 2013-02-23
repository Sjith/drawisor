package com.tripadvisor.drawisor.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.googlecode.androidannotations.annotations.EView;
import com.tripadvisor.drawisor.entities.Path;
import com.tripadvisor.drawisor.entities.Point;

@EView
public class CanvasView extends SurfaceView implements SurfaceHolder.Callback {

	SurfaceHolder surfaceHolder;

	boolean isReady;

	List<Path> paths;

	List<OnTouchListener> touchListeners;

	Paint paint;

	private Bitmap canvasBitmap;
	private Canvas canvas;
	private Matrix identityMatrix;


	public CanvasView(Context context) {
		super(context);
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);
		isReady = false;
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Style.STROKE);
		paint.setStrokeCap(Cap.ROUND);
		touchListeners = new ArrayList<View.OnTouchListener>();
	}

	public void drawLine(Point p1, Point p2) {
		canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint);

		refreshCanvas();
	}

	public void loadPaths(List<Path> paths) {
		this.paths = paths;
		clearAndDrawPaths(paths.size());
	}

	public void clearAndDrawPaths(int undoPosition) {
		if (!isReady || paths == null) {
			return;
		}

		int oldcolor = paint.getColor();
		float oldsize = paint.getStrokeWidth();

		canvas.drawColor(Color.WHITE);
		for (int i = 0; i < undoPosition; i++) {
			Path path = paths.get(i);
			drawPath(canvas, path);
		}
		refreshCanvas();

		paint.setColor(oldcolor);
		paint.setStrokeWidth(oldsize);
	}

	void refreshCanvas() {
		Canvas canvas = surfaceHolder.lockCanvas();
		canvas.drawBitmap(canvasBitmap, identityMatrix, null);
		surfaceHolder.unlockCanvasAndPost(canvas);
	}

	void drawPath(Canvas canvas, Path path) {
		paint.setStrokeWidth(path.size);
		paint.setColor(path.color);
		List<Point> points = path.points();
		for (int i = 1; i < points.size(); i++) {
			Point p1 = points.get(i - 1);
			Point p2 = points.get(i);
			canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint);
		}
	}

	public int getCurrentColor() {
		return paint.getColor();
	}

	public void setCurrentColor(int newColor) {
		paint.setColor(newColor);
	}

	public int getCurrentSize() {
		return (int) paint.getStrokeWidth();
	}

	public void setCurrentSize(int newSize) {
		paint.setStrokeWidth(newSize);
	}

	public void addTouchListener(OnTouchListener listener) {
		touchListeners.add(listener);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean consummed = false;
		for (OnTouchListener touchListener : touchListeners) {
			if (touchListener.onTouch(this, event)) {
				consummed = true;
			}
		}
		return consummed;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		isReady = true;
		canvasBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
		canvas = new Canvas();
		canvas.setBitmap(canvasBitmap);
		identityMatrix = new Matrix();
		clearAndDrawPaths(paths.size());
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

}
