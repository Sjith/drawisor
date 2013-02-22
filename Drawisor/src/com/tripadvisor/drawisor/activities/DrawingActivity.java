package com.tripadvisor.drawisor.activities;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.activeandroid.ActiveAndroid;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.OptionsItem;
import com.googlecode.androidannotations.annotations.OptionsMenu;
import com.googlecode.androidannotations.annotations.ViewById;
import com.tripadvisor.drawisor.R;
import com.tripadvisor.drawisor.entities.Drawing;
import com.tripadvisor.drawisor.entities.Path;
import com.tripadvisor.drawisor.entities.Point;
import com.tripadvisor.drawisor.views.CanvasView_;

@EActivity(R.layout.activity_drawing)
@OptionsMenu(R.menu.activity_drawing)
public class DrawingActivity extends Activity {

	Drawing drawing;

	@ViewById
	FrameLayout placeholder;
	CanvasView_ canvasView;

	@ViewById
	Button colorButton;
	private PopupMenuItem[] colors;
	Dialog colorDialog;

	@ViewById
	Button sizeButton;
	private PopupMenuItem[] sizes;
	private Dialog sizeDialog;

	List<Point> currentPath;

	Point lastPoint;

	@AfterViews
	void start() {
		canvasView = new CanvasView_(this);
		placeholder.addView(canvasView);
		canvasView.addTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent e) {
				if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE
						|| e.getAction() == MotionEvent.ACTION_UP) {
					handleOnTouch(e);
					return true;
				}
				return false;
			}
		});
		setupPopupMenus();

		currentPath = new ArrayList<Point>();

		loadDrawing(getIntent().getLongExtra("drawingId", -1));
	}

	void setupPopupMenus() {
		colors = new PopupMenuItem[] {
				new PopupMenuItem(R.drawable.color_black, R.string.black, Color.BLACK),
				new PopupMenuItem(R.drawable.color_red, R.string.red, Color.RED),
				new PopupMenuItem(R.drawable.color_yellow, R.string.yellow, Color.YELLOW),
				new PopupMenuItem(R.drawable.color_green, R.string.green, Color.GREEN),
				new PopupMenuItem(R.drawable.color_blue, R.string.blue, Color.BLUE),
				new PopupMenuItem(R.drawable.color_pink, R.string.pink, Color.MAGENTA) };

		ListView colorList = new ListView(this);
		colorList.setAdapter(new PopupMenuAdapter(this, android.R.layout.simple_dropdown_item_1line, colors));
		colorDialog = new Dialog(this);
		colorDialog.setContentView(colorList);
		colorDialog.setTitle("Color");
		colorList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				canvasView.setCurrentColor(colors[position].data);
				colorButton.setCompoundDrawablesWithIntrinsicBounds(colors[position].iconResId, 0, 0, 0);
				colorDialog.dismiss();
			}
		});
		canvasView.setCurrentColor(colors[0].data);
		colorButton.setCompoundDrawablesWithIntrinsicBounds(colors[0].iconResId, 0, 0, 0);

		sizes = new PopupMenuItem[] {
				new PopupMenuItem(R.drawable.size_1px, R.string._1px, 1),
				new PopupMenuItem(R.drawable.size_3px, R.string._3px, 7),
				new PopupMenuItem(R.drawable.size_5px, R.string._5px, 15),
				new PopupMenuItem(R.drawable.size_10px, R.string._10px, 20),
				new PopupMenuItem(R.drawable.size_15px, R.string._15px, 50) };

		ListView sizeList = new ListView(this);
		sizeList.setAdapter(new PopupMenuAdapter(this, android.R.layout.simple_dropdown_item_1line, sizes));
		sizeDialog = new Dialog(this);
		sizeDialog.setContentView(sizeList);
		sizeDialog.setTitle("Size");
		sizeList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				canvasView.setCurrentSize(sizes[position].data);
				sizeButton.setCompoundDrawablesWithIntrinsicBounds(sizes[position].iconResId, 0, 0, 0);
				sizeDialog.dismiss();
			}
		});
		canvasView.setCurrentSize(sizes[0].data);
		sizeButton.setCompoundDrawablesWithIntrinsicBounds(sizes[0].iconResId, 0, 0, 0);
	}

	void loadDrawing(long drawingId) {
		if (drawingId == -1) {
			finish();
		}
		drawing = Drawing.load(Drawing.class, drawingId);
		setTitle(drawing.name);
		canvasView.loadPaths(drawing.paths());
	}

	@Click
	void colorButtonClicked() {
		colorDialog.show();
	}

	@Click
	void sizeButtonClicked() {
		sizeDialog.show();
	}

	@Click
	void clearButtonClicked() {
		ActiveAndroid.beginTransaction();
		List<Path> paths = drawing.paths();
		for (Path path : paths) {
			path.delete();
		}
		canvasView.loadPaths(drawing.paths());
		canvasView.clearAndDrawPaths();
		ActiveAndroid.setTransactionSuccessful();
		ActiveAndroid.endTransaction();
	}

	void handleOnTouch(MotionEvent e) {
		Point point = new Point((int) e.getX(), (int) e.getY());
		currentPath.add(point);

		if (lastPoint != null) {
			canvasView.drawLine(lastPoint, point);
		}
		lastPoint = point;

		if (e.getAction() == MotionEvent.ACTION_UP) {
			savePath();
		}
	}

	void savePath() {
		ActiveAndroid.beginTransaction();
		Path path = new Path(canvasView.getCurrentColor(), canvasView.getCurrentSize());
		path.drawing = drawing;
		path.save();
		for (Point p : currentPath) {
			p.path = path;
			p.save();
		}
		ActiveAndroid.setTransactionSuccessful();
		ActiveAndroid.endTransaction();
		currentPath.clear();
		lastPoint = null;
	}

	@OptionsItem
	void home() {
		NavUtils.navigateUpFromSameTask(this);
	}

	private class PopupMenuAdapter extends ArrayAdapter<PopupMenuItem> {

		Context context;
		int layoutResourceId;
		PopupMenuItem data[] = null;

		public PopupMenuAdapter(Context context, int layoutResourceId, PopupMenuItem[] data) {
			super(context, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.context = context;
			this.data = data;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;

			// initialize a view first
			if (view == null) {
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				view = inflater.inflate(layoutResourceId, parent, false);
			}

			PopupMenuItem pItem = data[position];
			TextView text = (TextView) view.findViewById(android.R.id.text1);
			text.setText(pItem.textResId);
			text.setCompoundDrawablesWithIntrinsicBounds(pItem.iconResId, 0, 0, 0);

			return view;
		}
	}

	// PopupMenuItem is just a container
	private static class PopupMenuItem {
		public int iconResId;
		public int textResId;
		public int data;

		public PopupMenuItem(int iconResId, int textResId, int data) {
			this.iconResId = iconResId;
			this.textResId = textResId;
			this.data = data;
		}
	}
}
