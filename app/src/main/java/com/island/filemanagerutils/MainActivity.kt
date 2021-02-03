package com.island.filemanagerutils

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity()
{
    private val TAG="MainActivity"
    private val CREATE_FILE_SHORTCUT=1
    override fun onCreate(savedInstanceState: Bundle?)
    {
        Log.i(TAG,"OnCreate $savedInstanceState")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        val recyclerView=findViewById<RecyclerView>(R.id.sftp_accounts)
        recyclerView.adapter=SFTPAdapter(this)
        recyclerView.layoutManager=LinearLayoutManager(this)

        val sharedPreferences=getSharedPreferences(getString(R.string.shared_preferences),0)

        val rootSwitch=findViewById<SwitchMaterial>(R.id.root_documents_provider_switch)
        rootSwitch.isChecked=sharedPreferences.getBoolean(getString(R.string.root_enabled_preference),false)
        rootSwitch.setOnCheckedChangeListener()
        { _: CompoundButton, b: Boolean ->
            Log.i(TAG,"RootSwitchCheck $b")
            sharedPreferences.edit().putBoolean(getString(R.string.root_enabled_preference),b).apply()
            contentResolver.notifyChange(DocumentsContract.buildRootsUri(getString(R.string.root_authority)), null)
        }

        val apkSwitch=findViewById<SwitchMaterial>(R.id.apk_documents_provider_switch)
        apkSwitch.isChecked=sharedPreferences.getBoolean(getString(R.string.apk_enabled_preference),false)
        apkSwitch.setOnCheckedChangeListener()
        { _: CompoundButton, b: Boolean ->
            Log.i(TAG,"ApkSwitchCheck $b")
            sharedPreferences.edit().putBoolean(getString(R.string.apk_enabled_preference),b).apply()
            contentResolver.notifyChange(DocumentsContract.buildRootsUri(getString(R.string.apk_authority)), null)
        }
        if(!resources.getBoolean(R.bool.enable_apk_documents_provider))apkSwitch.visibility=View.GONE
        if(!(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O&&(getSystemService(ShortcutManager::class.java)).isRequestPinShortcutSupported))
            findViewById<View>(R.id.create_file_shortcut).visibility=View.GONE
    }

    fun createFileShortcut(view:View)
    {
        Log.i(TAG,"CreateFileShortcut $view")
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply()
        {
            addCategory(Intent.CATEGORY_OPENABLE)
            type="*/*"
        }
        startActivityForResult(intent,CREATE_FILE_SHORTCUT)
    }

    fun addSftpAccount(view:View)
    {
        Log.i(TAG,"AddSftpAccount $view")
        val accountManager:AccountManager= AccountManager.get(this)
        accountManager.addAccount(getString(R.string.account_type),null,null,null,this, null,null)
    }

    override fun onResume()
    {
        super.onResume()
        Log.i(TAG,"OnResume")
        (findViewById<RecyclerView>(R.id.sftp_accounts).adapter as SFTPAdapter).updateData()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        Log.i(TAG,"OnActivityResult $requestCode $resultCode $data")
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==CREATE_FILE_SHORTCUT&&Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            if(resultCode== RESULT_OK)
            {
                contentResolver.takePersistableUriPermission(data!!.data!!,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                createFileShortcut(data.data!!)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createFileShortcut(uri:Uri)
    {
        val shortcutManager=getSystemService(ShortcutManager::class.java)
        val document=DocumentFile.fromSingleUri(this,uri)!!

        val mime=document.type!!
        val icon=when(true)
        {
            "image" in mime -> R.mipmap.image_file
            "pdf" in mime -> R.mipmap.pdf_file
            else -> R.mipmap.generic_file
        }

        val shortcut=ShortcutInfo.Builder(this,uri.path!!)
            .setShortLabel(document.name!!)
            .setLongLabel(uri.path!!)
            .setIcon(Icon.createWithResource(this,icon))
            .setIntent(Intent(Intent.ACTION_VIEW).apply()
            {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                setDataAndType(uri,document.type)
            })
            .build()
        shortcutManager.requestPinShortcut(shortcut,null)
    }

    class SFTPAdapter(private val activity:Activity):RecyclerView.Adapter<SFTPAdapter.ViewHolder>()
    {
        private val TAG="SFTPAdapter"
        private val accountManager:AccountManager= AccountManager.get(activity)
        private var accounts=accountManager.getAccountsByType(activity.getString(R.string.account_type))
        class ViewHolder(view:View):RecyclerView.ViewHolder(view)
        {
            val text:TextView = view.findViewById(R.id.text)
            val button:Button = view.findViewById(R.id.button)
            val mountButton:Button=view.findViewById(R.id.mount_button)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
        {
            Log.i(TAG,"OnCreateViewHolder $parent $viewType")
            val view=LayoutInflater.from(parent.context).inflate(R.layout.sftp_item,parent,false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int)
        {
            Log.i(TAG,"OnBindViewHolder $holder $position")
            val account=accounts[position]
            holder.text.text=account.name
            holder.button.setOnClickListener()
            @Suppress("deprecation")
            {
                accountManager.removeAccount(account, {updateData()},null)
            }
            if(activity.getSharedPreferences(activity.getString(R.string.shared_preferences),0).getBoolean(account.name,true))
            {
                holder.mountButton.visibility=View.GONE
            }
            else
            {
                holder.mountButton.visibility=View.VISIBLE
                holder.mountButton.setOnClickListener()
                {
                    activity.getSharedPreferences(activity.getString(R.string.shared_preferences),0).edit().putBoolean(account.name,true).apply()
                    activity.contentResolver.notifyChange(DocumentsContract.buildRootsUri(activity.getString(R.string.sftp_authority)),null)
                    updateData()
                }
            }
        }

        override fun getItemCount(): Int
        {
            Log.i(TAG,"GetItemCount")
            return accounts.size
        }

        fun updateData()
        {
            accounts=accountManager.getAccountsByType(activity.getString(R.string.account_type))
            notifyDataSetChanged()
        }
    }
}