//
//  RTCalendar.m
//  c3
//
//  Created by INFRA dev1 on 12. 6. 27..
//  Copyright (c) 2012년 INFRAWARE. All rights reserved.
//

#import "RTCalendar.h"
#import "NSDictionary+Extensions.h"

static NSDictionary*	calendars_W3CtoEK_Dict = nil;
static NSDictionary*	calendars_EKtoW3C_Dict = nil;

@implementation RTCalendar

@synthesize callbackId;


- (RTCalendar*) init
{
    self = [super init];
    if (self)
    {
        self.callbackId = nil;
    }
    return self;
}

- (void) dealloc {
    
    [super dealloc]; // pretty important.
}

-(void)onAppTerminate
{
	//NSLog(@"Contacts::onAppTerminate");
	if(calendars_W3CtoEK_Dict != nil) {
        [calendars_W3CtoEK_Dict release];
        calendars_W3CtoEK_Dict = nil;
    }
    
    if(calendars_EKtoW3C_Dict != nil) {
        [calendars_EKtoW3C_Dict release];
        calendars_EKtoW3C_Dict = nil;
    }
}

+ (NSDictionary*) defaultEKtoW3C {
    if(calendars_EKtoW3C_Dict == nil) {
        calendars_EKtoW3C_Dict = [NSDictionary dictionaryWithObjectsAndKeys:
                                  kW3CalendarRecurrence_FrequencyDaily, [NSNumber numberWithInt:EKRecurrenceFrequencyDaily],
                                  kW3CalendarRecurrence_FrequencyWeekly, [NSNumber numberWithInt:EKRecurrenceFrequencyWeekly],
                                  kW3CalendarRecurrence_FrequencyMonthly, [NSNumber numberWithInt:EKRecurrenceFrequencyMonthly],
                                  kW3CalendarRecurrence_FrequencyYearly, [NSNumber numberWithInt:EKRecurrenceFrequencyYearly],
                                  kW3CalendarStatusPending, [NSNumber numberWithInt:EKEventStatusNone + kW3CalendarStatusEnum],
                                  kW3CalendarStatusTentative, [NSNumber numberWithInt:EKEventStatusTentative + kW3CalendarStatusEnum],
                                  kW3CalendarStatusConfirmed, [NSNumber numberWithInt:EKEventStatusConfirmed + kW3CalendarStatusEnum],
                                  kW3CalendarStatusCancelled, [NSNumber numberWithInt:EKEventStatusCanceled + kW3CalendarStatusEnum],
                                  nil];
        [calendars_EKtoW3C_Dict retain];
    }
    
    return calendars_EKtoW3C_Dict;
}

+ (NSDictionary*) defaultW3CtoEK {
    if(calendars_W3CtoEK_Dict == nil) {
        calendars_W3CtoEK_Dict = [NSDictionary dictionaryWithObjectsAndKeys:
                                  [NSNumber numberWithInt:EKRecurrenceFrequencyDaily], kW3CalendarRecurrence_FrequencyDaily,
                                  [NSNumber numberWithInt:EKRecurrenceFrequencyWeekly], kW3CalendarRecurrence_FrequencyWeekly,
                                  [NSNumber numberWithInt:EKRecurrenceFrequencyMonthly], kW3CalendarRecurrence_FrequencyMonthly,
                                  [NSNumber numberWithInt:EKRecurrenceFrequencyYearly], kW3CalendarRecurrence_FrequencyYearly,
                                  [NSNumber numberWithInt:EKEventStatusNone], kW3CalendarStatusPending,
                                  [NSNumber numberWithInt:EKEventStatusTentative], kW3CalendarStatusTentative,
                                  [NSNumber numberWithInt:EKEventStatusConfirmed], kW3CalendarStatusConfirmed,
                                  [NSNumber numberWithInt:EKEventStatusCanceled], kW3CalendarStatusCancelled,
                                  nil];
        [calendars_W3CtoEK_Dict retain];
    }
    
    return calendars_W3CtoEK_Dict;
}

