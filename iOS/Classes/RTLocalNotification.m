//
//  RTLocalNotification.m
//  c3
//
//  Created by INFRA dev1 on 12. 8. 28..
//  Copyright (c) 2012년 INFRAWARE. All rights reserved.
//

#import "RTLocalNotification.h"

@implementation RTLocalNotification

- (void)add:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options {
    NSString* cbId = [arguments objectAtIndex:0];
    
    NSMutableDictionary *repeatDict = [[NSMutableDictionary alloc] init];
    [repeatDict setObject:[NSNumber numberWithInt:NSDayCalendarUnit] forKey:@"daily"];
    [repeatDict setObject:[NSNumber numberWithInt:NSWeekCalendarUnit] forKey:@"weekly"];
    [repeatDict setObject:[NSNumber numberWithInt:NSMonthCalendarUnit] forKey:@"monthly"];
    [repeatDict setObject:[NSNumber numberWithInt:NSYearCalendarUnit] forKey:@"yearly"];
    [repeatDict setObject:[NSNumber numberWithInt:0] forKey:@""];
    
    // notif settings
	//double timestamp = [[options objectForKey:@"date"]];
	//NSDate *date = [NSDate dateWithTimeIntervalSince1970:timestamp];
    id org_date = [options objectForKey:@"date"];
    NSDate *date = nil;
    
    if([org_date isKindOfClass:[NSDate class]]) {
        date = org_date;
    } else if([org_date isKindOfClass:[NSString class]]) {
        NSLog(@"%@",org_date);
        NSDateFormatter *sDate = [[[NSDateFormatter alloc] init] autorelease];
        [sDate setDateFormat:@"MM/dd/yyyy/HH/mm"];
        date = [sDate dateFromString:org_date];
    } else if([org_date isKindOfClass:[NSNumber class]]) {
        date = [NSDate dateWithTimeIntervalSince1970:[org_date doubleValue]];        
    } else {
        
    }
    
    //NSDate *date = [options objectForKey:@"date"];
	NSString *msg = [options objectForKey:@"message"];
    NSInteger intId = [[options objectForKey:@"id"] integerValue];
    NSString *notificationId = [NSString stringWithFormat:@"%d",intId];
	NSString *action = [options objectForKey:@"action"];
	//NSString *notificationId = [options objectForKey:@"id"];
    NSString *sound = [options objectForKey:@"sound"];
    NSString *bg = [options objectForKey:@"background"];
    NSString *fg = [options objectForKey:@"foreground"];
    NSString *repeat = [options objectForKey:@"repeat"];
	NSInteger badge = [[options objectForKey:@"badge"] intValue];
	bool hasAction = ([[options objectForKey:@"hasAction"] intValue] == 1)?YES:NO;
    
    
	UILocalNotification *notif = [[UILocalNotification alloc] init];
	notif.fireDate = date;
	notif.hasAction = hasAction;
	notif.timeZone = [NSTimeZone defaultTimeZone];
    notif.repeatInterval = [[repeatDict objectForKey: repeat] intValue];
    
	notif.alertBody = ([msg isEqualToString:@""])?nil:msg;
	notif.alertAction = action;
    
    notif.soundName = sound;
    notif.applicationIconBadgeNumber = badge;
    
	NSDictionary *userDict = [NSDictionary dictionaryWithObjectsAndKeys:notificationId,@"notificationId",bg,@"background",fg,@"foreground",nil];
    
    notif.userInfo = userDict;
    
	[[UIApplication sharedApplication] scheduleLocalNotification:notif];
	NSLog(@"Notification Set: %@ (ID: %@, Badge: %i, sound: %@,background: %@, foreground: %@)", date, notificationId, badge, sound,bg,fg);
    
    NSData *data = [NSKeyedArchiver archivedDataWithRootObject:notif];
    [[NSUserDefaults standardUserDefaults] setObject:data forKey:notificationId];
    
	[notif release];
    
    RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK];
    [super writeJavascript:[result toSuccessCallbackString:cbId]]; 
}

- (void)cancel:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options {
    NSString* cbId = [arguments objectAtIndex:0];
    NSInteger intId = [[options objectForKey:@"id"] integerValue];
    NSString *notificationId = [NSString stringWithFormat:@"%d",intId];
    
    NSData *notification = [[NSUserDefaults standardUserDefaults] objectForKey:notificationId];
    if(notification != nil) {
        UILocalNotification *localnotif = [NSKeyedUnarchiver unarchiveObjectWithData:notification];
        NSLog(@"Notification Canceled: %@", notificationId);
        [[UIApplication sharedApplication] cancelLocalNotification:localnotif];
        [[NSUserDefaults standardUserDefaults] removeObjectForKey:notificationId];
        RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK];
        [super writeJavascript:[result toSuccessCallbackString:cbId]]; 
    } else {
        RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_ERROR messageAsString:@"Not found"];
        [super writeJavascript:[result toErrorCallbackString:cbId]]; 
    }
    /*
	NSArray *notifications = [[UIApplication sharedApplication] scheduledLocalNotifications];
    NSInteger count = notifications.count;
	for (UILocalNotification *notification in notifications) {
		NSString *notId = [notification.userInfo objectForKey:@"notificationId"];
		if ([notificationId isEqualToString:notId]) {
			NSLog(@"Notification Canceled: %@", notificationId);
			[[UIApplication sharedApplication] cancelLocalNotification:notification];
		}
	}
    */
}

- (void)cancelAll:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options {
    NSString* cbId = [arguments objectAtIndex:0];
	NSLog(@"All Notifications cancelled");
	[[UIApplication sharedApplication] cancelAllLocalNotifications];
    
    RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK];
    [super writeJavascript:[result toSuccessCallbackString:cbId]]; 
}

@end
