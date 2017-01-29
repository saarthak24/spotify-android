package com.saarthak.spotify;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;
import com.squareup.picasso.Picasso;

import java.util.concurrent.TimeUnit;

import static com.saarthak.spotify.R.id.durationText;
import static com.saarthak.spotify.R.id.progressText;
import static com.saarthak.spotify.R.id.seekBar;


public class MainActivity extends Activity implements
        SpotifyPlayer.NotificationCallback, ConnectionStateCallback {

    private static final String CLIENT_ID = "6bf4064695dd4d619c64870da29f5f57";
    private static final String REDIRECT_URI = "saarthaks-first-spotify-app://callback";

    private Player mPlayer;
    private ImageButton mpauseButton;
    private ImageButton mplayButton;
    private ImageButton mprevButton;
    private ImageButton mnextButton;
    private ImageView malbumCover;
    private TextView msongText;
    private TextView martistText;
    private int songsIndex = 0;
    private SeekBar mseekBar;
    private TextView mprogressText;
    private TextView mdurationText;
    private String temp;
    private boolean started = false;
    private String[] songs = {"spotify:track:7BKLCZ1jbUBVqRi2FVlTVw", "spotify:track:7zsXy7vlHdItvUSH8EwQss", "spotify:track:2bKhIGdMdcqCqQ2ZhSv5nE", "spotify:track:3dhjNA0jGA8vHBQ1VdD6vV", "spotify:track:14Rcq31SafFBHNEwXrtR2B"}; //Closer, Cold Water, Middle, I Feel It Coming, Hold On-We're Going Home,

    private static final int REQUEST_CODE = 24;

    private final Handler handler = new Handler();

    private final Runnable updatePositionRunnable = new Runnable() {
        public void run() {
            updatePosition();
        }
    };

    private void updatePosition() {
        handler.removeCallbacks(updatePositionRunnable);
        if (mPlayer != null) {
            mseekBar.setProgress((int) mPlayer.getPlaybackState().positionMs);
        }
        String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(mPlayer.getPlaybackState().positionMs),
                TimeUnit.MILLISECONDS.toMinutes(mPlayer.getPlaybackState().positionMs) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(mPlayer.getPlaybackState().positionMs) % TimeUnit.MINUTES.toSeconds(1));
        mprogressText.setText(hms);
        handler.postDelayed(updatePositionRunnable, 500);
        if(mPlayer.getPlaybackState().positionMs > 0)
        {
            started = true;
        }
        if(!mPlayer.getPlaybackState().isPlaying && started)
        {
            mnextButton.performClick();
        }
        System.out.println(started);
    }

    public void setData(Player player, Metadata.Track m) {
        setAlbumCover(player, m);
        setSongText(player, m);
        setArtistText(player, m);
        setSeekBar(player, m);
    }

    public void setAlbumCover(Player player, Metadata.Track m) {
        Picasso.with(this).load(m.albumCoverWebUrl).into(malbumCover);
        temp = m.albumCoverWebUrl;
    }

    public void setSongText(Player player, Metadata.Track m) {
        msongText.setText(m.name);
    }

    public void setArtistText(Player player, Metadata.Track m) {
        martistText.setText(m.artistName + " - " + m.albumName);
    }

    public void setSeekBar(Player player, Metadata.Track m) {
        int duration = (int) m.durationMs;
        mseekBar.setMax(duration);
        mseekBar.setProgress(0);
        String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(m.durationMs),
                TimeUnit.MILLISECONDS.toMinutes(m.durationMs) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(m.durationMs) % TimeUnit.MINUTES.toSeconds(1));
        mdurationText.setText(hms);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

        mpauseButton = (ImageButton) findViewById(R.id.pauseButton);
        mplayButton = (ImageButton) findViewById(R.id.playButton);
        mprevButton = (ImageButton) findViewById(R.id.prevButton);
        mnextButton = (ImageButton) findViewById(R.id.nextButton);
        malbumCover = (ImageView) findViewById(R.id.albumCover);
        msongText = (TextView) findViewById(R.id.songText);
        martistText = (TextView) findViewById(R.id.artistText);
        mseekBar = (SeekBar) findViewById(seekBar);
        mprogressText = (TextView) findViewById(progressText);
        mdurationText = (TextView) findViewById(durationText);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer) {
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addNotificationCallback(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }

        mpauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.pause(null);
                mpauseButton.setVisibility(View.GONE);
                mplayButton.setVisibility(View.VISIBLE);
            }
        });
        mplayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.resume(null);
                mplayButton.setVisibility(View.GONE);
                mpauseButton.setVisibility(View.VISIBLE);
            }
        });
        mprevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mpauseButton.setVisibility(View.VISIBLE);
                mplayButton.setVisibility(View.GONE);
                if (songsIndex > 0)
                    songsIndex--;
                else if (songsIndex == 0)
                    songsIndex = songs.length - 1;
                started = false;
                mPlayer.playUri(null, songs[songsIndex], 0, 0);
                updatePosition();
                Metadata.Track m = mPlayer.getMetadata().currentTrack;
                while (m == null) {
                    m = mPlayer.getMetadata().currentTrack;
                }
                setData(mPlayer, m);
            }
        });
        mnextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mpauseButton.setVisibility(View.VISIBLE);
                mplayButton.setVisibility(View.GONE);
                if (songsIndex < songs.length - 1)
                    songsIndex++;
                else if (songsIndex == songs.length - 1)
                    songsIndex = 0;
                started = false;
                mPlayer.playUri(null, songs[songsIndex], 0, 0);
                updatePosition();
                Metadata.Track m = mPlayer.getMetadata().currentTrack;
                while (m == null || temp.equals(m.albumCoverWebUrl)) {
                    m = mPlayer.getMetadata().currentTrack;
                }
                setData(mPlayer, m);

            }
        });
        mseekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            boolean userTouch;

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
                userTouch = false;
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                userTouch = true;
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                if (mPlayer.getPlaybackState().isPlaying && userTouch)
                    mPlayer.seekToPosition(null, arg1);
            }
        });
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
        mPlayer.playUri(null, songs[songsIndex], 0, 0);
        updatePosition();
        Metadata.Track m = mPlayer.getMetadata().currentTrack;
        while (m == null) {
            m = mPlayer.getMetadata().currentTrack;
        }
        setData(mPlayer, m);
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(int i) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }
}
