package com.tripadvisor.drawisor.entities;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Column.ForeignKeyAction;
import com.activeandroid.annotation.Table;

@Table(name = "Points")
public class Point extends Model {
	@Column(name = "Path", onDelete = ForeignKeyAction.CASCADE)
	public Path path;
	@Column(name = "X")
	public int x;
	@Column(name = "Y")
	public int y;

	public Point() {
		super();
	}

	public Point(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}
}
