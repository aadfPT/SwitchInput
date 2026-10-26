package pt.aadf.sourcesswitcher.stub;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

public class Trampoline extends Activity {
    private static final String MAIN_APP = "pt.aadf.sourcesswitcher";
    private static final String SWITCH_ACTIVITY = "pt.aadf.sourcesswitcher.SwitchActivity";
    private static final String INPUT_META_DATA = "pt.aadf.input";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent switchInput = new Intent()
                .setClassName(MAIN_APP, SWITCH_ACTIVITY)
                .putExtra("input", inputFromManifest())
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            startActivity(switchInput);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Switch Input app is not installed", Toast.LENGTH_LONG).show();
        }
        finish();
    }

    private String inputFromManifest() {
        try {
            Bundle md = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA).metaData;
            return md == null ? null : md.getString(INPUT_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
