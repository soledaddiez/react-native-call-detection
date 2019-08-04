//
//  CallDetectionManager.m
//
//
//  Created by Pritesh Nandgaonkar on 16/06/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import "CallDetectionManager.h"
@import CallKit;

typedef void (^CallBack)();
@interface CallDetectionManager()

@property(strong, nonatomic) RCTResponseSenderBlock block;
@property(strong, nonatomic) CXCallObserver* callObserver;

@end

@implementation CallDetectionManager
- (NSDictionary *)constantsToExport
{
    return @{
             @"Connected"   : @"Connected",
             @"Dialing"     : @"Dialing",
             @"Disconnected": @"Disconnected",
             @"Incoming"    : @"Incoming"
             };
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"PhoneCallStateUpdate"];
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(addCallBlock:(RCTResponseSenderBlock) block) {
  // Setup call tracking
  self.block = block;
  self.callObserver = [[CXCallObserver alloc] init];
  __typeof(self) weakSelf = self;
  [self.callObserver setDelegate:weakSelf queue:nil];
}

RCT_EXPORT_METHOD(startListener) {
    // Setup call tracking
    self.callObserver = [[CXCallObserver alloc] init];
    __typeof(self) weakSelf = self;
    [self.callObserver setDelegate:weakSelf queue:nil];
}

RCT_EXPORT_METHOD(stopListener) {
    // Setup call tracking
    self.callObserver = nil;
}

RCT_EXPORT_METHOD(currentCalls:(RCTResponseSenderBlock)_callback) {
    CXCallObserver *callObserver = [[CXCallObserver alloc] init]; // using my own callObserver to avoid interferrence with event listeners
    NSMutableArray<NSDictionary *> *calls = [[NSMutableArray alloc] init];

    for (CXCall *aCall in callObserver.calls) {
        NSString * status = @"Disconnected";
        if (aCall.hasEnded == true) {
            status = @"Disconnected";
        } else {
            if (aCall.hasConnected == true) {
                status = @"Connected";
            } else {
                if (aCall.isOutgoing == true) {
                    status = @"Dialing";
                } else {
                    status = @"Incoming";
                }
            }
        }
        [calls addObject:@{@"callID": [aCall.UUID UUIDString], @"callState": [self.constantsToExport valueForKey:status]}];
    }
    _callback(@[[NSNull null], calls]);
}

- (void)callObserver:(CXCallObserver *)callObserver callChanged:(CXCall *)call {

    NSString * callUUID = [call.UUID UUIDString];
    if (call.hasEnded == true) {
        [self sendEventWithName:@"PhoneCallStateUpdate" body:@{@"callID": callUUID, @"callState": [self.constantsToExport valueForKey:@"Disconnected"]}];
    }
    if (call.isOutgoing == true && call.hasConnected == false && call.hasEnded == false) {
        [self sendEventWithName:@"PhoneCallStateUpdate" body:@{@"callID": callUUID, @"callState": [self.constantsToExport valueForKey:@"Dialing"]}];
    }
    if (call.isOutgoing == false && call.hasConnected == false) {
        [self sendEventWithName:@"PhoneCallStateUpdate" body:@{@"callID": callUUID, @"callState": [self.constantsToExport valueForKey:@"Incoming"]}];
    }
    if (call.hasEnded == false && call.hasConnected == true) {
        [self sendEventWithName:@"PhoneCallStateUpdate" body:@{@"callID": callUUID, @"callState": [self.constantsToExport valueForKey:@"Connected"]}];
    }
}

@end
