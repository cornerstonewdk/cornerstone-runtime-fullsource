//
//  RTBluetooth.m
//  SRTLib
//
//  Created by INFRA dev1 on 13. 5. 28..
//  Copyright (c) 2013년 INFRAWARE. All rights reserved.
//

#import "RTBluetooth.h"

@interface RTBluetooth()<CBCentralManagerDelegate, CBPeripheralDelegate>

@property (strong, nonatomic) CBCentralManager      *centralManager;
@property (strong, nonatomic) CBPeripheral          *discoveredPeripheral;

@end

@implementation RTBluetooth

@synthesize currentSession;

#pragma mark - View Lifecycle

- (void) scanDevice:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options {
    // Start up the CBCentralManager
    //centralManager = [[CBCentralManager alloc] initWithDelegate:self queue:nil];
    
    
    [self showPicker];
}
 
- (void) stopScanDevice:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options {
    NSString* callbackId = [arguments objectAtIndex:0];
    RTPluginResult* result = [RTPluginResult resultWithStatus:RTCommandStatus_OK messageAsInt:11];
    [super writeJavascript:[result toSuccessCallbackString:callbackId]];
}

- (void) sendData:(NSMutableArray*)arguments withDict:(NSMutableDictionary*)options {
    NSString* message = @"testString";
    [self sendData:message];
}




- (void) showPicker{
    GKPeerPickerController* picker = [[GKPeerPickerController alloc]init];
    picker.delegate = self;
    //picker.connectionTypesMask = GKPeerPickerConnectionTypeNearby | GKPeerPickerConnectionTypeOnline;
    picker.connectionTypesMask = GKPeerPickerConnectionTypeNearby;
    
    [picker show];
}

/*
- (GKSession *)peerPickerController:(GKPeerPickerController *) picker sessionForConnectionType:(GKPeerPickerConnectionType)type{
    NSString *sessionIDString = @"MTBluetoothSessionId";
    GKSession *session = [[GKSession alloc] initWithSessionID:sessionIDString displayName:nil sessionMode:GKSessionModePeer];
    session.available = true;
    return session;
}
*/

- (void) peerPickerController: (GKPeerPickerController *)picker didConnectPeer:(NSString *)peerID toSession:(GKSession *)session{
    
    currentSession = session;
    session.delegate = self;
    [session setDataReceiveHandler:self withContext:nil];
    
    picker.delegate = nil;
    
    [picker dismiss];
    [picker autorelease];
    
}

- (void) peerPickerControllerDidCancel: (GKPeerPickerController*) picker{
    picker.delegate = nil;
    [picker autorelease];
}

- (void) disconnect {
    [currentSession disconnectFromAllPeers];
    [currentSession release];
    currentSession = nil;
}

- (void)session:(GKSession*)session peer:(NSString *)peerID didChangeState:(GKPeerConnectionState)state{
    switch(state){
        case GKPeerStateConnected:
            NSLog(@"Connect Success");
            break;
        case GKPeerStateDisconnected:
            NSLog(@"Connection end");
            [currentSession release];
            currentSession = nil;
            break;
    }
}

- (void)sendData:(NSString *)message{
    NSData* data;
    data = [message dataUsingEncoding:NSASCIIStringEncoding];
    
    if(currentSession)
        [currentSession sendDataToAllPeers:data withDataMode:GKSendDataReliable error:nil];
}

- (void)receiveData:(NSData *)data fromPeer:(NSString *)peer inSession:(GKSession *)session context:(void *)context{
    NSString *str;
    str = [[NSString alloc] initWithData:data encoding:NSASCIIStringEncoding];
    UIAlertView *alert = [[UIAlertView alloc]initWithTitle:@"data receive" message:str delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil];
    
    [alert show];
    [alert release];
}














/** centralManagerDidUpdateState is a required protocol method.
 *  Usually, you'd check for other states to make sure the current device supports LE, is powered on, etc.
 *  In this instance, we're just using it to wait for CBCentralManagerStatePoweredOn, which indicates
 *  the Central is ready to be used.
 */
- (void)centralManagerDidUpdateState:(CBCentralManager *)central
{
 
    NSLog(@"centralManagerDidUpdateState");
    NSString *desc;
    
    switch (central.state) {
        case CBCentralManagerStatePoweredOff:
        {
            desc = @"CoreBluetooth BLE hardware is not powered\r\n";
            break;
        }
        case CBCentralManagerStatePoweredOn:
        {
            desc = @"CoreBluetooth BLE hardware is powered on and ready\r\n";
            break;
        }
        case CBCentralManagerStateResetting:
        {
            desc = @"CoreBluetooth BLE hardware is resetting\r\n";
            break;
        }
        case CBCentralManagerStateUnauthorized:
        {
            desc = @"CoreBluetooth BLE state is unauthorized\r\n";
            break;
        }
        case CBCentralManagerStateUnknown:
        {
            desc = @"CoreBluetooth BLE state is unknown\r\n";
            break;
        }
        case CBCentralManagerStateUnsupported:
        {
            desc = @"블루투스 장치를 지원하지 않습니다.\r\n";         
            break;
        }
        default:
            break;
    }
    NSLog(@"%@",desc);
    
    if (central.state != CBCentralManagerStatePoweredOn) {
        // In a real app, you'd deal with all the states correctly
        return;
    }
    
    // The state must be CBCentralManagerStatePoweredOn...
    
    // ... so start scanning
    [self scan];
    
}

/** Scan for peripherals - specifically for our service's 128bit CBUUID
 */
- (void)scan
{
    //[self.centralManager scanForPeripheralsWithServices:@[[CBUUID UUIDWithString:TRANSFER_SERVICE_UUID]]
    //                                            options:@{ CBCentralManagerScanOptionAllowDuplicatesKey : @YES }];
    
    
    NSDictionary *options = [NSDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithBool: NO],                             
                             CBCentralManagerScanOptionAllowDuplicatesKey, nil];        
    [self.centralManager scanForPeripheralsWithServices:nil options:options];
    
    NSLog(@"Scanning started");
}

/** This callback comes whenever a peripheral that is advertising the TRANSFER_SERVICE_UUID is discovered.
 *  We check the RSSI, to make sure it's close enough that we're interested in it, and if it is, 
 *  we start the connection process
 */
- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary *)advertisementData RSSI:(NSNumber *)RSSI
{
    // Reject any where the value is above reasonable range
    if (RSSI.integerValue > -15) {
        return;
    }
    
    // Reject if the signal strength is too low to be close enough (Close is around -22dB)
    if (RSSI.integerValue < -35) {
        return;
    }
    
    NSLog(@"Discovered %@ at %@", peripheral.name, RSSI);
    
    // Ok, it's in range - have we already seen it?
    if (self.discoveredPeripheral != peripheral) {
        
        // Save a local copy of the peripheral, so CoreBluetooth doesn't get rid of it
        self.discoveredPeripheral = peripheral;
        
        // And connect
        NSLog(@"Connecting to peripheral %@", peripheral);
        [self.centralManager connectPeripheral:peripheral options:nil];
    }
}

@end
