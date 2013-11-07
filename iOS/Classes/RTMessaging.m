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


#import "RTMessaging.h"
#import "NSDictionary+Extensions.h"


@interface RTMessaging () {
}
@property (readwrite, assign) BOOL isRunning;
@end


@implementation RTMessaging

@synthesize callbackId, isRunning;


#define TYPE_SMS      1
#define TYPE_MMS      2
#define TYPE_EMAIL      3

- (RTMessaging*) init
{
    self = [super init];
    if (self)
    {
        self.callbackId = nil;
        self.isRunning = NO;
    }
    return self;
}

- (void) dealloc {
    [super dealloc]; // pretty important.
}

- (void)sendMessage:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    NSString* cbId = [arguments objectAtIndex:0];
    self.callbackId = cbId;
    
	if(!self.isRunning)
	{
		self.isRunning = YES;
        if (options) {
            //NSString* type = [options objectForKey:@"type"];
            NSInteger type = [options integerValueForKey:@"type" defaultValue:-1];
            
            if([MFMessageComposeViewController canSendText] && type == TYPE_SMS){
                MFMessageComposeViewController *smsController = [[MFMessageComposeViewController alloc] init];
                smsController.messageComposeDelegate = self;
                smsController.recipients = [options objectForKey:@"to"];                                
                smsController.body = [options objectForKey:@"body"];
                
                [self.viewController presentModalViewController:smsController animated:YES];
                [smsController release];
                
            }else if([MFMailComposeViewController canSendMail] && type == TYPE_EMAIL){
                MFMailComposeViewController *mailController = [[MFMailComposeViewController alloc]init];		
                mailController.mailComposeDelegate = self;
                [mailController setSubject:[options objectForKey:@"subject"]];
                [mailController setMessageBody:[options objectForKey:@"body"] isHTML:NO];
                [mailController setToRecipients:[options objectForKey:@"to"]];
                [mailController setCcRecipients:[options objectForKey:@"cc"]];
                [mailController setBccRecipients:[options objectForKey:@"bcc"]];
                NSArray *filearray = [options objectForKey:@"attachments"];
                for(NSMutableDictionary *file in filearray) {
                    NSString *fullpath = [file objectForKey:@"fullPath"];
                    NSString *name = [file objectForKey:@"name"];
                    NSFileHandle* filehandle = [ NSFileHandle fileHandleForReadingAtPath:fullpath];
                    
                    if(!filehandle){
                        // invalid path entry
                        RTPluginResult *result = [RTPluginResult resultWithStatus:RTCommandStatus_ERROR];
                        [self writeJavascript: [result toErrorCallbackString:self.callbackId]];
                        return;
                    } else {
                        NSData* readData = [ filehandle readDataToEndOfFile];                        
                        [filehandle closeFile];
                        NSString *mimetype = [self getMimeTypeFromPath:fullpath];
                        [mailController addAttachmentData:readData mimeType:mimetype fileName:name];
                    }
                }
                
                [self.viewController presentModalViewController:mailController animated:YES];
                [mailController release];
                
            }else{
                //..보낼수 없을떄의 처리
                self.isRunning = NO;
                RTPluginResult* pluginResult= [RTPluginResult resultWithStatus:RTCommandStatus_OK];;
                NSString* jsString = [pluginResult toErrorCallbackString:self.callbackId];
                [self writeJavascript: jsString];
            }

        }
	}
}

- (void)messageComposeViewController:(MFMessageComposeViewController *)controller didFinishWithResult:(MessageComposeResult)result
{
    self.isRunning = NO;
    RTPluginResult* pluginResult= nil;
    NSString* jsString = nil;
	switch (result) {
		case MessageComposeResultFailed:
            pluginResult = [RTPluginResult resultWithStatus:RTCommandStatus_OK];
            jsString = [pluginResult toErrorCallbackString:callbackId];
            break;
        case MessageComposeResultCancelled:
		case MessageComposeResultSent:
            pluginResult = [RTPluginResult resultWithStatus:RTCommandStatus_OK];
            jsString = [pluginResult toSuccessCallbackString:self.callbackId];
			break;
		default:
			break;
	}
    
    if (jsString){
		[self writeJavascript: jsString];
	}

	[self.viewController dismissModalViewControllerAnimated:YES];
}

- (void)mailComposeController:(MFMailComposeViewController*)controller didFinishWithResult:(MFMailComposeResult)result error:(NSError*)error
{
    self.isRunning = NO;
    RTPluginResult* pluginResult = nil;
    NSString* jsString = nil;
	switch (result) {
        case MFMailComposeResultFailed:
            pluginResult = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsInt:0];
            jsString = [pluginResult toErrorCallbackString:callbackId];
            break;
        case MFMailComposeResultCancelled:
		case MFMailComposeResultSaved:
        case MFMailComposeResultSent:
            pluginResult = [RTPluginResult resultWithStatus:RTCommandStatus_OK];
            jsString = [pluginResult toSuccessCallbackString:self.callbackId];
			break;
		default:
			break;
	}
    
    if (jsString){
		[self writeJavascript: jsString];
	}
	[self.viewController dismissModalViewControllerAnimated:YES];
}

-(NSString*) getMimeTypeFromPath: (NSString*) fullPath
{	
	
	NSString* mimeType = nil;
	if(fullPath) {
		CFStringRef typeId = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension,(CFStringRef)[fullPath pathExtension], NULL);
		if (typeId) {
			mimeType = (NSString*)UTTypeCopyPreferredTagWithClass(typeId,kUTTagClassMIMEType);
			if (mimeType) {
				[mimeType autorelease];
				//NSLog(@"mime type: %@", mimeType);
			} else {
                // special case for m4a
                if ([(NSString*)typeId rangeOfString: @"m4a-audio"].location != NSNotFound){
                    mimeType = @"audio/mp4";
                } else if ([[fullPath pathExtension] rangeOfString:@"wav"].location != NSNotFound){
                    mimeType = @"audio/wav";
                }
            }
			CFRelease(typeId);
		}
	}
	return mimeType;
}

@end
