package com.gc.triprec;

import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

public class PlaybackVideoActivity extends AppCompatActivity implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener{
    private VideoView m_videoView;
    private MediaController m_mediaController;
    private int m_position = -1;
    private static final String TAG = "PlaybackVideoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback_video);
        m_mediaController = new MediaController(this);
        m_videoView = findViewById(R.id.video_view);
        m_videoView.setMediaController(m_mediaController);
    }

    @Override
    protected void onStart() {
        Uri uri = getIntent().getParcelableExtra("video");
        m_videoView.setVideoURI(uri);
        m_videoView.start();
        super.onStart();
    }

    @Override
    protected void onPause() {
        m_position = m_videoView.getCurrentPosition();
        m_videoView.stopPlayback();
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (m_position > 0) {
            m_videoView.seekTo(m_position);
            m_position = -1;
        }
        super.onResume();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        this.finish();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }
}
