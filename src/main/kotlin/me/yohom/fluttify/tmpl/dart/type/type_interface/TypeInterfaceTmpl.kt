package me.yohom.fluttify.tmpl.dart.type.type_interface

import me.yohom.fluttify.ext
import me.yohom.fluttify.extensions.*
import me.yohom.fluttify.model.Platform
import me.yohom.fluttify.model.Type
import me.yohom.fluttify.tmpl.dart.type.common.getter.GetterTmpl
import me.yohom.fluttify.tmpl.dart.type.common.setter.SetterTmpl
import me.yohom.fluttify.tmpl.dart.type.type_interface.interface_method.InterfaceMethodTmpl
import me.yohom.fluttify.tmpl.dart.type.type_sdk.method.MethodTmpl

//import 'dart:typed_data';
//
//import 'package:#__current_package__#/src/ios/ios.export.g.dart';
//import 'package:#__current_package__#/src/android/android.export.g.dart';
//import 'package:flutter/foundation.dart';
//import 'package:flutter/services.dart';
//
//#__foundation__#
//
//#__sub_class__#
//
//mixin #__interface_type__# on #__super_mixins__# {
//  #__constants__#
//
//  #__sub_instance__#
//
//  #__getters__#
//
//  #__setters__#
//
//  #__interface_methods__#
//}
private val tmpl = getResource("/tmpl/dart/type_interface.dart.tmpl").readText()

fun TypeInterfaceTmpl(type: Type): String {
    val currentPackage = ext.projectName
    val typeName = if (type.declaredGenericTypes.isNotEmpty()) {
        "${type.name.toDartType()}<${type.declaredGenericTypes.joinToString()}>"
    } else {
        type.name.toDartType()
    }

    val constants = type.fields.filterConstants()

    val allSuperType = type.ancestorTypes
        .reversed()
        .filter { it.isNotBlank() }
        .filter { it.findType().platform != Platform.Unknown }
        .filter { !it.isObfuscated() }
        // 去掉NSObject的继承, 下面会添加回去的, 由于objc的class和protocol不在一个命名空间内, 所以存在class NSObject和protocol NSObject
        // 在dart端不太好处理
        .filter { it != "NSObject" }
    val subSuperMixins = if (allSuperType.isEmpty()) "" else "${allSuperType.joinToString().toDartType()}, "
    val superMixins = if (allSuperType.isEmpty()) {
        type.platform.objectType()
    } else {
        allSuperType.joinToString().toDartType()
    }

    val subclass = if (!type.isCallback)
        "class _${typeName}_SUB extends ${type.platform.objectType()} with $subSuperMixins$typeName {}"
    else
        ""
    // 给接口类型提供一个可供实例化的子类, mixin需要继承各平台Object类型, 并实现接口类
    val subInstance = if (!type.isCallback) "static $typeName subInstance() => _${typeName}_SUB();" else ""

    val methods = type.methods
        .filterMethod()
        .map {
            // 只有回调类不需要调用原生方法
            if (type.isCallback) {
                InterfaceMethodTmpl(it)
            } else {
                MethodTmpl(it)
            }
        }

    val getters = type.fields
        .filterGetters()
        .map { GetterTmpl(it) }

    val setters = type.fields
        .filterSetters()
        .map { SetterTmpl(it) }

    return tmpl
        .replace("#__current_package__#", currentPackage)
        .replaceParagraph(
            "#__foundation__#",
            ext.foundationVersion.keys.joinToString("\n") { "import 'package:$it/$it.dart';" })
        .replace("#__sub_class__#", subclass)
        .replace("#__sub_instance__#", subInstance)
        .replace("#__interface_type__#", typeName)
        .replace("#__sub_super_mixins__#", subSuperMixins)
        .replace("#__super_mixins__#", superMixins)
        .replaceParagraph(
            "#__constants__#",
            constants.joinToString("\n") { "static final ${it.variable.trueType.toDartType()} ${it.variable.name} = ${it.value};" })
        .replaceParagraph("#__interface_methods__#", methods.joinToString("\n"))
        .replaceParagraph("#__getters__#", getters.joinToString("\n"))
        .replaceParagraph("#__setters__#", setters.joinToString("\n"))
}