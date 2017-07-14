package com.smast.zeinf.smast.activities

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.smast.zeinf.smast.R
import java.util.*

class RecyclerViewAdapter(val summaries: ArrayList<Summary>) : RecyclerView.Adapter<RecyclerViewAdapter.CustomViewHolder>() {

    var itemClickFunc: ((position: Int) -> Unit)? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): CustomViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.summary_view, null)
        return CustomViewHolder(view)
    }

    override fun onBindViewHolder(customViewHolder: CustomViewHolder, i: Int) {
        val summary = summaries[i]

        customViewHolder.txtTitle.text = summary.post.title
        customViewHolder.txtInfo.text = "${summary.post.subReddit} ● ${summary.post.domain.split(".")[0]}"
        if (summary.topImage != null)
            customViewHolder.imgTopImage.setImageBitmap(summary.topImage)
        val listener = View.OnClickListener {
            if (itemClickFunc != null)
                itemClickFunc?.invoke(i)
        }
        customViewHolder.imgTopImage.setOnClickListener(listener)
        customViewHolder.txtTitle.setOnClickListener(listener)
    }

    override fun getItemCount(): Int {
        return summaries.size
    }

    inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgTopImage: ImageView = view.findViewById(R.id.imgTopImage) as ImageView
        val txtTitle: TextView = view.findViewById(R.id.txtTitle) as TextView
        val txtInfo: TextView = view.findViewById(R.id.txtInfo) as TextView
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
}
