package com.island.filemanagerutils.sftp

import android.accounts.Account
import android.accounts.AccountManager
import android.util.Log
import com.island.filemanagerutils.R
import com.island.filemanagerutils.utils.AbstractDocumentsProvider
import com.island.filemanagerutils.utils.AbstractRoot
import com.island.filemanagerutils.utils.UploadService
import java.io.File
import java.io.IOException

class SFTPDocumentsProvider:AbstractDocumentsProvider()
{
    private val TAG="SFTPDocumentsProvider"
    override fun getRoots(): Iterable<AbstractRoot>
    {
        val accountManager: AccountManager = AccountManager.get(context)
        val accounts:Array<Account> = accountManager.getAccountsByType(context!!.getString(R.string.account_type))
        val result: MutableList<AbstractRoot> = ArrayList()
        for(account in accounts)
        {
            if(!context!!.getSharedPreferences(context!!.getString(R.string.shared_preferences),0).getBoolean(account.name,true))continue
            try
            {
                result.add(SFTPRoot(account.name, context!!))
            }
            catch(e:IOException)
            {
                context!!.getSharedPreferences(context!!.getString(R.string.shared_preferences),0).edit().putBoolean(account.name,false).apply()
                Log.e(TAG,"GetRoots $e",e)
            }
        }
        return result
    }

    override fun getServiceClass(): Class<out UploadService>
    {
        return SFTPUploadService::class.java
    }

    override fun getCacheDir(): File
    {
        return File(context!!.cacheDir,"sftp")
    }

    override fun getAuthority(): String
    {
        return context!!.getString(R.string.sftp_authority)
    }
}