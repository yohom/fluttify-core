package me.yohom.fluttify.task

import me.yohom.fluttify.FluttifyExtension
import me.yohom.fluttify.extensions.file
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler

/**
 * 反编译Jar任务
 *
 * 输入: 单个class文件
 * 输出: 反编译后的单个java文件
 */
open class DecompileClass : DefaultTask() {

    override fun getGroup() = "fluttify"

    @TaskAction
    fun decompile() {
        val ext = project.extensions.getByType(FluttifyExtension::class.java)

        val classFilesDir = "${ext.jarDir}unzip/".file()
        val javaFilesDir = "${project.buildDir}/decompiled/".file()

        ConsoleDecompiler.main(arrayOf(
            "-dgs=1", "-din=0", "-rsy=1", "-hdc=0",
            classFilesDir.toString(),
            javaFilesDir.toString())
        )
    }
}