package nl.gussio.spotifyqueuer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.mappers.gson.GsonMapper;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "51c1a7b0c4bc499698b10eb15bfeaad3";
    private static final String REDIRECT_URI = "nl.gussio.spotifyqueuer://callback";
    private SpotifyAppRemote remote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .setJsonMapper(GsonMapper.create())
                .build();
        SpotifyAppRemote.CONNECTOR.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                remote = spotifyAppRemote;
                Log.d("Debugger", "Connected to Spotfiy.");
                connected();
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e("Debugger", throwable.getMessage(), throwable);
            }
        });
    }

    private void connected() {
        remote.getPlayerApi().play("spotify:track:6tv43DiBgTHgC0dtID2MXW");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Aaand we will finish off here.
    }
}
