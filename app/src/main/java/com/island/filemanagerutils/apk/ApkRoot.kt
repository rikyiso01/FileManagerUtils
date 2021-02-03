package com.island.filemanagerutils.apk

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.webkit.MimeTypeMap
import com.island.filemanagerutils.R
import com.island.filemanagerutils.utils.AbstractRoot
import com.island.filemanagerutils.utils.Document
import java.io.*

class ApkRoot(context: Context):AbstractRoot()
{
    val context:Context=context.applicationContext

    private fun getApkInfo(pkg:String):ApplicationInfo
    {
        val packageManager:PackageManager=context.packageManager
        return packageManager.getApplicationInfo(pkg,0)
    }

    override fun getScheme(): String
    {
        return "apk"
    }

    override fun getIcon(): Int
    {
        return R.mipmap.apk
    }

    override fun getTitle(context: Context): String
    {
        return context.getString(R.string.apk)
    }

    override fun getAuthority(): String
    {
        return ""
    }

    override fun supportsCreate(): Boolean
    {
        return false
    }

    @SuppressLint("QueryPermissionsNeeded")
    override fun list(dir: Document): Iterable<Document>
    {
        val packageManager=context.packageManager
        val applications=packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val result=ArrayList<Document>()
        for(application in applications)
        {
            val document=Document(dir,application.packageName)
            result.add(document)
        }
        return result
    }

    override fun isDirectory(document: Document): Boolean
    {
        return document.path=="/"
    }

    override fun length(document: Document): Long
    {
        return File(getApkInfo(document.name).sourceDir).length()
    }

    override fun lastModify(document: Document): Long
    {
        if(document.path=="/")return System.currentTimeMillis()
        return File(getApkInfo(document.name).sourceDir).lastModified()
    }

    override fun supportsCreateNewFile(document: Document): Boolean
    {
        return false
    }

    override fun supportsWrite(document: Document): Boolean
    {
        return false
    }

    override fun supportsDelete(document: Document): Boolean
    {
        return false
    }

    override fun write(document: Document): OutputStream
    {
        throw NotImplementedError()
    }

    override fun read(document: Document): InputStream
    {
        return FileInputStream(File(getApkInfo(document.name).sourceDir))
    }

    override fun mkDir(document: Document)
    {
        throw NotImplementedError()
    }

    override fun deleteFile(document: Document)
    {
        throw NotImplementedError()
    }

    override fun deleteDir(document: Document)
    {
        throw NotImplementedError()
    }

    override fun exists(document: Document): Boolean
    {
        if(document.path=="/")return true
        return try
        {
            getApkInfo(document.name)
            true
        }
        catch (e:PackageManager.NameNotFoundException){false}
    }

    override fun supportsThumbnail(document: Document): Boolean
    {
        return true
    }

    override fun getThumbnail(document: Document,size:Point): Bitmap
    {
        val result=Bitmap.createBitmap(size.x,size.y,Bitmap.Config.ARGB_8888)
        val drawable=getApkInfo(document.name).loadIcon(context.packageManager)
        drawable.setBounds(0,0,result.width,result.height)
        drawable.draw(Canvas(result))
        return result
    }

    override fun getMimeType(document: Document): String
    {
        if(document.path=="/")return super.getMimeType(document)
        val file=getApkInfo(document.name).sourceDir
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.substring(file.lastIndexOf(".")+1))!!
    }

    override fun displayName(document: Document): String
    {
        if(document.path=="/")return super.displayName(document)
        return getApkInfo(document.name).loadLabel(context.packageManager).toString()
    }

    override fun supportsEject(): Boolean
    {
        return false
    }

    override fun eject()
    {
        throw NotImplementedError()
    }
}