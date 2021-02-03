package com.island.filemanagerutils.sftp

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SFTPAccountService:Service()
{
    private val authenticator=SFTPAccountAuthenticator(this)
    override fun onBind(intent: Intent?): IBinder?
    {
        return authenticator.iBinder
    }
}