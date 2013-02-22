package com.tripadvisor.drawisor.entities;

import java.util.List;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "Drawings")
public class Drawing extends Model {
	@Column(name = "Name")
	public String name;

	public List<Path> paths() {
		return getMany(Path.class, "Drawing");
	}

	public Drawing() {
		super();
	}

	public Drawing(String name) {
		super();
		this.name = name;
	}

	@Override
	public void delete() {

		// TODO Auto-generated method stub
		super.delete();
	}

	@Override
	public String toString() {
		return name;
	}
}
