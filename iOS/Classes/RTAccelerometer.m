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


#import "RTAccelerometer.h"

@interface RTAccelerometer () {
}
@property (readwrite, assign) BOOL isRunning;
@end


@implementation RTAccelerometer

@synthesize callbackId, isRunning;

// defaults to 100 msec
#define kAccelerometerInterval      100 
// max rate of 40 msec
#define kMinAccelerometerInterval    40  
// min rate of 1/sec
#define kMaxAccelerometerInterval   1000
// g constant: -9.81 m/s^2
#define kGravitionalConstant        -9.81

- (RTAccelerometer*) init
{
    self = [super init];
    if (self)
    {
        x = 0;
        y = 0;
        z = 0;
        timestamp = 0;
        self.callbackId = nil;
        self.isRunning = NO;
    }
    return self;
}

- (void) dealloc {
    [self stop:nil withDict:nil];
    [super dealloc]; // pretty important.
}

- (void)start:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    NSString* cbId = [arguments objectAtIndex:0];
	NSTimeInterval desiredFrequency_num = kMaxAccelerometerInterval;
	UIAccelerometer* pAccel = [UIAccelerometer sharedAccelerometer];
	// accelerometer expects fractional seconds, but we have msecs
	pAccel.updateInterval = desiredFrequency_num / 1000;
    self.callbackId = cbId;
	if(!self.isRunning)
	{
		pAccel.delegate = self;
		self.isRunning = YES;
	}
}

- (void)stop:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
	UIAccelerometer*  theAccelerometer = [UIAccelerometer sharedAccelerometer];
	theAccelerometer.delegate = nil;
	self.isRunning = NO;
}

/**
 * Picks up accel updates from device and stores them in this class
 */
- (void) accelerometer:(UIAccelerometer *)accelerometer didAccelerate:(UIAcceleration *)acceleration 
{
	if(self.isRunning)
	{
        x = acceleration.x;
        y = acceleration.y;
        z = acceleration.z;
        timestamp = ([[NSDate date] timeIntervalSince1970] * 1000);
        [self returnAccelInfo];
        
	}
}

- (void)returnAccelInfo
{
    RTPluginResult* result = nil;
    NSString* jsString = nil;

    // Create an acceleration object
    NSMutableDictionary *accelProps = [NSMutableDictionary dictionaryWithCapacity:4];
    [accelProps setValue:[NSNumber numberWithDouble:x*kGravitionalConstant] forKey:@"x"];
    [accelProps setValue:[NSNumber numberWithDouble:y*kGravitionalConstant] forKey:@"y"];
    [accelProps setValue:[NSNumber numberWithDouble:z*kGravitionalConstant] forKey:@"z"];
    [accelProps setValue:[NSNumber numberWithDouble:timestamp] forKey:@"timestamp"];
    
    result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsDictionary:accelProps];
    [result setKeepCallback:[NSNumber numberWithBool:YES]];
    jsString = [result toSuccessCallbackString:self.callbackId];
    [self writeJavascript:jsString]; 
}

// TODO: Consider using filtering to isolate instantaneous data vs. gravity data -jm

/* 
 #define kFilteringFactor 0.1
 
 // Use a basic low-pass filter to keep only the gravity component of each axis.
 grav_accelX = (acceleration.x * kFilteringFactor) + ( grav_accelX * (1.0 - kFilteringFactor));
 grav_accelY = (acceleration.y * kFilteringFactor) + ( grav_accelY * (1.0 - kFilteringFactor));
 grav_accelZ = (acceleration.z * kFilteringFactor) + ( grav_accelZ * (1.0 - kFilteringFactor));
 
 // Subtract the low-pass value from the current value to get a simplified high-pass filter
 instant_accelX = acceleration.x - ( (acceleration.x * kFilteringFactor) + (instant_accelX * (1.0 - kFilteringFactor)) );
 instant_accelY = acceleration.y - ( (acceleration.y * kFilteringFactor) + (instant_accelY * (1.0 - kFilteringFactor)) );
 instant_accelZ = acceleration.z - ( (acceleration.z * kFilteringFactor) + (instant_accelZ * (1.0 - kFilteringFactor)) );
 

 */
@end
