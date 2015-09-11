package com.julia.myideas;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

class Idea implements Comparable<Idea> {
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	String content;
	Date created;
	public Idea(String content) {
		this.content = content;
		this.created = new Date();
	}
	public Idea(String dateStr, String content) {
		try {
			created = DATE_FORMAT.parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		this.content = content;
	}
	@Override
	public String toString() {
		return DATE_FORMAT.format(created) + ": " + unescapeEvernoteString(this.content);
	}

	private String unescapeEvernoteString(String input) {
		input = input.replace("&amp;", "&");
		return input;
	}

	@Override
	public int compareTo(Idea another) {
		return another.created.compareTo(this.created);
	}
	
	
}