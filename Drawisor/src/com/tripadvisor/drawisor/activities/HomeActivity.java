package com.tripadvisor.drawisor.activities;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ItemClick;
import com.googlecode.androidannotations.annotations.ItemLongClick;
import com.googlecode.androidannotations.annotations.OptionsItem;
import com.googlecode.androidannotations.annotations.OptionsMenu;
import com.googlecode.androidannotations.annotations.ViewById;
import com.tripadvisor.drawisor.R;
import com.tripadvisor.drawisor.entities.Drawing;
import com.tripadvisor.drawisor.entities.Path;
import com.tripadvisor.drawisor.entities.Point;

@EActivity(R.layout.activity_home)
@OptionsMenu(R.menu.activity_home)
public class HomeActivity extends SherlockActivity {

	@ViewById
	ListView drawingsList;

	List<Integer> selectedItems = new ArrayList<Integer>();

	ActionMode actionMode;

	@AfterViews
	void start() {
		testselect();
		loadDrawingsList();
	}

	// TODO: Remove
	private void testselect() {
		// Log.d("App", "TestSelect");
		// List<Drawing> drawings = Drawing.all(Drawing.class);
		//
		// for (Drawing drawing : drawings) {
		// Log.d("Drwaing Name", drawing.name);
		// for (Path path : drawing.paths()) {
		// Log.d("Path color", "" + path.color);
		// Log.d("Path size", "" + path.size);
		// for (Point point : path.points()) {
		// Log.d("Point", "" + point.x + " : " + point.y);
		// }
		// }
		// }
		Log.d("drawings", "" + Drawing.all(Drawing.class).size());
		Log.d("paths", "" + Path.all(Path.class).size());
		Log.d("points", "" + Point.all(Point.class).size());
	}

	void loadDrawingsList() {
		List<Drawing> drawings = Drawing.all(Drawing.class);
		ArrayAdapter<Drawing> adapter = new ArrayAdapter<Drawing>(this, R.layout.drawing_list_item, R.id.itemText, drawings);
		drawingsList.setAdapter(adapter);
	}

	@OptionsItem
	void newDrawing() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("New drawing");
		alert.setMessage("Please enter the name:");
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				Drawing drawing = new Drawing(value);
				drawing.save();
				startDrawingActivity(drawing);
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});

		alert.show();
	}

	@ItemClick
	void drawingsListItemClicked(int position) {
		if (actionMode == null) {
			Drawing drawing = (Drawing) drawingsList.getItemAtPosition(position);
			startDrawingActivity(drawing);
		} else {
			boolean checked = selectedItems.contains(position);
			setItemSelected(position, !checked);
		}
	}

	@ItemLongClick
	void drawingsListItemLongClicked(int position) {
		if (actionMode != null) {
			return;
		}

		actionMode = startActionMode(new ActionMode.Callback() {
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.activity_home_context, menu);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
				case R.id.deleteDrawing:
					List<Integer> items = new ArrayList<Integer>(selectedItems);
					deleteDrawings(items);
					mode.finish();
					return true;
				}
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				clearSelectedItems();
				actionMode = null;
			}
		});
		setItemSelected(position, true);
	}

	void startDrawingActivity(Drawing drawing) {
		Intent i = new Intent(this, DrawingActivity_.class);
		i.putExtra("drawingId", drawing.getId());
		startActivity(i);
	}

	void deleteDrawings(final List<Integer> items) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage("Delete selected drawings?");
		alert.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				for (int position : items) {
					Drawing drawing = (Drawing) drawingsList.getItemAtPosition(position);
					drawing.delete();
				}
				loadDrawingsList();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});

		alert.show();
	}

	/**
	 * Sets an item of the list to selected. I use my own selectedItems list and I change the background myself because ListView with ListView.CHOICE_MODE_MULTIPLE_MODAL choice mode is only from API
	 * 11 or above.
	 *
	 * @param position
	 * @param selected
	 */
	void setItemSelected(int position, boolean selected) {
		if (selected) {
			drawingsList.getChildAt(position).setBackgroundColor(getResources().getColor(R.color.abs__holo_blue_light));
			selectedItems.add(position);
		} else {
			drawingsList.getChildAt(position).setBackgroundColor(getResources().getColor(android.R.color.transparent));
			selectedItems.remove((Integer) position);
		}
	}

	void clearSelectedItems() {
		for (int position : selectedItems) {
			drawingsList.getChildAt(position).setBackgroundColor(getResources().getColor(android.R.color.transparent));
		}
		selectedItems.clear();
	}
}
