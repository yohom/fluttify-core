package me.yohom.fluttify.tmpl.objc.common.handler.common.result

import me.yohom.fluttify.extensions.getResource

// TODO 先运行一段时间, 看看直接用id接收会不会有什么问题
//// 返回值: Value
//id jsonableResult = @(result);
private val tmpl = getResource("/tmpl/objc/result_value.stmt.m.tmpl").readText()

fun ResultValueTmpl(): String {
    return tmpl
}