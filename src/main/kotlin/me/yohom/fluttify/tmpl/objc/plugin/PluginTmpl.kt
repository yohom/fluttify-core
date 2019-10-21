package me.yohom.fluttify.tmpl.objc.plugin

import me.yohom.fluttify.FluttifyExtension
import me.yohom.fluttify.extensions.*
import me.yohom.fluttify.model.Lib
import me.yohom.fluttify.tmpl.objc.common.callback.callback_method.CallbackMethodTmpl
import me.yohom.fluttify.tmpl.objc.common.handler.handler_getter.HandlerGetterTmpl
import me.yohom.fluttify.tmpl.objc.common.handler.handler_method.HandlerMethodTmpl
import me.yohom.fluttify.tmpl.objc.common.handler.handler_object_factory.handler_object_factory_ref.HandlerObjectFactoryRefTmpl
import me.yohom.fluttify.tmpl.objc.common.handler.handler_object_factory.handler_object_factory_struct.HandlerObjectFactoryStructTmpl
import me.yohom.fluttify.tmpl.objc.common.handler.handler_setter.HandlerSetterTmpl
import me.yohom.fluttify.tmpl.objc.common.handler.handler_type_cast.HandlerTypeCastTmpl
import me.yohom.fluttify.tmpl.objc.common.handler.handler_type_check.HandlerTypeCheckTmpl
import me.yohom.fluttify.tmpl.objc.plugin.register_platform_view.RegisterPlatformViewTmpl

