//
//  RTScreenShot.m
//  c3
//
//  Created by INFRA dev1 on 12. 8. 27..
//  Copyright (c) 2012년 INFRAWARE. All rights reserved.
//

#import "RTScreenShot.h"

@implementation RTScreenShot

- (void)captureScreenshot:(NSArray*)arguments withDict:(NSDictionary*)options
{
    NSString *callbackId = [arguments objectAtIndex:0];
    NSString *filename = [arguments objectAtIndex:1];
    
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *appDocsPath = [paths objectAtIndex:0];
    NSString *filepath = [NSString stringWithFormat: @"%@/%@",appDocsPath,filename];
    
	CGRect imageRect;
	CGRect screenRect = [[UIScreen mainScreen] bounds];
    
	// statusBarOrientation is more reliable than UIDevice.orientation
	UIInterfaceOrientation orientation = [UIApplication sharedApplication].statusBarOrientation;
    
	if (orientation == UIInterfaceOrientationLandscapeLeft || orientation == UIInterfaceOrientationLandscapeRight) { 
		// landscape check
		imageRect = CGRectMake(0, 0, CGRectGetHeight(screenRect), CGRectGetWidth(screenRect));
	} else {
		// portrait check
		imageRect = CGRectMake(0, 0, CGRectGetWidth(screenRect), CGRectGetHeight(screenRect));
	}
    
    //[20130719][chisu]screen shot crop
    int argc = [arguments count];
    
    if([arguments objectAtIndex:2] != [NSNull null] && [arguments objectAtIndex:3] != [NSNull null]
       && [arguments objectAtIndex:4] != [NSNull null] && [arguments objectAtIndex:5] != [NSNull null]){
        NSInteger x = argc > 2 ? [[arguments objectAtIndex:2] integerValue] : 0 ;
        NSInteger y = argc > 3 ? [[arguments objectAtIndex:3] integerValue] : 0 ;
        NSInteger width = argc > 4 ? [[arguments objectAtIndex:4] integerValue] : 0;
        NSInteger hieght = argc > 5 ? [[arguments objectAtIndex:5] integerValue] : 0;
        
        CGFloat viewwidth = CGRectGetWidth(screenRect);
        CGFloat viewheight = CGRectGetHeight(screenRect);
        
        if(width > viewwidth)
            width = viewwidth;
        if(hieght > viewheight)
            hieght = viewheight;
        
        imageRect = CGRectMake(x, y, width, hieght);
    }
    
	UIGraphicsBeginImageContext(imageRect.size);
    
	CGContextRef ctx = UIGraphicsGetCurrentContext();
	[[UIColor blackColor] set];
	CGContextTranslateCTM(ctx, 0, 0);
	CGContextFillRect(ctx, imageRect);
    
	[self.webView.layer renderInContext:ctx];
    
	UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    // save the image to photo album
	UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil);
	UIGraphicsEndImageContext();
    
    [self processImage:image path:filepath forCallbackId:callbackId];
}

-(void)processImage:(UIImage*)image path:(NSString*)filePath forCallbackId:(NSString*)callbackId
{
    RTPluginResult* result = nil;
    NSString* jsString = nil;
    
    NSData* data = nil;
    if([filePath hasSuffix:@"png"]) {
        data = UIImagePNGRepresentation(image);
    } else {
        data = UIImageJPEGRepresentation(image, 0.5);
    }
    // write to temp directory and reutrn URI
    //NSString* docsPath = [NSTemporaryDirectory() stringByStandardizingPath];  // use file system temporary directory
    NSError* err = nil;
    NSFileManager* fileMgr = [[NSFileManager alloc] init]; 
    
    if(![data writeToFile: filePath options: NSAtomicWrite error: &err]) {
        result = [RTPluginResult resultWithStatus: RTCommandStatus_OK];
        jsString = [result toErrorCallbackString: callbackId];
        if (err) {
            NSLog(@"Error saving image: %@", [err localizedDescription]);
        }
    } else {
        result = [RTPluginResult resultWithStatus: RTCommandStatus_OK messageAsString:filePath];
        jsString = [result toSuccessCallbackString:callbackId];
    }
    
    [fileMgr release];
    [super writeJavascript:jsString];
}

@end
