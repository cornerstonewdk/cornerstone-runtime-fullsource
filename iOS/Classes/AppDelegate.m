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

//
//  AppDelegate.m
//  c3
//
//  Created by INFRA dev1 on 12. 6. 13..
//  Copyright INFRAWARE 2012년. All rights reserved.
//

#import "AppDelegate.h"
#import "MainViewController.h"


#import "RTPlugin.h"
#import "RTURLProtocol.h"


@implementation AppDelegate

@synthesize window, viewController;

- (id) init
{	
	/** If you need to do any extra app-specific initialization, you can do it here
	 *  -jm
	 **/
    NSHTTPCookieStorage *cookieStorage = [NSHTTPCookieStorage sharedHTTPCookieStorage]; 
    [cookieStorage setCookieAcceptPolicy:NSHTTPCookieAcceptPolicyAlways];
        
    [RTURLProtocol registerURLProtocol];
    
    return [super init];
}

#pragma UIApplicationDelegate implementation

/**
 * This is main kick off after the app inits, the views and Settings are setup here. (preferred - iOS4 and up)
 */
- (BOOL) application:(UIApplication*)application didFinishLaunchingWithOptions:(NSDictionary*)launchOptions
{    
    // APNS에 디바이스를 등록한다.
    [[UIApplication sharedApplication] registerForRemoteNotificationTypes:
     UIRemoteNotificationTypeAlert|
     UIRemoteNotificationTypeBadge|
     UIRemoteNotificationTypeSound];
    
    NSURL* url = [launchOptions objectForKey:UIApplicationLaunchOptionsURLKey];
    NSString* invokeString = nil;
    
    if (url && [url isKindOfClass:[NSURL class]]) {
        invokeString = [url absoluteString];
		NSLog(@"c3 launchOptions = %@", url);
    }    
    
    CGRect screenBounds = [[UIScreen mainScreen] bounds];
    self.window = [[[UIWindow alloc] initWithFrame:screenBounds] autorelease];
    self.window.autoresizesSubviews = YES;
    
    CGRect viewBounds = [[UIScreen mainScreen] applicationFrame];
    
    self.viewController = [[[MainViewController alloc] init] autorelease];
    self.viewController.useSplashScreen = YES;
    self.viewController.wwwFolderName = @"www";
    //self.viewController.wwwFolderName = @"indexed";
    self.viewController.startPage = @"index.html";
    //self.viewController.startPage = @"screencapture.html";
    
    //[20130603][chisu]if this app is call from apns push 
    NSDictionary *userInfo = [launchOptions objectForKey:
                              UIApplicationLaunchOptionsRemoteNotificationKey];
    
    //[20130624][chisu]if start app from push message 
    if(userInfo != nil)
    {
        NSString *softpackagingVerPref = [[NSUserDefaults standardUserDefaults] stringForKey:@"softpackagingVer"];
        
        //소프트패키징을 사용하는데 푸쉬가 왔다면.
        if(softpackagingVerPref != nil){
            self.viewController.pushfromsp = true;
        }
        
        NSString *loadURL = [userInfo objectForKey:@"loadurl"];
        if(loadURL == nil)
            loadURL = @"index.html";
            
        NSString *query = [userInfo objectForKey:@"query"];
        if(query == nil)
            query = @"";
        else
            query = [NSString stringWithFormat:@"%@%@",@"?",query];
        
        self.viewController.startPage = [NSString stringWithFormat:@"%@",loadURL];
        self.viewController.query = query;
        //self.viewController.startPage = [NSString stringWithFormat:@"%@%@",loadURL,query];
    }
    
    self.viewController.invokeString = invokeString;
    self.viewController.view.frame = viewBounds;
    
    // check whether the current orientation is supported: if it is, keep it, rather than forcing a rotation
    BOOL forceStartupRotation = YES;
    UIDeviceOrientation curDevOrientation = [[UIDevice currentDevice] orientation];
    
    if (UIDeviceOrientationUnknown == curDevOrientation) {
        // UIDevice isn't firing orientation notifications yet… go look at the status bar
        curDevOrientation = (UIDeviceOrientation)[[UIApplication sharedApplication] statusBarOrientation];
    }
    
    if (UIDeviceOrientationIsValidInterfaceOrientation(curDevOrientation)) {
        for (NSNumber *orient in self.viewController.supportedOrientations) {
            if ([orient intValue] == curDevOrientation) {
                forceStartupRotation = NO;
                break;
            }
        }
    } 
    
    if (forceStartupRotation) {
        NSLog(@"supportedOrientations: %@", self.viewController.supportedOrientations);
        // The first item in the supportedOrientations array is the start orientation (guaranteed to be at least Portrait)
        UIInterfaceOrientation newOrient = [[self.viewController.supportedOrientations objectAtIndex:0] intValue];
        NSLog(@"AppDelegate forcing status bar to: %d from: %d", newOrient, curDevOrientation);
        [[UIApplication sharedApplication] setStatusBarOrientation:newOrient];
    }
    
    [self.window addSubview:self.viewController.view];
    [self.window makeKeyAndVisible];
    
    return YES;
}

