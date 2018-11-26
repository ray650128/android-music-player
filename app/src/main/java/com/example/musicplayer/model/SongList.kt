package com.example.musicplayer.model

import android.content.Context
import android.provider.MediaStore
import android.view.View
import com.example.musicplayer.view.PlaylistAdapter

object SongList {
    var playlist = ArrayList<Song>()

    var currentPosition = 0

    fun getPlayList(context: Context) {
        playlist.clear()

        //retrieve song info
        val musicResolver = context.contentResolver
        val musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.Audio.Media.IS_MUSIC
        val sortOrder = MediaStore.Audio.Media.DISPLAY_NAME

        // 經由ContentProvider來取得外部儲存媒體上的音樂檔案的情報
        val musicCursor = musicResolver.query(musicUri, null, selection, null, sortOrder)

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            val idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            //add songs to list
            do {
                val thisId = musicCursor.getLong(idColumn)
                val thisTitle = musicCursor.getString(titleColumn)
                val thisArtist = musicCursor.getString(artistColumn)

                val song = Song()
                song.id = thisId
                song.title = thisTitle
                song.artist = thisArtist

                playlist.add(song)
            } while (musicCursor.moveToNext())
        }

        musicCursor.close()

        // 排列由A~Z
        playlist.sortWith(Comparator { a, b ->
            a.title.compareTo(b.title)
        })
    }
}