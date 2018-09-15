package nl.gussio.spotifyqueuer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.mappers.gson.GsonMapper;

public class MainActivity extends AppCompatActivity implements DownloadCallback<String> {

    private static final String CLIENT_ID = "51c1a7b0c4bc499698b10eb15bfeaad3";
    private static final String REDIRECT_URI = "nl.gussio.spotifyqueuer://callback";
    private SpotifyAppRemote remote;

    private NetworkFragment mNetworkFragment;
    private boolean mDownloading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mNetworkFragment = NetworkFragment.getInstance(getFragmentManager(), "https://api.gussio.nl/?newclient");
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
        mNetworkFragment.startDownload();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SpotifyAppRemote.CONNECTOR.disconnect(remote);
    }

    @Override
    public void updateFromDownload(String result) {
        Log.d("SpotifyQueuer", result);
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
        switch(progressCode) {
            // You can add UI behavior for progress updates here.
            case Progress.ERROR:
                Log.d("SpotifyQueuer", "Error while retreiving data.");
                break;
            case Progress.CONNECT_SUCCESS:
                Log.d("SpotifyQueuer", "Succesfully connected.");
                break;
            case Progress.GET_INPUT_STREAM_SUCCESS:
                Log.d("SpotifyQueuer", "Succesfully retreived input stream.");
                break;
            case Progress.PROCESS_INPUT_STREAM_IN_PROGRESS:
                Log.d("SpotifyQueuer", "Processing input stream...");
                break;
            case Progress.PROCESS_INPUT_STREAM_SUCCESS:
                Log.d("SpotifyQueuer", "Succesfully processed input stream.");
                break;
        }
    }

    @Override
    public void finishDownloading() {
        mDownloading = false;
        if (mNetworkFragment != null) {
            mNetworkFragment.cancelDownload();
        }
    }
}
