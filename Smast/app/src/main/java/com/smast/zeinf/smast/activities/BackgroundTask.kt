package com.smast.zeinf.smast.activities

import android.os.AsyncTask

class BackgroundTask(val preFunc: (() -> Unit)?, val backFunc: () -> Int, val postFunc: ((result: Int) -> Unit)?) : AsyncTask<Void, Void, Int>() {

    init{
        execute()
    }

    override fun onPreExecute() {
        preFunc?.invoke()
    }

    override fun doInBackground(vararg params: Void?): Int {
        return backFunc()
    }

    override fun onPostExecute(result: Int) {
        if(postFunc != null)
            return postFunc.invoke(result)

    }
}