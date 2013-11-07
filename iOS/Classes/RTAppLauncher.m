//
//  RTAppLauncher.m
//  SRT_Template
//
//  Created by INFRA dev1 on 12. 9. 17..
//  Copyright (c) 2012년 INFRAWARE. All rights reserved.
//

#import "RTAppLauncher.h"

@implementation RTAppLauncher

- (NSDictionary*) getBundlePlist:(NSString*)plistName
{
    NSString *errorDesc = nil;
    NSPropertyListFormat format;
    NSString *plistPath = [[NSBundle mainBundle] pathForResource:plistName ofType:@"plist"];
    NSData *plistXML = [[NSFileManager defaultManager] contentsAtPath:plistPath];
    NSDictionary *temp = (NSDictionary *)[NSPropertyListSerialization
                                          propertyListFromData:plistXML
                                          mutabilityOption:NSPropertyListMutableContainersAndLeaves              
                                          format:&format errorDescription:&errorDesc];
    return temp;
}


- (void)launchApplication:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options {
    NSString* cbId = [arguments objectAtIndex:0];
    NSString* appname = [arguments objectAtIndex:1];
    NSString* appargs = nil;
    
    if(arguments.count > 2 && ![[arguments objectAtIndex:2] isKindOfClass:[NSNull class]] ) {
        appargs= [arguments objectAtIndex:2];
        //appargs= [[arguments objectAtIndex:2] JSONString];
        //NSData *appData = [appargs dataUsingEncoding:NSUTF8StringEncoding];
        //appargs = [appData base64EncodedString];
    }
    
    NSString* appUrl = [NSString stringWithFormat:@"%@://%@",appname, appargs];
    
    if([[UIApplication sharedApplication] canOpenURL:[NSURL URLWithString:appUrl]]) {
        RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK];
        [super writeJavascript:[result toSuccessCallbackString:cbId]];
        [[UIApplication sharedApplication] openURL:[NSURL URLWithString:appUrl]];
    } else {
        RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_ERROR messageAsString:@"not installed"];
        [super writeJavascript:[result toErrorCallbackString:cbId]]; 
    }
    
}

- (void)getInstalledApplications:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options {
    NSString* cbId = [arguments objectAtIndex:0];
    NSDictionary* SRTDict = [[[NSDictionary alloc]initWithDictionary:[self getBundlePlist:@"SRT"]]autorelease];
    NSDictionary* applistDict = [SRTDict objectForKey:@"InstalledApplications"];
    NSEnumerator *applist = [applistDict objectEnumerator];
    
    NSMutableArray *resultarray = [NSMutableArray arrayWithCapacity:10];
    NSString *appname;
    while(appname = [applist nextObject]) {
        NSString *appscheme = [appname stringByAppendingString:@"://"];
        if([[UIApplication sharedApplication] canOpenURL:[NSURL URLWithString:appscheme]]) {
            [resultarray addObject:appname];
        }
    }
    
    RTPluginResult* result = nil;
    if(resultarray == nil) {
        result = [RTPluginResult resultWithStatus:RTCommandStatus_OK];
    } else {
        result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsArray:resultarray];
    }
    
    [super writeJavascript:[result toSuccessCallbackString:cbId]]; 
}

@end
