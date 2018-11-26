package com.example.musicplayer.controller

import android.app.*
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.annotation.RequiresApi
import android.util.Log
import com.example.musicplayer.R
import com.example.musicplayer.model.Song
import com.example.musicplayer.model.SongList
import com.example.musicplayer.view.MainActivity
import com.example.musicplayer.view.PlaylistAdapter
import java.util.*

class MusicService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    //media player
    private lateinit var player: MediaPlayer
    //song list
    private lateinit var songs: ArrayList<Song>
    //current position
    //private var songPosn: Int = 0
    //binder
    private val musicBind = LocalBinder()
    //title of current song
    private var songTitle = ""
    //notification id
    private val NOTIFY_ID = 1
    //shuffle flag and random
    private var shuffle = false
    private var rand: Random? = null

    override fun onCreate() {
        //create the service
        super.onCreate()
        //initialize position
        //songPosn = 0
        //random
        rand = Random()
        //create player
        player = MediaPlayer()
        //initialize
        initMusicPlayer()
    }

    override fun onDestroy() {
        stopForeground(true)
    }

    private fun initMusicPlayer() {
        //set player properties
        player.setWakeMode(applicationContext,
                PowerManager.PARTIAL_WAKE_LOCK)
        //player.setAudioStreamType(AudioManager.STREAM_MUSIC)
        //set listeners
        player.setOnPreparedListener(this)
        player.setOnCompletionListener(this)
        player.setOnErrorListener(this)
    }

    //pass song list
    fun setList(theSongs: ArrayList<Song>) {
        songs = theSongs
    }

    //play a song
    fun playSong() {
        //play
        player.reset()
        //get song
        val playSong = songs[SongList.currentPosition]
        //get title
        songTitle = playSong.title
        //get id
        val currSong = playSong.id
        //set uri
        val trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong)
        //set the data source
        try {
            player.setDataSource(applicationContext, trackUri)
        } catch (e: Exception) {
            Log.e("MUSIC SERVICE", "Error setting data source", e)
        }

        player.prepareAsync()

        NoticeCenter.instance.notifyDataChanged("Update Position")
    }

    //set the song
    fun setSong(songIndex: Int) {
        SongList.currentPosition = songIndex
    }

    //playback methods
    fun getPosn(): Int = player.currentPosition

    fun getDur(): Int = player.duration

    fun isPng(): Boolean = player.isPlaying

    fun pausePlayer() {
        player.pause()
    }

    fun seek(posn: Int) {
        player.seekTo(posn)
    }

    fun go() {
        player.start()
    }

    //skip to previous track
    fun playPrev() {
        SongList.currentPosition--
        if (SongList.currentPosition < 0) SongList.currentPosition = songs.size - 1
        playSong()
    }

    //skip to next
    fun playNext() {
        if (shuffle) {
            var newSong = SongList.currentPosition
            while (newSong == SongList.currentPosition) {
                newSong = rand!!.nextInt(songs.size)
            }
            SongList.currentPosition = newSong
        } else {
            SongList.currentPosition++
            if (SongList.currentPosition >= songs.size) SongList.currentPosition = 0
        }

        playSong()
    }

    //toggle shuffle
    fun setShuffle() {
        shuffle = !shuffle
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPrepared(mp: MediaPlayer?) {

        //start playback
        mp!!.start()
        //notification
        val notIntent = Intent(this, MainActivity::class.java)
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder: Notification.Builder = Notification.Builder(this, NOTIFICATION_ID)

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.ic_play_arrow)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle)
        val not = builder.build()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notifyManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notifyChannel = NotificationChannel(NOTIFICATION_ID, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            notifyChannel.description = NOTIFICATION_DESC
            // Disable Light, Vibration, Sound when notification start.
            notifyChannel.enableLights(false)
            notifyChannel.enableVibration(false)
            notifyChannel.setSound(null, null)
            notifyManager.createNotificationChannel(notifyChannel)
        }

        startForeground(NOTIFY_ID, not)
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mp?.reset()
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (mp!!.currentPosition > 0) {
            playNext()
        }
    }

    override fun onBind(intent: Intent): IBinder? = musicBind

    override fun onUnbind(intent: Intent): Boolean {
        player.stop()
        player.release()
        return false
    }

    //binder
    inner class LocalBinder : Binder() {
        fun getService(): MusicService? {
            return this@MusicService
        }
    }

    companion object {
        const val NOTIFICATION_ID = "com.example.musicplayer"
        const val NOTIFICATION_NAME = "com.example.musicplayer.notifyController"
        const val NOTIFICATION_DESC = "com.example.musicplayer.notificationPlaybackController"
    }
}
