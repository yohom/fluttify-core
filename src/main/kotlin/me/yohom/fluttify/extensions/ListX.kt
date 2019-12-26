package me.yohom.fluttify.extensions

import me.yohom.fluttify.EXCLUDE_METHODS
import me.yohom.fluttify.EXCLUDE_TYPES
import me.yohom.fluttify.model.*


/**
 * 键值对转成dart的map字面量字符串
 */
fun List<Variable>.toDartMap(
    prefix: String = "{",
    postfix: String = "}",
    valueBuilder: ((Variable) -> String) = { it.name }
): String {
    if (isEmpty()) return ""
    return joinToString(prefix = prefix, postfix = postfix) { "\"${it.name.depointer()}\": ${valueBuilder(it)}" }
}

/**
 * 过滤出可以自动生成的方法
 */
fun List<Method>.filterMethod(): List<Method> {
    return asSequence()
        .filter { it.must("公开方法") { isPublic } }
        .filter { it.mustNot("忽略方法") { EXCLUDE_METHODS.any { methods -> methods.matches(name) } } }
        .filter { it.mustNot("废弃方法") { isDeprecated } }
        .filter { it.mustNot("父类已有的方法") { name in className.superType().flatMap { it.methods }.map { it.name } } } // 重写的方法其实没必要再生成一次, 就算调用的是父类的方法, native端仍然是预期行为
        .filter { it.must("所在类是公开类") { className.findType().isPublic } }
        .filter { it.must("所在类是静态类型") { className.findType().isStaticType } }
        .filter { it.must("形参类型全部都是公开类型") { formalParams.all { it.variable.isPublicType() } } }
        .filter { it.must("形参类型全部都是已知类型") { formalParams.all { it.variable.isKnownType() } } }
        .filter { it.must("形参全部是静态类型") { formalParams.all { it.variable.typeName.findType().isStaticType } } }
        .filter { it.mustNot("形参类型含有泛型") { formalParams.any { it.variable.isGenericType() } } }
        .filter { it.mustNot("形参类型含有混淆类") { formalParams.any { it.variable.typeName.isObfuscated() } } }
        // 不处理c指针类型参数的方法
        .filter { it.mustNot("形参含有是C指针类型") { formalParams.any { param -> param.variable.typeName.isCPointerType() } } }
        // 参数不能中含有排除的类
        .filter { it.mustNot("形参含有排除的类") { formalParams.any { param -> EXCLUDE_TYPES.any { it.matches(param.variable.typeName.depointer()) } } } }
        .filter {
            it.mustNot("形参父类含有未知类型") {
                formalParams
                    .map { it.variable.typeName.findType().superType }
                    .any { it.findType() == Type.UNKNOWN_TYPE }
            }
        }
        .filter {
            it.mustNot("形参父类是混淆类") {
                formalParams.any { it.variable.typeName.findType().superType.isObfuscated() }
            }
        }
        // 类似float*返回这样的类型的方法都暂时不处理
        .filter { it.mustNot("返回类型是C类型指针") { returnType.isCPointerType() } }
        .filter {
            it.must("返回类型是具体类型或者含有实体子类的抽象类") {
                returnType.findType().run { isConcret() || hasConcretSubtype() }
            }
        }
        .filter { it.mustNot("返回类型是混淆类") { returnType.isObfuscated() } }
        .filter { it.mustNot("返回类型是未知类") { returnType.findType() == Type.UNKNOWN_TYPE } }
        .filter { it.mustNot("返回类型是嵌套数组/列表") { returnType.run { genericLevel() > 1 || (isList() && genericType().isArray()) } } }
        .filter { it.mustNot("返回类型含有泛型") { returnType.findType().genericTypes.isNotEmpty() } }
        .filter { it.must("返回类型的父类是已知类") { returnType.findType().superType().all { it != Type.UNKNOWN_TYPE } } }
        .distinctBy { it.nameWithClass() }
        .filter { println("Method::${it.name}通过Method过滤"); true }
        .toList()
}

/**
 * 从field中过滤出getter
 */
fun List<Field>.filterGetters(): List<Field> {
    return asSequence()
        .filter { it.must("公开field") { isPublic } }
        .filter { it.mustNot("静态field") { isStatic } }
        .filter { it.variable.must("已知类型") { isKnownType() } }
        .filter { it.variable.mustNot("lambda类型") { Regex("""\(\^\w+\)\(\)""").matches(name) } }
        .filter { it.variable.must("公开类型") { isPublicType() } }
        .filter { it.variable.must("具体类型或者含有子类的抽象类") { isConcret() || hasConcretSubtype() } }
        .filter { it.variable.must("返回类型的父类是已知类") { typeName.findType().superType().all { it != Type.UNKNOWN_TYPE } } }
        .filter { it.variable.mustNot("混淆类") { typeName.isObfuscated() } }
        .filter { println("Field::${it.variable.name}通过Getter过滤"); true }
        .toList()
}

