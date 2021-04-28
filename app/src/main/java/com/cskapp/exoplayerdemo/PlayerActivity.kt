/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cskapp.exoplayerdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import java.io.File
import java.io.IOException


/**
 * A fullscreen activity to play audio or video streams.
 */
class PlayerActivity : AppCompatActivity() {
    private var playbackStateListener: PlaybackStateListener? = null
    private var playerView: PlayerView? = null
    private var player: SimpleExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.video_view)
        playbackStateListener = PlaybackStateListener()
    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            //getFile()
            permissionCheck()

        }


    }


    public override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (Util.SDK_INT <= 23 || player == null) {
            //initializePlayer(VideoType.RAW)
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun initializePlayer(videoType: VideoType, uri1: Uri? = null) {
        Log.d("MEDIA 1", uri1.toString())
        if (player == null) {
            val trackSelector = DefaultTrackSelector(this)
            trackSelector.setParameters(
                    trackSelector.buildUponParameters().setMaxVideoSizeSd()
            )
            player = SimpleExoPlayer.Builder(this)
                    .setTrackSelector(trackSelector)
                    .build()
        }
        playerView!!.player = player
        player!!.playWhenReady = playWhenReady
        player!!.seekTo(currentWindow, playbackPosition)
        player!!.addListener(playbackStateListener!!)
        when (videoType) {
            VideoType.CLOUD -> {
                val mediaItem1 = MediaItem.Builder()
                        .setUri("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4")
                        .setMimeType(MimeTypes.APPLICATION_MPD)
                        .build()
                val mediaItem = MediaItem.Builder()
                        .setUri(getString(R.string.media_url_dash))
                        .setMimeType(MimeTypes.APPLICATION_MPD)
                        .build()
                player!!.setMediaItem(mediaItem)
                player!!.prepare()

            }
            VideoType.RAW -> {
                val uri = RawResourceDataSource.buildRawResourceUri(R.raw.video)
                val mediaItem = MediaItem.fromUri(uri)
                player!!.setMediaItem(mediaItem)
                player!!.prepare()

            }

            VideoType.SD_CARD -> {
                val uri2 = Uri.parse(uri1.toString())
                val audioSource = ExtractorMediaSource(
                        uri2,
                        DefaultDataSourceFactory(this, "MyExoplayer"),
                        DefaultExtractorsFactory(),
                        null,
                        null
                )
                //  player!!.prepare(audioSource);
                val mediaItem = MediaItem.Builder()
                        .setUri(getString(R.string.media_url_dash))
                        .setMimeType(MimeTypes.APPLICATION_MPD)
                        .build()
                player!!.setMediaItem(MediaItem.fromUri(uri2))
                player!!.prepare()

            }

        }


    }

    private fun buildMediaSource(sampleUrl: String): MediaSource? {
        val dataSourceFactory = DefaultDataSourceFactory(this, "sample")
        return ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(""))
    }

    private fun releasePlayer() {
        if (player != null) {
            playbackPosition = player!!.currentPosition
            currentWindow = player!!.currentWindowIndex
            playWhenReady = player!!.playWhenReady
            player!!.removeListener(playbackStateListener!!)
            player!!.release()
            player = null
        }
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        playerView!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    private inner class PlaybackStateListener : Player.EventListener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String
            stateString = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
            Log.d(TAG, "changed state to $stateString")
        }
    }

    companion object {
        private val TAG = PlayerActivity::class.java.name
    }

    private fun getFile() {
        try {
            val contentUri = MediaStore.Files.getContentUri("external")
            val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"
            val selectionArgs = arrayOf(Environment.DIRECTORY_DOWNLOADS + "/video/")
            val cursor = contentResolver.query(contentUri, null, selection, selectionArgs, null)
            var uri: Uri? = null
            if (cursor!!.count == 0) {
                Toast.makeText(
                        this,
                        "No file found in \"" + Environment.DIRECTORY_DOWNLOADS + "/video/\"",
                        Toast.LENGTH_LONG
                ).show()
            } else {
                while (cursor.moveToNext()) {
                    val fileName =
                            cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
                    if (fileName == "test_video.mp4") {
                        val id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
                        uri = ContentUris.withAppendedId(contentUri, id)
                        Toast.makeText(this, uri.path, Toast.LENGTH_SHORT).show()
                        break
                    }
                }
                if (uri == null) {
                    Toast.makeText(this, "Video not found", Toast.LENGTH_SHORT).show()
                    val path = getString(R.string.media_url_dash)
                    //  initializePlayer(VideoType.CLOUD, Uri.parse(path))
                } else {
                    try {
                        initializePlayer(VideoType.SD_CARD, uri)
                    } catch (e: IOException) {
                        Toast.makeText(this, "Fail to read file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun buildMediaSourceNew(uri: Uri, buildType: Int): MediaSource? {
        val datasourceFactroy: DataSource.Factory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "Your App Name"))
        return ExtractorMediaSource.Factory(datasourceFactroy).createMediaSource(uri)
    }

    private fun permissionCheck() {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        110)

            } else {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        110)
            }
        } else {
            //  getFile()
            val m = getAllMedia()
           // initializePlayer(VideoType.SD_CARD, Uri.parse(m?.get(4)))
            if (m != null) {
                for ( video in m){
                    Log.d("MEDIA", video.toString())
                    if (video?.contains("Explain Tenet like Im 5 _v240P.mp4") == true){
                       initializePlayer(VideoType.SD_CARD, Uri.parse(video))
                        break
                    }
                }
            }
             Log.d("MEDIA", m?.size.toString() + " Videos")
/*
            if (hasExternalStoragePrivateFile()) {
                val file = File(getExternalFilesDir(null), "test_video.mp4")
                // initializePlayer(VideoType.SD_CARD, Uri.parse(file.absolutePath))
            }
*/

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            110 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                   // getFile()
                } else {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Permission")
                            .setMessage("Permission")
                            .setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                            }.setPositiveButton("Ok") { dialog, _ ->
                                dialog.dismiss()
                            }.show()
                }
                return
            }
        }
    }

    fun getAllMedia(): ArrayList<String?>? {
        val videoItemHashSet: HashSet<String> = HashSet()
        val projection = arrayOf(MediaStore.Video.VideoColumns.DATA, MediaStore.Video.Media.DISPLAY_NAME)
        val cursor: Cursor? = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)
        try {
            cursor?.moveToFirst()
            if (cursor != null) {
                do {
                    videoItemHashSet.add(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)))
                } while (cursor.moveToNext())
            }
            cursor?.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return ArrayList(videoItemHashSet)
    }

    fun hasExternalStoragePrivateFile(): Boolean {
        // Get path for the file on external storage.  If external
        // storage is not currently mounted this will fail.
        val file = File(getExternalFilesDir(null), "test_video.mp4")
        initializePlayer(VideoType.SD_CARD, Uri.parse(file.absolutePath))

        return file.exists()
    }
}
