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

	/**
	 * Constant for setting the clear action in the undoPosition to be able to undo a clear.
	 */
	private static final int CLEAR = -1;

	/**
	 * Constant for the state of no return of the undo/redo. It is the state when it is impossible to return to the original state
	 * of the drawing anymore.
	 */
	private static final int STATE_OF_NO_RETURN = -2;

	/**
	 * The current drawing. Needed for DB saving only.
	 */
	Drawing drawing;
	/**
	 * Whether we need to save or not.
	 */
	boolean needsSaving;

	/**
	 * Ugly workaround to be able to do input validation in the alertDialog for choosing the name.
	 */
	boolean clickedOk;

	/**
	 * Handle on the save button in the action bar (top right) to be able to show/hide it.
	 */
	MenuItem save;

	/**
	 * Handle on the placeholder view used to inject the CanvasView programmatically to init the Canvas correctly.
	 */
	@ViewById
	FrameLayout placeholder;
	/**
	 * The injected CanvasView. Notice the _ at the end for using the AndroidAnnotation processed class.
	 */
	CanvasView_ canvasView;

	@ViewById
	Button colorButton;
	private PopupMenuItem[] colors;
	AlertDialog colorDialog;

	@ViewById
	Button sizeButton;
	private PopupMenuItem[] sizes;
	AlertDialog sizeDialog;

	/**
	 * The current list of path. It is just used to optimize redraw when using undo/redo.
	 */
	List<Path> paths;
	/**
	 * The current path. Use for DB saving.
	 */
	List<Point> currentPath;
	/**
	 * The point from the last touch event to make a line to the next point.
	 */
	Point lastPoint;
	/**
	 * The position in the Path list of the undo/redo.
	 */
	int undoPosition;
	/**
	 * The starting undo position to be able to disable needsSaving if we undo back to the original drawing. However, if we undo
	 * below the original position and then draw a new path it is no more possible to come back to the original state, in this
	 * case it is set to the value STATE_OF_NO_RETURN.
	 */
	int startingUndoPosition;

	@AfterViews
	void start() {
		// Inject CanvasView into placeholder and set touch listener.
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

		// Start a SQLite transaction to optimize all DB actions and also to be able to discard all modifications at the end
		// (transaction is rollbacked else commited).
		ActiveAndroid.beginTransaction();
		loadDrawing(getIntent().getLongExtra("drawingId", -1));
	}

	/**
	 * Created the popup menu used to select the color and the size of the strokes.
	 */
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
			// Since we are in a transaction, we can save the new drawing. Mandatory for ActiveAndroid OneToMany functioning.
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
		// Clear actually is just like clicking undo until having an empty drawing, except that it is set to CLEAR so that undo
		// can undo the clear.
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

	/**
	 * Save the drawing and eventually asks for a name if it is not set.
	 *
	 * @param thenFinish
	 *            Whether to exit the activity after a successful save.
	 */
	void save(final boolean thenFinish) {
		if (drawing.name == null) {
			final EditText input = new EditText(this);
			clickedOk = false;
			final AlertDialog alert = new AlertDialog.Builder(this).setMessage("Enter name of drawing:").setView(input)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int whichButton) {
							// Ugly workaround to be able to do input validation using OnDismiss.
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
		// First trim the path at the end in case of undo.
		deleteUnusedPaths();
		// Commit the transation and start a new one.
		ActiveAndroid.setTransactionSuccessful();
		ActiveAndroid.endTransaction();
		setNeedsSaving(false);
		Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
		ActiveAndroid.beginTransaction();
	}

	void setNeedsSaving(boolean needsSaving) {
		this.needsSaving = needsSaving;
		// Toggle the visibility of the save button in the action bar.
		save.setVisible(needsSaving);
	}

	// This method is automatically backwards compatible with old SDK with AndroidAnnotation.
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
					// The transaction is rollbacked automatically before stopping the activity. That also means that if the
					// application crashes, the data is not corrupt because it gets rollbacked.
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
			// Save the path only when the finger is lifted up to limit work on the GUI thread.
			savePath();
		}
	}

	void savePath() {
		// Remove the paths that has been undone, they cannot be redone after a new path is created.
		deleteUnusedPaths();
		Path path = new Path(canvasView.getCurrentColor(), canvasView.getCurrentSize());
		path.drawing = drawing;
		path.save();
		paths.add(path);
		// We are inside a transaction, so these actions are just done in memory, so it is fast.
		for (Point p : currentPath) {
			p.path = path;
			p.save();
		}
		setNeedsSaving(true);
		undoPosition = paths.size();
		currentPath.clear();
		lastPoint = null;
	}

	/**
	 * Deletes the paths that are ahead of the undoPosition.
	 */
	void deleteUnusedPaths() {
		if (undoPosition < startingUndoPosition) {
			startingUndoPosition = STATE_OF_NO_RETURN;
		}
		if (undoPosition == CLEAR) {
			undoPosition = 0;
		}
		for (int i = undoPosition; i < paths.size(); i++) {
			paths.get(i).delete();
		}
		paths = paths.subList(0, undoPosition);
	}

	// Capture the home button of the action bar (top left).
	@OptionsItem
	void home() {
		onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Get a handle on the save button of the action bar.
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

			// Initialize a view first if the pool of view doesn't give us one.
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

	// PopupMenuItem is just a container for the PopupMenu Dialogs.
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
