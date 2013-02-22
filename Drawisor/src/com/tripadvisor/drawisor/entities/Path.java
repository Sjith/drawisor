package com.tripadvisor.drawisor.entities;

import java.util.List;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "Paths")
public class Path extends Model {
	@Column(name = "Drawing")
	public Drawing drawing;
	@Column(name = "Color")
	public int color;
	@Column(name = "Size")
	public int size;

	public List<Point> points() {
		return getMany(Point.class, "Path");
	}

	public Path() {
		super();
	}

	public Path(int color, int size) {
		super();
		this.color = color;
		this.size = size;
	}
}
