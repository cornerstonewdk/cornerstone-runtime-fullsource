//
//  RTPush.m
//  SRTLib
//
//  Created by dev1 INFRA on 13. 6. 25..
//
//

#import "RTPush.h"
#import "RTViewController.h"

@implementation RTPush

-(void) getRegistrationID:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options{
    NSString* callbackId = [arguments objectAtIndex:0];
    
    //[20120625][chisu]get deviceToken using NSUserDefaults
    NSString *deviceToken = [[NSUserDefaults standardUserDefaults] stringForKey:@"deviceToken"];
    NSLog(@"get device Token = %@",deviceToken);
    
    RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsString:deviceToken];
    [super writeJavascript:[result toSuccessCallbackString:callbackId]];
    
    /*
    NSString* startFilePath = [self.viewController pathForResource:@"menu_sample.html"];
    startFilePath = [NSString stringWithFormat:@"%@",startFilePath];
    
    //[self.viewController.webView loadRequest:[[NSURLRequest alloc] initWithURL:[[NSURL alloc] initWithString:startFilePath]]];
    
    NSURL *testURL = [NSURL URLWithString:startFilePath];
    NSURLRequest *testURLrequest = [NSURLRequest requestWithURL:testURL];
    [[super webView] loadRequest:testURLrequest];
     */
}

- (void) getCurrentURL:(NSMutableArray *)arguments withDict:(NSMutableDictionary *)options{
    NSString* callbackId = [arguments objectAtIndex:0];
    
    //[20120625][chisu]get deviceToken using NSUserDefaults
    
    NSString *currentURL = [self.viewController getCurrentURL];
    
    RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsString:currentURL];
    [super writeJavascript:[result toSuccessCallbackString:callbackId]];
}

@end
