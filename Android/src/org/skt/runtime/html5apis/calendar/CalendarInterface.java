package org.skt.runtime.html5apis.calendar;

import org.json.JSONArray;
import org.json.JSONObject;

public interface CalendarInterface {
	static final int SIM_CALENDAR = 0;
	static final int DEVICE_CALENDAR = 1;
	
	static final int NO_RECURRENCE = 0;
	static final int DAILY_RECURRENCE = 1;
	static final int WEEKLY_RECURRENCE = 2;
	static final int MONTHLY_RECURRENCE = 3;
	static final int YEARLY_RECURRENCE = 4;
	
	static final int TENTATIVE_STATUS = 0;
	static final int CONFIRMED_STATUS = 1;
	static final int CANCELLED_STATUS = 2;
	
	static final int NO_ALARM = 0;
	static final int SILENT_ALARM = 1;
	static final int SOUND_ALARM = 2;
	
	int getType();
	String getName();
	
	JSONObject addEvent(JSONObject event);
	
	int deleteEvent(String id);
	
	JSONArray findEvents(JSONObject options);
}
