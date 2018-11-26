package com.example.musicplayer.view

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.TranslateAnimation
import android.widget.MediaController

/**
 * Created by ray650128 on 2018/2/1.
 */

class MusicController(c: Context) : MediaController(c, false) {

    private var parentView: View? = null
    private var mContext: Context = c

    override fun setAnchorView(view: View?) {
        super.setAnchorView(view)
        parentView = view
        Log.e("Layout", "${this.layoutParams.height}")
    }

    override fun show(timeout: Int) {
        super.show(timeout)

        val params = parentView!!.layoutParams
        val view = parentView!!.parent as ViewGroup
        val parentHeight = view.height
        params.height = (parentHeight * 0.15).toInt()

        parentView!!.layoutParams = params
    }

    override fun hide() {
        super.hide()

        val params = parentView!!.layoutParams
        params.height = 0

        parentView!!.layoutParams = params
    }
}
