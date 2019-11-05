package me.yohom.fluttify.tmpl.java.plugin.register_platform_view

import me.yohom.fluttify.FluttifyExtension
import me.yohom.fluttify.extensions.simpleName
import me.yohom.fluttify.model.Type

//registrar
//        .platformViewRegistry()
//        .registerViewFactory("#__view_type__#", #__factory_name__#Factory(registrar))
internal class RegisterPlatformViewTmpl(
    private val viewType: Type,
    private val ext: FluttifyExtension
) {
    private val tmpl = this::class.java.getResource("/tmpl/java/register_platform_view.stmt.java.tmpl").readText()

    fun kotlinRegisterPlatformView(): String {
        return tmpl
            .replace("#__view_type__#", "${ext.outputOrg}/${viewType.name}")
            .replace("#__factory_name__#", viewType.name.simpleName())
    }
}