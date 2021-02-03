package com.island.filemanagerutils.sftp

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.island.filemanagerutils.R
import com.island.filemanagerutils.utils.Utils
import kotlinx.coroutines.*
import java.io.FileOutputStream


class SFTPAuthenticationActivity:AppCompatActivity(),CoroutineScope by MainScope()
{
    private val TAG="SFTPAuthActivity"
    private val OPEN_KEYFILE=1
    private var keyfile:Uri?=null
    private var confirmed=false
    override fun onCreate(savedInstanceState: Bundle?)
    {
        Log.i(TAG, "OnCreate")
        super.onCreate(savedInstanceState)
        if(intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)==null)
        {
            finish()
            return
        }
        setContentView(R.layout.authentication_activity)
        val uri=intent.data
        if (uri != null)
        {
            val ip = findViewById<EditText>(R.id.host)
            val port = findViewById<EditText>(R.id.port)
            val user = findViewById<EditText>(R.id.username)
            ip.setText(uri.host)
            user.setText(uri.userInfo)
            port.setText(uri.port.toString())
        }
    }

    fun confirm(view: View)
    {
        Log.i(TAG,"Confirm $view")
        if(confirmed)return
        else confirmed=true
        val ip = (findViewById<View>(R.id.host) as EditText).text.toString()
        val port = (findViewById<View>(R.id.port) as EditText).text.toString()
        val user = (findViewById<View>(R.id.username) as EditText).text.toString()
        val password = (findViewById<View>(R.id.password) as EditText).text.toString()
        if (ip.isEmpty() || port.isEmpty() || user.isEmpty() || password.isEmpty()) return
        val accountType = intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
        val username = "$user@$ip:$port"
        val account = Account(username, accountType)
        val accountManager = AccountManager.get(this)
        val userdata = Bundle()
        accountManager.addAccountExplicitly(account, password, userdata)
        contentResolver.notifyChange(DocumentsContract.buildRootsUri(getString(R.string.sftp_authority)), null)
        val data = Bundle()
        data.putString(AccountManager.KEY_ACCOUNT_NAME, username)
        data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType)
        val result = Intent()
        result.putExtras(data)
        setResult(RESULT_OK, result)
        val context:Context=this
        launch { createAccount(context,username) }
    }

    private suspend fun createAccount(context:Context,username:String) = withContext(Dispatchers.Main)
    {
        if(keyfile!=null)
        {
            launch { copyKey(context,username) }
        }
        finish()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun copyKey(context:Context,username:String)= withContext(Dispatchers.IO)
    {
        Utils.writeAll(contentResolver.openInputStream(keyfile!!)!!,FileOutputStream(SFTPRoot.getKeyFile(context,username)))
    }

    fun importKeyfile(view:View)
    {
        Log.i(TAG,"Import keyfile $view")
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply()
        {
            addCategory(Intent.CATEGORY_OPENABLE)
            type="*/*"
        }
        startActivityForResult(intent,OPEN_KEYFILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        Log.i(TAG,"OnActivityResult $requestCode $resultCode $data")
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==OPEN_KEYFILE)
        {
            if(resultCode== RESULT_OK)
            {
                (findViewById<View>(R.id.password) as EditText).setText(R.string.keyfile_value)
                keyfile=data!!.data!!
            }
        }
    }
}