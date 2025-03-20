package com.wardtn.modellibrary.util

import android.text.TextUtils
import java.io.File

/**
 * 检查指定路径的文件是否存在。
 *
 * @param filePath 文件路径
 * @return 如果文件存在，则返回 true；否则返回 false。
 */
fun isFileExists(filePath: String): Boolean {
    if (TextUtils.isEmpty(filePath)) return false
    val file = File(filePath)
    return file.exists()
}