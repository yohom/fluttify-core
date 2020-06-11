package me.yohom.fluttify.task

import me.yohom.fluttify.extensions.downloadFrom
import me.yohom.fluttify.extensions.file
import me.yohom.fluttify.extensions.fromJson
import me.yohom.fluttify.model.Podspec
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FalseFileFilter
import org.apache.commons.io.filefilter.IOFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.gradle.api.tasks.TaskAction
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.net.URI

open class DownloadAndroidSDK : FluttifyTask() {
    @TaskAction
    fun process() {
        if (ext.android.remote.run { "$org$name$version" }.isNotEmpty()) {
            project.repositories.run {
                maven { it.url = URI("http://maven.aliyun.com/nexus/content/groups/public/") }
                maven { it.url = URI("https://oss.sonatype.org/content/groups/public") }
                maven { it.url = URI("https://dl.bintray.com/aweme-open-sdk-team/public") }
                jcenter()
                mavenCentral()
            }
            val config = project.configurations.create("targetJar")
            val dep = project.dependencies.create(ext.android.remote.run { "$org:$name:$version" })
            config.dependencies.add(dep)
            if (config.files.isNotEmpty()) {
                config.files.first().run {
                    FileUtils.copyFile(this, "${ext.android.libDir}/${name}".file())
                }
            }
        }
    }
}

open class DownloadIOSSDK : FluttifyTask() {
    @TaskAction
    fun process() {
        ext.ios.libDir.file().run {
            if (list { _, name -> !name.startsWith(".") }?.isEmpty() == true
                &&
                ext.ios.remote.run { "$name$version" }.isNotEmpty()
            ) {
                val iosArchiveSpec: File?

                val specDir = "${System.getProperty("user.home")}/.cocoapods/repos/master/Specs/".file()
                val archiveName = ext.ios.remote.name
                val archiveVersion = ext.ios.remote.version
                // 找出目标pod所在的文件
                // cocoapods的Specs文件夹分为三层0x0-0xf的文件夹, 最后一层文件夹下的分别存放着所有的pod, 找到目标pod后再根据版本号找到目标podspec.json
                // 解析出下载地址后进行下载
                iosArchiveSpec = specDir.listFiles()
                    ?.flatMap<File, File> { it.listFiles()?.toList() ?: listOf() }
                    ?.flatMap<File, File> { it.listFiles()?.toList() ?: listOf() }
                    ?.flatMap<File, File> { it.listFiles()?.toList() ?: listOf() }
                    ?.firstOrNull { it.nameWithoutExtension == archiveName }

                // 找出目标版本所在文件夹
                val targetVersion: File? = iosArchiveSpec?.listFiles()?.firstOrNull { it.name.contains(archiveVersion) }
                println("targetVersion: $targetVersion")

                // 从podspec.json文件中读取下载地址, 并下载
                targetVersion?.run {
                    val podspec = File("$this/$archiveName.podspec.json").readText().fromJson<Podspec>()
                    podspec.source?.http?.run {
                        val archiveFile = "${ext.ios.libDir}/ARCHIVE.zip".file()

                        archiveFile.downloadFrom(this)
                        // 下载完成后解压
                        ZipUtil.unpack(archiveFile, archiveFile.parentFile)
                        // 如果包含vendored_frameworks, 那么需要再拿出framework
                        // 碰到一种情况, 下载下来带有demo和乱七八糟的东西, 需要再把framework找出来
                        val vendoredFrameworkPath = podspec.ios?.vendoredFrameworks ?: podspec.vendoredFrameworks
                        if (vendoredFrameworkPath != null) {
                            val trueFramework = if (vendoredFrameworkPath.contains("**/")) {
                                // 如果含有通配符, 那么从通配符开始查找目标framework
                                val subPath = vendoredFrameworkPath.substringBefore("**/")
                                val frameworkName = vendoredFrameworkPath.substringAfter("**/")
                                FileUtils.iterateFilesAndDirs(
                                    "${ext.ios.libDir}/$subPath".file(),
                                    FalseFileFilter.INSTANCE,
                                    TrueFileFilter.INSTANCE
                                )
                                    .asSequence()
                                    .find { it.name.endsWith(frameworkName) }
                                    ?.path!!
                            } else {
                                "${ext.ios.libDir}/$vendoredFrameworkPath"
                            }
                            // 因为其实每个pod都会有vendored_frameworks字段, 所以这里判断一下顶层是否已经有这个framework, 如果有的
                            // 话就不需要拷贝了
                            // 拿出framework文件, 然后拷贝到顶层
                            FileUtils.copyDirectoryToDirectory(trueFramework.file(), ext.ios.libDir.file())
                            ext.ios.libDir.file()
                                .listFiles()
                                ?.filter { it.name != trueFramework.file().name }
                                ?.forEach { it.deleteRecursively() }
                        }
                        // 删除压缩文件
                        archiveFile.delete()
                    }
                }
            } else {
                println("非远程依赖 或 本地已缓存依赖")
            }
        }
    }
}