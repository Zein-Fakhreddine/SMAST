package com.smast.zeinf.smast.activities


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.smast.zeinf.smast.activities.WebUtils.fromHtml
import com.zeinfakhreddine.summarizer.Summarizer
import java.io.InputStream
import java.net.URL

class Summary(val post: RedditPost) {

    var content: String? = null
    var topImage: Bitmap? = null
    var loadImageFunc: ((Int) -> Unit)? = null
    val bmOptions = BitmapFactory.Options()

    init {
        loadImage()
        bmOptions.inSampleSize = 2
    }

    fun loadContent(func: (Int) -> Unit) {
        BackgroundTask(preFunc = {}, backFunc = {
            var result: Int = 0

            content = Summarizer(post.url, title = post.title).summary?.fromHtml()

            if (content != null)
                result = 1

            result
        }, postFunc = func)
    }

    private fun loadImage() {
        BackgroundTask(backFunc = {
            var result: Int = 0
            try {
                val url = URL(post.mediaUrl)
                topImage = BitmapFactory.decodeStream(url.content as InputStream,
                        null, bmOptions)
                result = 1
            } catch (e: Exception) {
                e.printStackTrace()
            }

            result
        }, postFunc = { if (loadImageFunc != null && it == 1) (loadImageFunc as (Int) -> Unit)(summaries.indexOf(this@Summary)) })
    }

    data class RedditPost(var title: String, var url: String, var subReddit: String, var domain: String, var mediaUrl: String)

    companion object {
        var summaries = ArrayList<Summary>()
        var clickedPosition: Int = 0
    }

}