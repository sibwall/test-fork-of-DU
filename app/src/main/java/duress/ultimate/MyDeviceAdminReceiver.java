package duress.ultimate;

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
         String action = intent.getAction();
         if (Intent.ACTION_MEDIA_MOUNTED.equals(action) || "android.intent.action.MEDIA_READY".equals(action) || Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)) MyAccessibilityService.setWipeLimit(2, context);                                         
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
