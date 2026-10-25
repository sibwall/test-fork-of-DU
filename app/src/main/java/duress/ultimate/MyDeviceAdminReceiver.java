package duress.ultimate;

import java.util.Collections;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.content.SharedPreferences;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

     private static final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };   
    
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Context appContext = context.getApplicationContext();            
        Intent serviceIntent = new Intent(appContext, MyAccessibilityService.class);                
        appContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);                                
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
