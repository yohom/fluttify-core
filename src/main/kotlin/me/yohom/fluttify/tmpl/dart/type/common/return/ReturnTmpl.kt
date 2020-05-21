package me.yohom.fluttify.tmpl.dart.type.common.`return`

import me.yohom.fluttify.extensions.*
import me.yohom.fluttify.model.Method
import me.yohom.fluttify.tmpl.dart.type.type_sdk.common.result.ResultEnumTmpl
import me.yohom.fluttify.tmpl.dart.type.type_sdk.common.result.ResultJsonableTmpl
import me.yohom.fluttify.tmpl.dart.type.type_sdk.common.result.ResultListTmpl
import me.yohom.fluttify.tmpl.dart.type.type_sdk.common.result.ResultRefTmpl

fun ReturnTmpl(method: Method): String {
    return method.run {
        // 如果是多维列表, 那么不处理, 直接返回空列表
        if (returnType.isIterable() && returnType.iterableLevel() > 1) {
            return "[] /* 暂时不支持多维列表 */";
        }

        // 如果返回类型是抽象类, 那么先转换成它的子类
        val concretType: String = if (returnType.isIterable() && returnType.iterableLevel() != 0) {
            // 如果是(列表+抽象)类, 那么先把泛型类处理成实体类, 再加上`List`
            val genericType = returnType.genericTypes()[0]
            val concretGenericType = if (genericType.findType().isAbstract) {
                genericType.findType().run { firstConcretSubtype?.name ?: this.name }
            } else {
                genericType
            }
            returnType.replace(genericType, concretGenericType)
        } else {
            // 否则直接处理返回类型
            if (returnType.findType().isAbstract) {
                returnType.findType().run { firstConcretSubtype?.name ?: this.name }
            } else {
                returnType
            }
        }

        concretType.run {
            when {
                jsonable() || concretType.isVoid() -> {
                    ResultJsonableTmpl(concretType, method.platform)
                }
                // 返回枚举类型
                findType().isEnum -> ResultEnumTmpl(concretType)
                // 返回列表类型
                isIterable() -> {
                    val type = if (concretType.iterableLevel() != 0) {
                        concretType.genericTypes()[0]
                    } else {
                        method.platform.objectType()
                    }

                    ResultListTmpl(type, method.platform)
                }
                isStructPointer() -> ResultListTmpl(depointer(), method.platform)
                else -> ResultRefTmpl(concretType)
            }
        }
    }
}