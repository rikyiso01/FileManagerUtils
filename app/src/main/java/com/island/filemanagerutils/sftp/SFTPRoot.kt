package com.island.filemanagerutils.sftp

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.StrictMode
import androidx.annotation.DrawableRes
import com.island.filemanagerutils.R
import com.island.filemanagerutils.utils.AbstractRoot
import com.island.filemanagerutils.utils.Document
import com.island.filemanagerutils.utils.Utils
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.*
import java.security.Security

class SFTPRoot(private val authority:String, private val context: Context) : AbstractRoot()
{
    companion object
    {
        fun getKeyFile(context: Context,username:String):File
        {
            val dir=File(context.filesDir,context.getString(R.string.keys_folder))
            if(!dir.exists())dir.mkdirs()
            return File(dir,username)
        }
    }
    init
    {
        Security.removeProvider("BC")
        Security.insertProviderAt(org.bouncycastle.jce.provider.BouncyCastleProvider(), 0)
    }
    private val ssh: SSHClient= SSHClient()
    private val sftp: SFTPClient

    init
    {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        val accountManager=AccountManager.get(context)
        val accounts:Array<Account> = accountManager.getAccountsByType(context.getString(R.string.account_type))
        var account:Account?=null
        for(acc in accounts)if(acc.name==authority)account=acc
        val token=accountManager.getAuthToken(account,context.getString(R.string.sftp_login_token_type),null,false,null,null).result.getString(AccountManager.KEY_AUTHTOKEN)!!
        ssh.addHostKeyVerifier(PromiscuousVerifier())
        ssh.connect(uri.host,uri.port)
        if(token==context.getString(R.string.keyfile_value))
        {
            val tmp=File(context.cacheDir,context.getString(R.string.keyfile_tmp))
            val keyfile=accountManager.getAuthToken(account,context.getString(R.string.sftp_keyfile_token_type),null,false,null,null).result.getByteArray(AccountManager.KEY_AUTHTOKEN)!!
            Utils.writeAll(ByteArrayInputStream(keyfile),FileOutputStream(tmp))
            ssh.authPublickey(uri.userInfo, getKeyFile(context,uri.authority!!).path)
            tmp.delete()
        }
        else ssh.authPassword(uri.userInfo,token)
        sftp=ssh.newSFTPClient()
    }

    override fun getScheme(): String
    {
        return "sftp"
    }

    @DrawableRes
    override fun getIcon(): Int
    {
        return R.mipmap.ic_launcher
    }

    override fun getTitle(context:Context): String
    {
        return context.getString(R.string.sftp)
    }

    override fun getAuthority(): String
    {
        return authority
    }

    override fun supportsCreate(): Boolean
    {
        return true
    }

    override fun list(dir: Document): Iterable<Document>
    {
        val result=ArrayList<Document>()
        for (info in sftp.ls(dir.path))
        {
            val document=Document(this,File(info.path))
            if(document.exists) result.add(document)
        }
        return result
    }

    override fun isDirectory(document: Document): Boolean
    {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        return sftp.type(document.path)==FileMode.Type.DIRECTORY
    }

    override fun length(document: Document): Long
    {
        return sftp.size(document.path)
    }

    override fun lastModify(document: Document): Long
    {
        return sftp.mtime(document.path)*1000
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
        return sftp.open(document.path,HashSet(arrayOf(OpenMode.CREAT,OpenMode.WRITE,OpenMode.TRUNC).asList())).RemoteFileOutputStream()
    }

    override fun read(document: Document): InputStream
    {
        return sftp.open(document.path,HashSet(arrayOf(OpenMode.READ).asList())).RemoteFileInputStream()
    }

    override fun mkDir(document: Document)
    {
        sftp.mkdir(document.path)
    }

    override fun deleteFile(document: Document)
    {
        sftp.rm(document.path)
    }

    override fun deleteDir(document: Document)
    {
        sftp.rmdir(document.path)
    }

    override fun exists(document: Document): Boolean
    {
        return sftp.statExistence(document.path)!=null
    }

    override fun close()
    {
        sftp.close()
        ssh.disconnect()
    }

    override fun move(src: Document, dst: Document)
    {
        sftp.rename(src.path,dst.path)
    }

    override fun supportsEject(): Boolean
    {
        return true
    }

    override fun eject()
    {
        context.getSharedPreferences(context.getString(R.string.shared_preferences),0).edit().putBoolean(authority,false).apply()
        close()
    }
}