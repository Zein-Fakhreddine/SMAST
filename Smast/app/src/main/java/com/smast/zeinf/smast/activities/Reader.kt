package com.smast.zeinf.smast.activities


import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.liuguangqiang.swipeback.SwipeBackActivity
import com.liuguangqiang.swipeback.SwipeBackLayout
import com.smast.zeinf.smast.R

class Reader : SwipeBackActivity() {

    private var txtContent: TextView? = null
    private var pbLoading: ProgressBar? = null
    private val summary = Summary.summaries[Summary.clickedPosition]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)
        setDragEdge(SwipeBackLayout.DragEdge.LEFT)
        initView()
    }

    private fun initView() {
        val topImage = findViewById(R.id.imgTopImage) as ImageView
        topImage.setImageBitmap(summary.topImage)
        val txtTitle = findViewById(R.id.txtTitle) as TextView
        txtTitle.text = summary.post.title
        txtContent = findViewById(R.id.txtContent) as TextView
        txtContent?.movementMethod = ScrollingMovementMethod()
        pbLoading = findViewById(R.id.pbLoading) as ProgressBar
        pbLoading?.visibility = View.INVISIBLE
        loadSummaryContent()
    }

    private fun loadSummaryContent() {
        if (summary.content == null) {
            pbLoading?.visibility = View.VISIBLE
            summary.loadContent {
                pbLoading?.visibility = View.GONE
                if (it == 1)
                    txtContent?.text = summary.content
                else {
                    val error = Snackbar.make(findViewById(R.id.cord), "Could not load summary!", Snackbar.LENGTH_INDEFINITE).setAction("Open Article") { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(summary.post.url))) }

                    error.setActionTextColor(Color.YELLOW)
                    val sbView = error.view
                    val textView = sbView.findViewById(android.support.design.R.id.snackbar_text) as TextView
                    textView.setTextColor(Color.RED)
                    error.show()
                }
            }
        } else
            txtContent?.text = summary.content
    }

}