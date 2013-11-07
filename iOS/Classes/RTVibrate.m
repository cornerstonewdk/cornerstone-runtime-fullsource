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


#import "RTVibrate.h"
#import "NSDictionary+Extensions.h"

@implementation RTVibrate

@synthesize callbackId;

//[20130912][chisu]for soft packaging
@synthesize theConnection, DownLoad_Data,Total_FileSize ;

- (RTVibrate*) init
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

- (void)vibrate:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    AudioServicesPlaySystemSound(kSystemSoundID_Vibrate);
}

- (void)vibratepattern:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    [self vibrate:nil withDict:nil];
}

- (void)startBeep:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    count = [[arguments objectAtIndex:1 ] integerValue];
    if(timer != nil) {
        [timer invalidate];
    }
    timer = [[NSTimer scheduledTimerWithTimeInterval:1
                                              target:self
                                            selector:@selector(playBeep)
                                            userInfo:nil repeats:YES]retain];

}

-(void) playBeep
{
    AudioServicesPlaySystemSound(1002);
    if(--count <=0) {
        [timer invalidate];
        timer = nil;
    }

}

- (void)stopBeep:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    if(timer != nil) {
        [timer invalidate];
        timer = nil;
    }
}

- (void)setCallRingtone:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    NSString* cbId = [arguments objectAtIndex:0];
    RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_ERROR messageAsString:@"Not Support"];
    [super writeJavascript:[result toErrorCallbackString:cbId]]; 
}

- (void)setWallpaper:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    NSString* cbId = [arguments objectAtIndex:0];
    RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_ERROR messageAsString:@"Not Support"];
    [super writeJavascript:[result toErrorCallbackString:cbId]]; 
}

- (void)hydrationupdate:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    NSString* cbId = [arguments objectAtIndex:0];
    self.callbackId = cbId;
    
    [self downloadzip];
    
    //RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_NO_RESULT];
    //[result setKeepCallback:[NSNumber numberWithBool:NO]];
    //[super writeJavascript:[result toSuccessCallbackString:cbId]];
}

//[20130823][chisu]for hydration
- (void)downloadzip
{
    //NSString* Down_URL = @"http://121.78.237.180:20280/download/www.zip";
    NSString* Down_URL = [[NSUserDefaults standardUserDefaults] stringForKey:@"softpackagingURL"];
    
    NSURLRequest* theRequest = [NSURLRequest requestWithURL:[NSURL URLWithString:Down_URL] cachePolicy:NSURLRequestUseProtocolCachePolicy timeoutInterval:60.0];
    theConnection = [[NSURLConnection alloc] initWithRequest:theRequest delegate:self];
}

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response {
    if(connection != theConnection){
        RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_ERROR];
        [super writeJavascript:[result toErrorCallbackString:self.callbackId]];
        return;
    }
    //?�탈 바이??(총용?? : ?�버?�서 받�? Response�?분석?�서 ?�운로드???�일??�??�량??구해?�다.
    Total_FileSize = [[NSNumber numberWithLongLong:[response expectedContentLength]] longValue];
    NSLog(@"content-length: %ld bytes", Total_FileSize);
    
    DownLoad_Data = [[NSMutableData alloc] init];
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data{
    if(connection != theConnection)
        return;
    //data???�버???�일??조각조각 불러?�다. 로컬?�서 조각조각 불러???�일??붙여???�나???�일�?만들?�놓?�다.
    [DownLoad_Data appendData:data];
    //?�운받�? ?�일???�량??보여준??
    NSNumber* ResponeLength = [NSNumber numberWithUnsignedInteger:[DownLoad_Data length]];
    NSLog(@"Downloading... size : %ld", [ResponeLength longValue]);
    
    //총용??    float FileSize = (float)Total_FileSize;
    
    //?�운로드???�이???�량
    float Down_Filesize = [ResponeLength floatValue];
    NSLog(@"Down : %f", Down_Filesize / FileSize);
    
    RTPluginResult* result = nil;
    NSString* jsString = nil;
    
    int down = (int)(Down_Filesize / FileSize * 100);
    result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsInt:down];
    
    if(down != 100 )
        [result setKeepCallback:[NSNumber numberWithBool:YES]];
    
    jsString = [result toSuccessCallbackString:self.callbackId];
    [self writeJavascript:jsString];
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
    if(connection != theConnection)
        return;
    NSFileManager* FM = [NSFileManager defaultManager];
    //[iPhone] ?�일 ?�스??(Document Directory 경로찾기)
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsDirectory = [paths objectAtIndex:0];
    
    
    NSString *downloadPath = [documentsDirectory stringByAppendingPathComponent:@"hydapp/www.zip"];
    
    if([FM createFileAtPath:downloadPath contents:DownLoad_Data attributes:nil])
    {
        NSLog(@"?�이?��??�성�?);
        [self unzip];
    }
    
    //NSURL *pdfURL = [NSURL fileURLWithPath:[[NSBundle mainBundle] pathForResource:@"FileName" ofType:@"pdf"]];
    //[detailViewController.webDtl loadRequest:[NSURLRequest requestWithURL:pdfURL]];
    //?�이?�삭??    /*
     if(DownLoad_Data)
     {
     NSLog(@"Release");
     [DownLoad_Data release];
     }*/
}

//[20130823][chisu]for hydration
- (void) unzip
{
    //document path
    NSArray *down_pa = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *down_filepath2 = [down_pa objectAtIndex:0];
    NSString *down_filePath = [down_filepath2 stringByAppendingPathComponent:@"hydapp/www.zip"];
    
    //NSString* rootDir = [[NSBundle mainBundle] bundlePath];
    
    //NSString *zipPath = [rootDir stringByAppendingPathComponent:@"www/www.zip"];
    //NSString *destinationPath = [rootDir stringByAppendingPathComponent:@"test/"];
    
    NSString *destinationPath = [NSHomeDirectory() stringByAppendingPathComponent:@"Documents/hydapp"];
    [SSZipArchive unzipFileAtPath:down_filePath toDestination:destinationPath];
    
    //[20130926][chisu]?�집??종료?�었?�면 index.html?�일??로드?�다.
    NSString* loadurl = [destinationPath stringByAppendingPathComponent:@"index.html"];
    NSURL *testURL = [NSURL URLWithString:loadurl];
    NSURLRequest *testURLrequest = [NSURLRequest requestWithURL:testURL];
    [[super webView] loadRequest:testURLrequest];
}

@end
