package com.island.filemanagerutils.utils

import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import java.io.*

@Suppress("MemberVisibilityCanBePrivate")
class Document(val root:AbstractRoot,val file:File)
{
    constructor(document: Document,name:String) : this(document.root,File(document.file,name))

    val uri:Uri
    get()
    {
        return Uri.parse(root.uri.toString()+file.toString())
    }

    val children:Array<Document>
    get()
    {
        val result=ArrayList<Document>()
        for (document in root.list(this))
        {
            result.add(document)
        }
        return result.toArray(arrayOf())
    }

    val directory:Boolean
    get()
    {
        return root.isDirectory(this)
    }

    val length:Long
    get()
    {
        return root.length(this)
    }

    val supportsCreateNewFile:Boolean
    get()
    {
        return root.supportsCreateNewFile(this)
    }

    val supportsWrite:Boolean
    get()
    {
        return root.supportsWrite(this)
    }

    val name:String
    get()
    {
        return root.getName(this)
    }

    fun rename(name:String):Document
    {
        val result=Document(parent,name).unique
        root.rename(this,result.name)
        return result
    }

    val lastModify:Long
    get()
    {
        return root.lastModify(this)
    }

    val supportsDelete:Boolean
    get()
    {
        return root.supportsDelete(this)
    }

    val supportsCopy:Boolean
    get()
    {
        return root.supportsCopy(this)
    }

    val supportsMove:Boolean
    get()
    {
        return root.supportsMove(this)
    }

    val supportsRename:Boolean
    get()
    {
        return root.supportsRename(this)
    }

    val mimeType:String
    get()
    {
        return root.getMimeType(this)
    }

    fun read():InputStream
    {
        return root.read(this)
    }

    fun write():OutputStream
    {
        return root.write(this)
    }

    fun writeTo(output:OutputStream,callback:((progress:Float)->Unit)?=null)
    {
        val length = if(callback==null) 0 else this.length
        Utils.writeAll(read(),output, if(callback==null) {_->} else {written->callback(written.toFloat()/length)})
    }

    fun readFrom(input:InputStream,callback: ((progress: Int) -> Unit)={})
    {
        Utils.writeAll(input,write(),callback)
    }

    fun downloadFile(file:File,callback:((progress:Float)->Unit)?=null)
    {
        writeTo(FileOutputStream(file),callback)
    }

    fun uploadFile(file:File,callback:((progress:Float)->Unit)={})
    {
        val length=file.length()
        readFrom(FileInputStream(file)) { written -> callback(written.toFloat() / length) }
    }

    fun mkDir()
    {
        root.mkDir(this)
    }

    fun createNewFile()
    {
        root.createNewFile(this)
    }

    fun delete()
    {
        root.delete(this)
    }

    fun copy(dst:Document):Document
    {
        val result=dst.unique
        root.copy(this,result)
        return result
    }

    fun move(dst:Document):Document
    {
        val result=dst.unique
        root.move(this,result)
        return result
    }

    val exists:Boolean
    get()
    {
        return root.exists(this)
    }

    val parent:Document
    get()
    {
        return Document(root,file.parentFile!!)
    }

    val extension:String
    get()
    {
        return root.getExtension(this)
    }

    val nameWithoutExtension:String
    get()
    {
        return root.getNameWithoutExtension(this)
    }

    val unique:Document
    get()
    {
        return root.getNonExisting(this)
    }

    val path:String
    get()
    {
        return file.path
    }

    override fun toString(): String
    {
        return uri.toString()
    }

    fun getThumbnail(size:Point):Bitmap
    {
        return root.getThumbnail(this,size)
    }

    val supportsThumbnail:Boolean
    get()
    {
        return root.supportsThumbnail(this)
    }

    fun writeThumbnailTo(output: OutputStream,size:Point)
    {
        val out=BufferedOutputStream(output)
        getThumbnail(size).compress(Bitmap.CompressFormat.PNG,100,out)
        out.close()
    }

    fun downloadThumbnail(file:File,size:Point)
    {
        writeThumbnailTo(FileOutputStream(file),size)
    }

    val displayName:String
    get()
    {
        return root.displayName(this)
    }
}