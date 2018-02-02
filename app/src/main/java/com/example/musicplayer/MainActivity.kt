package com.example.musicplayer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.MediaController
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.Manifest.permission.*
import android.support.v7.app.AlertDialog


class MainActivity : AppCompatActivity(), MediaController.MediaPlayerControl {

    private lateinit var songList: ArrayList<Song>

    //service
    private var musicSrv: MusicService? = null
    private var playIntent: Intent? = null
    //binding
    private var musicBound = false

    //controller
    private var controller: MusicController? = null

    //activity and playback pause flags
    private var paused = false
    var playbackPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //setup controller
        setController()
    }

    //start and bind the service when the activity starts
    override fun onStart() {
        super.onStart()

        checkLocationPermission()
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onResume() {
        super.onResume()
        if (paused) {
            setController()
            paused = false
        }
    }

    override fun onStop() {
        controller!!.hide()
        super.onStop()
    }

    override fun onDestroy() {
        stopService(playIntent)
        musicSrv = null
        super.onDestroy()
    }

    override fun canPause(): Boolean {
        return true
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun getAudioSessionId(): Int {
        return 0
    }

    override fun getBufferPercentage(): Int {
        return 0
    }

    override fun getCurrentPosition(): Int {
        return if (musicSrv != null && musicBound && musicSrv!!.isPng())
            musicSrv!!.getPosn()
        else
            0
    }

    override fun getDuration(): Int {
        return if (musicSrv != null && musicBound && musicSrv!!.isPng())
            musicSrv!!.getDur()
        else
            0
    }

    override fun isPlaying(): Boolean {
        return if (musicSrv != null && musicBound) musicSrv!!.isPng() else false
    }

    override fun pause() {
        playbackPaused = true
        musicSrv!!.pausePlayer()
    }

    override fun seekTo(pos: Int) {
        musicSrv!!.seek(pos)
    }

    override fun start() {
        musicSrv!!.go()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //menu item selected
        when (item.itemId) {
            R.id.btnShuffle -> musicSrv!!.setShuffle()
            R.id.btnEnd -> {
                stopService(playIntent)
                musicSrv = null
                System.exit(0)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 取得權限 Do something...
                    initPlayList()
                } else {
                    // 使用者拒絕，顯示對話框告知
                    AlertDialog.Builder(this)
                            .setMessage("要使用定位功能，必須先允許定位權限")
                            .setPositiveButton("確定", null)
                            .show()
                }
                return
            }
        }
    }

    //connect to the service
    private val musicConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MusicService.LocalBinder
            //get service
            musicSrv = binder.getService()
            //pass list
            musicSrv!!.setList(songList)
            musicBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            musicBound = false
        }
    }

    private fun initPlayList() {

        songList = ArrayList()

        getSongList()

        Collections.sort(songList) { a, b ->
            a.getTitle().compareTo(b.getTitle())
        }

        //create and set adapter
        val songAdt = SongAdapter(this, songList)
        song_list.adapter = songAdt


        // Start Service
        if (playIntent == null) {
            playIntent = Intent(this, MusicService::class.java)
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE)
            startService(playIntent)
        }
    }

    //user song select
    fun songPicked(view: View) {
        musicSrv!!.setSong(Integer.parseInt(view.tag.toString()))
        musicSrv!!.playSong()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        controller!!.show(0)
    }

    private fun checkLocationPermission() {
        val permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // 未取得權限
            ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE),
                    1)
        } else {
            // 已取得權限 Do something...
            initPlayList()
        }
    }

    private fun getSongList() {
        val musicResolver = contentResolver
        val musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val musicCursor = musicResolver.query(musicUri, null, null, null, null)

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            val titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE)
            val idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID)
            val artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST)
            //add songs to list
            do {
                val thisId = musicCursor.getLong(idColumn)
                val thisTitle = musicCursor.getString(titleColumn)
                val thisArtist = musicCursor.getString(artistColumn)
                songList.add(Song(thisId, thisTitle, thisArtist))
            } while (musicCursor.moveToNext())

            musicCursor.close()
        }
    }

    //set the controller up
    private fun setController() {
        controller = MusicController(this)
        //set previous and next button listeners
        controller!!.setPrevNextListeners({ playNext() }, { playPrev() })
        //set and show
        controller!!.setMediaPlayer(this)
        controller!!.setAnchorView(findViewById(R.id.song_list))
        controller!!.isEnabled = true
    }

    private fun playNext() {
        musicSrv!!.playNext()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        controller!!.show(0)
    }

    private fun playPrev() {
        musicSrv!!.playPrev()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        controller!!.show(0)
    }
}
