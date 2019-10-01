#import "TwitterAuthPlugin.h"
#import <twitter_auth/twitter_auth-Swift.h>

@implementation TwitterAuthPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftTwitterAuthPlugin registerWithRegistrar:registrar];
}
@end
