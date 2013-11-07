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


#import "RTProximitySensor.h"
#import "NSDictionary+Extensions.h"

@implementation RTProximitySensor

@synthesize callbackId;


- (RTProximitySensor*) init
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

- (void)start:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    self.callbackId = [arguments objectAtIndex:0];
    UIDevice *device = [UIDevice currentDevice];
    device.proximityMonitoringEnabled = YES;
    
    NSMutableDictionary *proximity = [NSMutableDictionary dictionaryWithCapacity:2];
    [proximity setValue:[NSNumber numberWithInt:TYPE_PROXIMITY] forKey:@"type"];
    [proximity setValue:[NSNumber numberWithBool:device.proximityState] forKey:@"value"];
    
    RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsDictionary:proximity];
    [result setKeepCallback:[NSNumber numberWithBool:YES]];
    [self writeJavascript:[result toSuccessCallbackString:self.callbackId]]; 
}

- (void)stop:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options
{
    
}
@end
