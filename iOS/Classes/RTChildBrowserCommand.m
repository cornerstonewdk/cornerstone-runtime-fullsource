//  Created by Jesse MacFadyen on 10-05-29.
//  Copyright 2010 Nitobi. All rights reserved.
//  Copyright 2012, Randy McMillan

#import "RTChildBrowserCommand.h"
#import "RTViewController.h"



@implementation RTChildBrowserCommand

@synthesize childBrowser,callbackId;

- (void) showWebPage:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options // args: url
{
    self.callbackId = [arguments objectAtIndex:0];
    if(childBrowser == NULL)
    {
        childBrowser = [[ ChildBrowserViewController alloc ] initWithScale:FALSE ];
        childBrowser.delegate = self;
    }


    RTViewController* cont = (RTViewController*)[ super viewController ];
    childBrowser.supportedOrientations = cont.supportedOrientations;
    [ cont presentModalViewController:childBrowser animated:YES ];

    NSString *url = (NSString*) [arguments objectAtIndex:1];

    [childBrowser loadURL:url  ];

}
- (void) getPage:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options {
    NSString *url = (NSString*) [arguments objectAtIndex:1];
    [childBrowser loadURL:url  ];
}

-(void) close:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options // args: url
{
    [ childBrowser closeBrowser];

}

-(void) onClose
{
    NSString* jsCallback = [NSString stringWithFormat:@"navigator.childBrowser.onClose();",@""];
    [self.webView stringByEvaluatingJavaScriptFromString:jsCallback];
}

-(void) onOpenInSafari
{
    NSString* jsCallback = [NSString stringWithFormat:@"navigator.childBrowser.onOpenExternal();",@""];
    [self.webView stringByEvaluatingJavaScriptFromString:jsCallback];
}


-(void) onChildLocationChange:(NSString*)newLoc
{

    NSString* tempLoc = [NSString stringWithFormat:@"%@",newLoc];
    NSString* encUrl = [tempLoc stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];

    NSString* jsCallback = [NSString stringWithFormat:@"navigator.childBrowser.onLocationChange('%@');",encUrl];
    [self.webView stringByEvaluatingJavaScriptFromString:jsCallback];

}
@end
