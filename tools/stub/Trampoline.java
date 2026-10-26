package pt.aadf.sourcesswitcher.stub;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

/**
 * The only code in every per-input stub package. Launchers see the stub as a normal TV app
 * (own package, label and banner); opening it forwards the input type from the manifest
 * meta-data to the main app's SwitchActivity and finishes at once, so the launcher stays on
 * screen while the TV changes input.
 */
public class Trampoline extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String input = null;
        try {
            Bundle md = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA).metaData;
            if (md != null) input = md.getString("pt.aadf.input");
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        Intent i = new Intent()
                .setClassName("pt.aadf.sourcesswitcher", "pt.aadf.sourcesswitcher.SwitchActivity")
                .putExtra("input", input)
                // No NEW_TASK: SwitchActivity joins this stub's own task, so finishing both
                // returns to the launcher instead of surfacing the main app's picker task.
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Switch Input app is not installed", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
