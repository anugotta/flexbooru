/*
 * Copyright (C) 2019. by onlymash <im@fiepi.me>, All rights reserved
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package onlymash.flexbooru.content

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

/**
 * Image provider
 * */
class FlexProvider : FileProvider() {
    companion object {
        /**
         * return image uri [Uri] from file path
         * */
        fun getUriFromFilePath(context: Context, filePath: String): Uri = getUriFromFile(context, File(filePath))
        /**
         * return image uri [Uri] from file
         * */
        fun getUriFromFile(context: Context, file: File): Uri =
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> getUriForFile(
                    context,
                    context.applicationContext.packageName + ".onlymash",
                    file)
                else -> Uri.fromFile(file)
            }
    }
}