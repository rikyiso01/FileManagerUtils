package com.island.filemanagerutils.root

import android.content.Context
import android.util.Log
import com.island.filemanagerutils.BuildConfig
import com.island.filemanagerutils.R
import com.island.filemanagerutils.utils.AbstractRoot
import com.island.filemanagerutils.utils.Document
import java.io.*
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class RootRoot:AbstractRoot()
{
    private val TAG="RootRoot"
    private val process:Process=Runtime.getRuntime().exec("su")
    private val output=PrintStream(BufferedOutputStream(process.outputStream))
    private val input=Scanner(BufferedInputStream(process.inputStream))
    private val error=InputStreamReader(process.errorStream)

    private fun execute(command:String)
    {
        output.println(command)
        output.flush()
    }

    override fun getScheme(): String
    {
        return "root"
    }

    override fun getIcon(): Int
    {
        return R.mipmap.root
    }

    override fun getTitle(context: Context): String
    {
        return context.getString(R.string.root)
    }

    override fun getAuthority(): String
    {
        return ""
    }

    override fun supportsCreate(): Boolean
    {
        return true
    }

    override fun list(dir: Document): Iterable<Document>
    {
        execute("ls -A ${dir.path} && echo '.'")
        input.next()
        val result=ArrayList<Document>()
        while (true)
        {
            val name=input.next()
            if(name==".")break
            result.add(Document(dir,name))
        }
        input.nextLine()
        if(BuildConfig.DEBUG)Log.d(TAG,"name: $result")
        return result
    }

    override fun isDirectory(document: Document): Boolean
    {
        execute("stat -c %F '${document.path}'")
        val line=input.nextLine()
        return "file" !in line
    }

    override fun length(document: Document): Long
    {
        execute("stat -c %s '${document.path}'")
        return input.nextLine().toLong()
    }

    override fun lastModify(document: Document): Long
    {
        execute("stat -c %Y '${document.path}'")
        return input.nextLine().toLong()*1000
    }

    override fun supportsCreateNewFile(document: Document): Boolean
    {
        return true
    }

    override fun supportsWrite(document: Document): Boolean
    {
        return true
    }

    override fun supportsDelete(document: Document): Boolean
    {
        return true
    }

    override fun write(document: Document): OutputStream
    {
        return Runtime.getRuntime().exec("su -c 'cat > \"${document.path}\"'").outputStream
    }

    override fun read(document: Document): InputStream
    {
        return Runtime.getRuntime().exec("su -c cat '${document.path}'").inputStream
    }

    override fun mkDir(document: Document)
    {
        execute("mkdir '${document.path}'")
    }

    override fun deleteFile(document: Document)
    {
        throw NotImplementedError()
    }

    override fun deleteDir(document: Document)
    {
        throw NotImplementedError()
    }

    override fun delete(document: Document)
    {
        execute("rm -rf '${document.path}'")
    }

    override fun exists(document: Document): Boolean
    {
        execute("file '${document.path}'")
        return "No such" !in input.nextLine()
    }

    override fun copy(src: Document, dst: Document)
    {
        execute("cp -r '${src.path}' '${dst.path}'")
    }

    override fun move(src: Document, dst: Document)
    {
        execute("mv '${src.path}' '${dst.path}'")
    }

    override fun onException(exception: Exception)
    {
        Log.e(TAG,error.readText().replace('\n',' '))
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