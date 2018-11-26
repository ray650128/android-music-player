package com.example.musicplayer.view

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
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.Manifest.permission.*
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.widget.MediaController
import com.example.musicplayer.*
import com.example.musicplayer.controller.MusicService
import com.example.musicplayer.model.Song
import com.example.musicplayer.model.SongList
import com.example.musicplayer.controller.NoticeCenter

class MainActivity : AppCompatActivity(), MediaController.MediaPlayerControl {

    //Adapter and Song Array
    private lateinit var songAdaptor: PlaylistAdapter
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
    private var playbackPaused = false

    // Activity lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setController()

        showController.setOnClickListener {
            controller!!.show(5000)
        }

        NoticeCenter.instance.addOnDataChangedListener(object : NoticeCenter.OnDataChangedListener {

            override fun onDataChanged(msg: String) {
                if(msg == "Update Position") {
                    songAdaptor.setSelectedItem(SongList.currentPosition)
                    songAdaptor.notifyDataSetChanged()
                    // scroll to current position
                    song_list.scrollToPosition(SongList.currentPosition)
                }
            }
        })
    }


    override fun onStart() {
        super.onStart()

        checkPermissions()        //start and bind the service when the activity starts
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onResume() {
        super.onResume()
        if (paused) {
            //setController()
            paused = false
        }
    }

    override fun onDestroy() {
        stopService(playIntent)
        musicSrv = null
        super.onDestroy()
    }

    // MediaController implements
    override fun canPause(): Boolean = true

    override fun canSeekBackward(): Boolean = true

    override fun canSeekForward(): Boolean = true

    override fun getAudioSessionId(): Int = 0

    override fun getBufferPercentage(): Int = 0

    override fun getCurrentPosition(): Int =  if (musicSrv != null && musicBound && musicSrv!!.isPng())
            musicSrv!!.getPosn() else 0

    override fun getDuration(): Int = if (musicSrv != null && musicBound && musicSrv!!.isPng())
            musicSrv!!.getDur() else 0

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

    // ActionBar menu
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

    // Permission
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
                            .setMessage("請先允許本軟體讀取外部儲存空間的權限")
                            .setPositiveButton("確定", null)
                            .show()
                }
                return
            }
        }
    }

    // connect to the service
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
        SongList.getPlayList(this@MainActivity)

        songList = SongList.playlist

        //create and set adapter
        songAdaptor = PlaylistAdapter(songList)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        song_list.layoutManager = layoutManager
        // Show divider
        song_list.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        song_list.adapter = songAdaptor

        songAdaptor.setOnItemClickListener(object : PlaylistAdapter.OnItemClickListener{
            override fun onItemClick(view: View, position: Int) {
                songPicked(position)
            }
        })

        // Start Service
        if (playIntent == null) {
            playIntent = Intent(this, MusicService::class.java)
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE)
            startService(playIntent)
        }
    }

    //user song select
    fun songPicked(index: Int) {
        musicSrv!!.setSong(index)
        musicSrv!!.playSong()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        controller!!.show(5000)
    }

    private fun checkPermissions() {
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

    //set the controller up
    private fun setController() {
        controller = MusicController(this@MainActivity)
        //set previous and next button listeners
        controller!!.setPrevNextListeners({ playNext() }, { playPrev() })
        //set and show
        controller!!.setMediaPlayer(this)
        controller!!.setAnchorView(mediaControllerView)
        controller!!.isEnabled = true
    }

    private fun playNext() {
        musicSrv!!.playNext()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        controller!!.show(5000)
    }

    private fun playPrev() {
        musicSrv!!.playPrev()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        controller!!.show(5000)
    }
}