/*
- (EKRecurrenceFrequency)frequencyW3toEK:(NSString*)frequencyW3
{
    EKRecurrenceFrequency recurrenceFrequency = EKRecurrenceFrequencyDaily;
    if([frequencyW3 isEqualToString:@"daily"])
        return EKRecurrenceFrequencyDaily;
    else if([frequencyW3 isEqualToString:@"weekly"])
        return EKRecurrenceFrequencyWeekly;
    else if([frequencyW3 isEqualToString:@"monthly"])
        recurrenceFrequency = EKRecurrenceFrequencyMonthly;
    else if([frequencyW3 isEqualToString:@"yearly"])
        recurrenceFrequency = EKRecurrenceFrequencyYearly;

    return recurrenceFrequency; 
}
 */

- (NSString*)getStringFromDate:(NSDate*)key
{
    NSDateFormatter *sDate = [[[NSDateFormatter alloc] init] autorelease];
    [sDate setDateFormat:@"yyyy-MM-dd HH:mm"];
    return [sDate stringFromDate:key];
}


- (NSDate*)getDateFromString:(NSString*)key
{
    NSDateFormatter *sDate = [[[NSDateFormatter alloc] init] autorelease];
    [sDate setDateFormat:@"yyyy-MM-dd HH:mm"];
    return [sDate dateFromString:key];
}

- (NSDate*)getDateFromDict:(NSString*)key withDict:(NSMutableDictionary*)options
{
    if([options valueForKeyIsNull:key]) return nil;
    NSDateFormatter *sDate = [[[NSDateFormatter alloc] init] autorelease];
    [sDate setDateFormat:@"yyyy-MM-dd HH:mm"];
    NSString *date = [options valueForKey:key];
    return [sDate dateFromString:date];
}

- (EKRecurrenceRule*)getRecurrenceRule:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    EKRecurrenceRule *recurrRule = [[[EKRecurrenceRule alloc] init] autorelease]; 
    
    EKRecurrenceFrequency recurrenceFrequency = EKRecurrenceFrequencyDaily;

    NSMutableArray *daysOfTheWeek = nil;
    NSMutableArray *daysOfTheMonth = nil;
    NSMutableArray *daysOfTheYear = nil;
    NSMutableArray *weeksOfTheYear = nil;
    NSMutableArray *monthsOfTheYear = nil;
    NSMutableArray *setPositions = nil;;
    
    NSMutableDictionary* recurrDict = options;
    
    
    if(![recurrDict valueForKeyIsNull:kW3CalendarRecurrence_Frequency]) {
        NSString *fre = [recurrDict valueForKey:kW3CalendarRecurrence_Frequency];
        NSDictionary *dwe = [RTCalendar defaultW3CtoEK];
        NSString *t2 = [dwe valueForKey:fre];
        recurrenceFrequency = (EKRecurrenceFrequency)[t2 intValue];
    }

    NSInteger interval = [recurrDict integerValueForKey:kW3CalendarRecurrence_Interval defaultValue:1];

    if( recurrenceFrequency == EKRecurrenceFrequencyWeekly && 
       ![recurrDict valueForKeyIsNull:kW3CalendarRecurrence_daysInWeek]) {
        daysOfTheWeek = [[NSMutableArray alloc] init];
        NSArray *days = [recurrDict objectForKey:kW3CalendarRecurrence_daysInWeek];
        
        for(int i=0; i<days.count; i++) {
            NSInteger day = [[days objectAtIndex:i] intValue] + 1;
            if(day < 1)  day = 1;
            if(day > 7)  day = 7;
            [daysOfTheWeek addObject:[EKRecurrenceDayOfWeek dayOfWeek:day]];
        }
    }
    
    if( recurrenceFrequency == EKRecurrenceFrequencyMonthly && 
       ![recurrDict valueForKeyIsNull:kW3CalendarRecurrence_daysInMonth]) {
        NSMutableArray *days = [recurrDict objectForKey:kW3CalendarRecurrence_daysInMonth];
        
        for(int i=0; i < days.count; i++) {
            NSInteger day = [[days objectAtIndex:i] intValue];
            if(day <= 0) {
                [days replaceObjectAtIndex:i withObject:[NSNumber numberWithInt:day-1]];
            }
        }
        daysOfTheMonth = days;
    }
    
    if( recurrenceFrequency == EKRecurrenceFrequencyYearly && 
       ![recurrDict valueForKeyIsNull:kW3CalendarRecurrence_daysInYear]) {
        NSMutableArray *days = [recurrDict objectForKey:kW3CalendarRecurrence_daysInYear];
        for(int i=0; i < days.count; i++) {
            NSInteger day = [[days objectAtIndex:i] intValue];
            if(day <= 0) {
                [days replaceObjectAtIndex:i withObject:[NSNumber numberWithInt:day-1]];
            }
        }
        daysOfTheYear = days;
    }
    
    if( recurrenceFrequency == EKRecurrenceFrequencyYearly && 
       ![recurrDict valueForKeyIsNull:kW3CalendarRecurrence_monthsInYear]) {
        monthsOfTheYear = [recurrDict objectForKey:kW3CalendarRecurrence_monthsInYear];
    }
    
    
    NSDateFormatter *sDate = [[NSDateFormatter alloc] init];
    [sDate setDateFormat:@"yyyy-MM-dd HH:mm"];
    [sDate release];
    NSDate *recurrenceEndDate = [self getDateFromDict:kW3CalendarRecurrence_Expires withDict:recurrDict];
    
    if(recurrenceEndDate == nil) {
        recurrenceEndDate = [self getDateFromString:@"2030-12-31 00:00"];       
    }
    
    EKRecurrenceEnd *end = [EKRecurrenceEnd recurrenceEndWithEndDate:recurrenceEndDate];    
    //EKRecurrenceEnd *end = [EKRecurrenceEnd recurrenceEndWithOccurrenceCount:0];    
    
    
    [recurrRule initRecurrenceWithFrequency:recurrenceFrequency
                                   interval:interval
                              daysOfTheWeek:daysOfTheWeek
                             daysOfTheMonth:daysOfTheMonth
                            monthsOfTheYear:monthsOfTheYear
                             weeksOfTheYear:weeksOfTheYear
                              daysOfTheYear:daysOfTheYear
                               setPositions:setPositions
                                        end:end];
    
    [daysOfTheWeek release];
    return recurrRule;
}


