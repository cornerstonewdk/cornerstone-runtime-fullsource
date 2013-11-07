/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */


#import "RTConnection.h"
#import "RTReachability.h"

@interface RTConnection(PrivateMethods)
- (void) updateOnlineStatus;
@end

@implementation RTConnection

@synthesize connectionType, internetReach;

- (void) getConnectionInfo:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    RTPluginResult* result = nil;
    NSString* jsString = nil;
	NSString* callbackId = [arguments objectAtIndex:0];
    
    result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsString:self.connectionType];
    jsString = [result toSuccessCallbackString:callbackId];
    [self writeJavascript:jsString];
}

- (NSString*) w3cConnectionTypeFor:(RTReachability*)reachability
{
	NetworkStatus networkStatus = [reachability currentReachabilityStatus];
	switch(networkStatus)
	{
        case NotReachable:
			return @"unknown";
        case ReachableViaWWAN:
			return @"2g"; // no generic default, so we use the lowest common denominator
        case ReachableViaWiFi:
			return @"wifi chisu";
		default:
			return @"none";
    }
}

- (BOOL) isCellularConnection:(NSString*)theConnectionType
{
	return	[theConnectionType isEqualToString:@"2g"] ||
			[theConnectionType isEqualToString:@"3g"] ||
			[theConnectionType isEqualToString:@"4g"];
}

- (void) updateReachability:(RTReachability*)reachability
{
	if (reachability) {
        // check whether the connection type has changed
        NSString* newConnectionType = [self w3cConnectionTypeFor:reachability];
        if ([newConnectionType isEqualToString:self.connectionType]) { // the same as before, remove dupes
            return;
        } else {
            self.connectionType = [self w3cConnectionTypeFor:reachability];
        }
	}
	
	NSString* js = nil;
	// write the connection type
	js = [NSString stringWithFormat:@"navigator.connection.type = '%@';", self.connectionType];
	[super writeJavascript:js];
	
	// send "online"/"offline" event
	[self updateOnlineStatus];
}

- (void) updateConnectionType:(NSNotification*)note
{
	RTReachability* curReach = [note object];

	if (curReach != nil && [curReach isKindOfClass:[RTReachability class]])
	{
		[self updateReachability:curReach];
	}
}

- (void) updateOnlineStatus
{
	// send "online"/"offline" event
	NetworkStatus status = [self.internetReach currentReachabilityStatus];
	BOOL online = (status == ReachableViaWiFi) || (status == ReachableViaWWAN);
	if (online) {
		[super writeJavascript:@"srt.fireDocumentEvent('online');"];
	} else {
		[super writeJavascript:@"srt.fireDocumentEvent('offline');"];
	}
}

- (void) prepare
{
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(updateConnectionType:) 
												 name:kReachabilityChangedNotification object:nil];
	
	self.internetReach = [RTReachability reachabilityForInternetConnection];
	[self.internetReach startNotifier];
	self.connectionType = [self w3cConnectionTypeFor:self.internetReach];
	
	[self performSelector:@selector(updateOnlineStatus) withObject:nil afterDelay:1.0];
}

- (void) onPause 
{
	[self.internetReach stopNotifier];
}

- (void) onResume 
{
    [self.internetReach startNotifier];
    [self updateReachability:self.internetReach];
}

- (RTPlugin*) initWithWebView:(UIWebView*)theWebView
{
    self = (RTConnection*)[super initWithWebView:theWebView];
    if (self) {
        self.connectionType = @"none";
        [self prepare];
        if (&UIApplicationDidEnterBackgroundNotification && &UIApplicationWillEnterForegroundNotification) {
            [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onPause) name:UIApplicationDidEnterBackgroundNotification object:nil];
            [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onResume) name:UIApplicationWillEnterForegroundNotification object:nil];
        }
    }
    return self;
}

- (void)dealloc
{
	self.internetReach = nil;
	[[NSNotificationCenter defaultCenter] removeObserver:self 
													name:kReachabilityChangedNotification object:nil];

    [super dealloc];
}

@end
