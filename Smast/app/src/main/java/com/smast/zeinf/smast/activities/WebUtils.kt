package com.smast.zeinf.smast.activities


import android.text.Html
import com.smast.zeinf.smast.BuildConfig
import org.json.JSONException
import org.json.JSONObject
import java.net.URL
import java.util.*

object WebUtils {

    private val SMMRY_API_KEY = BuildConfig.SMMRY_API_KEY
    private val SENTENCE_LENGTH = 5
    private val ARTICLE_LENGTH = 25
    private val subReddits = arrayOf("worldnews", "news")

    @Suppress("DEPRECATION")
    fun String.fromHtml() : String{
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            return Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
        else
            return Html.fromHtml(this).toString()
    }

    fun getRedditPosts(): ArrayList<Summary.RedditPost> {
        val posts = ArrayList<Summary.RedditPost>()
        val children = JSONObject(URL("https://www.reddit.com/r/${subReddits.joinToString(separator = "+")}/hot.json").readText()).getJSONObject("data").getJSONArray("children")
        for (i in 0.rangeTo((children.length() - 1))) {
            val data = children.getJSONObject(i).getJSONObject("data")
            try {
                posts.add(Summary.RedditPost(data.getString("title").fromHtml(), data.getString("url"), data.getString("subreddit"), data.getString("domain"), data.getJSONObject("preview").getJSONArray("images").getJSONObject(0).getJSONObject("source").getString("url")))
            } catch (e: JSONException) {
                e.printStackTrace()
                continue
            }
            if (posts.size == ARTICLE_LENGTH)
                continue
        }
        return posts
    }

    fun getSummary(URL: String): String {
        return URL("http://api.smmry.com/&SM_API_KEY=$SMMRY_API_KEY&SM_LENGTH=$SENTENCE_LENGTH&SM_URL=$URL").readText()
    }
}