- (void)addEvent:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    
    self.callbackId = [arguments objectAtIndex:0];
	NSMutableDictionary* calendarDict = options;
    EKEventStore *eventStore = [[[EKEventStore alloc] init]autorelease];
    EKEvent *event  = [EKEvent eventWithEventStore:eventStore];

    event.title     = [calendarDict valueForKey:kW3CalendarSummary];
    event.startDate = [self getDateFromDict:kW3CalendarStart withDict:calendarDict];
    event.endDate   = [self getDateFromDict:kW3CalendarEnd withDict:calendarDict];
    event.notes     = [calendarDict valueForKey:kW3CalendarDescription];
    event.location  = [calendarDict valueForKey:kW3CalendarLocation];
    
    
    NSMutableDictionary *recurrDict = [calendarDict objectForKey:kW3CalendarRecurrence];
    if(recurrDict != nil && ![recurrDict isKindOfClass:[NSNull class]]) {
        [event addRecurrenceRule:[self getRecurrenceRule:nil withDict:recurrDict]];        
    } else {
        // no recurrenceRule
    }
    
    NSDate *reminderDate = [self getDateFromDict:kW3CalendarReminder withDict:calendarDict];
    
    // set a reminder
    if(reminderDate != nil) {
        [event addAlarm:[EKAlarm alarmWithAbsoluteDate:reminderDate]];
    } else if(![calendarDict valueForKeyIsNull:kW3CalendarReminder]) {
        NSString *reminder = [calendarDict valueForKey:kW3CalendarReminder];
        double reminderTime = [reminder doubleValue];
        if(reminderTime < 0) {
            [event addAlarm:[EKAlarm alarmWithRelativeOffset:reminderTime / 1000]];
        } else {
            // handling exception
        }
    }    

    [event setCalendar:[eventStore defaultCalendarForNewEvents]];
    NSError *err;
    [eventStore saveEvent:event span:EKSpanThisEvent error:&err];

    if (err == noErr)
    {
        NSString *id = event.eventIdentifier;
        [calendarDict setObject:id forKey:kW3CalendarId];
        RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsDictionary:calendarDict];
        [super writeJavascript:[result toSuccessCallbackString:self.callbackId]];  
    }
    else
    {
        RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_ERROR messageAsInt:UNKNOWN_ERROR];
        [super writeJavascript:[result toErrorCallbackString:self.callbackId]];
    }
}

