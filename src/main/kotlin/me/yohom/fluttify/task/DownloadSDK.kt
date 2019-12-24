package me.yohom.fluttify.task

import me.yohom.fluttify.extensions.*
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction
import org.zeroturnaround.zip.ZipUtil
import java.io.File

open class DownloadAndroidSDK : FluttifyTask() {
    @TaskAction
    fun process() {
        if (ext.androidArchiveCoordinate.isNotEmpty()) {
            project.repositories.jcenter()
            val config = project.configurations.create("targetJar")
            val dep = project.dependencies.create(ext.androidArchiveCoordinate)
            config.dependencies.add(dep)
            if (config.files.isNotEmpty()) {
                config.files.first().run {
                    FileUtils.copyFile(this, "${ext.archiveDir}/${name}".file())
                }
            }
        }
    }
}

open class DownloadIOSSDK : FluttifyTask() {
    @TaskAction
    fun process() {
        ext.frameworkDir.file().run {
            if (list()?.none { it.isIOSArchive() } == true && ext.iosArchiveCoordinate.isNotEmpty()) {
                var iosArchiveSpec: File? = null

                val specDir = "${System.getProperty("user.home")}/.cocoapods/repos/master/Specs/".file()
                val archiveName = ext.iosArchiveCoordinate.split(",").map { it.stripQuotes().trim() }[0]
                val archiveVersion = try {
                    ext.iosArchiveCoordinate.split(",").map { it.stripQuotes().trim() }[1]
                } catch (e: Exception) {
                    ""
                }.removePrefix("~>").trim()
                // 找出目标pod所在的文件
                // cocoapods的Specs文件夹分为三层0x0-0xf的文件夹, 最后一层文件夹下的分别存放着所有的pod, 找到目标pod后再根据版本号找到目标podspec.json
                // 解析出下载地址后进行下载
                val l0Files = specDir.listFiles()
                out@ for (i in 0..0xf) {
                    val l0 = l0Files?.get(i)
                    val l1Files = l0?.listFiles()
                    for (j in 0..0xf) {
                        val l1 = l1Files?.get(j)
                        val l2Files = l1?.listFiles()
                        for (k in 0..0xf) {
                            val l2 = l2Files?.get(k)
                            for (l3 in l2?.listFiles() ?: arrayOf()) {
                                if (l3.name == archiveName) {
                                    iosArchiveSpec = l3
                                    println("iosArchiveSpec: $iosArchiveSpec")
                                    break@out
                                }
                            }
                        }
                    }
                }

                // 找出目标版本所在文件夹
                val targetVersion: File? = iosArchiveSpec?.listFiles()?.firstOrNull { it.name.contains(archiveVersion) }
                println("targetVersion: $targetVersion")

                // 从podspec.json文件中读取下载地址, 并下载
                targetVersion?.run {
                    val podspecJson = File("$this/$archiveName.podspec.json").readText().fromJson<Map<String, Any>>()
                    val source: Map<String, String> = podspecJson["source"] as Map<String, String>
                    source["http"]?.run {
                        val archiveFile = "${ext.frameworkDir}/ARCHIVE.zip".file()

                        archiveFile.downloadFrom(this)
                        // 下载完成后解压
                        ZipUtil.unpack(archiveFile, archiveFile.parentFile)
                        // 删除压缩文件
                        archiveFile.delete()
                    }
                }
            }
        }
    }
}