/**
 * 从field中过滤出常量
 */
fun List<Field>.filterConstants(): List<Field> {
    return asSequence()
        .filter { it.must("公开field") { isPublic } }
        .filter { it.must("静态field") { isStatic == true } }
        .filter { it.must("不可变变量") { isFinal == true } }
        .filter { it.variable.must("数字或字符串类型") { typeName in listOf("int", "double", "String") } }
        .filter { it.must("有值") { value.isNotEmpty() } }
        .filter { it.mustNot("包含new关键字") { value.startsWith("new") } }
        .filter { println("Field::${it.variable.name}通过Constants过滤"); true }
        .toList()
}

/**
 * 从field中过滤出setter
 */
fun List<Field>.filterSetters(): List<Field> {
    return asSequence()
        .filter { it.must("公开field") { isPublic } }
        .filter { it.mustNot("不可改field") { isFinal } }
        .filter { it.mustNot("静态field") { isStatic } }
        .filter { it.variable.must("已知类型") { isKnownType() } }
        .filter { it.variable.mustNot("lambda类型") { Regex("""\(\^\w+\)\(\)""").matches(name) } }
        .filter { it.variable.must("公开类型") { typeName.findType().isPublic } }
        .filter { it.variable.must("返回类型的父类是已知类") { typeName.findType().superType().all { it != Type.UNKNOWN_TYPE } } }
        .filter { it.variable.mustNot("混淆类") { typeName.isObfuscated() } }
        .filter { println("Field::${it.variable.name}通过Setter过滤"); true }
        .toList()
}

/**
 * 过滤出可以自动生成的类
 */
fun List<Type>.filterType(): List<Type> {
    return asSequence()
        .filter { it.must("已知类型") { this != Type.UNKNOWN_TYPE } }
        .filter { it.must("公开类型") { isPublic } }
        .filter { it.must("父类是已知类型") { superType.findType() != Type.UNKNOWN_TYPE } }
        .filter { it.mustNot("含有泛型") { genericTypes.isNotEmpty() } }
        .filter { it.mustNot("混淆类型") { isObfuscated() } }
        .filter { it.mustNot("忽略类型") { EXCLUDE_TYPES.any { type -> type.matches(name) } } }
        .filter { it.mustNot("父类是忽略类型") { EXCLUDE_TYPES.any { type -> type.matches(superType) } } }
        .filter { it.mustNot("父类是混淆类型") { superType.isObfuscated() } }
        .filter {
            (it.isEnum() or !it.isInnerType or (it.constructors.any { it.isPublic } or it.constructors.isEmpty())).apply {
                if (!this) println("filterType: ${it.name} 由于构造器不是全公开且是内部类 被过滤")
            }
        }
        .filter { println("Type::${it.name}通过过滤"); true }
        .toList()
}

/**
 * 过滤出可以直接构造的类
 */
fun List<Type>.filterConstructable(): List<Type> {
    return filterType()
        .asSequence()
        .filter { it.constructable() }
        .toList()
}

/**
 * 过滤出可以构造的构造器
 */
fun List<Constructor>.filterConstructor(): List<Constructor> {
    return asSequence()
        // 构造器参数为空或者递归检查构造器参数的构造器是否符合相同条件
        .filter {
            ((it.formalParams.isEmpty() || it.formalParams.all { it.variable.typeName.findType().constructable() || it.variable.jsonable() }) && it.isPublic)
                .apply { if (!this) println("Constructor::${it.name} 由于构造器含有未知类 被过滤") }
        }
        .toList()
}

/**
 * 过滤出可以自动生成的参数
 */
fun List<Parameter>.filterFormalParams(): List<Parameter> {
    return asSequence()
        .filter { it.variable.mustNot("Lambda") { isLambda() } } // lambda不参与传递
        .filter { it.variable.mustNot("Callback") { isCallback() } } // 回调类不参与传递(但是接口类型参与传递)
        .filter { it.variable.mustNot("未知类型") { typeName.findType() == Type.UNKNOWN_TYPE } }
        .filter { println("Parameter::${it.variable.typeName}通过过滤"); true }
        .toList()
}

fun <T> List<T>.joinToStringX(
    separator: String = ", ",
    prefix: String = "",
    suffix: String = "",
    transform: ((T) -> CharSequence)? = null
): String {
    return if (isEmpty()) {
        ""
    } else {
        joinToString(separator, prefix, suffix, transform = transform)
    }
}
