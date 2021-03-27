#import "BackgroundUpload.h"
#import <AFNetworking.h>
#import <Bolts.h>

@implementation BackgroundUpload
{
  bool hasListeners;
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"onStateChange"];
}

// Will be called when this module's first listener is added.
-(void)startObserving {
    hasListeners = YES;
    // Set up any upstream listeners or background tasks as necessary
}

// Will be called when this module's last listener is removed, or on dealloc.
-(void)stopObserving {
    hasListeners = NO;
    // Remove upstream listeners, stop unnecessary background tasks
}

-(void)onStateChange:(NSNumber * _Nonnull)workId {
    if (hasListeners) {
        [self sendEventWithName:@"onStateChange" body:@{
            @"workId": workId,
            @"state": @"request metadata",
            @"response": @"request metadata",
            @"progress": @100
        }];
    }
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(stopBackgroundUpload:(NSNumber * _Nonnull)workId resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    resolve(@{
        @"workId": workId,
        @"state": @"request metadata",
        @"response": @"request metadata",
        @"progress": @100
    });
}

RCT_EXPORT_METHOD(getCurrentState:(NSNumber * _Nonnull)workId resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    resolve(@{
        @"workId": workId,
        @"state": @"request metadata",
        @"response": @"request metadata",
        @"progress": @100
    });
}

// Will be call to start upload video
RCT_EXPORT_METHOD(startBackgroundUploadVideo:(NSNumber * _Nonnull)workId
                  uploadUrl:(NSString * _Nonnull)uploadUrl
                  metadataUrl:(NSString * _Nonnull)metadataUrl
                  filePath:(NSString * _Nonnull)filePath
                  chunkSize:(NSNumber * _Nonnull)chunkSize
                  enableCompression:(BOOL * _Nonnull)enableCompression
                  chainTask:(NSDictionary * _Nullable)chainTask
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
){
    [self startObserving];
    NSLog(@"filePath: %@", filePath);
//    [self requestMetadata:metadataUrl numberOfChunks:[NSNumber numberWithInt: 1]];
    [[self requestMetadata:metadataUrl numberOfChunks:[NSNumber numberWithInt: 1]] continueWithBlock:^id _Nullable(BFTask* _Nonnull task) {
        if (task.error != nil) {
            NSLog(@"Error: %@", task.error);
            reject(@"RN Background upload video", task.error.localizedDescription, nil);
            [self stopObserving];
            return task;
        }
        NSLog(@"response: %@", task.result);
        resolve(workId);
        [self stopObserving];
        return task;
    }];
}

-(BFTask *) transcodeVideo:(NSString * _Nonnull)filePath enableCompression:(BOOL * _Nonnull)enableCompression {
    
}

-(BFTask *) requestMetadata:(NSString * _Nonnull)metadataUrl numberOfChunks:(NSNumber * _Nonnull)numberOfChunks {
    BFTaskCompletionSource *completionSource = [BFTaskCompletionSource taskCompletionSource];
    
    NSDictionary* requestMetadataPostDictionary = @{
        @"cto": [numberOfChunks stringValue],
        @"ext": @"mp4"
    };
    NSMutableURLRequest *request = [[AFHTTPRequestSerializer serializer] requestWithMethod:@"POST" URLString:metadataUrl parameters:(NSDictionary *)requestMetadataPostDictionary error:nil];

    AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];

    NSURLSessionDataTask *dataTask = [manager dataTaskWithRequest:request uploadProgress:nil downloadProgress:nil completionHandler:^(NSURLResponse * _Nonnull response, id _Nullable responseObject, NSError * _Nullable error) {
        if (error) {
            [completionSource setError:error];
        } else if ([responseObject isKindOfClass:[NSDictionary class]]) {
            //            NSLog(@"response: %@", responseObject[@"status"]);
            //            NSLog(@"response: %@", responseObject[@"message"]);
            //            NSLog(@"response: %@", responseObject[@"data"]);
            [completionSource setResult:(NSDictionary*)responseObject];
        } else {
            NSError *nsError = [NSError errorWithDomain:@"RN Background upload video" code:100 userInfo:@{
                NSLocalizedDescriptionKey:@"Invalid response object when request metadata"
            }];
            [completionSource setError:nsError];
        }
    }];
    [dataTask resume];
    return completionSource.task;
}

-(BFTask *) uploadVideoChunk:(NSString * _Nonnull)uploadUrl filePath:(NSString * _Nonnull)filePath fileName:(NSString * _Nonnull)fileName hash:(NSString * _Nonnull)hash prt:(NSString * _Nonnull)prt {
    BFTaskCompletionSource *completionSource = [BFTaskCompletionSource taskCompletionSource];
    NSMutableURLRequest *request = [[AFHTTPRequestSerializer serializer] multipartFormRequestWithMethod:@"POST" URLString:uploadUrl parameters:nil
        constructingBodyWithBlock:^(id<AFMultipartFormData> formData) {
            [formData appendPartWithFileURL:[NSURL fileURLWithPath:filePath] name:@"data" fileName:fileName mimeType:@"video/*" error:nil];
            [formData appendPartWithFormData:[fileName dataUsingEncoding:NSUTF8StringEncoding] name:@"filename"];
            [formData appendPartWithFormData:[hash dataUsingEncoding:NSUTF8StringEncoding] name:@"hash"];
            [formData appendPartWithFormData:[prt dataUsingEncoding:NSUTF8StringEncoding] name:@"prt"];
        }
        error:nil
    ];
    AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];
    NSURLSessionUploadTask *uploadTask = [manager uploadTaskWithStreamedRequest:request
        progress:^(NSProgress * _Nonnull uploadProgress) {
//            NSLog(@"Progress: %@", uploadProgress);
            NSLog(@"Progress: %i", (int)(uploadProgress.fractionCompleted * 100));
        }
        completionHandler:^(NSURLResponse * _Nonnull response, id  _Nullable responseObject, NSError * _Nullable error) {
            if (error) {
//                NSLog(@"Error: %@", error);
                [completionSource setError:error];
            } else if ([responseObject isKindOfClass:[NSDictionary class]]) {
                NSLog(@"status: %@", responseObject[@"status"]);
                NSLog(@"message: %@", responseObject[@"message"]);
                NSLog(@"data: %@", responseObject[@"data"]);
                [completionSource setResult:(NSDictionary*)responseObject];
            } else {
                NSError *nsError = [NSError errorWithDomain:@"RN Background upload video" code:100 userInfo:@{
                    NSLocalizedDescriptionKey:@"Invalid response object when upload video chunk"
                }];
                [completionSource setError:nsError];
            }
        }
    ];
    [uploadTask resume];
    return completionSource.task;
}

@end
