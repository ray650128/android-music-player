package com.example.musicplayer.controller

object NoticeCenter {

    private var mNoticeCenter: NoticeCenter? = null

    val instance: NoticeCenter
        get() {
            if (null == mNoticeCenter) {
                mNoticeCenter = NoticeCenter
                mNoticeCenter!!.init()
            }
            return mNoticeCenter!!
        }

    private var mOnDataChangedListener: ArrayList<OnDataChangedListener>? = null

    private fun init() {
        mOnDataChangedListener = ArrayList()
    }

    //observe pattern
    interface OnDataChangedListener {
        fun onDataChanged(msg: String)
    }

    fun addOnDataChangedListener(listener: OnDataChangedListener) {
        mOnDataChangedListener!!.add(listener)
    }

    fun removeOnDataChangedListener(listener: OnDataChangedListener) {
        mOnDataChangedListener!!.remove(listener)
    }

    fun notifyDataChanged(msg: String) {
        for (listener in mOnDataChangedListener!!) {
            listener.onDataChanged(msg)
        }
    }
}