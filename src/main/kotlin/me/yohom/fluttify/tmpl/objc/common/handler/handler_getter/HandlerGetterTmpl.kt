package me.yohom.fluttify.tmpl.objc.common.handler.handler_getter

import me.yohom.fluttify.extensions.enpointer
import me.yohom.fluttify.extensions.findType
import me.yohom.fluttify.extensions.isObjcValueType
import me.yohom.fluttify.model.Field

//@"#__method_name__#": ^(NSObject <FlutterPluginRegistrar> * registrar, NSDictionary<NSString *, id> * args, FlutterResult methodResult) {
//    NSLog(@"#__method_name__#");
//
//    // 引用对象
//    NSInteger refId = [args[@"refId"] integerValue];
//    #__class_name__# ref = (#__class_name__#) HEAP[@(refId)];
//
//    #__note__#
//
//    methodResult(#__getter__#);
//},
internal class HandlerGetterTmpl(private val field: Field) {
    private val tmpl = this::class.java.getResource("/tmpl/objc/handler_getter.stmt.m.tmpl").readText()

    fun objcGetter(): String {
        val methodName = field.getterMethodName()
        val className = if (field.className.findType().isInterface()) {
            "id<${field.className}>"
        } else {
            field.className.enpointer()
        }
        val getter = field.run {
            when {
                variable.typeName.isObjcValueType() -> "@(ref.$getterName)"
                variable.typeName.findType().isStruct() -> {
                    "nil/* 结构体getter暂时不支持 */"
//                        "@(${StructToNSValueTmpl(field.variable).objcStructToNSValue()}.hash)"
                }
                else -> "@(ref.$getterName.hash)"
            }
        }
        val note = if (field.variable.typeName.findType().isStruct()) "NSLog(@\"${methodName}:结构体getter暂时不支持\");" else ""
        return tmpl
            .replace("#__method_name__#", methodName)
            .replace("#__class_name__#", className)
            .replace("#__note__#", note)
            .replace("#__getter__#", getter)
    }
}