package nl.gussio.trackqueuer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.mappers.gson.GsonMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "51c1a7b0c4bc499698b10eb15bfeaad3";
    private static final String REDIRECT_URI = "nl.gussio.trackqueuer://callback";
    private static final String HEAD_URI = "https://trackqueuer.com/";
    private SpotifyAppRemote remote;

    private RequestQueue queue;

    private String uri;
    private String privatekey;
    private long created;

    private TimerTask timerTask;
    private Timer timer;

    private ListView historyList;
    private ArrayList<String> history;
    private ArrayAdapter<String> historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefManager manager = new PrefManager(this);
        if(!SpotifyAppRemote.isSpotifyInstalled(getApplicationContext()))
            manager.setFirstLaunch(true);
        if(manager.isFirstLaunch()){
            Intent i = new Intent(getApplicationContext(), IntroActivity.class);
            startActivity(i);
        }
        setContentView(R.layout.activity_main);
        queue = Volley.newRequestQueue(this);
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
                if(throwable.getMessage().contains("logged in")){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.Theme_AppCompat_DayNight_Dialog_Alert);
                    builder.setTitle(R.string.loggedOut)
                            .setMessage(R.string.loggedOutDesc)
                            .setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Uri location = Uri.parse("spotify:");
                                    Intent toSpotify = new Intent(Intent.ACTION_VIEW, location);
                                    startActivity(toSpotify);
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            }).show();
                }
            }
        });

        findViewById(R.id.shareButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, HEAD_URI+"?uri="+uri);
                intent.setType("text/plain");
                startActivity(intent);
            }
        });

        findViewById(R.id.refreshButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pullSongs(true);
            }
        });

        findViewById(R.id.newUri).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.newQueue)
                        .setMessage(R.string.newQueueConfirmMessage)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                newUri();
                            }
                        }).setNegativeButton(R.string.no, null).show();
            }
        });

        final Switch autoRefresh = findViewById(R.id.autoRefreshSwitch);
        autoRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(autoRefresh.isChecked()){
                    startTimer();
                }else{
                    stopTimer();
                }
            }
        });
    }

    private void connected(){
        File file = new File(getFilesDir(), "keys");
        if(file.exists()){
            try {
                FileInputStream fis = new FileInputStream(file);
                Scanner s = new Scanner(fis);
                String jsonOutput = "";
                while(s.hasNextLine()) {
                    jsonOutput += s.nextLine();
                }
                s.close();
                fis.close();
                JSONObject obj = new JSONObject(jsonOutput).getJSONObject("key");
                created = obj.getLong("created");
                if(created*1000+1000*60*60*8 > System.currentTimeMillis()) { //Validate experation time
                    uri = obj.getString("uri");
                    privatekey = obj.getString("privatekey");
                    validKey();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        if(uri == null || privatekey == null){
           newUri();
        }

        historyList = findViewById(R.id.historyList);
        history = getHistory();
        historyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, history);
        historyList.setAdapter(historyAdapter);
    }

    private void validKey(){
        TextView uriText = findViewById(R.id.uriText);
        uriText.setText(uri);
        startTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SpotifyAppRemote.CONNECTOR.disconnect(remote);
        stopTimer();
    }

    private void saveData(){
        try {
            JSONObject data = new JSONObject();
            JSONObject key = new JSONObject();
            key.put("uri", uri);
            key.put("privatekey", privatekey);
            key.put("created", created);
            data.put("key", key);
            FileOutputStream fos = new FileOutputStream(new File(getFilesDir(), "keys"));
            fos.write(data.toString().getBytes());
            fos.flush();
            fos.close();
        }catch (JSONException e){
            e.printStackTrace();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private ArrayList<String> getHistory(){
        File historyFile = new File(getFilesDir(), "history");
        ArrayList<String> output = new ArrayList<>();
        if(historyFile.exists()){
            try {
                FileInputStream fis = new FileInputStream(historyFile);
                Scanner s = new Scanner(fis);
                String outputJson = "";
                while(s.hasNextLine())
                    outputJson += s.nextLine();
                s.close();
                fis.close();
                JSONObject object = new JSONObject(outputJson);
                JSONArray history = object.getJSONArray("history");
                for(int i = 0; i < history.length(); i++){
                    output.add(history.getString(i));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return output;
    }

    private void addHistory(String track){
        history.add(0, track);
        historyAdapter.notifyDataSetChanged();
        try {
            JSONArray array = new JSONArray(history);
            JSONObject object = new JSONObject();
            object.put("history", array);
            FileOutputStream fos = new FileOutputStream(new File(getFilesDir(), "history"));
            fos.write(object.toString().getBytes());
            fos.flush();
            fos.close();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pullSongs(final boolean foreground){
        Log.d("TrackQueuer", "Pulling songs");
        final Button refresh = findViewById(R.id.refreshButton);
        if(foreground) {
            refresh.setClickable(false);
            refresh.setText(R.string.refreshing);
        }
        StringRequest request = new StringRequest(Request.Method.GET, HEAD_URI + "request.php?uri=" + uri + "&privatekey=" + privatekey + "&pullsongs", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    Log.d("json", obj.toString());
                    if(obj.has("tracks")){
                        JSONArray tracks = obj.getJSONArray("tracks");
                        for(int i = 0; i < tracks.length(); i++){
                            JSONObject track = tracks.getJSONObject(i);
                            remote.getPlayerApi().queue("spotify:track:"+track.getString("trackid"));
                            if(track.has("trackinfo")){
                                JSONObject trackInfo = track.getJSONObject("trackinfo");
                                String name = trackInfo.getString("name") + " - ";
                                JSONArray artists = trackInfo.getJSONArray("artists");
                                StringBuilder sb = new StringBuilder(name);
                                for(int j = 0; j < artists.length(); j++){
                                    sb.append(artists.get(j));
                                    if(j != artists.length()-1)
                                        sb.append(", ");
                                }
                                name = sb.toString();
                                addHistory(name);
                                Log.d("TrackQueuer", name);
                            }
                            Toast.makeText(getApplicationContext(), R.string.pullSongsSuccess, Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), R.string.pullSongsError, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                if(foreground) {
                    refresh.setClickable(true);
                    refresh.setText(R.string.refreshButton);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(foreground) {
                    Toast.makeText(getApplicationContext(), R.string.pullSongsError, Toast.LENGTH_SHORT).show();
                    refresh.setClickable(true);
                    refresh.setText(R.string.refreshButton);
                }
            }
        });
        queue.add(request);
    }

    public void newUri(){
        StringRequest request = new StringRequest(Request.Method.GET, HEAD_URI+"request.php?newclient", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if(json.has("uri") && json.has("privatekey"))
                        uri = json.getString("uri");
                    privatekey = json.getString("privatekey");
                    created = json.getLong("created");
                    saveData();
                    validKey();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("TrackQueuer", "error: "+error);
            }
        });
        queue.add(request);
        historyAdapter.clear();
        addHistory("Queue created.");
    }

    private void startTimer(){
        stopTimer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                pullSongs(false);
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 10000);
    }

    private void stopTimer(){
        if(timer != null)
            timer.cancel();
    }
}
