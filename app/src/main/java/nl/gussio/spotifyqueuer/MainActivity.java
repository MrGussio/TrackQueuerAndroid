package nl.gussio.spotifyqueuer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "51c1a7b0c4bc499698b10eb15bfeaad3";
    private static final String REDIRECT_URI = "nl.gussio.spotifyqueuer://callback";
    private static final String HEAD_URI = "https://api.gussio.nl/";
    private SpotifyAppRemote remote;

    private RequestQueue queue;

    private String uri;
    private String privatekey;
    private long created;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queue = Volley.newRequestQueue(this);

        findViewById(R.id.shareButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, "https://api.gussio.nl/addtrack?uri="+uri);
                intent.setType("text/plain");
                startActivity(intent);
            }
        });

        findViewById(R.id.refreshButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pullSongs();
            }
        });
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
        }else{
            StringRequest request = new StringRequest(Request.Method.GET, HEAD_URI+"?newclient", new Response.Listener<String>() {
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
                    Log.d("SpotifyQueuer", "error: "+error);
                }
            });
            queue.add(request);
        }
    }

    private void validKey(){
        TextView uriText = findViewById(R.id.uriText);
        uriText.setText(uri);
        pullSongs();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SpotifyAppRemote.CONNECTOR.disconnect(remote);
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

    private void pullSongs(){
        StringRequest request = new StringRequest(Request.Method.GET, HEAD_URI + "addtrack?uri=" + uri + "&privatekey=" + privatekey + "&pullsongs", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if(obj.has("tracks")){
                        JSONArray tracks = obj.getJSONArray("tracks");
                        for(int i = 0; i < tracks.length(); i++){
                            JSONObject track = tracks.getJSONObject(i);
                            remote.getPlayerApi().queue("spotify:track:"+track.getString("trackid"));
                            Toast.makeText(getApplicationContext(), R.string.pullSongsSuccess, Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), R.string.pullSongsError, Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), R.string.pullSongsError, Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(request);
    }

}
