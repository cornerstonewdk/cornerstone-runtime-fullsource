package org.skt.runtime.html5apis.calendar;

public class CalendarEventFilter {
	public long startBefore;
	public long startAfter;
	public long endBefore;
	public long endAfter;
	
	public String id; 
	public String description;
	public String location;
	public String summary;
	
	public long start;
	public long end;
	
	public String status;
	public String transparency;
	CalendarRepeatRule recurrence;
	public String reminder;
	
	public CalendarEventFilter(){
		
	}
	public CalendarEventFilter(long startBefore, long startAfter,
			long endBefore, long endAfter, String id, String description,
			String location, String summary, long start, long end,
			String status, String transparency, String reminder) {
		super();
		this.startBefore = startBefore;
		this.startAfter = startAfter;
		this.endBefore = endBefore;
		this.endAfter = endAfter;
		this.id = id;
		this.description = description;
		this.location = location;
		this.summary = summary;
		this.start = start;
		this.end = end;
		this.status = status;
		this.transparency = transparency;
		this.reminder = reminder;
	}
	
	
}
