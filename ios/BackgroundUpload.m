#import "BackgroundUpload.h"
#import <AVFoundation/AVFoundation.h>
#import <AVFoundation/AVAsset.h>
#import <AFNetworking.h>
#import <Bolts.h>

@implementation BackgroundUpload
{
  bool hasListeners;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
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

-(void)onStateChange:(NSNumber * _Nonnull)workId state:(NSString * _Nonnull)state response:(NSString * _Nonnull)response progress:(NSNumber * _Nonnull)progress {
    [_stateMap setValue:@{@"state": state, @"response": response} forKey:[workId stringValue]];
    if (hasListeners) {
        [self sendEventWithName:@"onStateChange" body:@{
            @"workId": workId,
            @"state": state,
            @"response": response,
            @"progress": progress
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
    @try {
        resolve([_stateMap objectForKey:[workId stringValue]]);
    } @catch (NSException *exception) {
        reject(@"RN Background upload", exception.reason, nil);
    }
}

// Will be call to start upload video
RCT_EXPORT_METHOD(startBackgroundUploadVideo:(NSNumber * _Nonnull)workId
                  uploadUrl:(NSString * _Nonnull)uploadUrl
                  metadataUrl:(NSString * _Nonnull)metadataUrl
                  filePath:(NSString * _Nonnull)filePath
                  chunkSize:(NSNumber * _Nonnull)chunkSize
                  enableCompression:(NSNumber * _Nonnull)enableCompression
                  chainTask:(NSDictionary * _Nullable)chainTask
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
){
    [self startObserving];
    [self onStateChange:workId state:StateIdle response:@"setup background work queue" progress:@0];
    // init array to save chunk file path after split
    NSMutableArray *chunks = [[NSMutableArray alloc] init];
    
    // init session manager for network task
    AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];
    
    // start upload queue
    [[[[[self transcodeVideo:workId filePath:filePath enableCompression:enableCompression] continueWithBlock:^id _Nullable(BFTask* _Nonnull task) {
        if (task.error != nil) {
            NSLog(@"Compress video error: %@", task.error);
            return [self splitVideoIntoChunks:workId transcodedFilePath:filePath originalFilePath:filePath chunkSize:chunkSize];
        }
        NSLog(@"Compress video path: %@", task.result);
        return [self splitVideoIntoChunks:workId transcodedFilePath:task.result originalFilePath:filePath chunkSize:chunkSize];
    }] continueWithBlock:^id _Nullable(BFTask* _Nonnull task) {
        if (task.error != nil) {
            NSLog(@"Split chunk error: %@", task.error.localizedDescription);
            [self onStateChange:workId state:StateFailed response:task.error.localizedDescription progress:@0];
            [self stopObserving];
            return nil;
        }
//        NSLog(@"Split video response: %@", task.result);
        [chunks addObjectsFromArray:task.result];
        return [self requestMetadata:workId metadataUrl:metadataUrl numberOfChunks:[NSNumber numberWithInteger:[chunks count]] sessionManager:manager];
    }] continueWithBlock:^id _Nullable(BFTask* _Nonnull task) {
        if (task.error != nil) {
            NSLog(@"Metadata error: %@", task.error.localizedDescription);
            [self onStateChange:workId state:StateFailed response:task.error.localizedDescription progress:@0];
            [self stopObserving];
            return nil;
        }
//        NSLog(@"Metadata response: %@", task.result);
        NSString *fileName = [task.result objectForKey:@"filename"];
        NSDictionary *hashes = [task.result objectForKey:@"hashes"];
        
        // Create a trivial completed task as a base case.
        BFTask *uploadTask = [BFTask taskWithResult:nil];
        for (int i = 0; i < [chunks count]; i++) {
            uploadTask = [uploadTask continueWithBlock:^id(BFTask *task) {
                // Return a task that will be marked as completed when the upload is finished.
                NSString *prt = [@(i + 1) stringValue];
                return [self uploadVideoChunk:workId uploadUrl:uploadUrl filePath:chunks[i] fileName:fileName hash:[hashes objectForKey:prt] prt:prt numberOfChunks:[NSNumber numberWithInteger:[chunks count]] sessionManager:manager];
            }];
        }
        return uploadTask;
    }] continueWithBlock:^id _Nullable(BFTask* _Nonnull task) {
        if (task.error != nil) {
            NSLog(@"Upload error: %@", task.error.localizedDescription);
            [self onStateChange:workId state:StateFailed response:task.error.localizedDescription progress:@0];
            [self stopObserving];
            // clear cache
            for (NSString *path in chunks) {
                [self cleanUpCache:path];
            }
            return nil;
        }
        if (chainTask) {
            
        } else {
            [self onStateChange:workId state:StateSuccess response:@"complete with no chain task" progress:@100];
        }
        
        // clear cache
        for (NSString *path in chunks) {
            [self cleanUpCache:path];
        }
        [self stopObserving];
        return task;
    }];
    resolve(workId);
}

// Method to transcode video
-(BFTask *) transcodeVideo:(NSNumber * _Nonnull)workId filePath:(NSString * _Nonnull)filePath enableCompression:(NSNumber * _Nonnull)enableCompression {
    BFTaskCompletionSource *completionSource = [BFTaskCompletionSource taskCompletionSource];
    
    [self onStateChange:workId state:StateTranscoding response:@"start" progress:@0];
    
    if ([enableCompression boolValue] == YES) {
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
            
            __block NSTimer *timer = [NSTimer scheduledTimerWithTimeInterval:2 repeats:YES block:^(NSTimer * _Nonnull timer) {
                NSLog(@"Transcode progress: %f", exportSession.progress);
                float progress = roundf(100 * exportSession.progress);
                [self onStateChange:workId state:StateTranscoding response:@"progress" progress:[NSNumber numberWithFloat:progress]];
            }];
                    
            [exportSession exportAsynchronouslyWithCompletionHandler:^(void) {
                [timer invalidate];
                timer = nil;
                if (exportSession.status == AVAssetExportSessionStatusCompleted) {
                    [self onStateChange:workId state:StateTranscoding response:@"success" progress:@100];
                    [completionSource setResult:outputFilePath];
                } else {
                    [completionSource setError:[self customNSError:@"Cannot compress video"]];
                }
            }];
        } @catch(NSException *e) {
            [completionSource setError:[self customNSError:e.reason]];
        }
    } else {
        [self onStateChange:workId state:StateTranscoding response:@"start" progress:@0];
        [completionSource setResult:[filePath stringByReplacingOccurrencesOfString:@"file://" withString:@""]];
    }
    return completionSource.task;
}

// Method to split video into chunks
-(BFTask *) splitVideoIntoChunks:(NSNumber * _Nonnull)workId transcodedFilePath:(NSString * _Nonnull)transcodedFilePath originalFilePath:(NSString * _Nonnull)originalFilePath chunkSize:(NSNumber * _Nonnull)chunkSize {
    BFTaskCompletionSource *completionSource = [BFTaskCompletionSource taskCompletionSource];
    
    [self onStateChange:workId state:StateSplitting response:@"start" progress:@0];
    
    @try {
        if([[NSFileManager defaultManager] fileExistsAtPath:transcodedFilePath]) {
            NSString* tempDirectory = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) lastObject];
            
            NSMutableArray *chunkArray= [[NSMutableArray alloc]init];
            NSData *data = [[NSFileManager defaultManager] contentsAtPath:transcodedFilePath];
            NSUInteger length = [data length];
            NSUInteger chunkSizeInt = length > 104857600 ? 10485760 : [chunkSize integerValue];
            NSUInteger offset = 0;
            do {
                NSUInteger thisChunkSize = length - offset > chunkSizeInt ? chunkSizeInt : length - offset;
                NSData *chunk = [NSData dataWithBytesNoCopy:(char *)[data bytes] + offset length:thisChunkSize freeWhenDone:NO];
                NSString *chunkPath = [tempDirectory stringByAppendingPathComponent: [NSString stringWithFormat:@"%@", [[NSProcessInfo processInfo] globallyUniqueString]]];
                [chunk writeToFile:chunkPath atomically:YES];
                [chunkArray addObject:[chunkPath stringByReplacingOccurrencesOfString:@"file://" withString:@""]];
                offset += thisChunkSize;
            } while (offset < length);
            
            // clear cache
            if ([transcodedFilePath isEqualToString:originalFilePath]) {
                [self cleanUpCache:transcodedFilePath];
            }
            
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
-(BFTask *) requestMetadata:(NSNumber * _Nonnull)workId metadataUrl:(NSString * _Nonnull)metadataUrl numberOfChunks:(NSNumber * _Nonnull)numberOfChunks sessionManager:(AFURLSessionManager *)manager {
    BFTaskCompletionSource *completionSource = [BFTaskCompletionSource taskCompletionSource];
    
    [self onStateChange:workId state:StateRequestMetadata response:@"start" progress:@0];
    
    @try {
        NSDictionary* requestMetadataPostDictionary = @{
            @"cto": [numberOfChunks stringValue],
            @"ext": @"mp4"
        };
        NSMutableURLRequest *request = [[AFHTTPRequestSerializer serializer] requestWithMethod:@"POST" URLString:metadataUrl parameters:(NSDictionary *)requestMetadataPostDictionary error:nil];

        NSURLSessionDataTask *dataTask = [manager dataTaskWithRequest:request uploadProgress:nil downloadProgress:nil completionHandler:^(NSURLResponse * _Nonnull response, id _Nullable responseObject, NSError * _Nullable error) {
            if (error) {
                [completionSource setError:error];
            } else if ([responseObject isKindOfClass:[NSDictionary class]]) {
                //    NSLog(@"response: %@", responseObject[@"status"]);
                //    NSLog(@"response: %@", responseObject[@"message"]);
                NSLog(@"response: %@", responseObject[@"data"]);
                NSNumber *status  = [responseObject objectForKey:@"status"];
                if ([status intValue] == 1) {
                    [self onStateChange:workId state:StateRequestMetadata response:[self convertJsonStringFromNSDictionary:responseObject] progress:@100];
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
-(BFTask *) uploadVideoChunk:(NSNumber * _Nonnull)workId uploadUrl:(NSString * _Nonnull)uploadUrl filePath:(NSString * _Nonnull)filePath fileName:(NSString * _Nonnull)fileName hash:(NSString * _Nonnull)hash prt:(NSString * _Nonnull)prt numberOfChunks:(NSNumber * _Nonnull)numberOfChunks sessionManager:(AFURLSessionManager *)manager {
    BFTaskCompletionSource *completionSource = [BFTaskCompletionSource taskCompletionSource];
    @try {
//        int initialProgress = ([prt intValue] - 1) * 100 / [numberOfChunks intValue];
//        NSString *eventEmitterResponse = [NSString stringWithFormat:@"progress uploading %@/%@", prt, [numberOfChunks stringValue]];
        NSMutableURLRequest *request = [[AFHTTPRequestSerializer serializer] multipartFormRequestWithMethod:@"POST" URLString:uploadUrl parameters:nil
            constructingBodyWithBlock:^(id<AFMultipartFormData> formData) {
//                [formData appendPartWithFileData:fileData name:@"data" fileName:fileName mimeType:@"video/*"];
                [formData appendPartWithFileURL:[NSURL fileURLWithPath:filePath] name:@"data" fileName:fileName mimeType:@"video/*" error:nil];
                [formData appendPartWithFormData:[fileName dataUsingEncoding:NSUTF8StringEncoding] name:@"filename"];
                [formData appendPartWithFormData:[hash dataUsingEncoding:NSUTF8StringEncoding] name:@"hash"];
                [formData appendPartWithFormData:[prt dataUsingEncoding:NSUTF8StringEncoding] name:@"prt"];
            }
            error:nil
        ];
        NSURLSessionUploadTask *uploadTask = [manager uploadTaskWithStreamedRequest:request
            progress:^(NSProgress * _Nonnull uploadProgress) {
//                int progress = initialProgress + (int)(uploadProgress.fractionCompleted * 100 / [numberOfChunks intValue]);
//                [self onStateChange:workId state:StateUploading response:eventEmitterResponse progress:[NSNumber numberWithInt:progress]];
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

-(NSString *) convertJsonStringFromNSDictionary: (NSDictionary *)dictionary {
    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dictionary options:NSJSONWritingPrettyPrinted error:&error];
    if (!jsonData) {
        NSLog(@"Got an error: %@", error);
        return @"";
    }
    return [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
}

-(void) cleanUpCache: (NSString *)filePath {
    NSError *error;
    [[NSFileManager defaultManager] removeItemAtPath:filePath error:&error];
    NSLog(@"Check clear cache: %d", [[NSFileManager defaultManager] fileExistsAtPath:filePath]);
}

@end
