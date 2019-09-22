package me.yohom.fluttify.tmpl.dart.type.type_sdk.getter

import me.yohom.fluttify.extensions.depointer
import me.yohom.fluttify.extensions.toDartType
import me.yohom.fluttify.model.Field

//Future<#__type__#> get_#__name__#() async {
//  final result = await _channel.invokeMethod("#__getter_method__#", {'refId': refId});
//  return result;
//}
/**
 * 生成普通类的dart接口
 */
class GetterTmpl(private val field: Field) {
    private val tmpl = this::class.java.getResource("/tmpl/dart/getter.mtd.dart.tmpl").readText()

    fun dartGetter(): String {
        return field.variable.run {
            tmpl
                .replace("#__type__#", typeName.toDartType())
                .replace("#__name__#", name.depointer())
                .replace("#__getter_method__#", field.getterMethodName())
        }
    }
}