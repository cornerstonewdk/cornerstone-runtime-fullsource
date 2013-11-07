//
//  RTDeviceStatus.m
//  c3
//
//  Created by INFRA dev1 on 12. 8. 22..
//  Copyright (c) 2012년 INFRAWARE. All rights reserved.
//

#import "RTDeviceStatus.h"

#define kSRTDeviceaspect            @"aspect"
#define kSRTDeviceproperty          @"property"

/*
#define kSRTDeviceCellularNetwork   @"CellularNetwork"
#define kSRTDeviceoperatorName      @"operatorName"
#define kSRTDeviceisInRoaming       @"isInRoaming"
#define kSRTDevicesignalStrength    @"signalStrength"
#define kSRTDevicemnc               @"mnc"
#define kSRTDevicemcc               @"mcc"

#define kSRTDeviceDevice            @"Device"
#define kSRTDeviceimei              @"imei"
#define kSRTDevicemodel             @"model"
#define kSRTDevicevendor            @"vendor"
#define kSRTDeviceimsi              @"imsi"
#define kSRTDeviceversion           @"version"
#define kSRTDeviceplatform          @"platform"

#define kSRTDeviceOperatingSystem   @"OperatingSystem"
#define kSRTDevicelanguage          @"language"
#define kSRTDeviceversion           @"version"
#define kSRTDevicename              @"name"

#define kSRTDeviceRuntime           @"Runtime"

#define kSRTDeviceWiFiNetwork       @"WiFiNetwork"
#define kSRTDevicessid              @"ssid"
#define kSRTDevicesignalStrength    @"signalStrength"
#define kSRTDevicenetworkStatus     @"networkStatus"
*/
 

@implementation RTDeviceStatus


- (void)getPropertyValue:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options {
    NSString *callbackId = [arguments objectAtIndex:0];
    NSString *aspect = [options objectForKey:kSRTDeviceaspect];
    NSString *property = [options objectForKey:kSRTDeviceproperty];
    
    NSString *method = [NSString stringWithFormat:@"%@in%@",property,aspect];
    if([self respondsToSelector:NSSelectorFromString(method)]) {
        NSString *value = [self performSelector:NSSelectorFromString(method)];
        
        NSDictionary *resultdict = [NSDictionary dictionaryWithObjectsAndKeys:
                                    options,kSRTDeviceproperty,
                                    value,@"value",
                                    nil];
        
        RTPluginResult *result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsDictionary:resultdict];
        [self writeJavascript:[result toSuccessCallbackString:callbackId]];
    } else {
        RTPluginResult *result = [RTPluginResult resultWithStatus:RTCommandStatus_ERROR messageAsString:@"Not Supported"];
        [self writeJavascript:[result toErrorCallbackString:callbackId]];
    }
}

- (NSString*) operatorNameinCellularNetwork {
    CTTelephonyNetworkInfo *networkInfo = [[[CTTelephonyNetworkInfo alloc] init] autorelease];
    CTCarrier *carrier = [networkInfo subscriberCellularProvider];
    
    return [carrier carrierName];
}

- (NSString*) mccinCellularNetwork {
    CTTelephonyNetworkInfo *networkInfo = [[[CTTelephonyNetworkInfo alloc] init] autorelease];
    CTCarrier *carrier = [networkInfo subscriberCellularProvider];
    
    return [carrier mobileCountryCode];
}

- (NSString*) mncinCellularNetwork {
    CTTelephonyNetworkInfo *networkInfo = [[[CTTelephonyNetworkInfo alloc] init] autorelease];
    CTCarrier *carrier = [networkInfo subscriberCellularProvider];
    
    return [carrier mobileNetworkCode];
}

- (NSString*) modelinDevice {
    return [[UIDevice currentDevice] model];;
}

- (NSString*) vendorinDevice {
    return @"Apple";
}

- (NSString*) versioninDevice {
    return [[UIDevice currentDevice] systemVersion];
}

- (NSString*) platforminDevice {
    return [[UIDevice currentDevice] systemName];
}

- (NSString*) languageinOperatingSystem {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSArray *languages = [defaults objectForKey:@"AppleLanguages"];

    return [languages objectAtIndex:0];
}

- (NSString*) versioninOperatingSystem {
    return [[UIDevice currentDevice] systemVersion];
}

- (NSString*) nameinOperatingSystem {
    return [[UIDevice currentDevice] systemName];
}

- (NSString*) vendorinOperatingSystem {
    return @"Apple";
}

- (NSString*) versioninRuntime{
    return @"1.0";
}

- (NSString*) nameinRuntime {
    return @"SKT HTML5 Runtime";
}

- (NSString*) vendorinRuntime {
    return @"SK Telecom";
}

- (NSString*) ssidinWiFiNetwork {
    NSArray *ifs = (id)CNCopySupportedInterfaces();
    NSLog(@"%s: Supported interfaces: %@", __func__, ifs);
    NSDictionary *info = nil;
    NSString *ssid = nil;
    
    for (NSString *ifnam in ifs) {
        info = (id)CNCopyCurrentNetworkInfo((CFStringRef)ifnam);
        NSLog(@"%s: %@ => %@", __func__, ifnam, info);
        if (info && [info count]) {
            ssid = [info objectForKey:(NSString*)kCNNetworkInfoKeySSID];
            break;
        }
        [info release];
    }
                    
    [ifs release];
    [info autorelease];
    return ssid;
}


- (NSString*) networkStatusinWiFiNetwork {
    NSArray *ifs = (id)CNCopySupportedInterfaces();
    NSDictionary *info = nil;
    NSString *result = @"Disconnected";
    
    for (NSString *ifnam in ifs) {
        info = (id)CNCopyCurrentNetworkInfo((CFStringRef)ifnam);
        NSLog(@"%s: %@ => %@", __func__, ifnam, info);
        if (info && [info count]) {
            result = @"Connected";
            break;
        }
        [info release];
    }
    
    [ifs release];
    [info autorelease];
    
    return result;
}

@end
