package nl.gussio.spotifyqueuer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.SpotifyAppRemote;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "51c1a7b0c4bc499698b10eb15bfeaad3";
    private static final String REDIRECT_URI = "nl.gussio.spotifyqueuer://callback";
    private SpotifyAppRemote mSpotifyAppRemote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

    }

    private void connected() {
        // Then we will write some more code here.
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Aaand we will finish off here.
    }
}
