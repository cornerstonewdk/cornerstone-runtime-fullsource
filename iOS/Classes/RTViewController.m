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

#import "RT.h"

#define SYMBOL_TO_NSSTRING_HELPER(x) @#x
#define SYMBOL_TO_NSSTRING(x) SYMBOL_TO_NSSTRING_HELPER(x)
#define degreesToRadian(x) (M_PI * (x) / 180.0)

@interface RTViewController ()

@property (nonatomic, readwrite, retain) NSDictionary* settings;
@property (nonatomic, readwrite, retain) NSDictionary* softpackagingPlist;
@property (nonatomic, readwrite, retain) CDVWhitelist* whitelist; 
@property (nonatomic, readwrite, retain) NSMutableDictionary* pluginObjects;
@property (nonatomic, readwrite, retain) NSDictionary* pluginsMap;
@property (nonatomic, readwrite, retain) NSArray* supportedOrientations;
@property (nonatomic, readwrite, assign) BOOL loadFromString;

@property (nonatomic, readwrite, retain) IBOutlet UIImageView* activityView;
@property (nonatomic, readwrite, retain) UIImageView* imageView;

@end


@implementation RTViewController

@synthesize webView, supportedOrientations;
@synthesize pluginObjects, pluginsMap, whitelist;
@synthesize settings, loadFromString;
@synthesize imageView, activityView, useSplashScreen, commandDelegate;
@synthesize wwwFolderName, startPage, invokeString, query;
@synthesize startloadingtime, endloadingtime;
@synthesize useStatusbarSpinner, useScreenSpinner;
//[20130912][chisu]for soft packaging
@synthesize theConnection, DownLoad_Data,Total_FileSize,pushfromsp,currentURL ;
@synthesize softpackagingPlist ;


- (id) __init
{
    if (self != nil) 
    {
        [[UIDevice currentDevice] beginGeneratingDeviceOrientationNotifications];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(receivedOrientationChange) 
                                                     name:UIDeviceOrientationDidChangeNotification object:nil];
        
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onAppWillTerminate:) 
                                                     name:UIApplicationWillTerminateNotification object:nil];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onAppWillResignActive:) 
                                                     name:UIApplicationWillResignActiveNotification object:nil];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onAppDidBecomeActive:) 
                                                     name:UIApplicationDidBecomeActiveNotification object:nil];
        
        if (IsAtLeastiOSVersion(@"4.0")) 
        {
            [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onAppWillEnterForeground:) 
                                                         name:UIApplicationWillEnterForegroundNotification object:nil];
            [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onAppDidEnterBackground:) 
                                                         name:UIApplicationDidEnterBackgroundNotification object:nil];
        }
        
        self.commandDelegate = self;
        self.wwwFolderName = @"www";
        //self.wwwFolderName = @"indexed";
        self.startPage = @"index.html";
        self.query = @"";

        [self setWantsFullScreenLayout:YES];
        
        [self printMultitaskingInfo];
    }
    
    return self; 
}

-(id) initWithNibName:(NSString*)nibNameOrNil bundle:(NSBundle*)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    return [self __init];
}

- (id) init
{
    self = [super init];
    return [self __init];
}

- (void) printMultitaskingInfo
{
    UIDevice* device = [UIDevice currentDevice];
    BOOL backgroundSupported = NO;
    if ([device respondsToSelector:@selector(isMultitaskingSupported)]) {
        backgroundSupported = device.multitaskingSupported;
    }
    
    NSNumber* exitsOnSuspend = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"UIApplicationExitsOnSuspend"];
    if (exitsOnSuspend == nil) { // if it's missing, it should be NO (i.e. multi-tasking on by default)
        exitsOnSuspend = [NSNumber numberWithBool:NO];
    }

    NSLog(@"Multi-tasking -> Device: %@, App: %@", (backgroundSupported? @"YES" : @"NO"), (![exitsOnSuspend intValue])? @"YES" : @"NO");
}


//[20130823][chisu]for hydration
- (void) getDocFolder
{
    BOOL flag = NO;
    
    //?åÏùº Îß§Îãà???ùÏÑ±
    NSFileManager* fileManager = [NSFileManager defaultManager];
    
    // Document ?îÎ†â?†Î¶¨ Î∂àÎü¨??    NSString* rootDir = [NSHomeDirectory() stringByAppendingPathComponent:@"Documents"];
    // App.app ?îÎ†â?†Î¶¨ Î∂àÎü¨?Ä
    //NSString* rootDir = [[NSBundle mainBundle] bundlePath];
    
    
    // ?îÎ†â?†Î¶¨ ?¥Í±∞???ùÏÑ±
    NSDirectoryEnumerator *enumerator = [fileManager enumeratorAtPath:rootDir];
    NSString *path;
    // nextObject ?¥Ïö© ?åÏùº?¥Î¶Ñ??Î∂àÎü¨??    while( (path = [enumerator nextObject]) != nil ){
        // fileExistsAtPath ?¥Ïö©, ?åÏùº??Ï°¥Ïû¨?òÎäîÏßÄ ?ïÏù∏
        // idDirectory???∏ÏûêÎ°?BOOL Î≥Ä???¨Ïù∏?∞Î? ÏßÄ?? ?¥Îçî?∏Ï? ?åÏùº?∏Ï? ?êÎã®
        if( [fileManager fileExistsAtPath:
             [rootDir stringByAppendingPathComponent:path] isDirectory:&flag] ){
            NSLog(@"%@ ?¥Î¶Ñ : %@", (flag ? (@"[?¥Îçî]") : (@"[?åÏùº]") ), path );
        }
    }
    
    //[self unzip];
}
//[20130823][chisu]for hydration
- (void) unzip
{
    //document path
    NSArray *down_pa = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *down_filepath2 = [down_pa objectAtIndex:0];
    NSString *down_filePath = [down_filepath2 stringByAppendingPathComponent:@"www.zip"];
    
    //NSString* rootDir = [[NSBundle mainBundle] bundlePath];
    
    //NSString *zipPath = [rootDir stringByAppendingPathComponent:@"www/www.zip"];
    //NSString *destinationPath = [rootDir stringByAppendingPathComponent:@"test/"];
    
    NSString *destinationPath = [NSHomeDirectory() stringByAppendingPathComponent:@"Documents/hydapp"];
    [SSZipArchive unzipFileAtPath:down_filePath toDestination:destinationPath];
    
    //[self getDocFolder];
}

