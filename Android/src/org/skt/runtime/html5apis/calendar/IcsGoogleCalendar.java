package org.skt.runtime.html5apis.calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.util.Log;

public class IcsGoogleCalendar implements CalendarInterface{

	private static final String LOGTAG = "IcsGoogleCalendar";

	/** Called when the activity is first created. */
	// Projection array. Creating indices for this array instead of doing
	// dynamic lookups improves performance.
	public static final String[] EVENT_PROJECTION = new String[] {
		Calendars._ID,                           // 0
		Calendars.ACCOUNT_NAME,                  // 1
		Calendars.CALENDAR_DISPLAY_NAME          // 2
	};

	private static final String[] PROJECTION = {
		Events._ID, Events.TITLE, Events.DESCRIPTION, Events.DTSTART, Events.DTEND, Events.DURATION,
		Events.EVENT_LOCATION, Events.RRULE, Events.LAST_DATE, Events.STATUS, Events.HAS_ALARM, Events.EVENT_TIMEZONE
	};
	
	private static final String[] PROJ_REMINDER = {
		Reminders._ID, Reminders.MINUTES, Reminders.METHOD
	};
	
	private Context mContext;
	private int mCalendarId;
	private String mName;
	
	public IcsGoogleCalendar(Context context, int calendarId, String calendarName) {
		mContext = context;
		mCalendarId = calendarId;
		mName = calendarName;
	}
	
	public String getName() {
		return mName;
	}
	
	public int getType() {
		return CalendarInterface.DEVICE_CALENDAR;
	}
	
