package com.smast.zeinf.smast.activities


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.net.URL

class Summary(val post: RedditPost) {
    var content: String? = null
    var topImage: Bitmap? = null
    var loadImageFunc: ((Int) -> Unit)? = null

    init {
        loadImage()
    }

    fun loadContent(func: (Int) -> Unit) {
        BackgroundTask(preFunc = {}, backFunc = {
            var result: Int = 0
            try {
                val smrryJSON = JSONObject(WebUtils.getSummary(post.url))
                content = smrryJSON.getString("sm_api_content")
                result = 1
            } catch (e: Exception) {
                e.printStackTrace()
            }

            result
        }, postFunc = func)
    }

    private fun loadImage() {
        BackgroundTask(preFunc = {}, backFunc = {
            var result: Int = 0
            try {
                val url = URL(post.mediaUrl)
                topImage = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                result = 1
            } catch (e: Exception) {
                e.printStackTrace()
            }

            result
        }, postFunc = { if (loadImageFunc != null && it == 1) (loadImageFunc as (Int) -> Unit).invoke(summaries.indexOf(this@Summary)) })
    }

    data class RedditPost(var title: String, var url: String, var subReddit: String, var domain: String, var mediaUrl: String)

    companion object {
        var summaries = ArrayList<Summary>()
        var clickedPosition: Int = 0
    }
}