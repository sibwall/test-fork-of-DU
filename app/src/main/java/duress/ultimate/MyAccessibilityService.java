package duress.ultimate;

import android.accessibilityservice.AccessibilityService;
import android.content.pm.ApplicationInfo;
import android.content.ComponentName;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

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
		
        if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED && isSystemApp(packageName.toString())) {

			MotionLimit = 1;

            SharedPreferences prefs = getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences("prefs", MODE_PRIVATE);            
            int duressLen = CryptoManager.getInt(prefs, CryptoManager.BFU_ALIAS, "duress_len", 4);
            if (duressLen < 4) duressLen = 4;

			AccessibilityNodeInfo node = event.getSource();   
			if (node == null) return;
                                                        
            if (node.isPassword()) {
                CharSequence text = node.getText();
                int length = (text != null) ? text.length() : 0;                
                  if (length == duressLen) {
					  MotionLimit = 1;                                                
                  } else if (length > 0) {                                                
                      MotionLimit = 10;                      
                  }                                
            }
			node.recycle();
        } else if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && MotionLimit != 0) {
			setWipeLimit(MotionLimit, this);
			MotionLimit = 0;
		}
    }

	static void setWipeLimit(int limit, Context c) {
        try {
			if (limit==1) limit = 2;
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
