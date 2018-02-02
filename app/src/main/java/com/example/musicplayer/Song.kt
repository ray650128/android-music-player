package com.example.musicplayer

/**
 * Created by ray650128 on 2018/2/1.
 *
 */

class Song(thisId: Long, thisTitle: String, thisArtist: String) {
    private var id = thisId
    private var title = thisTitle
    private var artist = thisArtist

    fun getID(): Long {
        return id
    }

    fun getTitle(): String {
        return title
    }

    fun getArtist(): String {
        return artist
    }
}
