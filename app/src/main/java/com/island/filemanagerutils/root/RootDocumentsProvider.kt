package com.island.filemanagerutils.root

import com.island.filemanagerutils.R
import com.island.filemanagerutils.utils.AbstractDocumentsProvider
import com.island.filemanagerutils.utils.AbstractRoot
import com.island.filemanagerutils.utils.UploadService
import java.io.File
import java.io.IOException

class RootDocumentsProvider:AbstractDocumentsProvider()
{
    override fun getRoots(): Iterable<AbstractRoot>
    {
        return try
        {
            if (context!!.getSharedPreferences(context!!.getString(R.string.shared_preferences), 0).getBoolean(context!!.getString(R.string.root_enabled_preference), false))
                arrayListOf(RootRoot()) else emptyList()
        }
        catch (e:IOException)
        {
            emptyList()
        }
    }

    override fun getServiceClass(): Class<out UploadService>
    {
        return RootUploadService::class.java
    }

    override fun getCacheDir(): File
    {
        return File(context!!.cacheDir,"root")
    }

    override fun getAuthority(): String
    {
        return context!!.getString(R.string.root_authority)
    }
}