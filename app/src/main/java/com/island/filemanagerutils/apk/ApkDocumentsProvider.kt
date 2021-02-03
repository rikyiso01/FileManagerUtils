package com.island.filemanagerutils.apk

import android.os.Build
import com.island.filemanagerutils.R
import com.island.filemanagerutils.utils.AbstractDocumentsProvider
import com.island.filemanagerutils.utils.AbstractRoot
import com.island.filemanagerutils.utils.UploadService
import java.io.File

class ApkDocumentsProvider:AbstractDocumentsProvider()
{
    override fun getRoots(): Iterable<AbstractRoot>
    {
        return if(context!!.getSharedPreferences(context!!.getString(R.string.shared_preferences),0).getBoolean(context!!.getString(R.string.apk_enabled_preference),false)  && Build.VERSION.SDK_INT< Build.VERSION_CODES.R)
            arrayListOf(ApkRoot(context!!)) else emptyList()
    }

    override fun getServiceClass(): Class<out UploadService>
    {
        throw NotImplementedError()
    }

    override fun getCacheDir(): File
    {
        return File(context!!.cacheDir,"apk")
    }

    override fun getAuthority(): String
    {
        return context!!.getString(R.string.apk_authority)
    }
}