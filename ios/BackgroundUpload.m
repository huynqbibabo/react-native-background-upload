#import "BackgroundUpload.h"
#import <AVFoundation/AVFoundation.h>
#import <AVFoundation/AVAsset.h>
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

// Will be call to stop upload video
RCT_EXPORT_METHOD(stopBackgroundUpload:(NSNumber * _Nonnull)workId resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    resolve(workId);
}

// Will be call to get the current state of video uplaoding process
RCT_EXPORT_METHOD(getCurrentState:(NSNumber * _Nonnull)workId resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    resolve(@{
        @"state": @"request metadata",
        @"response": @"request metadata",
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
    NSMutableString *compressedPath = [[NSMutableString alloc] init];
    NSMutableArray *chunks = [[NSMutableArray alloc] init];
    [[[[[self transcodeVideo:filePath enableCompression:enableCompression] continueWithBlock:^id _Nullable(BFTask* _Nonnull task) {
        if (task.error != nil) {
            NSLog(@"Compress video error: %@", task.error);
            [self stopObserving];
            return nil;
        }
        NSLog(@"Compress video path: %@", task.result);
        // set compressed video path
        [compressedPath appendString:task.result];
        return [self splitVideoIntoChunks: compressedPath chunkSize:chunkSize];
    }] continueWithSuccessBlock:^id _Nullable(BFTask* _Nonnull task) {
        NSLog(@"Split video response: %@", task.result);
        [chunks addObjectsFromArray:task.result];
        return [self requestMetadata:metadataUrl numberOfChunks:[NSNumber numberWithInteger:[chunks count]]];
    }] continueWithBlock:^id _Nullable(BFTask* _Nonnull task) {
        if (task.error != nil) {
            NSLog(@"Metadata error: %@", task.error.localizedDescription);
            [self stopObserving];
            return nil;
        }
//        NSLog(@"Metadata response: %@", task.result);
        NSString *fileName = [task.result objectForKey:@"filename"];
        NSDictionary *hashes = [task.result objectForKey:@"hashes"];
        
        // Create a trivial completed task as a base case.
        BFTask *uploadTask = [BFTask taskWithResult:nil];
        for(id key in hashes) {
            NSLog(@"Key:%@ Value:%@", key, hashes[key]);
            uploadTask = [uploadTask continueWithBlock:^id(BFTask *task) {
                // Return a task that will be marked as completed when the upload is finished.
                int index = [key intValue] - 1;
                return [self uploadVideoChunk:uploadUrl fileData:[chunks objectAtIndex:(NSInteger) index] fileName:fileName hash:hashes[key] prt:key];
            }];
        }
        return uploadTask;
    }] continueWithBlock:^id _Nullable(BFTask* _Nonnull task) {
        if (task.error != nil) {
            NSLog(@"Upload error: %@", task.error.localizedDescription);
            [self stopObserving];
            return nil;
        }
        NSLog(@"Upload response: %@", task.result);
        return task;
    }];
    resolve(workId);
}

// Method to transcode video
-(BFTask *) transcodeVideo:(NSString * _Nonnull)filePath enableCompression:(BOOL * _Nonnull)enableCompression {
    BFTaskCompletionSource *completionSource = [BFTaskCompletionSource taskCompletionSource];
    if (enableCompression) {
        @try {
            filePath = [filePath stringByReplacingOccurrencesOfString:@"file://" withString:@""];
            NSString *preset = AVAssetExportPreset1280x720;
                    
            // save to temp directory
            NSString* tempDirectory = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) lastObject];
            NSString *outputFilePath = [[tempDirectory stringByAppendingPathComponent: [NSString stringWithFormat:@"%@.mp4", [[NSProcessInfo processInfo] globallyUniqueString]]] stringByReplacingOccurrencesOfString:@"file://" withString:@""];
                    
            NSURL* inputURL = [NSURL fileURLWithPath:filePath];
            NSURL* outputURL = [NSURL fileURLWithPath:outputFilePath];
                    
            [[NSFileManager defaultManager] removeItemAtURL:outputURL error:nil];
            AVURLAsset *asset = [AVURLAsset URLAssetWithURL:inputURL options:nil];
            AVAssetExportSession *exportSession = [[AVAssetExportSession alloc] initWithAsset:asset presetName:preset];
            exportSession.shouldOptimizeForNetworkUse = YES;
            exportSession.outputURL = outputURL;
            exportSession.outputFileType = AVFileTypeMPEG4;
                    
            [exportSession exportAsynchronouslyWithCompletionHandler:^(void) {
                if (exportSession.status == AVAssetExportSessionStatusCompleted) {
                    [completionSource setResult:outputFilePath];
                } else {
                    [completionSource setError:[self customNSError:@"Cannot compress video"]];
                }
            }];
        } @catch(NSException *e) {
            [completionSource setError:[self customNSError:e.reason]];
        }
    } else {
        [completionSource setResult:[filePath stringByReplacingOccurrencesOfString:@"file://" withString:@""]];
    }
    return completionSource.task;
}

