package org.skt.runtime.html5apis.calendar;

public class CalendarRepeatRule {
	
	public String frequency;
	public long interval;
	public long expires;
	public String[] exceptionDates;
	public short[] daysInWeek;
	public short[] daysInMonth;
	public short[] daysInYear;
	public short[] weeksInMonth;
	public short[] monthsInYear;
	
	public CalendarRepeatRule(){
		
	}
	
	public CalendarRepeatRule(String frequency, long interval, long expires,
			String[] exceptionDates, short[] daysInWeek, short[] daysInMonth,
			short[] daysInYear, short[] weeksInMonth, short[] monthsInYear) {
		super();
		this.frequency = frequency;
		this.interval = interval;
		this.expires = expires;
		this.exceptionDates = exceptionDates;
		this.daysInWeek = daysInWeek;
		this.daysInMonth = daysInMonth;
		this.daysInYear = daysInYear;
		this.weeksInMonth = weeksInMonth;
		this.monthsInYear = monthsInYear;
	}
	
	
}
