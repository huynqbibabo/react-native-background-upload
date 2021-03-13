#import "BackgroundUpload.h"
#import <AFNetworking.h>

@implementation BackgroundUpload

RCT_EXPORT_MODULE()

// Example method
// See // https://reactnative.dev/docs/native-modules-ios
RCT_EXPORT_METHOD(startBackgroundUploadVideo:(NSString * _Nonnull)uploadUrl
                  metadataUrl:(NSString * _Nonnull)metadataUrl
                  filePath:(NSString * _Nonnull)filePath
                  chunkSize:(NSNumber * _Nonnull)chunkSize
                  enableCompression:(BOOL * _Nonnull)enableCompression
                  chainTask:(NSDictionary * _Nullable)chainTask
){
    NSLog(@"filePath: %@", filePath);
    NSOperationQueue* aQueue = [[NSOperationQueue alloc] init];
//    [self requestMetadata:metadataUrl numberOfChunks:[NSNumber numberWithInt: 1]];
    [aQueue addOperationWithBlock:^{
        [self uploadVideoChunk:uploadUrl filePath:filePath fileName:@"0rekmhgeo38.mp4" hash:@"Qv9jEEE6MoCZq0ycDR9W" prt:@"1"];
    }];
}

-(void) uploadVideoChunk:(NSString * _Nonnull)uploadUrl filePath:(NSString * _Nonnull)filePath fileName:(NSString * _Nonnull)fileName
                    hash:(NSString * _Nonnull)hash prt:(NSString * _Nonnull)prt {
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
                NSLog(@"Error: %@", error);
            } else {
//              NSLog(@"response: %@", response);
                if ([responseObject isKindOfClass:[NSDictionary class]]) {
                    NSLog(@"status: %@", responseObject[@"status"]);
                    NSLog(@"message: %@", responseObject[@"message"]);
                    NSLog(@"data: %@", responseObject[@"data"]);
                }
            }
        }
    ];
    [uploadTask resume];
}

-(void) requestMetadata:(NSString * _Nonnull)metadataUrl numberOfChunks:(NSNumber * _Nonnull)numberOfChunks {
    NSDictionary* requestMetadataPostDictionary = @{
        @"cto": [numberOfChunks stringValue],
        @"ext": @"mp4"
    };
    NSMutableURLRequest *request = [[AFHTTPRequestSerializer serializer] requestWithMethod:@"POST" URLString:metadataUrl
                                    parameters:(NSDictionary *)requestMetadataPostDictionary error:nil];

    AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];

    NSURLSessionDataTask *dataTask = [manager dataTaskWithRequest:request uploadProgress:nil downloadProgress:nil completionHandler:^(NSURLResponse * _Nonnull response, id _Nullable responseObject, NSError * _Nullable error) {
        if (error) {
            NSLog(@"Error: %@", error);
        } else if ([responseObject isKindOfClass:[NSDictionary class]]) {
            NSLog(@"response: %@", responseObject[@"status"]);
            NSLog(@"response: %@", responseObject[@"message"]);
            NSLog(@"response: %@", responseObject[@"data"]);
        } else {
            NSLog(@"response: %@", response);
        }
    }];
    [dataTask resume];
}

@end
