package nl.gussio.trackqueuer;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context context;

    int PRIVATE_MODE = 0;

    private static final String PREF_NAME = "spotifyqueuer-intro";
    private static final String FIRST_LAUNCH = "FirstLaunch";

    public PrefManager(Context context){
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setFirstLaunch(boolean firstLaunch){
        editor.putBoolean(FIRST_LAUNCH, firstLaunch);
        editor.commit();
    }

    public boolean isFirstLaunch(){
        return pref.getBoolean(FIRST_LAUNCH, true);
    }

}
