package com.smast.zeinf.smast.activities


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.afollestad.materialdialogs.GravityEnum
import com.afollestad.materialdialogs.MaterialDialog
import com.smast.zeinf.smast.R
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator
import java.util.*


class Summaries : AppCompatActivity() {

    //Items
    private var summary_view: RecyclerView? = null
    private var articleAdapter: RecyclerViewAdapter? = null
    private var swipeLayout: SwipeRefreshLayout? = null
    private var posts: ArrayList<Summary.RedditPost> = ArrayList()
    private var sharedPrefs: SharedPreferences? = null


    @Suppress("DEPRECATION")
    fun Int.convertToDrawable(): Drawable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            return resources.getDrawable(this, applicationContext.theme)

        return resources.getDrawable(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summaries)

        sharedPrefs = getPreferences(Context.MODE_PRIVATE)
        val subreddits = sharedPrefs?.getStringSet("subreddits", null)
        if (subreddits != null)
            WebUtils.subReddits = subreddits.toMutableList()
        initView()
        loadWebsites()
    }

    private fun initView() {
        summary_view = findViewById(R.id.recycler_view) as RecyclerView
        summary_view?.layoutManager = LinearLayoutManager(this)
        summary_view?.itemAnimator = SlideInLeftAnimator()
        swipeLayout = findViewById(R.id.swipeRefreshLayout) as SwipeRefreshLayout
        swipeLayout?.setOnRefreshListener {
            loadWebsites()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_reset -> { if(swipeLayout?.isRefreshing?.not() as Boolean) loadWebsites() }
            R.id.action_subreddits -> openSubreddits()
        }

        return super.onOptionsItemSelected(item)
    }


    private fun openSubreddits() {
        val dialog = MaterialDialog.Builder(this)
                .title("Subreddits")
                .titleGravity(GravityEnum.CENTER)
                .icon(R.drawable.ic_reddit_snoo.convertToDrawable())
                .customView(R.layout.subreddit_view, false)
                .positiveText("Reload")
                .negativeText("Cancel")
                .onPositive({ _, _ ->
                    if(swipeLayout?.isRefreshing?.not() as Boolean)
                        loadWebsites()
                })
                .show()

        val lstSubreddits = dialog.customView?.findViewById(R.id.lstSubreddits) as ListView
        val etxtSubreddit = dialog.customView?.findViewById(R.id.etxtSubreddit) as EditText
        val imgAdd = dialog.customView?.findViewById(R.id.imgAdd) as ImageView
        val pbLoading = dialog.customView?.findViewById(R.id.pbLoading) as ProgressBar

        etxtSubreddit.setOnEditorActionListener(TextView.OnEditorActionListener{_, actionId: Int, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                imgAdd.performClick()
                return@OnEditorActionListener true
            }
            return@OnEditorActionListener false
        })
        lstSubreddits.adapter = ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, WebUtils.subReddits)
        lstSubreddits.onItemClickListener = AdapterView.OnItemClickListener({ _, _, i: Int, _ ->
            if (WebUtils.subReddits.size == 1)
                return@OnItemClickListener

            WebUtils.subReddits.removeAt(i)

            val editor = sharedPrefs?.edit()
            editor?.putStringSet("subreddits", WebUtils.subReddits.toMutableSet())
            editor?.apply()

            lstSubreddits.adapter = ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, WebUtils.subReddits)
        })

        imgAdd.setOnClickListener {
            val subreddit = etxtSubreddit.text.toString()
            if (WebUtils.subReddits.contains(subreddit))
                return@setOnClickListener
            BackgroundTask(preFunc = { pbLoading.visibility = View.VISIBLE }, backFunc = {
                var result = 0
                if (WebUtils.checkSubredditExists(subreddit))
                    result = 1
                result
            }, postFunc = {
                if (it == 1) {
                    WebUtils.subReddits.add(subreddit)
                    lstSubreddits.adapter = ArrayAdapter<String>(this,
                            android.R.layout.simple_list_item_1, android.R.id.text1, WebUtils.subReddits)

                    val editor = sharedPrefs?.edit()
                    editor?.putStringSet("subreddits", WebUtils.subReddits.toMutableSet())
                    editor?.apply()
                } else {
                    etxtSubreddit.error = "Could not add Subreddit: " + subreddit
                }
                pbLoading.visibility = View.GONE
            })
        }
    }

    private fun loadWebsites() {
        if (isNetworkAvailable) {
            BackgroundTask(preFunc = { swipeLayout?.isRefreshing = true}, backFunc = {
                var result: Int = 0
                posts = WebUtils.getRedditPosts()
                if (posts.size > 0)
                    result = 1
                result
            }, postFunc = {
                swipeLayout?.isRefreshing = false
                if (it == 1) {
                    Summary.summaries.clear()
                    if (articleAdapter != null)
                        articleAdapter?.notifyDataSetChanged()
                    loadSummary()
                } else{
                    Toast.makeText(this@Summaries, "Error Finding Reddit Articles", Toast.LENGTH_SHORT).show()
                    swipeLayout?.isRefreshing = false
                }
            })
        } else {
            val error = Snackbar.make(findViewById(R.id.cord), "No Internet Connection", Snackbar.LENGTH_INDEFINITE).setAction("Open Wireless Settings") { startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)) }

            error.setActionTextColor(Color.YELLOW)
            val textView = error.view.findViewById(android.support.design.R.id.snackbar_text) as TextView
            textView.setTextColor(Color.RED)
            error.show()
        }
    }

    private val isNetworkAvailable: Boolean
        get() {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

    private fun loadSummary(websiteIteration: Int = 0) {
        var summary: Summary? = null
        BackgroundTask(preFunc = {
            swipeLayout?.isRefreshing = true
        }, backFunc = {
            var result: Int = 0
            summary = Summary(posts[websiteIteration])
            if (summary != null)
                result = 1
            result
        }, postFunc = {
            if (it == 1) {
                Summary.summaries.add(summary as Summary)
                if (articleAdapter == null) {
                    articleAdapter = RecyclerViewAdapter(Summary.summaries)
                    summary_view?.adapter = articleAdapter
                }

                summary?.loadImageFunc = { articleAdapter?.notifyItemChanged(it) }

                articleAdapter?.notifyItemInserted(websiteIteration)

                articleAdapter?.itemClickFunc = {
                    Summary.clickedPosition = it
                    startActivity(Intent(this, Reader::class.java))
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                }
            } else
                Toast.makeText(this@Summaries, "Error Loading Summary", Toast.LENGTH_SHORT).show()

            if (websiteIteration != posts.size - 1) {
                loadSummary(websiteIteration + 1)
            } else
                swipeLayout?.isRefreshing = false

        })
    }

}
