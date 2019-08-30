package me.yohom.fluttify.common.tmpl.swift.plugin.handler

import me.yohom.fluttify.common.extensions.findType
import me.yohom.fluttify.common.extensions.jsonable
import me.yohom.fluttify.common.extensions.replaceParagraph
import me.yohom.fluttify.common.model.Method
import me.yohom.fluttify.common.tmpl.swift.plugin.handler.invoke.InvokeTmpl

//private func #__method_name__#(registrar: Registrar, args: Dictionary<String, Any>, methodResult: FlutterResult) {
//    // 参数
//    #__args__#
//
//    // 调用对象引用
//    #__ref__#
//
//    // 日志打印
//    #__log__#
//
//    // 开始调用
//    #__invoke__#
//
//    // 调用结果
//    #__result__#
//}
internal class HandleMethodTmpl(private val method: Method) {
    private val tmpl = this::class.java.getResource("/tmpl/swift/handler_method.mtd.swift.tmpl").readText()

    fun swiftHandlerMethod(): String {
        val methodName = method.handleMethodName()
        // 参数分为三种, 分情况分别构造以下三种模板
        // 1. 枚举
        // 2. jsonable
        // 3. 引用
        val args = method.formalParams
            .filter { !it.variable.typeName.findType().isCallback() }
            .joinToString("\n") {
                when {
                    it.variable.typeName.jsonable() -> ArgJsonableTmpl(it.variable).swiftArgJsonable()
                    it.variable.typeName.findType().isEnum() -> ArgEnumTmpl(it.variable).swiftArgEnum()
                    else -> ArgRefTmpl(it.variable).swiftArgRef()
                }
            }
        val log = if (method.isStatic) {
            "print(\"fluttify-swift: ${method.className}::${method.name}(${method.formalParams.filter { it.variable.typeName.jsonable() }.map { "\\\"${it.variable.name}\\\":\\(${it.variable.name})\\n" }})\")"
        } else {
            "print(\"fluttify-swift: ${method.className}@\\(refId)::${method.name}(${method.formalParams.filter { it.variable.typeName.jsonable() }.map { "\\\"${it.variable.name}\\\":\\(${it.variable.name})\\n" }})\")"
        }

        // 获取当前调用方法的对象引用
        val ref = RefTmpl(method).swiftRef()

        // 调用kotlin端对应的方法
        val invoke = InvokeTmpl(method).swiftInvoke()

        // 调用结果 分为void, (jsonable, ref)两种情况 void时返回"success", jsonable返回本身, ref返回refId
        val result = RefResultTmpl(method.returnType).swiftRefResult()
        return tmpl
            .replace("#__method_name__#", methodName)
            .replaceParagraph("#__args__#", args)
            .replaceParagraph("#__ref__#", ref)
            .replaceParagraph("#__log__#", log)
            .replaceParagraph("#__invoke__#", invoke)
            .replaceParagraph("#__result__#", result)
    }
}