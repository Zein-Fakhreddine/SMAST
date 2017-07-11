package com.smast.zeinf.smast.activities


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.util.Log
import android.view.*
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
    private var drawerListView: ListView? = null
    private var articleAdapter: RecyclerViewAdapter? = null
    private var drawerAdapter: DrawerViewAdapter? = null
    private var pbLoading: ProgressBar? = null
    private var mDrawerLayout: DrawerLayout? = null
    private var posts: ArrayList<Summary.RedditPost> = ArrayList()
    private var sharedPrefs: SharedPreferences? = null

    fun Int.convertToBitmap(): Bitmap = BitmapFactory.decodeResource(resources, this)
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
        val drawerItems = arrayOf(DrawerItem(android.R.drawable.ic_media_pause.convertToBitmap(), "Test"))
        summary_view = findViewById(R.id.recycler_view) as RecyclerView
        summary_view?.layoutManager = LinearLayoutManager(this)
        summary_view?.itemAnimator = SlideInLeftAnimator()
        drawerListView = findViewById(R.id.navList) as ListView
        drawerAdapter = DrawerViewAdapter(this, drawerItems)
        drawerListView?.adapter = drawerAdapter
        pbLoading = findViewById(R.id.pbLoading) as ProgressBar
        mDrawerLayout = findViewById(R.id.drawer_layout) as DrawerLayout
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_reset -> loadWebsites()
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
                return@OnEditorActionListener true;
            }
            return@OnEditorActionListener false;
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
            BackgroundTask(preFunc = { pbLoading?.visibility = View.VISIBLE }, backFunc = {
                var result: Int = 0
                posts = WebUtils.getRedditPosts()
                if (posts.size > 0)
                    result = 1
                result
            }, postFunc = {
                pbLoading?.visibility = View.GONE
                if (it == 1) {
                    Summary.summaries.clear()
                    if (articleAdapter != null)
                        articleAdapter?.notifyDataSetChanged()
                    loadSummary()
                } else
                    Toast.makeText(this@Summaries, "Error Finding Reddit Articles", Toast.LENGTH_SHORT).show()
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
            pbLoading?.visibility = View.VISIBLE
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
                    this@Summaries.startActivity(Intent(this@Summaries, Reader::class.java))
                    this@Summaries.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                }
            } else
                Toast.makeText(this@Summaries, "Error Loading Summary", Toast.LENGTH_SHORT).show()

            if (websiteIteration != posts.size - 1) {
                loadSummary(websiteIteration + 1)
            } else
                pbLoading?.visibility = View.GONE
        })
    }

    inner class DrawerViewAdapter constructor(ctx: Context, private val items: Array<DrawerItem>) : ArrayAdapter<DrawerItem>(ctx, -1, items) {
        private var rowView: View? = null

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val inflater = context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            if (rowView == null)
                rowView = inflater.inflate(R.layout.drawer_view, parent, false)
            val txtAction = rowView?.findViewById(R.id.txtAction) as TextView
            txtAction.text = items[position].action
            val imgIcon = rowView?.findViewById(R.id.imgIcon) as ImageView
            imgIcon.setImageBitmap(items[position].icon)
            return rowView as View
        }
    }

    data class DrawerItem(val icon: Bitmap, val action: String)
}
