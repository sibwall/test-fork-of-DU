package duress.ultimate;

import java.util.Collections;
import android.content.SharedPreferences;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {
        
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Intent intentX = new Intent(context, MyAccessibilityService.class);
        context.startService(intentX);    
    }
        
    @Override
    public void onEnabled(Context context, Intent intent) {         
        Toast.makeText(context, "Device Admin Enabled", Toast.LENGTH_SHORT).show();        
    }
    
    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context, "Device Admin Disabled", Toast.LENGTH_SHORT).show();
    }
}
