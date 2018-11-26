package com.example.musicplayer.view

import android.content.Context
import android.graphics.Color
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.musicplayer.R
import com.example.musicplayer.model.Song


class PlaylistAdapter(private var songList: ArrayList<Song>) : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {

    private var selectedItem: Int = 0

    private lateinit var mContext: Context

    private var mOnItemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    fun setOnItemClickListener(mOnItemClickListener: OnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.song, parent, false)

        mContext = parent.context
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return songList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.songTitle.text = songList[position].title
        holder.songArtist.text = songList[position].artist


        if (position == selectedItem) {
            holder.itemView.background = AppCompatResources.getDrawable(mContext, R.drawable.recyclerview_select)
            holder.songTitle.setTextColor(Color.WHITE)
            holder.songArtist.setTextColor(Color.WHITE)
        } else {
            holder.itemView.background = AppCompatResources.getDrawable(mContext, R.drawable.recyclerview_unselect)
            holder.songTitle.setTextColor(Color.parseColor("#FF333333"))
            holder.songArtist.setTextColor(Color.parseColor("#FF333333"))
        }

        if (mOnItemClickListener != null) {
            //為ItemView設置監聽器
            holder.itemView.setOnClickListener {
                val layoutPosition = holder.layoutPosition // 1
                mOnItemClickListener!!.onItemClick(holder.itemView, layoutPosition) // 2
                selectedItem = layoutPosition

                //holder.itemView.background = AppCompatResources.getDrawable(mContext, R.drawable.recyclerview_select)
                notifyDataSetChanged()
            }
        }
    }

    fun setSelectedItem(pos: Int) {
        selectedItem = pos
        notifyDataSetChanged()
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var songTitle: TextView = v.findViewById(R.id.song_title)
        var songArtist: TextView = v.findViewById(R.id.song_artist)

    }
}