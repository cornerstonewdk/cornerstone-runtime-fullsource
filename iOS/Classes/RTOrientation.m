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


#import "RTOrientation.h"

@interface RTOrientation () {
}
@property (readwrite, assign) BOOL isRunning;
@end


@implementation RTOrientation

@synthesize callbackId, isRunning, locationManager;

// defaults to 100 msec
#define kGyroInterval      100 
// max rate of 40 msec
#define kMinGyroInterval    40  
// min rate of 1/sec
#define kMaxGyroInterval   1000

- (RTOrientation*) init
{
    self = [super init];
    if (self)
    {
        alpha  = 0;
        beta = 0;
        gamma = 0;
        timestamp = 0;
        self.locationManager = nil;
        self.callbackId = nil;
        self.isRunning = NO;
    }
    return self;
}

- (void) dealloc {
    [locationManager stopUpdatingHeading];
    [locationManager release];
    [self stop:nil withDict:nil];
    [super dealloc]; // pretty important.
}

- (void)start:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    NSString* cbId = [arguments objectAtIndex:0];
    self.locationManager = [[[CLLocationManager alloc]init] autorelease];
        
    self.callbackId = cbId;
	if(!self.isRunning)
	{
		self.isRunning = YES;
        if( [CLLocationManager headingAvailable] == YES)
        {
            locationManager.headingFilter = kCLHeadingFilterNone;
            locationManager.delegate = self;
            [locationManager startUpdatingHeading];
        }
        else
        {
            //gyroscopeLabel.text = @"This device has no gyroscope";
        }

	}
}

- (void)stop:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    
	[locationManager stopUpdatingHeading];
	self.isRunning = NO;
     
}

// This delegate method is invoked when the location manager has heading data.
- (void)locationManager:(CLLocationManager *)manager didUpdateHeading:(CLHeading *)heading {
    // Update the labels with the raw x, y, and z values.
    
    if(self.isRunning)
	{
        //(CLLocationDirection *)cd = heading.trueHeading;
        alpha = heading.x;
        beta = heading.y;
        gamma = heading.z;
        timestamp = ([[NSDate date] timeIntervalSince1970] * 1000);
        [self returnAccelInfo];
    }
        
;
    
    // Compute and display the magnitude (size or strength) of the vector.
	//      magnitude = sqrt(x^2 + y^2 + z^2)
//	CGFloat magnitude = sqrt(heading.x*heading.x + heading.y*heading.y + heading.z*heading.z);
//    [magnitudeLabel setText:[NSString stringWithFormat:@"%.1f", magnitude]];
	
}

// This delegate method is invoked when the location managed encounters an error condition.
- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error {
    if ([error code] == kCLErrorDenied) {
        // This error indicates that the user has denied the application's request to use location services.
        [manager stopUpdatingHeading];
    } else if ([error code] == kCLErrorHeadingFailure) {
        // This error indicates that the heading could not be determined, most likely because of strong magnetic interference.
    }
}

/**
 * Picks up accel updates from device and stores them in this class
 */

- (void)returnAccelInfo
{
    RTPluginResult* result = nil;
    NSString* jsString = nil;

    // Create an acceleration object
    NSMutableDictionary *accelProps = [NSMutableDictionary dictionaryWithCapacity:4];
    [accelProps setValue:[NSNumber numberWithDouble:alpha] forKey:@"alpha"];
    [accelProps setValue:[NSNumber numberWithDouble:beta] forKey:@"beta"];
    [accelProps setValue:[NSNumber numberWithDouble:gamma] forKey:@"gamma"];
    [accelProps setValue:[NSNumber numberWithDouble:timestamp] forKey:@"timestamp"];
    
    result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsDictionary:accelProps];
    [result setKeepCallback:[NSNumber numberWithBool:YES]];
    jsString = [result toSuccessCallbackString:self.callbackId];
    [self writeJavascript:jsString]; 
}

@end
