#import "Orientation.h"
#if __has_include(<React/RCTEventDispatcher.h>)
#import <React/RCTEventDispatcher.h>
#elif __has_include("RCTEventDispatcher.h")
#import "RCTEventDispatcher.h"
#elif __has_include("React/RCTEventDispatcher.h")
#import "React/RCTEventDispatcher.h"   // Required when used as a Pod in a Swift project
#endif


@implementation Orientation
@synthesize bridge = _bridge;

static UIInterfaceOrientationMask _orientation = UIInterfaceOrientationMaskAllButUpsideDown;
+ (void) initialize {
    if ( UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad ) {
        _orientation = UIInterfaceOrientationMaskAll;
    } else {
        _orientation = UIInterfaceOrientationMaskAllButUpsideDown;
    }
}
+ (void)setOrientation: (UIInterfaceOrientationMask)orientation {
    _orientation = orientation;
}
+ (UIInterfaceOrientationMask)getOrientation {
    return _orientation;
}

- (instancetype)init
{
    if ((self = [super init])) {
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(deviceOrientationDidChange:) name:UIApplicationDidChangeStatusBarOrientationNotification object:nil];
    }
    return self;
    
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)deviceOrientationDidChange:(NSNotification *)notification
{
    UIInterfaceOrientation orientation = [[UIApplication sharedApplication] statusBarOrientation];
    [self.bridge.eventDispatcher sendDeviceEventWithName:@"specificOrientationDidChange"
                                                    body:@{@"specificOrientation": [self getSpecificOrientationStr:orientation]}];
    
    [self.bridge.eventDispatcher sendDeviceEventWithName:@"orientationDidChange"
                                                    body:@{@"orientation": [self getOrientationStr:orientation]}];
    
}

- (NSString *)getOrientationStr: (UIInterfaceOrientation)orientation {
    NSString *orientationStr;
    switch (orientation) {
        case UIInterfaceOrientationPortrait:
            orientationStr = @"PORTRAIT";
            break;
        case UIInterfaceOrientationLandscapeLeft:
        case UIInterfaceOrientationLandscapeRight:
            
            orientationStr = @"LANDSCAPE";
            break;
            
        case UIInterfaceOrientationPortraitUpsideDown:
            orientationStr = @"PORTRAITUPSIDEDOWN";
            break;
            
        default:
            orientationStr = @"UNKNOWN";
            break;
    }
    return orientationStr;
}

- (NSString *)getSpecificOrientationStr: (UIInterfaceOrientation)orientation {
    NSString *orientationStr;
    switch (orientation) {
        case UIInterfaceOrientationPortrait:
            orientationStr = @"PORTRAIT";
            break;
            
        case UIInterfaceOrientationLandscapeLeft:
            orientationStr = @"LANDSCAPE-LEFT";
            break;
            
        case UIInterfaceOrientationLandscapeRight:
            orientationStr = @"LANDSCAPE-RIGHT";
            break;
            
        case UIInterfaceOrientationPortraitUpsideDown:
            orientationStr = @"PORTRAITUPSIDEDOWN";
            break;
            
        default:
            orientationStr = @"UNKNOWN";
            break;
    }
    return orientationStr;
}

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(getOrientation:(RCTResponseSenderBlock)callback)
{
    UIInterfaceOrientation orientation = [[UIApplication sharedApplication] statusBarOrientation];
    NSString *orientationStr = [self getOrientationStr:orientation];
    callback(@[[NSNull null], orientationStr]);
}

RCT_EXPORT_METHOD(getSpecificOrientation:(RCTResponseSenderBlock)callback)
{
    UIInterfaceOrientation orientation = [[UIApplication sharedApplication] statusBarOrientation];
    NSString *orientationStr = [self getSpecificOrientationStr:orientation];
    callback(@[[NSNull null], orientationStr]);
}

RCT_EXPORT_METHOD(lockToPortrait)
{
#if DEBUG
    NSLog(@"Locked to Portrait");
#endif
    
    if ( UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad ) {
        [Orientation setOrientation:(UIInterfaceOrientationMaskPortrait | UIInterfaceOrientationMaskPortraitUpsideDown)];
    } else {
        [Orientation setOrientation:UIInterfaceOrientationMaskPortrait];
    }
    UIInterfaceOrientation orientation = [[UIApplication sharedApplication] statusBarOrientation];
    if (!UIInterfaceOrientationIsPortrait(orientation)) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [[UIDevice currentDevice] setValue:[NSNumber numberWithInteger: UIInterfaceOrientationPortrait] forKey:@"orientation"];
            [UIViewController attemptRotationToDeviceOrientation];
        });
    }

}

RCT_EXPORT_METHOD(lockToLandscape)
{
#if DEBUG
    NSLog(@"Locked to Landscape");
#endif
    UIDeviceOrientation orientation = [[UIDevice currentDevice] orientation];
    NSString *orientationStr = [self getSpecificOrientationStr:orientation];
    if ([orientationStr isEqualToString:@"LANDSCAPE-LEFT"]) {
        [Orientation setOrientation:UIInterfaceOrientationMaskLandscape];
        dispatch_async(dispatch_get_main_queue(), ^{
            [[UIDevice currentDevice] setValue:[NSNumber numberWithInteger: UIInterfaceOrientationLandscapeRight] forKey:@"orientation"];
            [UIViewController attemptRotationToDeviceOrientation];
        });
    } else {
        [Orientation setOrientation:UIInterfaceOrientationMaskLandscape];
        dispatch_async(dispatch_get_main_queue(), ^{
            [[UIDevice currentDevice] setValue:[NSNumber numberWithInteger: UIInterfaceOrientationLandscapeLeft] forKey:@"orientation"];
            [UIViewController attemptRotationToDeviceOrientation];
        });
    }
}

RCT_EXPORT_METHOD(lockToLandscapeRight)
{
#if DEBUG
    NSLog(@"Locked to Landscape Right");
#endif
    [Orientation setOrientation:UIInterfaceOrientationMaskLandscapeRight];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        [[UIDevice currentDevice] setValue:[NSNumber numberWithInteger: UIInterfaceOrientationLandscapeRight] forKey:@"orientation"];
        [UIViewController attemptRotationToDeviceOrientation];
    });
    
}

RCT_EXPORT_METHOD(lockToLandscapeLeft)
{
#if DEBUG
    NSLog(@"Locked to Landscape Left");
#endif
    [Orientation setOrientation:UIInterfaceOrientationMaskLandscapeLeft];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        [[UIDevice currentDevice] setValue:[NSNumber numberWithInteger: UIInterfaceOrientationLandscapeLeft] forKey:@"orientation"];
        [UIViewController attemptRotationToDeviceOrientation];
        
    });
}

RCT_EXPORT_METHOD(unlockAllOrientations)
{
#if DEBUG
    NSLog(@"Unlock All Orientations");
#endif
    
    if ( UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad ) {
        [Orientation setOrientation:UIInterfaceOrientationMaskAll];
    } else {
        [Orientation setOrientation:UIInterfaceOrientationMaskAllButUpsideDown];
    }
}

- (NSDictionary *)constantsToExport
{
    
    UIInterfaceOrientation orientation = [[UIApplication sharedApplication] statusBarOrientation];
    NSString *orientationStr = [self getOrientationStr:orientation];
    
    return @{
             @"initialOrientation": orientationStr
             };
}

@end
