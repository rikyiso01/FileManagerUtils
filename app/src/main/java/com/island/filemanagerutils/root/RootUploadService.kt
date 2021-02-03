package com.island.filemanagerutils.root

import android.net.Uri
import com.island.filemanagerutils.R
import com.island.filemanagerutils.utils.AbstractRoot
import com.island.filemanagerutils.utils.UploadService

class RootUploadService:UploadService()
{
    override fun getRoot(uri: Uri): AbstractRoot
    {
        return RootRoot()
    }

    override fun notificationId(): Int
    {
        return resources.getInteger(R.integer.root_upload_notification_id)
    }

    override fun channelName(): String
    {
        return getString(R.string.root_upload_channel_name)
    }

    override fun channelId(): String
    {
        return getString(R.string.root_upload_channel_id)
    }

    override fun title(): String
    {
        return getString(R.string.root_upload_notification_title)
    }

    override fun icon(): Int
    {
        return R.drawable.root_upload
    }
}