- (void)findEvents:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options {
    self.callbackId = [arguments objectAtIndex:0];
    NSMutableDictionary* findOptions = options;
    NSDate *startBefore = nil;
    NSDate *startAfter = nil;
    NSDate *endBefore = nil;
    NSDate *endAfter = nil;    
    
    BOOL multiple = YES; // default is true
    BOOL searchAll = NO;
    if (findOptions == nil || [findOptions isKindOfClass:[NSNull class]]){
        searchAll = YES;
    } else {
        id value = [findOptions objectForKey:kW3CalendarFindOptions_multiple];
        if ([value isKindOfClass:[NSNumber class]]){
            multiple = [(NSNumber*)value boolValue];
        }
        if([findOptions valueForKeyIsNull:kW3CalendarFindOptions_filter]) {
            searchAll = YES;
        } else {
            NSMutableDictionary* filter = [findOptions objectForKey:kW3CalendarFindOptions_filter];
            startBefore = [self getDateFromDict:kW3CalendarFindOptions_startBefore withDict:filter];
            startAfter = [self getDateFromDict:kW3CalendarFindOptions_startAfter withDict:filter];
            endBefore = [self getDateFromDict:kW3CalendarFindOptions_endBefore withDict:filter];
            endAfter = [self getDateFromDict:kW3CalendarFindOptions_endAfter withDict:filter];
        }
    }
    
    EKEventStore *eventStore = [[[EKEventStore alloc] init]autorelease];

    NSDate *start = [self getDateFromString:@"2010-01-01 00:00"];
    NSDate *finish = [self getDateFromString:@"2020-12-31 00:00"];
    
    // use Dictionary for remove duplicates produced by events covered more one year segment
    NSMutableDictionary *eventsDict = [NSMutableDictionary dictionaryWithCapacity:30];
    NSDate* currentStart = [NSDate dateWithTimeInterval:0 sinceDate:start];
    
    int seconds_in_year = 60*60*24*365*3;
    
    // enumerate events by one year segment because iOS do not support predicate longer than 4 year !
    while ([currentStart compare:finish] == NSOrderedAscending) {
        NSDate* currentFinish = [NSDate dateWithTimeInterval:seconds_in_year sinceDate:currentStart];
        if ([currentFinish compare:finish] == NSOrderedDescending) {
            currentFinish = [NSDate dateWithTimeInterval:0 sinceDate:finish];
        }
        NSPredicate *predicate = [eventStore predicateForEventsWithStartDate:currentStart endDate:currentFinish calendars:nil];
        [eventStore enumerateEventsMatchingPredicate:predicate usingBlock:^(EKEvent *event, BOOL *stop) {
            if (!event) {
                return;
            }
                        
            bool find = NO;
            if(startBefore != nil && ([event.startDate compare:startBefore]) == NSOrderedAscending) {
                find = YES;
            }
            
            if(startAfter != nil && ([event.startDate compare:startAfter]) == NSOrderedDescending) {
                find = YES;
            }
            
            if(endBefore != nil && ([event.endDate compare:endBefore]) == NSOrderedAscending) {
                find = YES;
            }
            
            if(endAfter != nil && ([event.endDate compare:endAfter]) == NSOrderedDescending) {
                find = YES;
            }
            
            if(find || searchAll) {
                if(multiple) {
                    *stop = NO;
                    [eventsDict setObject:event forKey:event.eventIdentifier];
                } else {
                    *stop = YES;
                    if(eventsDict.count == 0) {
                        [eventsDict setObject:event forKey:event.eventIdentifier];
                    }
                }
            }
 
        }];       
        currentStart = [NSDate dateWithTimeInterval:(seconds_in_year + 1) sinceDate:currentStart];
    }
    
    NSArray *events = [eventsDict allValues];
    
    NSMutableArray *arrayCalendars = [NSMutableArray arrayWithCapacity:30];
    
    for(EKEvent *event in events) {
        
        NSMutableDictionary *calendarDict = [NSMutableDictionary dictionaryWithCapacity:10];
        NSString *eventid = event.eventIdentifier;
        NSString *start = [self getStringFromDate:event.startDate];
        NSString *end = [self getStringFromDate:event.endDate];
        NSString *description = event.notes;
        NSString *location = event.location;
        NSString *summary = event.title;
        NSString *status = [[RTCalendar defaultEKtoW3C] objectForKey:[NSNumber numberWithInt:event.status + kW3CalendarStatusEnum]];
        NSString *reminder = nil;
        NSArray *alarms = [event alarms];
        for(EKAlarm *alarm in alarms) {
            if(alarm.absoluteDate) {
                reminder = [self getStringFromDate:alarm.absoluteDate];
            } else {
                reminder = [NSString stringWithFormat:@"%f",alarm.relativeOffset * 1000];
            }
        }
        
        NSMutableDictionary *recurrDict = [NSMutableDictionary dictionaryWithCapacity:6];
        EKRecurrenceRule *ere = event.recurrenceRules;
        NSString *frequency = [[RTCalendar defaultEKtoW3C] objectForKey:[NSNumber numberWithInt:ere.frequency]];
        NSNumber *interval = [NSNumber numberWithInt:ere.interval];
        
        if(frequency != nil)
            [recurrDict setObject:frequency forKey:kW3CalendarRecurrence_Frequency];
        if(interval != nil)
            [recurrDict setObject:interval forKey:kW3CalendarRecurrence_Interval];
        
        if(ere.daysOfTheWeek != nil) {
            NSMutableArray *daysOfTheWeek = [NSMutableArray arrayWithCapacity:7];
            for(EKRecurrenceDayOfWeek *day in ere.daysOfTheWeek) {
                NSNumber *daynumber = [NSNumber numberWithInteger:day.dayOfTheWeek-1];
                [daysOfTheWeek addObject:daynumber];
            }
            [recurrDict setObject:daysOfTheWeek forKey:kW3CalendarRecurrence_daysInWeek];
        }
        
        if(ere.daysOfTheMonth != nil) {
            [recurrDict setObject:ere.daysOfTheMonth forKey:kW3CalendarRecurrence_daysInMonth];
        }
        if(ere.daysOfTheYear != nil) {
            [recurrDict setObject:ere.daysOfTheYear forKey:kW3CalendarRecurrence_daysInYear];
        }
        if(ere.monthsOfTheYear != nil) {
            [recurrDict setObject:ere.monthsOfTheYear forKey:kW3CalendarRecurrence_monthsInYear];
        }
        if(recurrDict != nil)
            [calendarDict setObject:recurrDict forKey:kW3CalendarRecurrence];

        
        if(eventid != nil)
            [calendarDict setObject:eventid forKey:kW3CalendarId];
        if(start != nil)
            [calendarDict setObject:start forKey:kW3CalendarStart];
        if(end != nil)
            [calendarDict setObject:end forKey:kW3CalendarEnd];
        if(description != nil)
            [calendarDict setObject:description forKey:kW3CalendarDescription];
        if(location != nil)
            [calendarDict setObject:location forKey:kW3CalendarLocation];
        if(summary != nil)
            [calendarDict setObject:summary forKey:kW3CalendarSummary];
        if(status != nil)
            [calendarDict setObject:status forKey:kW3CalendarStatus];
        if(reminder != nil)
            [calendarDict setObject:reminder forKey:kW3CalendarReminder];
        [arrayCalendars addObject:calendarDict];
        
    }
    
    RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsArray:arrayCalendars];
    [super writeJavascript:[result toSuccessCallbackString:self.callbackId]]; 
}

- (void)deleteEvent:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options {
    self.callbackId = [arguments objectAtIndex:0];
    NSString *id = [arguments objectAtIndex:1];
    EKEventStore *eventStore = [[[EKEventStore alloc] init]autorelease];
    EKEvent *event = [eventStore eventWithIdentifier:id];
    
    NSError *err;
    [eventStore removeEvent:event span:EKSpanFutureEvents error:&err];
    
    if (event != nil && err == noErr)
    {
        RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK];
        [super writeJavascript:[result toSuccessCallbackString:self.callbackId]]; 
    } else {
        RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_ERROR];
        [super writeJavascript:[result toErrorCallbackString:self.callbackId]]; 
    }
    
}
@end                                                                                                                                                                     
