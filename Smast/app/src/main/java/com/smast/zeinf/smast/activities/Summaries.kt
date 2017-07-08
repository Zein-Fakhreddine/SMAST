package com.smast.zeinf.smast.activities


import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*
import com.smast.zeinf.smast.R
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator

class Summaries : AppCompatActivity() {

    //Items
    private var summary_view: RecyclerView? = null
    private var drawerListView: ListView? = null
    private var articleAdapter: RecyclerViewAdapter? = null
    private var drawerAdapter: DrawerViewAdapter? = null
    private var pbLoading: ProgressBar? = null
    private var mDrawerLayout: DrawerLayout? = null
    private var posts: ArrayList<Summary.RedditPost> = ArrayList()
    private var websiteIteration = 0

    fun Int.convertToBitmap(): Bitmap = BitmapFactory.decodeResource(resources, this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summaries)
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
            R.id.action_reset -> reload()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun reload() {
        websiteIteration = 0
        loadWebsites()
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
                    Summary.summaries = ArrayList<Summary>()
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

    private fun loadSummary() {
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
                websiteIteration++
                loadSummary()
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
