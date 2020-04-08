package me.yohom.fluttify.tmpl.java.common.handler.common.arg

import me.yohom.fluttify.extensions.genericTypes
import me.yohom.fluttify.extensions.getResource
import me.yohom.fluttify.model.Variable

//// list arg
//List<Integer> #__arg_name__#RefIdList = (List<Integer>) ((Map<String, Object>) args).get("#__arg_name__#");
//#__type_name__# #__arg_name__# = new ArrayList<>();
//for (int refId : #__arg_name__#RefIdList) {
//    #__arg_name__#.add((#__generic_type_name__#) getHEAP().get(refId));
//}
private val tmpl = getResource("/tmpl/java/arg_list.stmt.java.tmpl").readText()

fun ArgListTmpl(variable: Variable): String {
    // 只处理非列表和一维列表, 多维列表一律返回一个空的列表
    return if (variable.getIterableLevel() <= 1) {
        tmpl
            .replace("#__type_name__#", variable.typeName.replace("$", "."))
            .replace("#__generic_type_name__#", variable.typeName.replace("$", ".").genericTypes()[0])
            .replace("#__arg_name__#", variable.name)
    } else {
        val typeName = variable.typeName.replace("$", ".")
        "$typeName ${variable.name} = new ArrayList<>();"
    }
}