// Method to split video into chunks
-(BFTask *) splitVideoIntoChunks:(NSString * _Nonnull)filePath chunkSize:(NSNumber * _Nonnull)chunkSize {
    BFTaskCompletionSource *completionSource = [BFTaskCompletionSource taskCompletionSource];
    @try {
        if([[NSFileManager defaultManager] fileExistsAtPath:filePath]) {
            NSMutableArray *chunkArray= [[NSMutableArray alloc]init];
            NSData *data = [[NSFileManager defaultManager] contentsAtPath:filePath];
            NSUInteger length = [data length];
            NSUInteger chunkSizeInt = [chunkSize integerValue];
            NSUInteger offset = 0;
            do {
                NSUInteger thisChunkSize = length - offset > chunkSizeInt ? chunkSizeInt : length - offset;
                NSData* chunk = [NSData dataWithBytesNoCopy:(char *)[data bytes] + offset length:thisChunkSize freeWhenDone:NO];
                [chunkArray addObject:chunk];
                offset += thisChunkSize;
            } while (offset < length);
            [completionSource setResult:chunkArray];
        } else {
            [completionSource setError:[self customNSError:@"File doesn't exits"]];
        }
    } @catch(NSException *e) {
        [completionSource setError:[self customNSError:e.reason]];
    }
    return completionSource.task;
}

// Method to request metadata for upload media file.
-(BFTask *) requestMetadata:(NSString * _Nonnull)metadataUrl numberOfChunks:(NSNumber * _Nonnull)numberOfChunks {
    BFTaskCompletionSource *completionSource = [BFTaskCompletionSource taskCompletionSource];
    @try {
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
                NSLog(@"response: %@", responseObject[@"data"]);
                NSNumber *status  = [responseObject objectForKey:@"status"];
                if ([status intValue] == 1) {
                    [completionSource setResult:(NSDictionary*)[responseObject objectForKey:@"data"]];
                } else {
                    [completionSource setError:[self customNSError:@"Response status = 0 when request metadata"]];
                }
            } else {
                [completionSource setError:[self customNSError:@"Invalid response object when request metadata"]];
            }
        }];
        [dataTask resume];
    } @catch (NSException *e) {
        [completionSource setError:[self customNSError:e.reason]];
    }
    return completionSource.task;
}

// Method to upload video by chunks
-(BFTask *) uploadVideoChunk:(NSString * _Nonnull)uploadUrl fileData:(NSData * _Nonnull)fileData fileName:(NSString * _Nonnull)fileName hash:(NSString * _Nonnull)hash prt:(NSString * _Nonnull)prt {
    BFTaskCompletionSource *completionSource = [BFTaskCompletionSource taskCompletionSource];
    @try {
        NSMutableURLRequest *request = [[AFHTTPRequestSerializer serializer] multipartFormRequestWithMethod:@"POST" URLString:uploadUrl parameters:nil
            constructingBodyWithBlock:^(id<AFMultipartFormData> formData) {
                [formData appendPartWithFileData:fileData name:@"data" fileName:fileName mimeType:@"video/*"];
//                [formData appendPartWithFileD:[NSURL fileURLWithPath:filePath] name:@"data" fileName:fileName mimeType:@"video/*" error:nil];
                [formData appendPartWithFormData:[fileName dataUsingEncoding:NSUTF8StringEncoding] name:@"filename"];
                [formData appendPartWithFormData:[hash dataUsingEncoding:NSUTF8StringEncoding] name:@"hash"];
                [formData appendPartWithFormData:[prt dataUsingEncoding:NSUTF8StringEncoding] name:@"prt"];
            }
            error:nil
        ];
        AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];
        NSURLSessionUploadTask *uploadTask = [manager uploadTaskWithStreamedRequest:request
            progress:^(NSProgress * _Nonnull uploadProgress) {
                NSLog(@"Progress: %i", (int)(uploadProgress.fractionCompleted * 100));
            }
            completionHandler:^(NSURLResponse * _Nonnull response, id  _Nullable responseObject, NSError * _Nullable error) {
                if (error) {
                    [completionSource setError:error];
                } else if ([responseObject isKindOfClass:[NSDictionary class]]) {
                    NSNumber *status  = [responseObject objectForKey:@"status"];
                    if ([status intValue] == 1) {
                        [completionSource setResult:(NSDictionary*)[responseObject objectForKey:@"data"]];
                    } else {
                        [completionSource setError:[self customNSError:[responseObject objectForKey:@"message"]]];
                    }
                } else {
                    [completionSource setError:[self customNSError:@"Invalid response object when upload video chunk"]];
                }
            }
        ];
        [uploadTask resume];
    } @catch(NSException *e) {
        [completionSource setError:[self customNSError:e.reason]];
    }
    return completionSource.task;
}

-(NSError *) customNSError: (NSString * _Nonnull)localizedDescription {
    return [NSError errorWithDomain:@"RN Background upload video" code:100 userInfo:@{
        NSLocalizedDescriptionKey:localizedDescription
    }];
}

@end
