package com.island.filemanagerutils.sftp

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import com.island.filemanagerutils.R
import com.island.filemanagerutils.utils.Utils
import java.io.ByteArrayOutputStream
import java.io.FileInputStream


class SFTPAccountAuthenticator(private val context: Context): AbstractAccountAuthenticator(context)
{
    private val TAG="SFTPAccountAuthenticate"

    override fun editProperties(
        response: AccountAuthenticatorResponse?,
        accountType: String?
    ): Bundle
    {
        throw NotImplementedError()
    }

    override fun addAccount(response: AccountAuthenticatorResponse?, accountType: String?,
                            authTokenType: String?, requiredFeatures: Array<out String>?,
                            options: Bundle?): Bundle
    {
        Log.i(TAG, "AddAccount $response $accountType $authTokenType $requiredFeatures $options")
        val intent=Intent(context, SFTPAuthenticationActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, context.getString(R.string.account_type))
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun confirmCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        options: Bundle?
    ): Bundle
    {
        throw NotImplementedError()
    }

    override fun getAuthToken(response: AccountAuthenticatorResponse?, account: Account?,
                              authTokenType: String?, options: Bundle?): Bundle
    {
        Log.i(TAG, "GetAuthToken $response $account $authTokenType $options")
        val accountManager=AccountManager.get(context)
        val authToken=accountManager.peekAuthToken(account,authTokenType)
        val result=Bundle()
        result.putString(AccountManager.KEY_ACCOUNT_NAME,account!!.name)
        result.putString(AccountManager.KEY_ACCOUNT_TYPE,account.type)
        if(authToken==null)
        {
            if(authTokenType==context.getString(R.string.sftp_login_token_type))
            {
                result.putString(AccountManager.KEY_AUTHTOKEN,accountManager.getPassword(account))
            }
            else if(authTokenType==context.getString(R.string.sftp_keyfile_token_type))
            {
                val file=SFTPRoot.getKeyFile(context,account.name)
                val bytes=ByteArrayOutputStream()
                Utils.writeAll(FileInputStream(file),bytes)
                result.putByteArray(AccountManager.KEY_AUTHTOKEN,bytes.toByteArray())
            }
        }
        else result.putString(AccountManager.KEY_AUTHTOKEN,authToken)
        return result
    }

    override fun getAuthTokenLabel(authTokenType: String?): String
    {
        throw NotImplementedError()
    }

    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle
    {
        throw NotImplementedError()
    }

    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ): Bundle
    {
        throw NotImplementedError()
    }

    override fun getAccountRemovalAllowed(response: AccountAuthenticatorResponse?, account: Account?): Bundle
    {
        Log.i(TAG,"GetAccountRemovalAllowed $response $account")
        context.contentResolver.notifyChange(DocumentsContract.buildRootsUri(context.getString(R.string.sftp_authority)),null)
        return super.getAccountRemovalAllowed(response,account)
    }
}