	public JSONObject addEvent(JSONObject event) {
		Log.d(LOGTAG, "task addEvent begins");

		boolean checkresult = checkIfEventValid(event);
		if(!checkresult){
			return null;
		}

		ContentValues values;
		try {
			values = eventJSONObjectToContentValues(event);

			Uri eventUri = Events.CONTENT_URI;
			Uri uri = mContext.getContentResolver().insert(eventUri, values);
			String path = uri.toString();
			if(path == null || !path.startsWith("content://com.android.calendar/events")){
				return null;
			}

			if(getJsonString(event, "reminder") != null){
				String id = path.substring(path.lastIndexOf('/')+1);
				Log.d(LOGTAG, "inserting reminder info in " + id);
				ContentValues v = alarmToContentValues(id, event);
				mContext.getContentResolver().insert(Reminders.CONTENT_URI,v);
			}

			JSONObject addedevent = null;

			// query again
			Cursor c = 
					mContext.getContentResolver().query(
							uri, PROJECTION, null, null, null);
			if(c != null && c.moveToFirst()) {
				addedevent = cursorToJSONObject(c);
			}
			c.close();
			Log.d(LOGTAG, "calendar addEvent ends");
			
			return addedevent;
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return null;
	}

	public int deleteEvent(String id) {
		
		ContentResolver r = mContext.getContentResolver();
		Uri uri = Uri.parse(Events.CONTENT_URI.toString() + "/" + id);
		Cursor c = r.query(uri,new String[]{ Events._ID }, null, null, null);
		
		if(c == null) {
			return 0;
		}
		
		c.moveToFirst();
		
		String fetchedid = c.getString(0);
		
		c.close();
		if(!id.equals(fetchedid)){
			return 0;
		}
			
		// delete
		return r.delete(uri, null, null);		
	}
		
	public JSONArray findEvents(JSONObject options) {
		
		SimpleDateFormat format = new SimpleDateFormat();
		ContentResolver r = mContext.getContentResolver();
		Uri eventUri = Events.CONTENT_URI;
		ArrayList<String> selArgs = new ArrayList<String>();
		
		JSONObject filterjson = new JSONObject();
		CalendarEventFilter filter = new CalendarEventFilter();
		boolean multiple = true;
		
		if(options == null){
			//do notthing 
		}
		else{
			//Step 1. make java find option from JSONobject
			filterjson = getJSONObject(options,"filter");	 //calendarFindOption::filter
			
			try {
				multiple = options.getBoolean("multiple");				//calendarFindOption::multiple
			} catch (JSONException e) {
			}
			
			
			if(filterjson != null){
				filter = makeFindFilter(filterjson, filter);
			}
		}
		
		//Step 2. make a query
		String selection = "calendar_id=? AND deleted=?";
		selArgs.add(Integer.toString(mCalendarId));
		selArgs.add("0");
		
		if(filter != null){
			if(filter.startAfter != 0){												//startAfter
				selection += " AND dtstart >= ?";
				selArgs.add(Long.toString(filter.startAfter));
			}
			
			if(filter.startBefore != 0){
				selection += " AND dtstart <= ?";
				selArgs.add(Long.toString(filter.startBefore));						//startBefore
			}
			
			if(filter.endAfter != 0){
				selection += " AND dtend >= ?";
				selArgs.add(Long.toString(filter.endAfter));						//endAfter
			}
			
			if(filter.endBefore != 0){
				selection += " AND dtend <= ?";
				selArgs.add(Long.toString(filter.endBefore));						//endBefore
			}
			
			if(filter.id != null) {													//id
				selection += " AND _id=?";
				selArgs.add(filter.id);
			}
			if(filter.description != null) {										//description
				if(filter.description.contains("%"))
					selection += " AND description like ?";
				else
					selection += " AND description=?";
				selArgs.add(filter.description);
			}
			if(filter.location != null) {											//location
				if(filter.location.contains("%"))
					selection += " AND eventLocation like ?";
				else
					selection += " AND eventLocation=?";
				selArgs.add(filter.location);
			}			
			if(filter.summary != null) {											//summary
				if(filter.summary.contains("%"))
					selection += " AND title like ?";
				else
					selection += " AND title=?";
				selArgs.add(filter.summary);
			}
			if(filter.start != 0){													//start
				selection += " AND dtstart = ?";
				selArgs.add(Long.toString(filter.start));
			}
			if(filter.end != 0){													//end
				selection += " AND dtend = ?";
				selArgs.add(Long.toString(filter.end));
			}
			if(filter.end != 0){													//end
				selection += " AND dtend = ?";
				selArgs.add(Long.toString(filter.end));
			}
			if(filter.status != null) {						
				selection += " AND eventStatus = ?";								//status
				selArgs.add(Integer.toString(getStatusFromString(filter.status)));
			}
			
		}
						
		
		//Step 3. query to calendar
		String[] selArgArray = new String[selArgs.size()];
		selArgs.toArray(selArgArray);

		String qry = new String();
		for(int i = 0, j = 0; i < selection.length(); i++) {
			char ch = selection.charAt(i);
			if(ch == '?') {
				// replacing
				qry.concat(String.format("\"%s\"", selArgArray[j++]));
			}
			else
				qry.concat(String.format("%c", ch));
		}
		Log.d(LOGTAG, "findEvents Query = " + qry);
		Log.d(LOGTAG, "selection = " + selection);

		Cursor c = r.query(eventUri, PROJECTION, selection,
							selArgArray, null);

		if(c == null){
			//TODO exception!!
			return null;
		}

		//Step 4. make event from cursor 
		if(c.moveToFirst()) {
			
			JSONArray events = new JSONArray();
			if(multiple == true){						// all finded events
				do {
					events.put(cursorToJSONObject(c));
				}
				while(c.moveToNext());
			}
			else										// only one event
				events.put(cursorToJSONObject(c));
			
			c.close();
			
			return events;
		}
		
		return new JSONArray();
	}

	
	//[20120625][chisu]Util
	public CalendarEventFilter makeFindFilter(JSONObject filterjson , CalendarEventFilter filter){
		String temp = getJsonString(filterjson, "startBefore");		
		if(temp != null)
			filter.startBefore = getTimeFromDateString(temp);

		temp = getJsonString(filterjson, "startAfter");		
		if(temp != null)
			filter.startAfter = getTimeFromDateString(temp);
		
		temp = getJsonString(filterjson, "endBefore");		
		if(temp != null)
			filter.endBefore = getTimeFromDateString(temp);

		temp = getJsonString(filterjson, "endAfter");		
		if(temp != null)
			filter.endAfter = getTimeFromDateString(temp);
				
		filter.id = getJsonString(filterjson, "id");
		filter.description = getJsonString(filterjson, "description");
		filter.location = getJsonString(filterjson, "location");
		filter.summary = getJsonString(filterjson, "summary");
		
		temp = getJsonString(filterjson, "start");		
		if(temp != null)
			filter.start = getTimeFromDateString(temp);
		temp = getJsonString(filterjson, "end");		
		if(temp != null)
			filter.end = getTimeFromDateString(temp);
		
		filter.status = getJsonString(filterjson, "status");
		filter.transparency = getJsonString(filterjson, "transparency");
		filter.reminder = getJsonString(filterjson, "reminder");
		
        // make java recurrence option from JSONobject.
		CalendarRepeatRule temprepeat = new CalendarRepeatRule();
        JSONObject recurrencejson = getJSONObject(filterjson,"recurrence");
        if(recurrencejson != null){
        	temprepeat.frequency = getJsonString(recurrencejson, "frequency");
        	temprepeat.interval = getJSONlong(recurrencejson, "interval");
        	temp = getJsonString(recurrencejson, "expires");		
			if(temp != null)
				temprepeat.expires = getTimeFromDateString(temp);
        	///TODO other filed is not supported. 
        }
        filter.recurrence = temprepeat; 
        
        return filter;
	}
	
	public long getTimeFromDateString(String dateStr){
		String[] dateformats = new String[] {
				"yyyy-MM-dd'T'HH:mm:ss.SSSZ",
				"yyyy-MM-dd HH:mm:ss.SSSZ",
				"yyyy-MM-dd HH:mmZ",
				"yyyy-MM-dd HH:mm",
				"yyyy-MM-dd",
				 };
		//test
		dateStr = dateStr.replace("T", " ");
		long datetime = 0;
		for(String format : dateformats){
			SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			try {
				datetime = sdf.parse(dateStr).getTime();
				return datetime;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}	
		}
		
		return 0;
	}
	
	private boolean checkIfEventValid(JSONObject event) {

		String start = getJsonString(event, "start");											
		String end = getJsonString(event, "end");
		
		long startdate = 0;
		long enddate = 0;
		
		if(start != null){
			startdate = getTimeFromDateString(start);
		}
		if(end != null){
			enddate = getTimeFromDateString(end);
		}
		
		// reported in Compliance AC Bug #1812
		if(end != null && start != null && enddate < startdate)
			return false;
		
		return true;
	}
	
	private ContentValues eventJSONObjectToContentValues(JSONObject ev) throws JSONException{
		ContentValues cv = new ContentValues();
		
		TimeZone tz = TimeZone.getDefault();
		cv.put(Events.CALENDAR_ID, mCalendarId);
		//[20111124][chisu]if wrt don't add eventtimezone attr , native calendar app must raise exception 
		//                 when edit event. 
		cv.put(Events.EVENT_TIMEZONE, tz.getID());			//Asia/Seoul
		
		// event values
		cv.put(Events.DESCRIPTION, getJsonString(ev,"description"));						//description
		cv.put(Events.TITLE, getJsonString(ev,"summary"));									//summary
		cv.put(Events.EVENT_LOCATION, getJsonString(ev,"location"));						//location
		cv.put(Events.STATUS,getStatusFromString(getJsonString(ev, "status")));				//status
		//[20120625][chisu]transparency is no supported 
		//if(getJsonString(ev, "transparency") != null)										//transparency
		cv.put(Events.HAS_ALARM,1);
					
		String start = getJsonString(ev, "start");											//start
		String end = getJsonString(ev, "end");												//end
		if(start == null && end != null){
			throw new JSONException("start value must be setted.");
		}
		
		if(start != null){
			cv.put(Events.DTSTART,getTimeFromDateString(start));
		}
		if(end != null){
			cv.put(Events.DTEND,getTimeFromDateString(end));
		}
		else{ // end is null;
			cv.put(Events.DURATION, String.format("PT%dM", 0));
		}
				
		JSONObject recurrence = getJSONObject(ev,"recurrence");								//recurrence
		if(recurrence != null){
			String rrule = null;
			String frequency = getJsonString(recurrence, "frequency");						//recurrence :: frequency
			if(frequency != null){
				if(frequency.equalsIgnoreCase("yearly")){
					rrule = "FREQ=YEARLY;WKST=SU";
				}
				else if(frequency.equalsIgnoreCase("monthly")){
					rrule = "FREQ=MONTHLY;WKST=SU";
				}
				else if(frequency.equalsIgnoreCase("weekly")){
					rrule = "FREQ=WEEKLY;WKST=SU";
				}
				else if(frequency.equalsIgnoreCase("daily")){
					rrule = "FREQ=DAILY;WKST=SU";
				}
			}
			
			if(rrule != null) {
				String expires = getJsonString(recurrence, "expires");						//recurrence :: expires
				if(expires != null) {		
					Date expiredate = new Date(getTimeFromDateString(expires));
					int year = expiredate.getYear() + 1900;
					int month = expiredate.getMonth() + 1;
					int day = expiredate.getDate();
					int hour = expiredate.getHours();
					int minute = expiredate.getMinutes();
					int second = expiredate.getSeconds();

					rrule += String.format(";UNTIL=%04d%02d%02dT%02d%02d%02dZ", 
							year, month, day, hour, minute, second);
					// putting in redundantely
					//cv.put("lastDate", ev.expires.getTime());
				}
				
				long interval = getJSONlong(recurrence,"interval");							//recurrence :: interval
				if(interval != 0) {
					rrule += String.format(";INTERVAL=%d", interval);
				}
				
				JSONArray daysInWeek = getJsonArray(recurrence , "daysInWeek");				//recurrence  :: daysInWeek
				if(daysInWeek != null && frequency.equalsIgnoreCase("weekly")){				// only frequency is "weekly"
					StringBuilder byday = new StringBuilder();
					byday.append(";BYDAY=");
					for(int i = 0 ; i < daysInWeek.length() ; i ++){
						long dayvalue = daysInWeek.getLong(i);
						
						switch((int)dayvalue){
						case 0: byday.append("SU");break;
						case 1: byday.append("MO");break;
						case 2: byday.append("TU");break;
						case 3: byday.append("WE");break;
						case 4: byday.append("TH");break;
						case 5: byday.append("FR");break;
						case 6: byday.append("SA");break;
						default : throw new JSONException("daysInWeek day must be [0 ~ 6]"); 
						}
						
						if(i != daysInWeek.length() -1)
							 byday.append(",");
					}
					rrule += byday.toString();
				}
				
				JSONArray daysInMonth = getJsonArray(recurrence , "daysInMonth");				//recurrence  :: daysInMonth
				if(daysInMonth != null && frequency.equalsIgnoreCase("monthly")){			// only frequency is "monthly"
					StringBuilder bymonthday = new StringBuilder();
					bymonthday.append(";BYMONTHDAY=");
					for(int i = 0 ; i < daysInMonth.length() ; i ++){
						long dayvalue = daysInMonth.getLong(i);
						if(dayvalue > 31 || dayvalue < -30)
							throw new JSONException("dayInMonth day must be [1 ~ 31] , [0 ~ -30]");
						
						bymonthday.append(dayvalue);
						
						if(i != daysInMonth.length() -1)
							bymonthday.append(",");
					}
					rrule += bymonthday.toString();
				}
				//TODO :: Not supported in native calendar
				JSONArray daysInYear = getJsonArray(recurrence , "daysInYear");				//recurrence  :: daysInYear
				if(daysInYear != null && frequency.equalsIgnoreCase("yearly")){			// only frequency is "yearly"
					StringBuilder byyearday = new StringBuilder();
					byyearday.append(";BYYEARDAY=");
					for(int i = 0 ; i < daysInYear.length() ; i ++){
						long dayvalue = daysInYear.getLong(i);
						if(dayvalue > 365 || dayvalue < -364)
							throw new JSONException("daysInYear day must be [1 ~ 365] , [0 ~ -364]");
						
						byyearday.append(dayvalue);
						
						if(i != daysInYear.length() -1)
							byyearday.append(",");
					}
					rrule += byyearday.toString();
				}
				//TODO ::  Not supported in native calendar
				JSONArray weeksInMonth = getJsonArray(recurrence , "weeksInMonth");				//recurrence  :: weeksInMonth 
				if(weeksInMonth != null && frequency.equalsIgnoreCase("monthly")){				// only frequency is "monthly "
					StringBuilder byweekno = new StringBuilder();
					byweekno.append(";BYWEEKNO=");
					for(int i = 0 ; i < weeksInMonth.length() ; i ++){
						long weekno = weeksInMonth.getLong(i);
						if(weekno > 4 || weekno < -3)
							throw new JSONException("weeksInMonth day must be [1 ~ 4] , [0 ~ -3]");
						
						byweekno.append(weekno);
						
						if(i != weeksInMonth.length() -1)
							byweekno.append(",");
					}
					rrule += byweekno.toString();
				}
				
				JSONArray monthsInYear = getJsonArray(recurrence , "monthsInYear");				//recurrence  :: monthsInYear 
				if(monthsInYear != null && frequency.equalsIgnoreCase("yearly")){				// only frequency is "yearly "
					StringBuilder bymonth = new StringBuilder();
					bymonth.append(";BYMONTH=");
					for(int i = 0 ; i < monthsInYear.length() ; i ++){
						long value = monthsInYear.getLong(i);
						if(value > 12 || value < 1)
							throw new JSONException("weeksInMonth day must be [1 ~ 12]");
						
						bymonth.append(value);
						
						if(i != monthsInYear.length() -1)
							bymonth.append(",");
					}
					rrule += bymonth.toString();
				}
				
				Log.d(LOGTAG, "RRULE=" + rrule);
				cv.put(Events.RRULE, rrule);
				
//				JSONArray exceptionDates = getJsonArray(recurrence,"exceptionDates");		//recurrence :: exceptionDates
//				if(exceptionDates != null){
//					String exceptiondatesStr = "";
//					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
//					for(int i = 0 ; i <exceptionDates.length() ; i++){
//						try {
//							Date test = format.parse(exceptionDates.get(i).toString());
//							exceptiondatesStr += String.format("%dZ", test.getTime());
//							if(i != exceptionDates.length() - 1)
//								exceptiondatesStr += ",";
//						} catch (ParseException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					}
//					Log.d(LOGTAG, "EXDATE=" + exceptiondatesStr);
//					cv.put(Events.EXDATE, exceptiondatesStr);
//				}
			}
		}
		
		return cv;
	}
	
	protected JSONArray getJsonArray(JSONObject obj, String property){
		JSONArray value = null;
        try {
            if (obj != null) {
                value = obj.getJSONArray(property);
            }
        }
        catch (JSONException e) {
            Log.d(LOGTAG, "Could not get = " + property);
            value = null;
        }   
        return value;
	}
	
    protected String getJsonString(JSONObject obj, String property) {
        String value = null;
        try {
            if (obj != null) {
                value = obj.getString(property);
                if (value.equals("null")) {
                    Log.d(LOGTAG, property + " is string called 'null'");
                    value = null;
                }
            }
        }
        catch (JSONException e) {
            Log.d(LOGTAG, "Could not get = " + e.getMessage());
        }   
        return value;
    }
    
    protected JSONObject getJSONObject(JSONObject obj, String property) {
    	JSONObject value = null;
        try {
            if (obj != null) {
                value = obj.getJSONObject(property);
            }
        }
        catch (JSONException e) {
            Log.d(LOGTAG, "Could not get = " + property);
            value = null;
        }   
        return value;
    }
    
    protected long getJSONlong(JSONObject obj, String property) {
    	long value = 0;
        try {
            if (obj != null) {
                value = obj.getLong(property);
            }
        }
        catch (JSONException e) {
        	return 0; 
        }   
        return value;
    }
    
	private ContentValues alarmToContentValues(String id, JSONObject ev) {
		ContentValues cv = new ContentValues();
		cv.put(Reminders.EVENT_ID, Integer.parseInt(id));
		long reminder;
		try {
			reminder = ev.getLong("reminder");
			reminder = reminder/60000;
			cv.put(Reminders.MINUTES, (int)Math.abs(reminder));
			cv.put(Reminders.METHOD, 1);
		} catch (JSONException e) {
			e.printStackTrace();
			cv.put(Reminders.MINUTES, Reminders.MINUTES_DEFAULT);
			cv.put(Reminders.METHOD, 1);
		}
		
		return cv;
	}
	
    private int getStatusFromString(String status){
    	if(status == null) return 0;
    	else if (status.equalsIgnoreCase("tentative")) return Events.STATUS_TENTATIVE;
    	else if (status.equalsIgnoreCase("confirmed")) return Events.STATUS_CONFIRMED;
    	else if (status.equalsIgnoreCase("cancelled")) return Events.STATUS_CANCELED;  	
    	return 0;
    }
       
    private String makeStatusFromint(int status){
    	if (Events.STATUS_TENTATIVE == status) return "tentative";
    	else if (Events.STATUS_CONFIRMED== status) return "confirmed";
    	else if (Events.STATUS_CANCELED== status) return "cancelled";  	
    	return "tentative";
    }
    
    protected HashMap<String,Boolean> buildPopulationSet(JSONArray fields) {
        HashMap<String,Boolean> map = new HashMap<String,Boolean>();
    
        String key;
        try {
            if (fields.length() == 1 && fields.getString(0).equals("*")) {
                map.put("description ", true);
                map.put("end ", true);
                map.put("location ", true);
                map.put("recurrence ", true);
                map.put("reminder ", true);
                map.put("start ", true);
                map.put("status ", true);
                map.put("summary ", true);
                map.put("transparency ", true);
            } 
            else {
                for (int i=0; i<fields.length(); i++) {
                    key = fields.getString(i);
                    if (key.startsWith("description")) {
                        map.put("description", true);
                    }
                    else if (key.startsWith("end")) {
                        map.put("end", true);
                    }
                    else if (key.startsWith("location")) {
                        map.put("location", true);
                    }
                    else if (key.startsWith("start")) {
                        map.put("start", true);
                    }
                    else if (key.startsWith("status")) {
                        map.put("status", true);
                    }
                    else if (key.startsWith("summary")) {
                        map.put("summary", true);
                    }
                    else if (key.startsWith("transparency")) {
                        map.put("transparency", true);
                    }
                }
            }
        }
        catch (JSONException e) {
            Log.e(LOGTAG, e.getMessage(), e);
        }
        return map;
    }
    
    private JSONObject cursorToJSONObject(Cursor c) {
    	Log.d(LOGTAG, "cursorToEvent begins");

    	JSONObject event = new JSONObject();
    	try {
    		event.put("id", c.getString(c.getColumnIndex(Events._ID)));							//id
    		event.put("description", c.getString(c.getColumnIndex(Events.DESCRIPTION)));		//description
    		event.put("summary", c.getString(c.getColumnIndex(Events.TITLE)));					//summary
    		
    		event.put("start", c.getLong(c.getColumnIndex(Events.DTSTART)));					//start
    		//Date startdate = new Date(c.getLong(c.getColumnIndex(Events.DTSTART)));
    		//event.put("start", startdate.toString());											
    		
    		event.put("end", c.getLong(c.getColumnIndex(Events.DTEND)));						//end
    		//Date enddate = new Date(c.getLong(c.getColumnIndex(Events.DTEND)));
    		//event.put("end", enddate);															
    		
    		event.put("location", c.getString(c.getColumnIndex(Events.EVENT_LOCATION)));		//location
    		event.put("status", makeStatusFromint(c.getColumnIndex(Events.STATUS)));			//status
    		event.put("transparency", null);													//transparency

    		JSONObject recurrence = new JSONObject(); 
    		String rrule = c.getString(c.getColumnIndex(Events.RRULE));
    		// rrule may covers recurrence, interval and expires fields
    		if(rrule != null) {
    			try {
    				parseRRuleString(rrule, recurrence);
    				event.put("recurrence", recurrence);										//recurrence
    			}
    			catch (Exception e) {
    				//TODO
    			}
    		}


    		Uri reminderUri = Reminders.CONTENT_URI;
    		String reminderwhere = Reminders.EVENT_ID+"=?";
    		Cursor rc = mContext.getContentResolver().query(
    				reminderUri, PROJ_REMINDER , 
    				reminderwhere, new String[] {c.getString(c.getColumnIndex(Events._ID)) }, "minutes ASC");

    		// we neeeeeeeeeed only one entry
    		if(rc != null && rc.moveToFirst()) {
    			event.put("reminder", -(rc.getInt(rc.getColumnIndex(Reminders.MINUTES)) *6000 ));	    //reminder
    		}
    		rc.close();


    		Log.d(LOGTAG, "cursorToEvent ends");
    	} catch (JSONException e1) {
    		// TODO Auto-generated catch block
    		e1.printStackTrace();
    	}
    	return event;
    }
    
    private void parseRRuleString(String recur, JSONObject ev)
			throws NumberFormatException, ParseException , JSONException {
		/*
		 * RFC2445:
		 * 
	     recur      = "FREQ"=freq *(
	
	                ; either UNTIL or COUNT may appear in a 'recur',
	                ; but UNTIL and COUNT MUST NOT occur in the same 'recur'
	
	                ( ";" "UNTIL" "=" enddate ) /
	                ( ";" "COUNT" "=" 1*DIGIT ) /
	
	                ; the rest of these keywords are optional,
	                ; but MUST NOT occur more than once
	
	                ( ";" "INTERVAL" "=" 1*DIGIT )          /
	                ( ";" "BYSECOND" "=" byseclist )        /
	                ( ";" "BYMINUTE" "=" byminlist )        /
	                ( ";" "BYHOUR" "=" byhrlist )           /
	                ( ";" "BYDAY" "=" bywdaylist )          /
	                ( ";" "BYMONTHDAY" "=" bymodaylist )    /
	                ( ";" "BYYEARDAY" "=" byyrdaylist )     /
	                ( ";" "BYWEEKNO" "=" bywknolist )       /
	                ( ";" "BYMONTH" "=" bymolist )          /
	                ( ";" "BYSETPOS" "=" bysplist )         /
	                ( ";" "WKST" "=" weekday )              /
	                ( ";" x-name "=" text )
	                )
	
	     freq       = "SECONDLY" / "MINUTELY" / "HOURLY" / "DAILY"
	                / "WEEKLY" / "MONTHLY" / "YEARLY"
	
	     enddate    = date
	     enddate    =/ date-time            ;An UTC value
	
	     byseclist  = seconds / ( seconds *("," seconds) )
	
	     seconds    = 1DIGIT / 2DIGIT       ;0 to 59
	
	     byminlist  = minutes / ( minutes *("," minutes) )
	
	     minutes    = 1DIGIT / 2DIGIT       ;0 to 59
	
	     byhrlist   = hour / ( hour *("," hour) )
	
	     hour       = 1DIGIT / 2DIGIT       ;0 to 23
	
	     bywdaylist = weekdaynum / ( weekdaynum *("," weekdaynum) )
	
	     weekdaynum = [([plus] ordwk / minus ordwk)] weekday
	
	     plus       = "+"
	
	     minus      = "-"
	
	     ordwk      = 1DIGIT / 2DIGIT       ;1 to 53
	
	     weekday    = "SU" / "MO" / "TU" / "WE" / "TH" / "FR" / "SA"
	     ;Corresponding to SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY,
	     ;FRIDAY, SATURDAY and SUNDAY days of the week.
	
	     bymodaylist = monthdaynum / ( monthdaynum *("," monthdaynum) )
	
	     monthdaynum = ([plus] ordmoday) / (minus ordmoday)
	
	     ordmoday   = 1DIGIT / 2DIGIT       ;1 to 31
	
	     byyrdaylist = yeardaynum / ( yeardaynum *("," yeardaynum) )
	
	     yeardaynum = ([plus] ordyrday) / (minus ordyrday)
	
	     ordyrday   = 1DIGIT / 2DIGIT / 3DIGIT      ;1 to 366
	
	     bywknolist = weeknum / ( weeknum *("," weeknum) )
	
	
	
	     weeknum    = ([plus] ordwk) / (minus ordwk)
	
	     bymolist   = monthnum / ( monthnum *("," monthnum) )
	
	     monthnum   = 1DIGIT / 2DIGIT       ;1 to 12
	
	     bysplist   = setposday / ( setposday *("," setposday) )
	
	     setposday  = yeardaynum

		 */
		String[] tokens = recur.split(";");
		// get frequency
		if(!tokens[0].startsWith("FREQ="))	// string should start with this
			throw new ParseException("Invalid RRULE field in google calendar", 0);
		String freq = tokens[0].substring(5);
		if(freq.equals("YEARLY")) {
			ev.put("frequency" , "yearly");
		}
		else if(freq.equals("MONTHLY")) {
			ev.put("frequency" , "monthly");
		}
		else if(freq.equals("WEEKLY")) {
			ev.put("frequency" , "weekly");
		}
		else if(freq.equals("DAILY")) {
			ev.put("frequency" , "daily");
		}
		else {
			// WAC doesn't support more precise recurrence type like hourly
			// we just ignore them and leave as NO_RECURRENCE
			return; // no need to parse more
		}
		for(int i = 1; i < tokens.length; i++) {
			// matches to expire
			if(tokens[i].startsWith("UNTIL=")) {
				String enddate = tokens[i].substring(6);
				// 20110303, 20010303T193300Z
				int year = 0;
				int month = 0;
				int day = 0;
				int hour = 0;
				int minute = 0;
				int second = 0;
				int tz = 0;
				int len = enddate.length();
				if(len == 8) {	// date format; 20110303
					try {
						year = Integer.parseInt(enddate.substring(0, 4));
						month = Integer.parseInt(enddate.substring(4, 6));
						day = Integer.parseInt(enddate.substring(6, 8));
						hour = 23;
						minute = 59;
						second = 59;
					}
					catch (NumberFormatException e) {
						throw new ParseException("UNTIL date format is not valid", i);
					}
				}
				else if(len >= 15) {
					// google calendar takes date-time as UTC time
					// regardless of tailing 'Z'
					if(enddate.charAt(8) == 'T') {
						try {
							year = Integer.parseInt(enddate.substring(0, 4));
							month = Integer.parseInt(enddate.substring(4, 6));
							day = Integer.parseInt(enddate.substring(6, 8));
							hour = Integer.parseInt(enddate.substring(9, 11));
							minute = Integer.parseInt(enddate.substring(11, 13));
							second = Integer.parseInt(enddate.substring(13, 15));
						}
						catch (NumberFormatException e) {
							throw new ParseException("UNTIL date format is not valid", i);
						}
					}
				}

				if(year == 0 || month == 0 || day == 0)
					throw new ParseException("UNTIL date value is not valid", i);

				// now we can get expires
				Date exfiredate = new Date(
						year - 1900, month - 1, day,
						hour, minute, second);
				
				//ev.put("expires", exfiredate.toString());
				ev.put("expires", exfiredate.getTime());
				
			}
			/*
			// matches to COUNT
			else if(tokens[i].startsWith("COUNT=")) {
				String count = tokens[i].substring(6);
				// if both of UNTIL and COUNT are specified,
				// COUNT would be ignored.
				if(ev.getString("expires") != null)
					continue;	

				int repeat = 0;
				try {
					repeat = Integer.parseInt(count) - 1;
				}
				catch (NumberFormatException e) {
					throw new ParseException("COUNT number values is not valid", i);
				}
				
				switch(ev.recurrence) {
				case Calendar.YEARLY_RECURRENCE:
					ev.expires = new Date(ev.startTime.getTime() + DateUtils.YEAR_IN_MILLIS * repeat);
					break;
				case Calendar.MONTHLY_RECURRENCE:
					{
						int year = ev.startTime.getYear();
						int month = ev.startTime.getMonth();

						year += (int)Math.floor((month + repeat) / 12.0f);
						month += (month + repeat) % 12;
						ev.expires = new Date(year, month,
								ev.startTime.getDate(),
								ev.startTime.getHours(),
								ev.startTime.getMinutes(),
								ev.startTime.getSeconds());
					}
					break;
				case Calendar.WEEKLY_RECURRENCE:
					ev.expires = new Date(ev.startTime.getTime() + DateUtils.WEEK_IN_MILLIS * repeat);
					break;
				case Calendar.DAILY_RECURRENCE:
					ev.expires = new Date(ev.startTime.getTime() + DateUtils.DAY_IN_MILLIS * repeat);
					break;
				}
			}
			*/
			else if(tokens[i].startsWith("INTERVAL=")) {
				String interval = tokens[i].substring(9);
				try {
					ev.put("interval", Integer.parseInt(interval));
				}
				catch (NumberFormatException e) {
					throw new ParseException("INTERVAL value is not valid", i);
				}
				catch (JSONException e){
					
				}
			}
			else if(tokens[i].startsWith("BYDAY=")){
				String byday = tokens[i].substring(6);
				String[] bydayarray = byday.split(",");
				JSONArray jsonbyday = new JSONArray();
				for(int j = 0 ; j < bydayarray.length ; j ++){
					if(bydayarray[j].equals("SU"))
						jsonbyday.put(0);	
					else if(bydayarray[j].equals("MO"))
						jsonbyday.put(1);
					else if(bydayarray[j].equals("TU"))
						jsonbyday.put(2);
					else if(bydayarray[j].equals("WE"))
						jsonbyday.put(3);
					else if(bydayarray[j].equals("TH"))
						jsonbyday.put(4);
					else if(bydayarray[j].equals("FR"))
						jsonbyday.put(5);
					else if(bydayarray[j].equals("SA"))
						jsonbyday.put(6);
				}
				ev.put("daysInWeek", jsonbyday);
			}
			else if(tokens[i].startsWith("BYMONTHDAY=")){
				String bymonthday = tokens[i].substring(11);
				String[] bymonthdayarray = bymonthday.split(",");
				JSONArray jsonbymonthday = new JSONArray();
				for(int j = 0 ; j < bymonthdayarray.length ; j ++){
					jsonbymonthday.put(Short.valueOf(bymonthdayarray[j]));
				}
				ev.put("daysInMonth", jsonbymonthday);
			}
			else if(tokens[i].startsWith("BYYEARDAY=")){
				String byyearday = tokens[i].substring(10);
				String[] byyeardayarray = byyearday.split(",");
				JSONArray jsonbyyearday = new JSONArray();
				for(int j = 0 ; j < byyeardayarray.length ; j ++){
					jsonbyyearday.put(Short.valueOf(byyeardayarray[j]));
				}
				ev.put("daysInYear", jsonbyyearday);
			}
			else if(tokens[i].startsWith("BYWEEKNO=")){
				String byweekno = tokens[i].substring(9);
				String[] byweeknoarray = byweekno.split(",");
				JSONArray jsonbyweekno = new JSONArray();
				for(int j = 0 ; j < byweeknoarray.length ; j ++){
					jsonbyweekno.put(Short.valueOf(byweeknoarray[j]));
				}
				ev.put("weeksInMonth", jsonbyweekno);
			}
			else if(tokens[i].startsWith("BYMONTH=")){
				String bymonth = tokens[i].substring(8);
				String[] bymontharray = bymonth.split(",");
				JSONArray jsonbymonth = new JSONArray();
				for(int j = 0 ; j < bymontharray.length ; j ++){
					jsonbymonth.put(Short.valueOf(bymontharray[j]));
				}
				ev.put("monthsInYear", jsonbymonth);
			}
			
			else {
				Log.d(LOGTAG, "Unsupported optional RECUR keyword has been provided: " + tokens[i]);
			}
		}
	}

}
