package com.island.filemanagerutils.utils

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream

class Utils
{
    companion object
    {
        fun writeAll(inp: InputStream, out: OutputStream, callback: (written:Int)->Unit={})
        {
            val input= BufferedInputStream(inp)
            val output= BufferedOutputStream(out)
            val buffer= ByteArray(1024)
            var written=0
            while (true)
            {
                val length=input.read(buffer)
                if(length==-1)break
                output.write(buffer,0,length)
                written+=length
                callback(length)
            }
            input.close()
            output.close()
        }
    }
}