// this happens while we are running ( in the background, or from within our own app )
// only valid if c3-Info.plist specifies a protocol to handle
- (BOOL) application:(UIApplication*)application handleOpenURL:(NSURL*)url 
{
    if (!url) { 
        return NO; 
    }
    
    //[20130627][chisu]Custom URL scheme
    NSLog(@"Custom URL scheme = %@",[url scheme]);
    NSLog(@"Custom URL host = %@",[url host]);
    NSLog(@"Custom URL query = %@",[url query]);
    NSLog(@"Custom URL relativePath = %@",[url relativePath]);
    if([[url scheme] isEqualToString:@"cornerstone"]){
        
        NSString *loadURL = [url relativePath];
        if([loadURL isEqualToString:@""])
            loadURL = @"index.html";
        else
            loadURL = [loadURL stringByReplacingOccurrencesOfString:@"/" withString:@""];
        
        NSString *query = [url query];
        if(query == nil)
            query = @"";
        else
            query = [NSString stringWithFormat:@"%@%@",@"?",query];
        
        NSString* startFilePath = [self.viewController pathForResource:loadURL];
        startFilePath = [NSString stringWithFormat:@"%@%@",startFilePath,query];
        
        self.viewController.currentURL = startFilePath;
        
        [self.viewController.webView loadRequest:[[NSURLRequest alloc] initWithURL:[[NSURL alloc] initWithString:startFilePath]]];
        
        return YES;
    }
    
	// calls into javascript global function 'handleOpenURL'
    NSString* jsString = [NSString stringWithFormat:@"handleOpenURL(\"%@\");", url];
    [self.viewController.webView stringByEvaluatingJavaScriptFromString:jsString];
    
    // all plugins will get the notification, and their handlers will be called 
    [[NSNotificationCenter defaultCenter] postNotification:[NSNotification notificationWithName:RTPluginHandleOpenURLNotification object:url]];
    
    return YES;    
}

//[20130603][chisu]APNS use
- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
    NSMutableString *deviceId = [NSMutableString string];
    const unsigned char* ptr = (const unsigned char*) [deviceToken bytes];
    
    for(int i = 0 ; i < 32 ; i++)
    {
        [deviceId appendFormat:@"%02x", ptr[i]];
    }
    
    NSLog(@"APNS Device Token: %@", deviceId);
    
    //[20120625][chisu]save deviceToken using NSUserDefaults
    NSUserDefaults *userDefaults = [NSUserDefaults standardUserDefaults];
    [userDefaults setObject:deviceId forKey:@"deviceToken"];
    [userDefaults synchronize];
}

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo
{
//    NSString *string = [NSString stringWithFormat:@"%@", userInfo];
//    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:nil
//                                                    message:string delegate:nil
//                                          cancelButtonTitle:@"In App OK"
//                                          otherButtonTitles:nil];
//    [alert show]; 
//    [alert release];
    
    NSString *softpackagingVerPref = [[NSUserDefaults standardUserDefaults] stringForKey:@"softpackagingVer"];
    
    if(softpackagingVerPref != nil){
        NSString *loadURL = [userInfo objectForKey:@"loadurl"];
        if(loadURL == nil)
            loadURL = @"index.html";
        
        NSString *query = [userInfo objectForKey:@"query"];
        if(query == nil)
            query = @"";
        else
            query = [NSString stringWithFormat:@"%@%@",@"?",query];
        
        NSString *startFilePath = [NSHomeDirectory() stringByAppendingPathComponent:@"Documents/hydapp"];
        //startFilePath = [NSString stringWithFormat:@"%@%@%@%@",startFilePath,@"/",loadURL,query];
        startFilePath = [NSString stringWithFormat:@"%@%@%@",startFilePath,@"/",loadURL];
        
        self.viewController.currentURL = [NSString stringWithFormat:@"%@%@",startFilePath,query];
        
        [self.viewController.webView loadRequest:[[NSURLRequest alloc] initWithURL:[[NSURL alloc] initWithString:startFilePath]]];
    }
    else{
        NSString *loadURL = [userInfo objectForKey:@"loadurl"];
        if(loadURL == nil)
            loadURL = @"index.html";
        
        NSString *query = [userInfo objectForKey:@"query"];
        if(query == nil)
            query = @"";
        else
            query = [NSString stringWithFormat:@"%@%@",@"?",query];
        
        NSString* startFilePath = [self.viewController pathForResource:loadURL];
        //startFilePath = [NSString stringWithFormat:@"%@%@",startFilePath,query];
        
        self.viewController.currentURL = [NSString stringWithFormat:@"%@%@",startFilePath,query];
        
        [self.viewController.webView loadRequest:[[NSURLRequest alloc] initWithURL:[[NSURL alloc] initWithString:startFilePath]]];
    }
}

- (void)application:(UIApplication*)application didFailToRegisterForRemoteNotificationsWithError:(NSError*)error
{
    
    NSLog(@"Failed to get token, error: %@", error);
}

- (void) dealloc
{
	[super dealloc];
}

@end
