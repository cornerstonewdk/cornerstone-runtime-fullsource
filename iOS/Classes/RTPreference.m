//
//  RTPreference.m
//  SRTLib
//
//  Created by dev1 INFRA on 13. 6. 25..
//
//

#import "RTPreference.h"
#import "RTViewController.h"

@implementation RTPreference

-(void) setItem:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options{
    NSString* callbackId = [arguments objectAtIndex:0];
    
    NSString* key = [arguments objectAtIndex:1];
    NSString* value = [arguments objectAtIndex:2];
    
    //NSLog(@"preference key = %@",key);
    //NSLog(@"preference value = %@",value);

    NSUserDefaults *userDefaults = [NSUserDefaults standardUserDefaults];
    [userDefaults setObject:value forKey:key];
    [userDefaults synchronize];
    
    RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK];
    [super writeJavascript:[result toSuccessCallbackString:callbackId]];
}

-(void) getItem:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options{
    NSString* callbackId = [arguments objectAtIndex:0];
    NSString* key = [arguments objectAtIndex:1];
    
    NSString *value = [[NSUserDefaults standardUserDefaults] stringForKey:key];
    
    //NSLog(@"preference value = %@",value);
    
    RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsString:value];
    [super writeJavascript:[result toSuccessCallbackString:callbackId]];
    
}

-(void) removeItem:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options{
    NSString* callbackId = [arguments objectAtIndex:0];
    
    NSString *deviceToken = [[NSUserDefaults standardUserDefaults] stringForKey:@"deviceToken"];
    RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsString:deviceToken];
    [super writeJavascript:[result toSuccessCallbackString:callbackId]];
    
}

-(void) clear:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options{
    NSString* callbackId = [arguments objectAtIndex:0];
    
    NSString *deviceToken = [[NSUserDefaults standardUserDefaults] stringForKey:@"deviceToken"];
    RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsString:deviceToken];
    [super writeJavascript:[result toSuccessCallbackString:callbackId]];
    
}

@end