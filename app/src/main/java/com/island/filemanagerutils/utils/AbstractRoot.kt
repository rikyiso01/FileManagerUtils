package com.island.filemanagerutils.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.annotation.DrawableRes
import java.io.*
import java.lang.Exception

abstract class AbstractRoot
{
    abstract fun getScheme():String
    @DrawableRes
    abstract fun getIcon():Int
    abstract fun getTitle(context: Context):String
    abstract fun getAuthority():String
    abstract fun supportsCreate():Boolean
    abstract fun list(dir:Document):Iterable<Document>
    abstract fun isDirectory(document:Document):Boolean
    abstract fun length(document:Document):Long
    abstract fun lastModify(document:Document):Long
    abstract fun supportsCreateNewFile(document:Document):Boolean
    abstract fun supportsWrite(document:Document):Boolean
    abstract fun supportsDelete(document:Document):Boolean
    abstract fun supportsEject():Boolean
    abstract fun eject()
    abstract fun write(document:Document):OutputStream
    abstract fun read(document:Document):InputStream
    abstract fun mkDir(document:Document)
    abstract fun deleteFile(document:Document)
    abstract fun deleteDir(document:Document)
    abstract fun exists(document:Document):Boolean

    fun getExtension(document:Document):String
    {
        return document.name.substring(document.name.lastIndexOf('.')+1)
    }

    fun getNameWithoutExtension(document: Document):String
    {
        return document.name.substring(0,document.name.lastIndexOf('.'))
    }

    open fun getMimeType(document: Document):String
    {
        return if (document.directory) DocumentsContract.Document.MIME_TYPE_DIR
        else MimeTypeMap.getSingleton().getMimeTypeFromExtension(document.extension) ?: "application/octet-stream"
    }

    val uri:Uri
    get()
    {
        return Uri.parse(getScheme()+"://"+getAuthority())
    }

    open fun getName(document:Document):String
    {
        return document.file.name
    }

    open fun supportsCopy(document: Document):Boolean
    {
        return document.supportsCreateNewFile
    }

    open fun supportsMove(document: Document):Boolean
    {
        return document.supportsCopy && document.supportsDelete
    }

    open fun supportsRename(document: Document):Boolean
    {
        return document.supportsMove
    }

    open fun createNewFile(document: Document)
    {
        document.write().close()
    }

    open fun delete(document: Document)
    {
        if(!document.directory)deleteFile(document)
        else
        {
            for(child in document.children)child.delete()
            deleteDir(document)
        }
    }

    open fun copy(src:Document,dst:Document)
    {
        if(src.directory)
        {
            dst.mkDir()
            for (child in src.children)child.copy(Document(dst,child.name))
        }
        else Utils.writeAll(read(src),write(dst))
    }

    open fun move(src:Document,dst:Document)
    {
        src.copy(dst)
        src.delete()
    }

    open fun rename(document:Document,name:String)
    {
        document.move(Document(document.parent,name))
    }

    open fun getNonExisting(document:Document):Document
    {
        var result=document
        while (result.exists)
        {
            result=Document(result.parent,"${result.nameWithoutExtension} (1).${result.extension}")
        }
        return result
    }

    open fun close(){}

    open fun supportsThumbnail(document: Document):Boolean
    {
        val mime=document.mimeType
        return "image" in mime
    }

    open fun getThumbnail(document:Document,size:Point):Bitmap
    {
        val input=BufferedInputStream(document.read())
        val bitmap=BitmapFactory.decodeStream(input)!!
        input.close()
        val result=Bitmap.createBitmap(size.x,size.y,Bitmap.Config.ARGB_8888)
        Canvas(result).drawBitmap(bitmap,Rect(0,0,bitmap.width,bitmap.height),Rect(0,0,result.width,result.height),null)
        return result
    }

    open fun displayName(document: Document):String
    {
        return document.name
    }

    open fun onException(exception: Exception){}
}