//[20130823][chisu]for hydration
- (void)downloadzip
{
    NSString* Down_URL = @"http://121.78.237.180:20280/download/www.zip";
    
    NSURLRequest* theRequest = [NSURLRequest requestWithURL:[NSURL URLWithString:Down_URL] cachePolicy:NSURLRequestUseProtocolCachePolicy timeoutInterval:60.0];
    theConnection = [[NSURLConnection alloc] initWithRequest:theRequest delegate:self];
}

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response {
    if(connection != theConnection)
        return;
    //?†ÌÉà Î∞îÏù¥??(Ï¥ùÏö©?? : ?úÎ≤Ñ?êÏÑú Î∞õÏ? ResponseÎ•?Î∂ÑÏÑù?¥ÏÑú ?§Ïö¥Î°úÎìú???åÏùº??Ï¥??©Îüâ??Íµ¨Ìï¥?®Îã§.
    Total_FileSize = [[NSNumber numberWithLongLong:[response expectedContentLength]] longValue];
    NSLog(@"content-length: %ld bytes", Total_FileSize);
    
    DownLoad_Data = [[NSMutableData alloc] init];
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data{
    if(connection != theConnection)
        return;
    //data???úÎ≤Ñ???åÏùº??Ï°∞Í∞ÅÏ°∞Í∞Å Î∂àÎü¨?®Îã§. Î°úÏª¨?êÏÑú Ï°∞Í∞ÅÏ°∞Í∞Å Î∂àÎü¨???åÏùº??Î∂ôÏó¨???òÎÇò???åÏùºÎ°?ÎßåÎì§?¥ÎÜì?îÎã§.
    [DownLoad_Data appendData:data];
    //?§Ïö¥Î∞õÏ? ?åÏùº???©Îüâ??Î≥¥Ïó¨Ï§Ä??
    NSNumber* ResponeLength = [NSNumber numberWithUnsignedInteger:[DownLoad_Data length]];
    NSLog(@"Downloading... size : %ld", [ResponeLength longValue]);
    
    //Ï¥ùÏö©??    float FileSize = (float)Total_FileSize;
    
    //?§Ïö¥Î°úÎìú???∞Ïù¥???©Îüâ
    float Down_Filesize = [ResponeLength floatValue];
    NSLog(@"Down : %f", Down_Filesize / FileSize);
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
    if(connection != theConnection)
        return;
    NSFileManager* FM = [NSFileManager defaultManager];
    //[iPhone] ?åÏùº ?úÏä§??(Document Directory Í≤ΩÎ°úÏ∞æÍ∏∞)
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsDirectory = [paths objectAtIndex:0];
    
    
    NSString *downloadPath = [documentsDirectory stringByAppendingPathComponent:@"www.zip"];

    if([FM createFileAtPath:downloadPath contents:DownLoad_Data attributes:nil])
    {
        NSLog(@"?∞Ïù¥?∞Ï??•ÏÑ±Í≥?);
        [self unzip];
    }
    
    //NSURL *pdfURL = [NSURL fileURLWithPath:[[NSBundle mainBundle] pathForResource:@"FileName" ofType:@"pdf"]];
    //[detailViewController.webDtl loadRequest:[NSURLRequest requestWithURL:pdfURL]];
    //?∞Ïù¥?∞ÏÇ≠??    /*
     if(DownLoad_Data)
     {
     NSLog(@"Release");
     [DownLoad_Data release];
     }*/
}

//[20130913][chisu]
- (BOOL) useSoftpackaging
{
    NSString *softpackagingVerPref = [[NSUserDefaults standardUserDefaults] stringForKey:@"softpackagingVer"];
   // NSString *softpackagingURLPref = [[NSUserDefaults standardUserDefaults] stringForKey:@"softpackagingURL"];
   // NSString *softpackagingCheckURLPref = [[NSUserDefaults standardUserDefaults] stringForKey:@"softpackagingCheckURL"];
    
    //[20130905][chisu]softpackaging plist
    NSString* softpackagingPlistName = @"packaginginfo";
    NSDictionary* tempPlist = [[self class] getBundlePlist:softpackagingPlistName];
    if (tempPlist == nil) {
        NSLog(@"WARNING: %@.plist is missing.", softpackagingPlistName);
        return false;
    }
    else{
        self.softpackagingPlist = [[[NSDictionary alloc] initWithDictionary:tempPlist] autorelease];
        
        NSString* softpackagingVer  = [self.softpackagingPlist objectForKey:@"softpackagingVer"];
        NSString* softpackagingURL  = [self.softpackagingPlist objectForKey:@"softpackagingURL"];
        NSString* softpackagingCheckURL  = [self.softpackagingPlist objectForKey:@"softpackagingCheckURL"];
        
        if(softpackagingVer != nil){
            if(softpackagingVerPref == nil){
                NSUserDefaults *userDefaults = [NSUserDefaults standardUserDefaults];
                [userDefaults setObject:softpackagingVer forKey:@"softpackagingVer"];
                [userDefaults setObject:softpackagingURL forKey:@"softpackagingURL"];
                [userDefaults setObject:softpackagingCheckURL forKey:@"softpackagingCheckURL"];
                [userDefaults synchronize];
                
                //ÏµúÏ¥à?§Ìñâ?¥ÎãàÍπ? www.zip ??document folder???ïÏ∂ï???ºÎã§.
                NSString* rootDir = [[NSBundle mainBundle] bundlePath];
                NSString *zipPath = [rootDir stringByAppendingPathComponent:@"www/www.zip"];
                NSString *destinationPath = [NSHomeDirectory() stringByAppendingPathComponent:@"Documents/hydapp"];
                [SSZipArchive unzipFileAtPath:zipPath toDestination:destinationPath];
                
                //[self getDocFolder];
            }
        }
    
        return true;
    }
}
// Implement viewDidLoad to do additional setup after loading the view, typically from a nib.
- (void) viewDidLoad 
{
    //[self unzip];
    //[self getDocFolder];
    //[self downloadzip];
    
    [super viewDidLoad];
	self.startloadingtime = [[NSDate date] timeIntervalSince1970];
    
    self.pluginObjects = [[[NSMutableDictionary alloc] initWithCapacity:4] autorelease];
    
	// read from UISupportedInterfaceOrientations (or UISupportedInterfaceOrientations~iPad, if its iPad) from -Info.plist
    self.supportedOrientations = [self parseInterfaceOrientations:
									  [[[NSBundle mainBundle] infoDictionary] objectForKey:@"UISupportedInterfaceOrientations"]];
    
    // read from Cordova.plist in the app bundle
    NSString* appPlistName = @"SRT";
    NSDictionary* srtPlist = [[self class] getBundlePlist:appPlistName];
    if (srtPlist == nil) {
        NSLog(@"WARNING: %@.plist is missing.", appPlistName);
		return;
    }
        
    self.settings = [[[NSDictionary alloc] initWithDictionary:srtPlist] autorelease];
	
    // read from Plugins dict in Cordova.plist in the app bundle
    NSString* pluginsKey = @"Plugins";
    NSDictionary* pluginsDict = [self.settings objectForKey:@"Plugins"];
    if (pluginsDict == nil) {
        NSLog(@"WARNING: %@ key in %@.plist is missing! SRT will not work, you need to have this key.", pluginsKey, appPlistName);
        return;
    }
    
    // set the whitelist
    self.whitelist = [[[CDVWhitelist alloc] initWithArray:[self.settings objectForKey:@"ExternalHosts"]] autorelease];
	
    self.pluginsMap = [pluginsDict dictionaryWithLowercaseKeys];
    
    id showStatusbarSpinnerValue = [self.settings objectForKey:@"ShowStatusbarSpinner"];
    if (showStatusbarSpinnerValue == nil || ![showStatusbarSpinnerValue boolValue]) {
        self.useStatusbarSpinner = NO;
    } else {
        self.useStatusbarSpinner = YES;
    }
    
    //[20130625][chisu]if app start to push message with query 
	NSString* startFilePath = [self pathForResource:self.startPage];
    if(![self.query isEqualToString:@""]){
        //startFilePath = [NSString stringWithFormat:@"%@%@",startFilePath,self.query];
        self.currentURL = [NSString stringWithFormat:@"%@%@",startFilePath,self.query];
    }
    else{
        self.currentURL = startFilePath;
    }
    
    
	NSURL* appURL  = nil;
    NSString* loadErr = nil;
    
    if (startFilePath == nil) {
        loadErr = [NSString stringWithFormat:@"ERROR: Start Page at '%@/%@' was not found.", self.wwwFolderName, self.startPage];
        NSLog(@"%@", loadErr);
        self.loadFromString = YES;
        appURL = nil;
    } else {
    //[20131022][chisu]simulator build
    #if TARGET_IPHONE_SIMULATOR
        appURL = [NSURL fileURLWithPath:startFilePath];
    #else
        appURL = [NSURL URLWithString:startFilePath];
    #endif
        //[20130625][chisu]old code
        //appURL = [NSURL fileURLWithPath:startFilePath];
        //[20130625][chisu]if app start to push message with query
        //appURL = [NSURL URLWithString:startFilePath];
    }
    
    //// Fix the iOS 5.1 SECURITY_ERR bug (CB-347), this must be before the webView is instantiated ////

    [CDVLocalStorage __verifyAndFixDatabaseLocations];
    
    //// Instantiate the WebView ///////////////

    [self createGapView];
    
    ///////////////////
    
    NSNumber* enableLocation       = [self.settings objectForKey:@"EnableLocation"];
    NSString* enableViewportScale  = [self.settings objectForKey:@"EnableViewportScale"];
    NSNumber* allowInlineMediaPlayback = [self.settings objectForKey:@"AllowInlineMediaPlayback"];
    BOOL mediaPlaybackRequiresUserAction = YES;  // default value
    if ([self.settings objectForKey:@"MediaPlaybackRequiresUserAction"]) {
        mediaPlaybackRequiresUserAction = [(NSNumber*)[settings objectForKey:@"MediaPlaybackRequiresUserAction"] boolValue];
    }
    
    self.webView.scalesPageToFit = [enableViewportScale boolValue];
    //[20130806][chisu]test webview zoom setting
    //self.webView.scalesPageToFit = YES;
    
    /*
     * Fire up the GPS Service right away as it takes a moment for data to come back.
     */
    
    if ([enableLocation boolValue]) {
        [[self.commandDelegate getCommandInstance:@"Geolocation"] getLocation:nil withDict:nil];
    }
    
    /*
     * Fire up CDVLocalStorage to work-around iOS 5.1 WebKit storage limitations
     */
    [self.commandDelegate registerPlugin:[[[CDVLocalStorage alloc] initWithWebView:self.webView] autorelease] withClassName:NSStringFromClass([CDVLocalStorage class])];
    
    /*
     * This is for iOS 4.x, where you can allow inline <video> and <audio>, and also autoplay them
     */
    if ([allowInlineMediaPlayback boolValue] && [self.webView respondsToSelector:@selector(allowsInlineMediaPlayback)]) {
        self.webView.allowsInlineMediaPlayback = YES;
    }
    if (mediaPlaybackRequiresUserAction == NO && [self.webView respondsToSelector:@selector(mediaPlaybackRequiresUserAction)]) {
        self.webView.mediaPlaybackRequiresUserAction = NO;
    }
    
    // UIWebViewBounce property - defaults to true
    NSNumber* bouncePreference = [self.settings objectForKey:@"UIWebViewBounce"];
    BOOL bounceAllowed = (bouncePreference==nil || [bouncePreference boolValue]); 
    
    // prevent webView from bouncing
    // based on UIWebViewBounce key in Cordova.plist
    if (!bounceAllowed) {
        if ([ self.webView respondsToSelector:@selector(scrollView) ]) {
            ((UIScrollView *) [self.webView scrollView]).bounces = NO;
        } else {
            for (id subview in self.webView.subviews)
                if ([[subview class] isSubclassOfClass: [UIScrollView class]])
                    ((UIScrollView *)subview).bounces = NO;
        }
    }
    
    ///////////////////
    //[20130913][chisu]softpackaing Î∂ÑÍ∏∞
    if([self useSoftpackaging]){
        if(self.pushfromsp){
            
            self.pushfromsp = false;
            
            NSString *loadURL = self.startPage;
            NSString *pushquery = self.query;

            NSString *startFilePath = [NSHomeDirectory() stringByAppendingPathComponent:@"Documents/hydapp"];
            //startFilePath = [NSString stringWithFormat:@"%@%@%@%@",startFilePath,@"/",loadURL,pushquery];
            startFilePath = [NSString stringWithFormat:@"%@%@%@",startFilePath,@"/",loadURL];
            
            self.currentURL = [NSString stringWithFormat:@"%@%@",startFilePath,pushquery];
            
            [self.webView loadRequest:[[NSURLRequest alloc] initWithURL:[[NSURL alloc] initWithString:startFilePath]]];
            
        }
        else{
            //NSString *sofrpackaging_startpage = [NSHomeDirectory() stringByAppendingPathComponent:@"Documents/hydapp/index.html"];
            NSString* sofrpackaging_startpage = [self pathForResource:@"softpackaging.html"];
            NSURL *sofptacgaging_appURL = [NSURL URLWithString:sofrpackaging_startpage];
            NSURLRequest *sp_appReq = [NSURLRequest requestWithURL:sofptacgaging_appURL cachePolicy:NSURLRequestUseProtocolCachePolicy timeoutInterval:20.0];
            [self.webView loadRequest:sp_appReq];
        }
    }
    else{
        if (!loadErr) {
            NSURLRequest *appReq = [NSURLRequest requestWithURL:appURL cachePolicy:NSURLRequestUseProtocolCachePolicy timeoutInterval:20.0];
            [self.webView loadRequest:appReq];
        } else {
            NSString* html = [NSString stringWithFormat:@"<html><body> %@ </body></html>", loadErr];
            [self.webView loadHTMLString:html baseURL:nil];
        }
    }
}

- (NSString*) getCurrentURL{
    return self.currentURL;
}

- (NSArray*) parseInterfaceOrientations:(NSArray*)orientations
{
    NSMutableArray* result = [[[NSMutableArray alloc] init] autorelease];
	
    if (orientations != nil) 
    {
        NSEnumerator* enumerator = [orientations objectEnumerator];
        NSString* orientationString;
        
        while (orientationString = [enumerator nextObject]) 
        {
            if ([orientationString isEqualToString:@"UIInterfaceOrientationPortrait"]) {
                [result addObject:[NSNumber numberWithInt:UIInterfaceOrientationPortrait]];
            } else if ([orientationString isEqualToString:@"UIInterfaceOrientationPortraitUpsideDown"]) {
                [result addObject:[NSNumber numberWithInt:UIInterfaceOrientationPortraitUpsideDown]];
            } else if ([orientationString isEqualToString:@"UIInterfaceOrientationLandscapeLeft"]) {
                [result addObject:[NSNumber numberWithInt:UIInterfaceOrientationLandscapeLeft]];
            } else if ([orientationString isEqualToString:@"UIInterfaceOrientationLandscapeRight"]) {
                [result addObject:[NSNumber numberWithInt:UIInterfaceOrientationLandscapeRight]];
            }
        }
    }
    
    // default
    if ([result count] == 0) {
        [result addObject:[NSNumber numberWithInt:UIInterfaceOrientationPortrait]];
    }
    
    return result;
}

- (BOOL) shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation 
{
	// First ask the webview via JS if it wants to support the new orientation -jm
	int i = 0;
	
	switch (interfaceOrientation){
            
		case UIInterfaceOrientationPortraitUpsideDown:
			i = 180;
			break;
		case UIInterfaceOrientationLandscapeLeft:
			i = -90;
			break;
		case UIInterfaceOrientationLandscapeRight:
			i = 90;
			break;
		default:
		case UIInterfaceOrientationPortrait:
			// noop
			break;
	}
	
	NSString* jsCall = [NSString stringWithFormat:
                        @"(function(){ \
                                if('shouldRotateToOrientation' in window) { \
                                    return window.shouldRotateToOrientation(%d); \
                                } \
                            })()"
                        , i];
	NSString* res = [webView stringByEvaluatingJavaScriptFromString:jsCall];
	
	if([res length] > 0)
	{
		return [res boolValue];
	}
	
	// if js did not handle the new orientation ( no return value ) we will look it up in the plist -jm
	
	BOOL autoRotate = [self.supportedOrientations count] > 0; // autorotate if only more than 1 orientation supported
	if (autoRotate)
	{
		if ([self.supportedOrientations containsObject:
			 [NSNumber numberWithInt:interfaceOrientation]]) {
			return YES;
		}
    }
	
	// default return value is NO! -jm
	
	return NO;
}


/**
 Called by UIKit when the device starts to rotate to a new orientation.  This fires the \c setOrientation
 method on the Orientation object in JavaScript.  Look at the JavaScript documentation for more information.
 */
- (void)didRotateFromInterfaceOrientation: (UIInterfaceOrientation)fromInterfaceOrientation
{
	int i = 0;
	
	switch (self.interfaceOrientation){
		case UIInterfaceOrientationPortrait:
			i = 0;
			break;
		case UIInterfaceOrientationPortraitUpsideDown:
			i = 180;
			break;
		case UIInterfaceOrientationLandscapeLeft:
			i = -90;
			break;
		case UIInterfaceOrientationLandscapeRight:
			i = 90;
			break;
	}
    
    if (!IsAtLeastiOSVersion(@"5.0")) {
        NSString* jsCallback = [NSString stringWithFormat:
                                @"window.__defineGetter__('orientation',function(){ return %d; }); \
                                  srt.fireWindowEvent('orientationchange');"
                                , i];
        [self.webView stringByEvaluatingJavaScriptFromString:jsCallback];    
    }
}

- (RTWebView*) newCordovaViewWithFrame:(CGRect)bounds
{
    return [[RTWebView alloc] initWithFrame:bounds];
}

- (void) createGapView
{
    CGRect webViewBounds = self.view.bounds;
    webViewBounds.origin = self.view.bounds.origin;
	
    if (!self.webView) 
	{
        self.webView = [[self newCordovaViewWithFrame:webViewBounds] autorelease];
		self.webView.autoresizingMask = (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
		
		[self.view addSubview:self.webView];
		[self.view sendSubviewToBack:self.webView];
		
		self.webView.delegate = self;
    }
}

- (void) didReceiveMemoryWarning {
    
    // iterate through all the plugin objects, and call hasPendingOperation
    // if at least one has a pending operation, we don't call [super didReceiveMemoryWarning]
    
    NSEnumerator* enumerator = [self.pluginObjects objectEnumerator];
    RTPlugin* plugin;
    
    BOOL doPurge = YES;
    while ((plugin = [enumerator nextObject])) 
    {
        if (plugin.hasPendingOperation) {
            NSLog(@"Plugin '%@' has a pending operation, memory purge is delayed for didReceiveMemoryWarning.", NSStringFromClass([plugin class]));
            doPurge = NO;
        }
    }
    
	if (doPurge) {
        // Releases the view if it doesn't have a superview.
        [super didReceiveMemoryWarning];
    }
	
	// Release any cached data, images, etc. that aren't in use.
}


- (void) viewDidUnload {
	// Release any retained subviews of the main view.
	// e.g. self.myOutlet = nil;
    
    self.webView.delegate = nil;
    self.webView = nil;
}


#pragma mark UIWebViewDelegate

/**
 When web application loads Add stuff to the DOM, mainly the user-defined settings from the Settings.plist file, and
 the device's data such as device ID, platform version, etc.
 */
- (void) webViewDidStartLoad:(UIWebView*)theWebView 
{
    if(self.useStatusbarSpinner) {
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:YES];
    }
    
    if(self.useScreenSpinner) {
        [self.activityView startAnimating];
        self.activityView.hidden = NO;
    }
}

/**
 Called when the webview finishes loading.  This stops the activity view and closes the imageview
 */
- (void) webViewDidFinishLoad:(UIWebView*)theWebView 
{
    self.endloadingtime = [[NSDate date] timeIntervalSince1970];
    NSDictionary *deviceProperties = [ self deviceProperties];
    NSMutableString* result = [[NSMutableString alloc] initWithFormat:
                               @"(function() { \
                                    try { \
                                        srt.sktrequire('srt/plugin/ios/device').setInfo(%@); \
                                    } catch (e) { \
                                        return \"Error: executing module function 'setInfo' in module 'srt/plugin/ios/device'. Have you included the iOS version of the srt-%@.js file?\"; \
                                    } \
                               })()", 
                               [deviceProperties JSONString], [RTViewController cordovaVersion]];
    
    /* Settings.plist
     * Read the optional Settings.plist file and push these user-defined settings down into the web application.
     * This can be useful for supplying build-time configuration variables down to the app to change its behaviour,
     * such as specifying Full / Lite version, or localization (English vs German, for instance).
     */
    // TODO: turn this into an iOS only plugin
    NSDictionary *temp = [[self class] getBundlePlist:@"Settings"];
    if ([temp respondsToSelector:@selector(JSONString)]) {
        [result appendFormat:@"\nwindow.Settings = %@;", [temp JSONString]];
    }
    
    NSString* jsResult = [theWebView stringByEvaluatingJavaScriptFromString:result];
    // if jsResult is not nil nor empty, an error
    if (jsResult != nil && [jsResult length] > 0) {
        NSLog(@"%@", jsResult);
    }
    
    [result release];
    
    /*
     * Hide the Top Activity THROBBER in the Battery Bar
     */
    
    if(self.useStatusbarSpinner) {
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
    }
    
    if(self.useScreenSpinner) {
        [self.activityView stopAnimating];
        self.activityView.hidden = YES;
    }
	
    id autoHideSplashScreenValue = [self.settings objectForKey:@"AutoHideSplashScreen"];
    // if value is missing, default to yes
    if (autoHideSplashScreenValue == nil || [autoHideSplashScreenValue boolValue]) {
        self.imageView.hidden = YES;
        [self.view.superview bringSubviewToFront:self.webView];        
    }
    
    [self didRotateFromInterfaceOrientation:(UIInterfaceOrientation)[[UIDevice currentDevice] orientation]];
    
    // Tell the webview that native is ready.
    NSString* nativeReady = @"try{srt.sktrequire('srt/channel').onNativeReady.fire();}catch(e){window._nativeReady = true;}";
    [theWebView stringByEvaluatingJavaScriptFromString:nativeReady];
}

- (void) webView:(UIWebView*)webView didFailLoadWithError:(NSError*)error 
{
    NSLog(@"Failed to load webpage with error: %@", [error localizedDescription]);
    
    if(self.useStatusbarSpinner) {
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
    }
    
    if(self.useScreenSpinner) {
        [self.activityView stopAnimating];
        self.activityView.hidden = YES;
    }
    /*
	 if ([error code] != NSURLErrorCancelled)
	 alert([error localizedDescription]);
     */
}

- (BOOL) webView:(UIWebView*)theWebView shouldStartLoadWithRequest:(NSURLRequest*)request navigationType:(UIWebViewNavigationType)navigationType
{
	NSURL* url = [request URL];
    
    /*
     * Execute any commands queued with srt.exec() on the JS side.
     * The part of the URL after gap:// is irrelevant.
     */
	if ([[url scheme] isEqualToString:@"gap"]) {
        [self flushCommandQueue];
        return NO;
	}
    /*
     * If a URL is being loaded that's a file/http/https URL, just load it internally
     */
    else if ([url isFileURL])
    {
        return YES;
    }
    else if ([self.whitelist schemeIsAllowed:[url scheme]])
    {            
        if ([self.whitelist URLIsAllowed:url] == YES)
        {
            NSNumber *openAllInWhitelistSetting = [self.settings objectForKey:@"OpenAllWhitelistURLsInWebView"];
            if ((nil != openAllInWhitelistSetting) && [openAllInWhitelistSetting boolValue]) {
                NSLog(@"OpenAllWhitelistURLsInWebView set: opening in webview");
                return YES;
            }
			
            // mainDocument will be nil for an iFrame
            NSString* mainDocument = [theWebView.request.mainDocumentURL absoluteString];
			
            // anchor target="_blank" - load in Mobile Safari
            if (navigationType == UIWebViewNavigationTypeOther && mainDocument != nil)
            {
                [[UIApplication sharedApplication] openURL:url];
                return NO;
            }
            // other anchor target - load in srt webView
            else
            {
                return YES;
            }
        }
        
        return NO;
    }
    /*
     *    If we loaded the HTML from a string, we let the app handle it
     */
    else if (self.loadFromString == YES)
    {
        self.loadFromString = NO;
        return YES;
    }
    /*
     * all tel: scheme urls we let the UIWebview handle it using the default behaviour
     */
    else if ([[url scheme] isEqualToString:@"tel"])
    {
        return YES;
    }
    /*
     * all about: scheme urls are not handled
     */
    else if ([[url scheme] isEqualToString:@"about"])
    {
        return NO;
    }
    /*
     * We don't have a srt or web/local request, load it in the main Safari browser.
     * pass this to the application to handle.  Could be a mailto:dude@duderanch.com or a tel:55555555 or sms:55555555 facetime:55555555
     */
    else
    {
        NSLog(@"AppDelegate::shouldStartLoadWithRequest: Received Unhandled URL %@", url);
		
        if ([[UIApplication sharedApplication] canOpenURL:url]) {
            [[UIApplication sharedApplication] openURL:url];
        } else { // handle any custom schemes to plugins
            [[NSNotificationCenter defaultCenter] postNotification:[NSNotification notificationWithName:RTPluginHandleOpenURLNotification object:url]];
        }
		
        return NO;
    }
    
    return YES;
}

#pragma mark GapHelpers

- (void) javascriptAlert:(NSString*)text
{
    NSString* jsString = [NSString stringWithFormat:@"alert('%@');", text];
    [webView stringByEvaluatingJavaScriptFromString:jsString];
}

+ (BOOL) isIPad 
{
#ifdef UI_USER_INTERFACE_IDIOM
    return (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad);
#else
    return NO;
#endif
}

+ (NSString*) resolveImageResource:(NSString*)resource
{
    NSString* systemVersion = [[UIDevice currentDevice] systemVersion];
    BOOL isLessThaniOS4 = ([systemVersion compare:@"4.0" options:NSNumericSearch] == NSOrderedAscending);


    
    // the iPad image (nor retina) differentiation code was not in 3.x, and we have to explicitly set the path
    if (isLessThaniOS4)
    {
        if ([[self class] isIPad]) {
            return [NSString stringWithFormat:@"%@~ipad.png", resource];
        } else {
            return [NSString stringWithFormat:@"%@.png", resource];
        }
    }
    
    if ([[UIScreen mainScreen] respondsToSelector:@selector(scale)] == YES && [[UIScreen mainScreen] scale] == 2.00) {
        
        return [NSString stringWithFormat:@"%@@2x.png", resource];
        
    }
    
    return resource;
}

- (NSString*) pathForResource:(NSString*)resourcepath
{
    NSBundle * mainBundle = [NSBundle mainBundle];
    NSMutableArray *directoryParts = [NSMutableArray arrayWithArray:[resourcepath componentsSeparatedByString:@"/"]];
    NSString       *filename       = [directoryParts lastObject];
    [directoryParts removeLastObject];
    
    NSString* directoryPartsJoined =[directoryParts componentsJoinedByString:@"/"];
    NSString* directoryStr = self.wwwFolderName;
    
    if ([directoryPartsJoined length] > 0) {
        directoryStr = [NSString stringWithFormat:@"%@/%@", self.wwwFolderName, [directoryParts componentsJoinedByString:@"/"]];
    }
    
    return [mainBundle pathForResource:filename ofType:@"" inDirectory:directoryStr];
}

+ (NSString*) applicationDocumentsDirectory 
{
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *basePath = ([paths count] > 0) ? [paths objectAtIndex:0] : nil;
    return basePath;
}

- (void) showSplashScreen
{
    NSString* launchImageFile = [self.settings objectForKey:@"SplashImage"];
    if (launchImageFile == nil || [launchImageFile isEqualToString:@""]) { // fallback if no launch image was specified
        launchImageFile = @"Default"; 
    }
    
    NSString* orientedLaunchImageFile = nil;    
    CGAffineTransform startupImageTransform = CGAffineTransformIdentity;
    UIDeviceOrientation deviceOrientation = [UIDevice currentDevice].orientation;
    CGRect screenBounds = [[UIScreen mainScreen] bounds];
    CGRect statusBarFrame = [UIApplication sharedApplication].statusBarFrame;
    UIInterfaceOrientation statusBarOrientation = [UIApplication sharedApplication].statusBarOrientation;
    BOOL isIPad = [[self class] isIPad];
    UIImage* launchImage = nil;
    
    // default to center of screen as in the original implementation. This will produce the 20px jump
    CGPoint center = CGPointMake((screenBounds.size.width / 2), (screenBounds.size.height / 2));
    
    if (isIPad)
    {
        if (!UIDeviceOrientationIsValidInterfaceOrientation(deviceOrientation)) {
            deviceOrientation = (UIDeviceOrientation)statusBarOrientation;
        }
        
        switch (deviceOrientation) 
        {
            case UIDeviceOrientationLandscapeLeft: // this is where the home button is on the right (yeah, I know, confusing)
            {
                orientedLaunchImageFile = [NSString stringWithFormat:@"%@-Landscape", launchImageFile];
                startupImageTransform = CGAffineTransformMakeRotation(degreesToRadian(90));
                center.x -= MIN(statusBarFrame.size.width, statusBarFrame.size.height)/2;
            }
                break;
            case UIDeviceOrientationLandscapeRight: // this is where the home button is on the left (yeah, I know, confusing)
            {
                orientedLaunchImageFile = [NSString stringWithFormat:@"%@-Landscape", launchImageFile];
                startupImageTransform = CGAffineTransformMakeRotation(degreesToRadian(-90));
                center.x += MIN(statusBarFrame.size.width, statusBarFrame.size.height)/2;
           } 
                break;
            case UIDeviceOrientationPortraitUpsideDown:
            {
                orientedLaunchImageFile = [NSString stringWithFormat:@"%@-Portrait", launchImageFile];
                startupImageTransform = CGAffineTransformMakeRotation(degreesToRadian(180));
                center.y -= MIN(statusBarFrame.size.width, statusBarFrame.size.height)/2;
            } 
                break;
            case UIDeviceOrientationPortrait:
            default:
            {
                orientedLaunchImageFile = [NSString stringWithFormat:@"%@-Portrait", launchImageFile];
                startupImageTransform = CGAffineTransformIdentity;
                center.y += MIN(statusBarFrame.size.width, statusBarFrame.size.height)/2;
            }
                break;
        }
        
        launchImage = [UIImage imageNamed:[[self class] resolveImageResource:orientedLaunchImageFile]];
    }
    else // not iPad
    {
        orientedLaunchImageFile = launchImageFile;
        launchImage = [UIImage imageNamed:[[self class] resolveImageResource:orientedLaunchImageFile]];
    }
    
    if (launchImage == nil) {
        NSLog(@"WARNING: Splash-screen image '%@' was not found. Orientation: %d, iPad: %d", orientedLaunchImageFile, deviceOrientation, isIPad);
    }
    
    self.imageView = [[[UIImageView alloc] initWithImage:launchImage] autorelease];    
    self.imageView.tag = 1;
    self.imageView.center = center;
    
    self.imageView.autoresizingMask = (UIViewAutoresizingFlexibleWidth & UIViewAutoresizingFlexibleHeight & UIViewAutoresizingFlexibleLeftMargin & UIViewAutoresizingFlexibleRightMargin);    
    [self.imageView setTransform:startupImageTransform];
    [self.view.superview addSubview:self.imageView];   
    
    id showScreenSpinnerValue = [self.settings objectForKey:@"ShowScreenSpinner"];
    id ScreenSpinnerImages = [self.settings objectForKey:@"ScreenSpinnerImages"];
    if (showScreenSpinnerValue != nil
        && [showScreenSpinnerValue boolValue]
        && ScreenSpinnerImages !=nil) {
        self.useScreenSpinner = YES;
        
        NSArray* imageScreenSpinnerArray = [NSArray arrayWithArray:ScreenSpinnerImages];
        NSMutableArray* animationImages = [NSMutableArray arrayWithCapacity:10];
        for(NSString* name in imageScreenSpinnerArray) {
            UIImage *img = [UIImage imageNamed:name];
            if(img != nil)
                [animationImages addObject:img];
        }
        
        UIImage *statusImage = [animationImages objectAtIndex:0];
        UIImageView *activityImageView = [[UIImageView alloc] initWithImage:statusImage];
        
        activityImageView.animationImages = animationImages;         
        activityImageView.animationDuration = 0.8;
        activityImageView.frame = CGRectMake(
                                             self.view.frame.size.width/2
                                             -statusImage.size.width/2, 
                                             self.view.frame.size.height/2
                                             -statusImage.size.height/2, 
                                             statusImage.size.width, 
                                             statusImage.size.height);
        
        self.activityView = activityImageView;
        [self.view.superview addSubview:self.activityView];
        self.activityView.center = self.view.center;
        [self.activityView startAnimating];
    } else {
        self.useScreenSpinner = NO;
    }
    
    [self.view.superview layoutSubviews];
}    

BOOL gSplashScreenShown = NO;
- (void) receivedOrientationChange
{
    if (self.imageView == nil) {
        gSplashScreenShown = YES;
        if (self.useSplashScreen) {
            [self showSplashScreen];
        }
    }
}

#pragma mark srtCommands

/**
 * Fetches the command queue and executes each command. It is possible that the
 * queue will not be empty after this function has completed since the executed
 * commands may have run callbacks which queued more commands.
 *
 * Returns the number of executed commands.
 */
- (int) executeQueuedCommands
{
    // Grab all the queued commands from the JS side.
    NSString* queuedCommandsJSON = [self.webView stringByEvaluatingJavaScriptFromString:
									@"srt.sktrequire('srt/plugin/ios/nativecomm')()"];
	
	
    // Parse the returned JSON array.
    NSArray* queuedCommands =
	[queuedCommandsJSON objectFromJSONString];
	
    // Iterate over and execute all of the commands.
    for (NSString* commandJson in queuedCommands) {
		
        if(![self.commandDelegate execute:
		 [RTInvokedUrlCommand commandFromObject:
		  [commandJson mutableObjectFromJSONString]]])
		{
			NSLog(@"FAILED pluginJSON = %@",commandJson);
		}
    }
	
    return [queuedCommands count];
}

/**
 * Repeatedly fetches and executes the command queue until it is empty.
 */
- (void) flushCommandQueue
{
    [self.webView stringByEvaluatingJavaScriptFromString:
	 @"srt.commandQueueFlushing = true"];
	
    // Keep executing the command queue until no commands get executed.
    // This ensures that commands that are queued while executing other
    // commands are executed as well.
    int numExecutedCommands = 0;
    do {
        numExecutedCommands = [self executeQueuedCommands];
    } while (numExecutedCommands != 0);
	
    [self.webView stringByEvaluatingJavaScriptFromString:
	 @"srt.commandQueueFlushing = false"];
}

- (BOOL) execute:(RTInvokedUrlCommand*)command
{
    if (command.className == nil || command.methodName == nil) {
        return NO;
    }
    
    // Fetch an instance of this class
    RTPlugin* obj = [self.commandDelegate getCommandInstance:command.className];
    
    if (!([obj isKindOfClass:[RTPlugin class]])) { // still allow deprecated class, until 1.0 release
        NSLog(@"ERROR: Plugin '%@' not found, or is not a RTPlugin. Check your plugin mapping in srt.plist.", command.className);
        return NO;
    }
    BOOL retVal = YES;
    
    // construct the fill method name to ammend the second argument.
    NSString* fullMethodName = [[NSString alloc] initWithFormat:@"%@:withDict:", command.methodName];
    NSLog(@"COMMAND: Method '%@' defined in Plugin '%@'", fullMethodName, command.className);
    if ([obj respondsToSelector:NSSelectorFromString(fullMethodName)]) {
        [obj performSelector:NSSelectorFromString(fullMethodName) withObject:command.arguments withObject:command.options];
    } else {
        // There's no method to call, so throw an error.
        NSLog(@"ERROR: Method '%@' not defined in Plugin '%@'", fullMethodName, command.className);
        retVal = NO;
    }
    
    [fullMethodName release];
    
    return retVal;
}

- (void) registerPlugin:(RTPlugin*)plugin withClassName:(NSString*)className
{
    if ([plugin respondsToSelector:@selector(setViewController:)]) { 
        [plugin setViewController:self];
    }
    
    if ([plugin respondsToSelector:@selector(setCommandDelegate:)]) { 
        [plugin setCommandDelegate:self.commandDelegate];
    }
    
    [self.pluginObjects setObject:plugin forKey:className];
}

/**
 Returns an instance of a srtCommand object, based on its name.  If one exists already, it is returned.
 */
- (id) getCommandInstance:(NSString*)pluginName
{
    // first, we try to find the pluginName in the pluginsMap 
    // (acts as a whitelist as well) if it does not exist, we return nil
    // NOTE: plugin names are matched as lowercase to avoid problems - however, a 
    // possible issue is there can be duplicates possible if you had:
    // "org.apache.srt.Foo" and "org.apache.srt.foo" - only the lower-cased entry will match
    NSString* className = [self.pluginsMap objectForKey:[pluginName lowercaseString]];
    if (className == nil) {
        return nil;
    }
    
    id obj = [self.pluginObjects objectForKey:className];
    if (!obj) 
    {
        // attempt to load the settings for this command class
        NSDictionary* classSettings = [self.settings objectForKey:className];
		
        if (classSettings) {
            obj = [[[NSClassFromString(className) alloc] initWithWebView:webView settings:classSettings] autorelease];
        } else {
            obj = [[[NSClassFromString(className) alloc] initWithWebView:webView] autorelease];
        }
        
        if (obj != nil && [obj isKindOfClass:[RTPlugin class]]) {
            [self registerPlugin:obj withClassName:className];
        } else {
            NSLog(@"RTPlugin class %@ (pluginName: %@) does not exist.", className, pluginName);
        }
    }
    return obj;
}


#pragma mark -

- (NSDictionary*) deviceProperties
{
    UIDevice *device = [UIDevice currentDevice];
    CGRect screenRect = [[UIScreen mainScreen] bounds];
    
    NSMutableDictionary *devProps = [NSMutableDictionary dictionaryWithCapacity:4];
    [devProps setObject:[device model] forKey:@"platform"];
    [devProps setObject:[device systemVersion] forKey:@"version"];
    [devProps setObject:[device uniqueAppInstanceIdentifier] forKey:@"uuid"];
    [devProps setObject:[device name] forKey:@"name"];
   // [devProps setObject:[RTViewController srtVersion] forKey:@"srt"];
    [devProps setObject:[NSNumber numberWithFloat:screenRect.size.width] forKey:@"resolutionWidth"];
    
        
    NSDictionary *devReturn = [NSDictionary dictionaryWithDictionary:devProps];
    return devReturn;
}

- (NSString*) appURLScheme
{
    NSString* URLScheme = nil;
    
    NSArray *URLTypes = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleURLTypes"];
    if(URLTypes != nil ) {
        NSDictionary* dict = [URLTypes objectAtIndex:0];
        if(dict != nil ) {
            NSArray* URLSchemes = [dict objectForKey:@"CFBundleURLSchemes"];
            if( URLSchemes != nil ) {    
                URLScheme = [URLSchemes objectAtIndex:0];
            }
        }
    }
    
    return URLScheme;
}

/**
 Returns the contents of the named plist bundle, loaded as a dictionary object
 */
+ (NSDictionary*) getBundlePlist:(NSString*)plistName
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

/**
 Returns the current version of srt as read from the VERSION file
 This only touches the filesystem once and stores the result in the class variable gapVersion
 */
static NSString* cdvVersion;
+ (NSString*) cordovaVersion
{
#ifdef CDV_VERSION
    cdvVersion = SYMBOL_TO_NSSTRING(CDV_VERSION);
#else
	
    if (cdvVersion == nil) {
        NSBundle *mainBundle = [NSBundle mainBundle];
        NSString *filename = [mainBundle pathForResource:@"VERSION" ofType:nil];
        // read from the filesystem and save in the variable
        // first, separate by new line
        NSString* fileContents = [NSString stringWithContentsOfFile:filename encoding:NSUTF8StringEncoding error:NULL];
        NSArray* all_lines = [fileContents componentsSeparatedByCharactersInSet:[NSCharacterSet newlineCharacterSet]];
        NSString* first_line = [all_lines objectAtIndex:0];        
        
        cdvVersion = [first_line retain];
    }
#endif
    return cdvVersion;
}


#pragma mark -
#pragma mark UIApplicationDelegate impl

/*
 This method lets your application know that it is about to be terminated and purged from memory entirely
 */
- (void) onAppWillTerminate:(NSNotification*)notification
{
    
    // empty the tmp directory
    NSFileManager* fileMgr = [[NSFileManager alloc] init];
    NSError* err = nil;    
    
    // clear contents of NSTemporaryDirectory 
    NSString* tempDirectoryPath = NSTemporaryDirectory();
    NSDirectoryEnumerator* directoryEnumerator = [fileMgr enumeratorAtPath:tempDirectoryPath];    
    NSString* fileName = nil;
    BOOL result;
    
    while ((fileName = [directoryEnumerator nextObject])) {
        NSString* filePath = [tempDirectoryPath stringByAppendingPathComponent:fileName];
        result = [fileMgr removeItemAtPath:filePath error:&err];
        if (!result && err) {
            NSLog(@"Failed to delete: %@ (error: %@)", filePath, err);
        }
    }    
    [fileMgr release];
}

/*
 This method is called to let your application know that it is about to move from the active to inactive state.
 You should use this method to pause ongoing tasks, disable timer, ...
 */
- (void) onAppWillResignActive:(NSNotification*)notification
{
    //NSLog(@"%@",@"applicationWillResignActive");
    [self.webView stringByEvaluatingJavaScriptFromString:@"srt.fireDocumentEvent('resign');"];
}

/*
 In iOS 4.0 and later, this method is called as part of the transition from the background to the inactive state. 
 You can use this method to undo many of the changes you made to your application upon entering the background.
 invariably followed by applicationDidBecomeActive
 */
- (void) onAppWillEnterForeground:(NSNotification*)notification
{
    //NSLog(@"%@",@"applicationWillEnterForeground");
    [self.webView stringByEvaluatingJavaScriptFromString:@"srt.fireDocumentEvent('resume');"];
}

// This method is called to let your application know that it moved from the inactive to active state. 
- (void) onAppDidBecomeActive:(NSNotification*)notification
{
    //NSLog(@"%@",@"applicationDidBecomeActive");
    [self.webView stringByEvaluatingJavaScriptFromString:@"srt.fireDocumentEvent('active');"];
}

/*
 In iOS 4.0 and later, this method is called instead of the applicationWillTerminate: method 
 when the user quits an application that supports background execution.
 */
- (void) onAppDidEnterBackground:(NSNotification*)notification
{
    //NSLog(@"%@",@"applicationDidEnterBackground");
    [self.webView stringByEvaluatingJavaScriptFromString:@"srt.fireDocumentEvent('pause');"];
}

// ///////////////////////


- (void)dealloc 
{
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationWillTerminateNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationWillResignActiveNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationWillEnterForegroundNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationDidBecomeActiveNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationDidEnterBackgroundNotification object:nil];
    
    [super dealloc];
}

@end