////////////////////////////////////////////////////////////
//// GENERATED BY FLUTTIFY. DO NOT EDIT IT.
////////////////////////////////////////////////////////////
//
//#import "#__plugin_name__#Plugin.h"
//
//typedef void (^Handler)(NSObject <FlutterPluginRegistrar> *, NSDictionary<NSString *, NSObject *> *, FlutterResult);
//
//// Dart端一次方法调用所存在的栈, 只有当MethodChannel传递参数受限时, 再启用这个容器
//NSMutableDictionary<NSNumber *, NSObject *> *STACK_#__plugin_name__#;
//// Dart端随机存取对象的容器
//NSMutableDictionary<NSNumber *, NSObject *> *HEAP_#__plugin_name__#;
//
//@implementation #__plugin_name__#Plugin {
//  NSObject <FlutterPluginRegistrar> * _registrar;
//  NSDictionary<NSString *, Handler> *_handlerMap;
//}
//
//- (instancetype) initWithFlutterPluginRegistrar: (NSObject <FlutterPluginRegistrar> *) registrar {
//  self = [super init];
//  if (self) {
//    _registrar = registrar;
//    // 处理方法们
//    _handlerMap = @{
//      #__handlers__#
//    };
//  }
//
//  return self;
//}
//
//+ (void)registerWithRegistrar:(NSObject <FlutterPluginRegistrar> *)registrar {
//  // 栈容器
//  STACK_#__plugin_name__# = @{}.mutableCopy;
//  // 堆容器
//  HEAP_#__plugin_name__# = @{}.mutableCopy;
//
//  FlutterMethodChannel *channel = [FlutterMethodChannel
//      methodChannelWithName:@"#__method_channel__#"
//            binaryMessenger:[registrar messenger]];
//
//  [registrar addMethodCallDelegate:[[#__plugin_name__#Plugin alloc] initWithFlutterPluginRegistrar:registrar]
//                           channel:channel];
//
//  // 注册View
//  #__register_platform_views__#
//}
//
//// Method Handlers
//- (void)handleMethodCall:(FlutterMethodCall *)methodCall result:(FlutterResult)methodResult {
//  NSDictionary<NSString *, id> *args = (NSDictionary<NSString *, id> *) [methodCall arguments];
//  // 是否一个对象
//  if ([@"ObjectFactory::release" isEqualToString:methodCall.method]) {
//    NSLog(@"ObjectFactory::释放对象: %@", (NSNumber *) args[@"refId"]);
//
//    [HEAP_#__plugin_name__# removeObjectForKey:(NSNumber *) args[@"refId"]];
//    methodResult(@"success");
//
//    NSLog(@"HEAP_#__plugin_name__#: %@", HEAP_#__plugin_name__#);
//  }
//  // 清空堆
//  else if ([@"ObjectFactory::clearHeap" isEqualToString:methodCall.method]) {
//    NSLog(@"ObjectFactory::清空堆");
//
//    [HEAP_#__plugin_name__# removeAllObjects];
//    methodResult(@"success");
//
//    NSLog(@"HEAP_#__plugin_name__#: %@", HEAP_#__plugin_name__#);
//  }
//  // 创建CLLocationCoordinate2D
//  else if ([@"ObjectFactory::createCLLocationCoordinate2D" isEqualToString:methodCall.method]) {
//    CLLocationDegrees latitude = [args[@"latitude"] doubleValue];
//    CLLocationDegrees longitude = [args[@"longitude"] doubleValue];
//
//    CLLocationCoordinate2D data = CLLocationCoordinate2DMake(latitude, longitude);
//
//    NSValue* dataValue = [NSValue value:&data withObjCType:@encode(CLLocationCoordinate2D)];
//    HEAP_#__plugin_name__#[@(dataValue.hash)] = dataValue;
//
//    methodResult(@(dataValue.hash));
//  }
//  // 创建UIImage
//  else if ([@"ObjectFactory::createUIImage" isEqualToString:methodCall.method]) {
//    FlutterStandardTypedData* bitmapBytes = (FlutterStandardTypedData*) args[@"bitmapBytes"];
//
//    UIImage* bitmap = [UIImage imageWithData:bitmapBytes.data];
//
//    HEAP_#__plugin_name__#[@(bitmap.hash)] = bitmap;
//
//    methodResult(@(bitmap.hash));
//  } else {
//    if (_handlerMap[methodCall.method] != nil) {
//      _handlerMap[methodCall.method](_registrar, args, methodResult);
//    } else {
//      methodResult(FlutterMethodNotImplemented);
//    }
//  }
//}
//
//// 委托方法们
//#__delegate_methods__#
//
//@end
class PluginTmpl(
    private val libs: List<Lib>,
    private val ext: FluttifyExtension
) {
    private val hTmpl = this::class.java.getResource("/tmpl/objc/plugin.h.tmpl").readText()
    private val mTmpl = this::class.java.getResource("/tmpl/objc/plugin.m.tmpl").readText()

    fun objcPlugin(): List<String> {
        // 插件名称
        val pluginClassName = ext.outputProjectName.underscore2Camel(true)

        // method channel
        val methodChannel = "${ext.outputOrg}/${ext.outputProjectName}"

        val platformViewHeader = mutableListOf<String>()
        // 注册PlatformView
        val registerPlatformViews = libs
            .flatMap { it.types }
            .filter { it.isView() }
            .onEach { platformViewHeader.add("#import \"${it.name}Factory.h\"") }
            .joinToString("\n") {
                RegisterPlatformViewTmpl(it, ext).objcRegisterPlatformView()
            }

        val getterHandlers = libs
            .flatMap { it.types }
            .filterType()
            .flatMap { it.fields }
            .filterGetters()
            .map { HandlerGetterTmpl(it).objcGetter() }

        val setterHandlers = libs
            .flatMap { it.types }
            .filterType()
            .flatMap { it.fields }
            .filterSetters()
            .map { HandlerSetterTmpl(it).objcSetter() }

        val methodHandlers = libs
            .flatMap { it.types }
            .filterType()
            .flatMap { it.methods }
            .filterMethod()
            .map { HandlerMethodTmpl(it).objcHandlerMethod() }

        val typeCastHandlers = libs
            .flatMap { it.types }
            .filterType()
            .filterNot { it.isLambda() }
            .distinctBy { it.name }
            .filter { !it.isInterface() && !it.isEnum() && !it.isStruct() }
            .map { HandlerTypeCastTmpl(it).objcTypeCast() }

        val typeCheckHandlers = libs
            .flatMap { it.types }
            .filterType()
            .filterNot { it.isLambda() }
            .distinctBy { it.name }
            .filter { !it.isInterface() && !it.isEnum() && !it.isStruct() }
            .map { HandlerTypeCheckTmpl(it).objcTypeCheck() }

        val createObjectHandlers = libs
            .flatMap { it.types }
            .filterConstructable()
            .distinctBy { it.name }
            .map {
                if (it.isStruct()) {
                    HandlerObjectFactoryStructTmpl(it).objcObjectFactoryStruct()
                } else {
                    HandlerObjectFactoryRefTmpl(it).objcObjectFactoryRef()
                }
            }

        val callbackMethods = libs
            .flatMap { it.types }
            .filterType()
            .filter { it.isCallback() }
            .flatMap { it.methods }
            .distinctBy { "${it.name}${it.formalParams}" }
            .map { CallbackMethodTmpl(it).objcDelegateMethod() }

        return listOf(
            hTmpl
                .replace("#__imports__#", libs
                    .map { "#import <${it.name}/${it.name}.h>" }
                    .union(platformViewHeader)
                    .joinToString("\n"))
                .replace("#__plugin_name__#", pluginClassName)
                .replace("#__protocols__#", libs
                    .flatMap { it.types }
                    .filter { it.isCallback() }
                    .map { it.name }
                    .union(listOf("FlutterPlugin")) // 补上FlutterPlugin协议
                    .joinToString(", ")),
            mTmpl
                .replace("#__plugin_name__#", pluginClassName)
                .replace("#__method_channel__#", methodChannel)
                .replaceParagraph("#__getter_branches__#", "")
                .replaceParagraph("#__setter_branches__#", "")
                .replaceParagraph("#__register_platform_views__#", registerPlatformViews)
                .replaceParagraph(
                    "#__handlers__#",
                    methodHandlers.union(getterHandlers)
                        .union(setterHandlers)
                        .union(typeCheckHandlers)
                        .union(typeCastHandlers)
                        .union(createObjectHandlers)
                        .joinToString("\n")
                )
                .replaceParagraph(
                    "#__delegate_methods__#",
                    callbackMethods.joinToString("\n")
                )
                .replace("#__plugin_name__#", ext.outputProjectName.underscore2Camel(true))
        )
    }
}