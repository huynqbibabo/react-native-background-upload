#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

#define StateIdle @"idle"
#define StateTranscoding @"transcoding"
#define StateSplitting @"splitting"
#define StateRequestMetadata @"requestMetadata"
#define StateUploading @"uploading"
#define StateSuccess @"success"
#define StateFailed @"failed"
#define StateCancelled @"cancelled"

@interface BackgroundUpload : RCTEventEmitter <RCTBridgeModule>

@property (nonatomic, strong) NSDictionary* stateMap;

@end
