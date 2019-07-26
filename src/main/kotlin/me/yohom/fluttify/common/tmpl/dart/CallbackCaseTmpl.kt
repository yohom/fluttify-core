package me.yohom.fluttify.common.tmpl.dart

import me.yohom.fluttify.common.extensions.jsonable
import me.yohom.fluttify.common.extensions.toDartType
import me.yohom.fluttify.common.model.Method

//case '#__callback_case__#':
//if (#__callback_handler__# != null) {
//    #__callback_handler__#(#__callback_args__#);
//}
//break;
/**
 * 回调case
 */
class CallbackCaseTmpl(
    private val callerMethod: Method,
    private val callbackMethod: Method
) {
    private val tmpl = this::class.java.getResource("/tmpl/dart/callback_case.stmt.dart.tmpl").readText()

    fun callbackCase(): String {
        val callbackCase = "${callerMethod.className}::${callerMethod.name}_Callback::${callbackMethod.name}"
        val callbackHandler = callbackMethod.name
        val callbackArgs = callbackMethod.formalParams.joinToString {
            if (it.typeName.jsonable()) {
                "args['${it.name}']"
            } else {
                "${it.typeName.toDartType()}.withRefId(args['${it.name}'])"
            }
        }

        return tmpl
            .replace("#__callback_case__#", callbackCase)
            .replace("#__callback_handler__#", callbackHandler)
            .replace("#__callback_args__#", callbackArgs)
    }
}