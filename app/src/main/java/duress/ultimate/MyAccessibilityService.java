package duress.ultimate;

import android.accessibilityservice.AccessibilityService;
import android.content.pm.ApplicationInfo;
import android.content.ComponentName;
import android.app.KeyguardManager;
import android.os.UserManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

public class MyAccessibilityService extends AccessibilityService {
    
    @Override
    public void onCreate() {
        super.onCreate();
        setWipeLimit(1);     
    }

    private void setWipeLimit(int limit) {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminName = new ComponentName(this, MyDeviceAdminReceiver.class);
            dpm.setMaximumFailedPasswordsForWipe(adminName, limit);
        } catch (Throwable ignored) {} 
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        
        CharSequence packageName = event.getPackageName();   
		int eventType = event.getEventType();
        
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            setWipeLimit(1);            
        }

		if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {            
            String className = String.valueOf(event.getClassName());        
            if ("android.widget.Toast".equals(className)) {                 
                if (isSystemApp(packageName.toString())) {                                                     
                    if (km.isKeyguardLocked()) {                                      
                        setWipeLimit(1);                        
                    }
                }
            } 
        }      
        
        if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {

            if (!isSystemApp(packageName.toString())) return;            
            AccessibilityNodeInfo node = event.getSource();
                       
            SharedPreferences prefs = getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences("prefs", MODE_PRIVATE);
            
            int duressLen = CryptoManager.getInt(prefs, CryptoManager.BFU_ALIAS, "duress_len", 4);
            if (duressLen < 4) duressLen = 4;
                                                
            if (node.isPassword()) {
                CharSequence text = node.getText();
                int length = (text != null) ? text.length() : 0;
                
                if (length > 0) {                    

                    if (length == duressLen) {
                        setWipeLimit(1);                        
                    } else {                                                
                        setWipeLimit(10);                        
                    }
                    
                }
            }
			node.recycle();
        }
    }

    private boolean isSystemApp(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return (appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        } catch (Throwable e) {
            return false;
        }
    }
    
    @Override
    public void onInterrupt() {        
    }
}
