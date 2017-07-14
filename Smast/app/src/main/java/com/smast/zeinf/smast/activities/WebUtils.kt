package com.smast.zeinf.smast.activities


import android.text.Html
import org.json.JSONException
import org.json.JSONObject
import java.net.URL

object WebUtils {

    private val ARTICLE_LENGTH = 25
    var subReddits = arrayOf("worldnews", "news").toMutableList()


    @Suppress("DEPRECATION")
    fun String.fromHtml(): String {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            return Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
        else
            return Html.fromHtml(this).toString()
    }

    fun getRedditPosts(): ArrayList<Summary.RedditPost> {
        val posts = ArrayList<Summary.RedditPost>()
        try{
            val children = JSONObject(URL("https://www.reddit.com/r/${subReddits.joinToString(separator = "+")}/hot.json").readText()).getJSONObject("data").getJSONArray("children")
            for (i in 0.rangeTo((children.length() - 1))) {
                val data = children.getJSONObject(i).getJSONObject("data")
                try {
                    posts.add(Summary.RedditPost(data.getString("title").fromHtml(), data.getString("url"), data.getString("subreddit"), data.getString("domain"), data.getJSONObject("preview").getJSONArray("images").getJSONObject(0).getJSONObject("source").getString("url")))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                if (posts.size == ARTICLE_LENGTH)
                    continue
            }
        } catch (e: Exception){
            e.printStackTrace()
        }

        return posts
    }

    fun checkSubredditExists(subReddit: String): Boolean {
        try {
            return JSONObject(URL("https://www.reddit.com/r/$subReddit/hot.json").readText()).getJSONObject("data").getJSONArray("children").length() != 0
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return false
    }
}

