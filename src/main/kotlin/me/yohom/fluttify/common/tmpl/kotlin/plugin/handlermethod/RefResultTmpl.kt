package me.yohom.fluttify.common.tmpl.kotlin.plugin.handlermethod

import me.yohom.fluttify.common.TYPE_NAME

//val returnRefId = result.hashCode()
//REF_MAP[returnRefId] = result
//
//methodResult.success(returnRefId)
class RefResultTmpl(val returnType: TYPE_NAME) {

    private val tmpl = this::class.java.getResource("/tmpl/kotlin/ref_result.stmt.kt.tmpl").readText()

    fun kotlinRefResult(): String {
        return if (returnType == "void") "methodResult.success(\"success\")" else tmpl
    }

}