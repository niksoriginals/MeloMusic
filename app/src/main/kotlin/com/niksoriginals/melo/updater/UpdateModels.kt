package com.niksoriginals.melo.updater

data class UpdateInfo(
    val tagName: String,
    val versionName: String,
    val isAvailable: Boolean,
    val apkUrl: String,
    val apkSizeMb: String,
    val publishedDate: String,
    val changelog: String,
    val fileName: String
)
