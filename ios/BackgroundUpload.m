#import "BackgroundUpload.h"
#import <AFNetworking.h>

@implementation BackgroundUpload

RCT_EXPORT_MODULE()

// Example method
// See // https://reactnative.dev/docs/native-modules-ios
RCT_EXPORT_METHOD(startBackgroundUploadVideo:(NSString *)uploadUrl
                  metadataUrl:(NSString *)metadataUrl
                  filePath:(NSString *)filePath
                  chunkSize:(NSNumber * _Nonnull)chunkSize
                  enableCompression:(BOOL *)enableCompression
                  chainTask:(NSDictionary *)chainTask
){
    NSLog(@"filePath: %@", filePath);
//    NSDictionary* requestMetadataPostDictionary = @{
//        @"cto": @"1",
//        @"ext": @"mp4"
//    };
//    NSMutableURLRequest *request = [[AFHTTPRequestSerializer serializer] requestWithMethod:@"POST" URLString:metadataUrl
//                                    parameters:(NSDictionary *)requestMetadataPostDictionary error:nil];
//
//    AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];
//
//    NSURLSessionDataTask *dataTask = [manager dataTaskWithRequest:request uploadProgress:nil downloadProgress:nil completionHandler:^(NSURLResponse * _Nonnull response, id  _Nullable responseObject, NSError * _Nullable error) {
//        if (error) {
//            NSLog(@"Error: %@", error);
//        } else {
//            NSLog(@"responseObject: %@", responseObject);
//            if ([responseObject isKindOfClass:[NSDictionary class]]) {
//                NSLog(@"response: %@", responseObject[@"status"]);
//                NSLog(@"response: %@", responseObject[@"message"]);
//                NSLog(@"response: %@", responseObject[@"data"]);
//            }
//        }
//    }];
//    [dataTask resume];
    
    NSMutableURLRequest *request = [[AFHTTPRequestSerializer serializer] multipartFormRequestWithMethod:@"POST" URLString:uploadUrl parameters:nil
        constructingBodyWithBlock:^(id<AFMultipartFormData> formData) {
            [formData appendPartWithFileURL:[NSURL fileURLWithPath:filePath] name:@"data" fileName:@"01a61l83bub.mp4" mimeType:@"video/*" error:nil];
            [formData appendPartWithFormData:[@"01a61l83bub.mp4" dataUsingEncoding:NSUTF8StringEncoding] name:@"filename"];
            [formData appendPartWithFormData:[@"PV616J4JEOmF2UggukWw" dataUsingEncoding:NSUTF8StringEncoding] name:@"hash"];
            [formData appendPartWithFormData:[@"1" dataUsingEncoding:NSUTF8StringEncoding] name:@"prt"];
        }
        error:nil
    ];
    AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];
    NSURLSessionUploadTask *uploadTask = [manager uploadTaskWithStreamedRequest:request
        progress:^(NSProgress * _Nonnull uploadProgress) {
            NSLog(@"Progress: %i", (int)uploadProgress.fractionCompleted * 100);
        }
        completionHandler:^(NSURLResponse * _Nonnull response, id  _Nullable responseObject, NSError * _Nullable error) {
            if (error) {
                NSLog(@"Error: %@", error);
            } else {
//              NSLog(@"response: %@", response);
                NSLog(@"responseObject: %@", responseObject);
            }
        }
    ];
    [uploadTask resume];
}

@end
