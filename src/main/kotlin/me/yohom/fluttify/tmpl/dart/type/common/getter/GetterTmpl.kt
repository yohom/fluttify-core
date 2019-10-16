package me.yohom.fluttify.tmpl.dart.type.common.getter

import me.yohom.fluttify.FluttifyExtension
import me.yohom.fluttify.extensions.depointer
import me.yohom.fluttify.extensions.isVoid
import me.yohom.fluttify.extensions.toDartType
import me.yohom.fluttify.model.Field
import me.yohom.fluttify.tmpl.dart.type.type_sdk.common.result.*

//Future<#__type__#> get_#__name__#() async {
//  final result = await MethodChannel(#__method_channel__#).invokeMethod("#__getter_method__#", {'refId': refId});
//  return #__result__#;
//}
class GetterTmpl(
    private val field: Field,
    private val ext: FluttifyExtension
) {
    private val tmpl = this::class.java.getResource("/tmpl/dart/getter.mtd.dart.tmpl").readText()

    fun dartGetter(): String {
        val typeName = field.variable.run {
            var result = typeName.toDartType()
            for (i in 0 until genericLevel) {
                result = "List<$result>"
            }
            result
        }
        val name = field.variable.name.depointer()
        val methodChannel = ext.methodChannelName
        val getter = field.getterMethodName()
        val result = when {
            field.variable.jsonable() -> ResultJsonableTmpl().dartResultJsonable()
            field.variable.isList -> ResultListTmpl(field.variable.typeName).dartResultList()
            field.variable.isEnum() -> ResultEnumTmpl(field.variable.typeName).dartResultEnum()
            field.variable.typeName.isVoid() -> ResultVoidTmpl().dartResultVoid()
            else -> ResultRefTmpl(field.variable.typeName).dartResultRef()
        }

        return field.variable.run {
            tmpl
                .replace("#__type__#", typeName)
                .replace("#__name__#", name)
                .replace("#__method_channel__#", methodChannel)
                .replace("#__getter_method__#", getter)
                .replace("#__result__#", result)
        }
    }
}