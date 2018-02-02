package com.example.musicplayer

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
import java.util.*

class MusicService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    //media player
    private lateinit var player: MediaPlayer
    //song list
    private lateinit var songs: ArrayList<Song>
    //current position
    private var songPosn: Int = 0
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
        songPosn = 0
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
        player.setAudioStreamType(AudioManager.STREAM_MUSIC)
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
        val playSong = songs[songPosn]
        //get title
        songTitle = playSong.getTitle()
        //get id
        val currSong = playSong.getID()
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
    }

    //set the song
    fun setSong(songIndex: Int) {
        songPosn = songIndex
    }

    //playback methods
    fun getPosn(): Int {
        return player.currentPosition
    }

    fun getDur(): Int {
        return player.duration
    }

    fun isPng(): Boolean {
        return player.isPlaying
    }

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
        songPosn--
        if (songPosn < 0) songPosn = songs.size - 1
        playSong()
    }

    //skip to next
    fun playNext() {
        if (shuffle) {
            var newSong = songPosn
            while (newSong == songPosn) {
                newSong = rand!!.nextInt(songs.size)
            }
            songPosn = newSong
        } else {
            songPosn++
            if (songPosn >= songs.size) songPosn = 0
        }
        playSong()
    }

    //toggle shuffle
    fun setShuffle() {
        shuffle = !shuffle
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPrepared(mp: MediaPlayer?) {
        val NOTIF_ID = "com.example.musicplayer"
        val NOTIF_NAME = "com.example.musicplayer.notifyController"
        val NOTIF_DESC = "com.example.musicplayer.notificationPlaybackController"

        //start playback
        mp!!.start()
        //notification
        val notIntent = Intent(this, MainActivity::class.java)
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIF_ID)
        } else {
            Notification.Builder(this)
        }

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.ic_play_arrow)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle)
        val not = builder.build()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notifyManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notifyChannel = NotificationChannel(NOTIF_ID, NOTIF_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            notifyChannel.description = NOTIF_DESC
            // Disable Light, Vibration, Sound when notification start.
            notifyChannel.enableLights(false)
            notifyChannel.enableVibration(false)
            notifyChannel.setSound(null, null)
            assert(notifyManager != null)
            notifyManager.createNotificationChannel(notifyChannel)
        }

        startForeground(NOTIFY_ID, not)
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCompletion(mp: MediaPlayer?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBind(intent: Intent): IBinder? {
        return musicBind
    }

    override fun onUnbind(intent: Intent): Boolean {
        player.stop()
        player.release()
        return false
    }

    //binder
    inner class LocalBinder : Binder() {
        fun getService() : MusicService? {
            return this@MusicService
        }
    }
}
