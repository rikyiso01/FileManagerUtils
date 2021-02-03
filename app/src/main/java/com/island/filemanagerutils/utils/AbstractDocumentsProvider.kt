package com.island.filemanagerutils.utils

import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.net.Uri
import android.os.*
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.io.File
import java.io.FileNotFoundException

abstract class AbstractDocumentsProvider: DocumentsProvider(),CoroutineScope by MainScope()
{
    private val TAG="AbstractDocumentsProvider"
    private var roots:Iterable<AbstractRoot> = ArrayList()

    private val DEFAULT_ROOT_PROJECTION: Array<String> = arrayOf(
        DocumentsContract.Root.COLUMN_ROOT_ID,
        DocumentsContract.Root.COLUMN_FLAGS,
        DocumentsContract.Root.COLUMN_ICON,
        DocumentsContract.Root.COLUMN_TITLE,
        DocumentsContract.Root.COLUMN_SUMMARY,
        DocumentsContract.Root.COLUMN_DOCUMENT_ID,
    )

    private val DEFAULT_DOCUMENT_PROJECTION: Array<String> = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        DocumentsContract.Document.COLUMN_FLAGS,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
    )

    override fun onCreate(): Boolean
    {
        return true
    }

    protected abstract fun getRoots(): Iterable<AbstractRoot>
    protected abstract fun getServiceClass():Class<out UploadService>
    protected abstract fun getCacheDir():File
    protected abstract fun getAuthority():String

    private fun notifyChange(document:Document)
    {
        context!!.contentResolver.notifyChange(DocumentsContract.buildDocumentUri(getAuthority(),document.toString()),null)
    }

    private fun getCacheFile(name:String):File
    {
        val dir=getCacheDir()
        if(!dir.exists())dir.mkdirs()
        return File(dir,name)
    }

    private fun getDocument(uri:Uri):Document
    {
        for (root in roots)if (root.getScheme()==uri.scheme && root.getAuthority()==uri.authority)
        {
            return Document(root,File(uri.path!!))
        }

        closeRoots()
        roots=getRoots()
        for (document in roots)if (document.getScheme()==uri.scheme && document.getAuthority()==uri.authority)
        {
            return Document(document,File(uri.path!!))
        }
        throw NoSuchElementException("No document available for $uri")
    }

    private fun closeRoots()
    {
        for(root in roots)root.close()
    }

    override fun queryRoots(projection: Array<out String>?): Cursor
    {
        val function="QueryRoots"
        Log.i(TAG,"$function $projection")
        try
        {
            val result=MatrixCursor(resolveRootProjection(projection))
            closeRoots()
            roots=getRoots()
            for (root in roots)
            {
                val uri=root.uri
                val row=result.newRow()
                row.add(DocumentsContract.Root.COLUMN_ROOT_ID,uri.toString())
                val documentId= "$uri/"
                row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID,documentId)
                row.add(DocumentsContract.Root.COLUMN_ICON,root.getIcon())
                var flags = 0
                if(root.supportsCreate()) flags=flags or DocumentsContract.Root.FLAG_SUPPORTS_CREATE
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O && root.supportsEject())flags=flags or DocumentsContract.Root.FLAG_SUPPORTS_EJECT
                row.add(DocumentsContract.Root.COLUMN_FLAGS,flags)
                row.add(DocumentsContract.Root.COLUMN_TITLE,root.getTitle(context!!))
                row.add(DocumentsContract.Root.COLUMN_SUMMARY,uri.authority)
            }
            return result
        }
        catch (e:Exception)
        {
            throw exception(e,function)
        }
    }

    override fun queryChildDocuments(parentDocumentId: String?, projection: Array<out String>?, sortOrder: String?): Cursor
    {
        val function="QueryChildDocuments"
        Log.i(TAG,"$function $parentDocumentId $projection $sortOrder")
        try
        {
            val result=MatrixCursor(resolveDocumentProjection(projection))
            val parentDocument=getDocument(Uri.parse(parentDocumentId))
            for(child in parentDocument.children)
            {
                putFileInfo(result.newRow(),child)
            }
            return result
        }
        catch (e:Exception)
        {
            throw exception(e,function)
        }
    }

    override fun ejectRoot(rootId: String?)
    {
        val function="EjectRoot"
        Log.i(TAG,"$function $rootId")
        try
        {
            val document=getDocument(Uri.parse(rootId))
            document.root.eject()
            context!!.contentResolver.notifyChange(DocumentsContract.buildRootsUri(getAuthority()),null)
        }
        catch (e:Exception)
        {
            throw exception(e,function)
        }
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor
    {
        val function="QueryDocument"
        Log.i(TAG,"$function $documentId $projection")
        try
        {
            val result=MatrixCursor(resolveDocumentProjection(projection))
            putFileInfo(result.newRow(),getDocument(Uri.parse(documentId)))
            return result
        }
        catch (e:Exception)
        {
            throw exception(e,function)
        }
    }

    override fun openDocument(documentId: String?, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor
    {
        val function="OpenDocument"
        Log.i(TAG,"$function $documentId $mode $signal")
        try
        {
            val accessMode=ParcelFileDescriptor.parseMode(mode)
            val isWrite=mode!!.indexOf('w')!=-1
            val documentUri=Uri.parse(documentId)
            val document=getDocument(documentUri)
            val cache=getCacheFile(document.name)
            document.downloadFile(cache)
            return ParcelFileDescriptor.open(cache,accessMode,Handler(Looper.getMainLooper()!!))
            {
                Log.i(TAG,"FileDescriptorClose $it")
                if (it == null)
                {
                    if(isWrite)
                    {
                        val intent = Intent(context, getServiceClass())
                        intent.data = documentUri
                        context!!.startService(intent)
                    }
                }
                else
                {
                    exception(it, "OnCloseListener")
                }
                cache.delete()
            }
        }
        catch (e:Exception)
        {
            throw exception(e,function)
        }
    }

    override fun createDocument(parentDocumentId: String?, mimeType: String?, displayName: String?): String
    {
        val function="createDocument"
        Log.i(TAG,"$function $parentDocumentId $mimeType $displayName")
        try
        {
            val parentDocument=getDocument(Uri.parse(parentDocumentId))
            val document=Document(parentDocument,displayName!!)
            if(mimeType==DocumentsContract.Document.MIME_TYPE_DIR)document.mkDir()
            else document.createNewFile()
            notifyChange(parentDocument)
            return document.toString()
        }
        catch (e:Exception)
        {
            throw exception(e,function)
        }
    }

    override fun deleteDocument(documentId: String?)
    {
        val function="DeleteDocument"
        Log.i(TAG,"$function $documentId")
        try
        {
            val document=getDocument(Uri.parse(documentId))
            document.delete()
            notifyChange(document.parent)
        }
        catch (e:Exception)
        {
            throw exception(e,function)
        }
    }

    override fun getDocumentType(documentId: String?): String
    {
        val function="GetDocumentType"
        Log.i(TAG,"$function $documentId")
        try
        {
            return getDocument(Uri.parse(documentId)).mimeType
        }
        catch (e:Exception)
        {
            throw exception(e,function)
        }
    }

    override fun renameDocument(documentId: String?, displayName: String?): String
    {
        val function="RenameDocument"
        Log.i(TAG,"$function $documentId $displayName")
        try
        {
            val document=getDocument(Uri.parse(documentId)).rename(displayName!!)
            notifyChange(document.parent)
            return document.toString()
        }
        catch (e:Exception)
        {
            throw exception(e,function)
        }
    }

    override fun moveDocument(sourceDocumentId: String?, sourceParentDocumentId: String?, targetParentDocumentId: String?): String
    {
        val function="MoveDocument"
        Log.i(TAG,"$function $sourceDocumentId $sourceParentDocumentId $targetParentDocumentId")
        try
        {
            val src=getDocument(Uri.parse(sourceDocumentId))
            val result=src.move(Document(getDocument(Uri.parse(targetParentDocumentId)),src.name))
            notifyChange(src.parent)
            notifyChange(result.parent)
            return result.toString()
        }
        catch (e:Exception)
        {
            throw exception(e,function)
        }
    }

    override fun copyDocument(sourceDocumentId: String?, targetParentDocumentId: String?): String
    {
        val function="CopyDocument"
        Log.i(TAG,"$function $sourceDocumentId $targetParentDocumentId")
        try
        {
            val src=getDocument(Uri.parse(sourceDocumentId))
            val result=src.copy(Document(getDocument(Uri.parse(targetParentDocumentId)),src.name))
            notifyChange(result.parent)
            return result.toString()
        }
        catch (e:Exception)
        {
            throw exception(e,function)
        }
    }

    override fun openDocumentThumbnail(documentId: String?, sizeHint: Point?, signal: CancellationSignal?): AssetFileDescriptor
    {
        val function="OpenDocumentThumbnail"
        Log.i(TAG,"$function $documentId $sizeHint $signal")
        try
        {
            val document=getDocument(Uri.parse(documentId))
            val cache=getCacheFile(document.name+".png")
            document.downloadThumbnail(cache,sizeHint!!)
            val descriptor=ParcelFileDescriptor.open(cache,ParcelFileDescriptor.MODE_READ_ONLY,Handler(Looper.getMainLooper()))
            {
                Log.i(TAG,"ThumbnailClose $it")
                cache.delete()
            }
            return AssetFileDescriptor(descriptor,0,cache.length())
        }
        catch (e:Exception)
        {
            throw exception(e,function)
        }
    }

    private fun resolveRootProjection(projection:Array<out String>?):Array<out String>
    {
        return projection ?: DEFAULT_ROOT_PROJECTION
    }

    private fun resolveDocumentProjection(projection:Array<out String>?):Array<out String>
    {
        return projection ?: DEFAULT_DOCUMENT_PROJECTION
    }

    private fun exception(e:Exception,msg:String,vararg objects:Any):FileNotFoundException
    {
        var message:String=msg
        for(arg in objects)message+= " $arg"
        Log.e(TAG,message,e)
        val exception= FileNotFoundException(message)
        exception.initCause(e)
        for(root in roots)root.onException(e)
        return exception
    }

    private fun putFileInfo(row:MatrixCursor.RowBuilder,document:Document)
    {
        var flags=0
        if(document.directory)
        {
            if(document.supportsCreateNewFile)flags=flags or DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
        }
        else
        {
            if(document.supportsWrite)flags=flags or DocumentsContract.Document.FLAG_SUPPORTS_WRITE
            if(document.supportsThumbnail)flags=flags or DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL
            row.add(DocumentsContract.Document.COLUMN_SIZE,document.length)
        }
        if(document.supportsDelete)flags=flags.or(DocumentsContract.Document.FLAG_SUPPORTS_DELETE)
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
        {
            if(document.supportsCopy)flags=flags or DocumentsContract.Document.FLAG_SUPPORTS_COPY
            if(document.supportsMove)flags=flags or DocumentsContract.Document.FLAG_SUPPORTS_MOVE
            if(document.supportsRename)flags=flags or DocumentsContract.Document.FLAG_SUPPORTS_RENAME
        }
        row.add(DocumentsContract.Document.COLUMN_FLAGS,flags)
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE,document.mimeType)
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME,document.displayName)
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID,document.toString())
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED,document.lastModify)
    }
}