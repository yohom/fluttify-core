package me.yohom.fluttify

import me.yohom.fluttify.extensions.file
import me.yohom.fluttify.extensions.fromJson
import me.yohom.fluttify.model.SDK

val sdk = "/Users/yohom/Github/Util/Kotlin/fluttify-core/src/test/resources/ir/android/json_representation.json".file().readText().fromJson<SDK>()
val ext = FluttifyExtension().apply { outputOrg = "me.yohom"; outputProjectName = "amap_base_flutter" }