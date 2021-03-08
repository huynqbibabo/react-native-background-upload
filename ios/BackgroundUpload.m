#import "BackgroundUpload.h"
#import <AFNetworking.h>

@implementation BackgroundUpload

RCT_EXPORT_MODULE()

// Example method
// See // https://reactnative.dev/docs/native-modules-ios
RCT_EXPORT_METHOD(startBackgroundUpload:
                  (NSString *)requestUrl
                  filePath:(NSString *)filePath
                  fileName:(NSString *)fileName
                  hash:(NSDictionary *)hash
                  chunkSize:(NSNumber * _Nonnull)chunkSize)
{
    NSLog(@"filePath: %@", filePath);
    NSMutableURLRequest *request = [
    [AFHTTPRequestSerializer serializer]
        multipartFormRequestWithMethod:@"POST"
        URLString:requestUrl parameters:nil
        constructingBodyWithBlock:^(id<AFMultipartFormData> formData) {
            [formData appendPartWithFileURL:[NSURL fileURLWithPath:filePath]
                                       name:@"data" fileName:fileName mimeType:@"video/*" error:nil
            ];
            [formData appendPartWithFormData:[fileName dataUsingEncoding:NSUTF8StringEncoding] name:@"filename"];
            [formData appendPartWithFormData:[@"za4nDFtOi5JmTw23vXZM" dataUsingEncoding:NSUTF8StringEncoding] name:@"hash"];
            [formData appendPartWithFormData:[@"1" dataUsingEncoding:NSUTF8StringEncoding] name:@"prt"];
        }
        error:nil
    ];
    

    AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration]];

    NSURLSessionUploadTask *uploadTask;
    uploadTask = [manager
                  uploadTaskWithStreamedRequest:request
                  progress:^(NSProgress * _Nonnull uploadProgress) {
//                        NSLog(@"Progress: @%.20f", uploadProgress.fractionCompleted);
                        NSLog(@"Progress: %i", (int)uploadProgress.fractionCompleted * 100);
                  }
                  completionHandler:^(NSURLResponse * _Nonnull response, id  _Nullable responseObject, NSError * _Nullable error) {
                      if (error) {
                          NSLog(@"Error: %@", error);
                      } else {
//                          NSLog(@"response: %@", response);
                          NSLog(@"responseObject: %@", responseObject);
                      }
                  }];

    [uploadTask resume];
}

@end
