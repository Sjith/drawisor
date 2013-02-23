package com.tripadvisor.drawisor.activities;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
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
public class DrawingActivity extends SherlockActivity {

	private static final int CLEAR = -1;

	Drawing drawing;
	boolean needsSaving;

	boolean clickedOk;

	MenuItem save;

	@ViewById
	FrameLayout placeholder;
	CanvasView_ canvasView;

	@ViewById
	Button colorButton;
	private PopupMenuItem[] colors;
	AlertDialog colorDialog;

	@ViewById
	Button sizeButton;
	private PopupMenuItem[] sizes;
	AlertDialog sizeDialog;

	List<Path> paths;
	List<Point> currentPath;
	Point lastPoint;
	int undoPosition;
	int startingUndoPosition;

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

		ActiveAndroid.beginTransaction();
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

		colorDialog = new AlertDialog.Builder(this)
				.setTitle("Color")
				.setAdapter(new PopupMenuAdapter(this, R.layout.drawing_list_item, colors),
						new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								canvasView.setCurrentColor(colors[which].data);
								colorButton.setCompoundDrawablesWithIntrinsicBounds(colors[which].iconResId, 0, 0, 0);
								colorDialog.dismiss();
							}
						}).create();
		canvasView.setCurrentColor(colors[0].data);
		colorButton.setCompoundDrawablesWithIntrinsicBounds(colors[0].iconResId, 0, 0, 0);

		sizes = new PopupMenuItem[] {
				new PopupMenuItem(R.drawable.size_1px, R.string._1px, 1),
				new PopupMenuItem(R.drawable.size_3px, R.string._3px, 7),
				new PopupMenuItem(R.drawable.size_5px, R.string._5px, 15),
				new PopupMenuItem(R.drawable.size_10px, R.string._10px, 20),
				new PopupMenuItem(R.drawable.size_15px, R.string._15px, 50) };

		sizeDialog = new AlertDialog.Builder(this)
				.setTitle("Size")
				.setAdapter(new PopupMenuAdapter(this, R.layout.drawing_list_item, sizes),
						new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								canvasView.setCurrentSize(sizes[which].data);
								sizeButton.setCompoundDrawablesWithIntrinsicBounds(sizes[which].iconResId, 0, 0, 0);
								sizeDialog.dismiss();
							}
						}).create();
		canvasView.setCurrentSize(sizes[0].data);
		sizeButton.setCompoundDrawablesWithIntrinsicBounds(sizes[0].iconResId, 0, 0, 0);
	}

	void loadDrawing(long drawingId) {
		if (drawingId == -1) {
			drawing = new Drawing();
			drawing.save();
			setTitle("New drawing");
		} else {
			drawing = Drawing.load(Drawing.class, drawingId);
			setTitle(drawing.name);
		}
		paths = drawing.paths();
		undoPosition = paths.size();
		startingUndoPosition = undoPosition;
		canvasView.loadPaths(paths);
		needsSaving = false;
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
		undoPosition = CLEAR;
		canvasView.clearAndDrawPaths(undoPosition);
		if (startingUndoPosition == 0) {
			setNeedsSaving(false);
		} else {
			setNeedsSaving(true);
		}
	}

	@Click
	void undoButtonClicked() {
		if (undoPosition != 0) {
			if (undoPosition == CLEAR) {
				undoPosition = paths.size();
			} else {
				undoPosition--;
			}
			canvasView.clearAndDrawPaths(undoPosition);
			if (undoPosition == startingUndoPosition) {
				setNeedsSaving(false);
			} else {
				setNeedsSaving(true);
			}
		}
	}

	@Click
	void redoButtonClicked() {
		if (undoPosition < paths.size()) {
			undoPosition++;
			canvasView.clearAndDrawPaths(undoPosition);
			if (undoPosition == startingUndoPosition) {
				setNeedsSaving(false);
			} else {
				setNeedsSaving(true);
			}
		}
	}

	@OptionsItem
	void save() {
		save(false);
	}

	void save(final boolean thenFinish) {
		if (drawing.name == null) {
			final EditText input = new EditText(this);
			clickedOk = false;
			final AlertDialog alert = new AlertDialog.Builder(this).setMessage("Enter name of drawing:").setView(input)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int whichButton) {
							clickedOk = true;
						}
					}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int whichButton) {
							// Canceled.
						}
					}).show();
			alert.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					if (clickedOk) {
						clickedOk = false;
						String value = input.getText().toString();
						if (value.equals("")) {
							input.setError("Name cannot be empty");
							alert.show();
						} else {
							drawing.name = value;
							drawing.save();
							setTitle(drawing.name);
							commitToDb();
							if (thenFinish) {
								stop();
							}
						}
					}
				}
			});
		} else {
			commitToDb();
			if (thenFinish) {
				stop();
			}
		}
	}

	void commitToDb() {
		deleteUnusedPaths();
		ActiveAndroid.setTransactionSuccessful();
		ActiveAndroid.endTransaction();
		setNeedsSaving(false);
		Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
		ActiveAndroid.beginTransaction();
	}

	void setNeedsSaving(boolean needsSaving) {
		this.needsSaving = needsSaving;
		save.setVisible(needsSaving);
	}

	@Override
	public void onBackPressed() {
		if (needsSaving) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Save changes?");
			alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					save(true);
				}
			});
			alert.setNegativeButton("Discard", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					stop();
				}
			});
			alert.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
				}
			});
			alert.show();
		} else {
			stop();
		}
	}

	void stop() {
		ActiveAndroid.endTransaction();
		finish();
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
		deleteUnusedPaths();
		Path path = new Path(canvasView.getCurrentColor(), canvasView.getCurrentSize());
		path.drawing = drawing;
		path.save();
		paths.add(path);
		for (Point p : currentPath) {
			p.path = path;
			p.save();
		}
		setNeedsSaving(true);
		undoPosition = paths.size();
		currentPath.clear();
		lastPoint = null;
	}

	void deleteUnusedPaths() {
		if (undoPosition < startingUndoPosition) {
			startingUndoPosition = -2;
		}
		if (undoPosition == CLEAR) {
			undoPosition = 0;
		}
		for (int i = undoPosition; i < paths.size(); i++) {
			paths.get(i).delete();
		}
		paths = paths.subList(0, undoPosition);
	}

	@OptionsItem
	void home() {
		onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		save = menu.findItem(R.id.save);
		save.setVisible(false);
		return super.onCreateOptionsMenu(menu);
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
			TextView text = (TextView) view.findViewById(R.id.itemText);
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
