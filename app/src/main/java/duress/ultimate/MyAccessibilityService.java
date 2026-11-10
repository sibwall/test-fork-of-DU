package duress.ultimate;

import android.accessibilityservice.AccessibilityService;
import android.content.pm.ApplicationInfo;
import android.content.ComponentName;
import android.app.KeyguardManager;
import android.os.UserManager;
import android.os.Bundle;
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
        setWipeLimit(1, this);     		
    }
	
	private volatile int MotionLimit=1;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
                
		final CharSequence packageName = event.getPackageName();   
		final int eventType = event.getEventType();

		if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED  && MotionLimit !=0) {
			android.graphics.Rect bounds = new android.graphics.Rect();
			event.getSource().getBoundsInScreen(bounds);
			int screenHeight = getResources().getDisplayMetrics().heightPixels;
			if (bounds.centerY() > screenHeight * 0.8) setWipeLimit(MotionLimit, this);
			MotionLimit = 0;
		}

		MotionLimit = 0;

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            setWipeLimit(1, this);      
		}

		final KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

		if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {            
            String className = String.valueOf(event.getClassName());        
            if ("android.widget.Toast".equals(className)) {                 
                if (isSystemApp(packageName.toString())) {                                                     
                    if (km.isKeyguardLocked()) {                                      
                        setWipeLimit(1, this);						
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
						MotionLimit = 1;                                                
                    } else {                                                
                        MotionLimit = 10;                      
                    }
                    
                }
            }
			node.recycle();
        }
    }

	static void setWipeLimit(int limit, Context c) {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminName = new ComponentName(c, MyDeviceAdminReceiver.class);
            dpm.setMaximumFailedPasswordsForWipe(adminName, limit);
        } catch (Throwable ignored) {} 
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
		setWipeLimit(1, this);     
    }

}
