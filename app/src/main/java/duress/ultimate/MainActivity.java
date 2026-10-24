package duress.ultimate;

import android.text.util.Linkify;
import android.text.method.LinkMovementMethod;
import android.app.PendingIntent;
import android.os.UserManager;
import java.io.File;
import android.content.pm.ApplicationInfo;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import java.io.OutputStream;
import android.app.ActivityManager;
import android.app.admin.DeviceAdminInfo;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.provider.Settings;
import android.util.Base64;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS = "prefs";
    private static final String CE_PREFS = "ce_prefs";
    private static final String READ_INTRO = "read_intro";
    private static final String DURESS_LEN = "duress_len";

    private TextView text;
	private AlertDialog dialog;
    private LinearLayout buttonBox;
    private TextView customInputDisplay;
    private StringBuilder currentInput = new StringBuilder();
    private boolean dialogShown = false;

	private static boolean WasRead=false;

	private static final String SECRET_CODE_HASH = "secret_code_hash";

	private static final String SECRET_CODE_SALT = "secret_code_salt";

	private AlertDialog hideLauncherDialog;
			
	private void mask() throws java.lang.Exception {
    android.content.pm.PackageManager pm = getPackageManager();

    pm.setComponentEnabledSetting(
        new android.content.ComponentName(getPackageName(), "duress.ultimate.ProxyAlias"),
        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        android.content.pm.PackageManager.DONT_KILL_APP
    );

    pm.setComponentEnabledSetting(
        new android.content.ComponentName(getPackageName(), "duress.ultimate.ProxyAlias2"),
        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        android.content.pm.PackageManager.DONT_KILL_APP
    );
	}
	
	private String generateRandom5DigitCode() {
    SecureRandom random = new SecureRandom();
    int code = 10000 + random.nextInt(90000);
    return String.valueOf(code);

	}
	
	private void showHideLauncherAlert() {
    if (hideLauncherDialog != null && hideLauncherDialog.isShowing()) return;

    final String generatedCode = generateRandom5DigitCode();

    String title = isEn() ? "Mask application" : "Замаскировать приложение";
	String message = isEn() 
    ? "Mask the application on the homescreen? It will disguise itself as a calculator. To get into the app, you will need to enter " + generatedCode + "="
    : "Замаскировать приложение на рабочем столе? Оно будет косить под калькулятор. Чтобы войти вам нужно будет ввести " + generatedCode + "=";

    hideLauncherDialog = new AlertDialog.Builder(MainActivity.this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(isEn() ? "Cancel" : "Отмена", (dialog, which) -> hideLauncherDialog = null)
            .setPositiveButton("OK", (dialog, which) -> {
                try {                    
                    byte[] saltBytes = new byte[16];
                    new SecureRandom().nextBytes(saltBytes);
                    String saltBase64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP);
                                        
                    String codeHash = hashPin(generatedCode, saltBase64);
                    
                    SharedPreferences protectedPrefs = getProtectedPrefs();
                    CryptoManager.putString(protectedPrefs, CryptoManager.DE_ALIAS, SECRET_CODE_SALT, saltBase64);
                    CryptoManager.putString(protectedPrefs, CryptoManager.DE_ALIAS, SECRET_CODE_HASH, codeHash);
                                        
					mask();

					finishAndRemoveTask();
                    
				} catch (Throwable e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
				}
            })
            .create();

    hideLauncherDialog.setOnDismissListener(dialog -> hideLauncherDialog = null);
    hideLauncherDialog.show();

    Window window = hideLauncherDialog.getWindow();
    if (window != null) {
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.x = 0;
        layoutParams.y = 0;
        window.setAttributes(layoutParams);
    }

    TextView messageView = hideLauncherDialog.findViewById(android.R.id.message);
    if (messageView != null) {
        messageView.setTextIsSelectable(true);
    }

	}
                   
    private boolean isEn() { return !Locale.getDefault().getLanguage().equals("ru"); }

    @Override
    protected void onCreate(Bundle b) {		
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);       
        super.onCreate(b);		             
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        CryptoManager.initKeys();    
		ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(64, 64, 64, 64);
		
        text = new TextView(this);
        text.setGravity(Gravity.CENTER_HORIZONTAL);
        text.setTextSize(16f);
        text.setTextColor(Color.WHITE);

        buttonBox = new LinearLayout(this);
        buttonBox.setOrientation(LinearLayout.VERTICAL);
        buttonBox.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        boxParams.setMargins(0, 64, 0, 0);
        buttonBox.setLayoutParams(boxParams);

        root.addView(text);
        root.addView(buttonBox);
        scrollView.addView(root);
        setContentView(scrollView);
              
    }

    @Override
    protected void onResume() {
        super.onResume();		
        hideSystemUI();
        updateUI();    
    }

    private SharedPreferences getProtectedPrefs() {
        return getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private SharedPreferences getCEPrefs() {
        return getApplicationContext().getSharedPreferences(CE_PREFS, MODE_PRIVATE);
    }
    
    private boolean isDeviceOwner() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isDeviceOwnerApp(getPackageName());
    }    

    private void updateUI() {
        SharedPreferences p = getProtectedPrefs();
        SharedPreferences ce = getCEPrefs();

        boolean readIntro = CryptoManager.getBoolean(p, CryptoManager.BFU_ALIAS, READ_INTRO, false);
        boolean admin = isAdmin();
        boolean accessibility = isAccessibilityEnabled();
        boolean hasDuressLen = p.contains(DURESS_LEN);
        
		if (!readIntro) {
			text.setText(isEn() ? TEXT_INTRO_EN : TEXT_INTRO);
			Linkify.addLinks(text, Linkify.WEB_URLS);
			text.setMovementMethod(LinkMovementMethod.getInstance());
			renderButtons(isEn() ? new String[]{"Continue"} : new String[]{"Продолжить"}, null, false);
			return;
		}

        if (!hasDuressLen) {
			showUsbWarningAlert(false);                
            render(isEn() ? TEXT_DURESS_LEN_EN : TEXT_DURESS_LEN);
            renderInputStep(isEn() ? "Save" : "Сохранить", 4, Integer.MAX_VALUE);
            return;
        }        
                        
        if (!admin) {            
            render(isEn() ? TEXT_ADMIN_EN : TEXT_ADMIN);
            renderButtons(isEn() ? new String[]{"Grant rights"} : new String[]{"Дать права"}, null, false);
            return;
        }
        if (!accessibility) {  			
            if (dialogShown) {
                render(isEn() ? TEXT_RESTRICTED_EN : TEXT_RESTRICTED);
                renderButtons(isEn() ? new String[]{"App Settings", "Accessibility Settings"} : new String[]{"Настройки приложения", "Настройки спецвозможностей"}, null, false);
            } else {
				render(isEn() ? TEXT_ACCESSIBILITY_EN : TEXT_ACCESSIBILITY);
                renderButtons(isEn() ? new String[]{"Enable Accessibility"} : new String[]{"Включить спецвозможности"}, null, false);
            }
            return;
        }        
		showAccessibilityCrashAlert();

        int duressLen = CryptoManager.getInt(p, CryptoManager.BFU_ALIAS, DURESS_LEN, 4);
        
        String infoText = isEn()
        ? "Screen lock password length after entering and submitting which phone data will be wiped: " + duressLen
        : "Длина пароля разблокировки экрана после ввода и отправки которой происходит сброс данных телефона: " + duressLen;

        render(infoText);
        renderMainSettingsMenu(isEn()
                ? new String[]{"Change screen lock password length for reset"}
                : new String[]{"Изменить длину пароля блокировки экрана для сброса"});
    }

    private void render(String textValue) { text.setText(textValue); }

    private void renderMainSettingsMenu(String[] actions) {
        buttonBox.removeAllViews();

        SharedPreferences p = getProtectedPrefs();
        
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminName = new ComponentName(this, MyDeviceAdminReceiver.class);
        boolean isDO = isDeviceOwner();
			
		Context deContext = getApplicationContext().createDeviceProtectedStorageContext();
        SharedPreferences dePrefs = deContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String savedHash = CryptoManager.getString(dePrefs, CryptoManager.DE_ALIAS, SECRET_CODE_HASH, null);
        String savedSalt = CryptoManager.getString(dePrefs, CryptoManager.DE_ALIAS, SECRET_CODE_SALT, null);

        if (savedHash == null && savedSalt == null) {           

		Button hideButton = new Button(this);
		hideButton.setText(isEn() ? "App masking" : "Маскировка приложения");

		GradientDrawable hideShape = new GradientDrawable();
		hideShape.setShape(GradientDrawable.RECTANGLE);
		hideShape.setColor(Color.parseColor("#34495e"));
		hideShape.setCornerRadius(6f);

		hideButton.setBackground(hideShape);
		hideButton.setTextColor(Color.WHITE);

		hideButton.setPadding(32, 32, 32, 32);

		LinearLayout.LayoutParams hideParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT

		);

		hideParams.setMargins(0, 16, 0, 16);
		hideButton.setLayoutParams(hideParams);

		hideButton.setOnClickListener(v -> showHideLauncherAlert());

		buttonBox.addView(hideButton);
		}
		
        for (String a : actions) {
            Button b = new Button(this);
            b.setText(a);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setColor(Color.parseColor("#34495e"));
            shape.setCornerRadius(6f);
            b.setBackground(shape);
            b.setTextColor(Color.WHITE);
            b.setPadding(32, 32, 32, 32);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 16, 0, 16);
            b.setLayoutParams(params);
            b.setOnClickListener(v -> handleAction(a));
            buttonBox.addView(b);
        }
    }	

	
    private void renderButtons(String[] actions, Button[] outButtonRef, boolean initialDisabled) {
        buttonBox.removeAllViews();
        for (int i = 0; i < actions.length; i++) {
            String a = actions[i];
            Button b = new Button(this);
            b.setText(a);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setColor(initialDisabled ? Color.parseColor("#4a6278") : Color.parseColor("#34495e"));
            shape.setCornerRadius(6f);
            b.setBackground(shape);
            b.setTextColor(Color.WHITE);
            b.setPadding(32, 32, 32, 32);
            b.setEnabled(!initialDisabled);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 16, 0, 16);
            b.setLayoutParams(params);
            b.setOnClickListener(v -> handleAction(a));
            buttonBox.addView(b);

            if (outButtonRef != null && i == 0) {
                outButtonRef[0] = b;
            }

            if (actions.length == 1 && (a.equals("Включить спецвозможности") || a.equals("Enable Accessibility"))) {
                TextView hint = new TextView(this);
                hint.setText(isEn() ? "After granting permissions or if issues occur, return to the app using the back button or gesture." : "После выдачи разрешений или в случае возникновения проблем, вернитесь в приложение используя кнопку или жест 'назад'.");
                hint.setTextColor(Color.WHITE);
                hint.setTextSize(16f);
                hint.setGravity(Gravity.CENTER);
                hint.setPadding(0, 32, 0, 0);
                buttonBox.addView(hint);
            }
        }
    }

    private void renderInputStep(String actionName, int minVal, int maxVal) {
        buttonBox.removeAllViews();
        currentInput.setLength(0);

        customInputDisplay = new TextView(this);
        customInputDisplay.setGravity(Gravity.CENTER);
        customInputDisplay.setTextSize(22f);
        customInputDisplay.setTextColor(Color.WHITE);
        customInputDisplay.setText("");
        customInputDisplay.setPadding(32, 32, 32, 32);

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setShape(GradientDrawable.RECTANGLE);
        bgShape.setColor(Color.parseColor("#2c3e50"));
        bgShape.setCornerRadius(8f);
        bgShape.setStroke(2, Color.parseColor("#7f8c8d"));
        customInputDisplay.setBackground(bgShape);

        LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        displayParams.setMargins(0, 0, 0, 16);
        customInputDisplay.setLayoutParams(displayParams);
        buttonBox.addView(customInputDisplay);

        LinearLayout keypadBox = new LinearLayout(this);
        keypadBox.setOrientation(LinearLayout.VERTICAL);
        keypadBox.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams keypadParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        keypadParams.setMargins(0, 16, 0, 16);
        keypadBox.setLayoutParams(keypadParams);

        final Button[] okBtnRef = new Button[1];

        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"⌫", "0", "OK"}
        };

        for (String[] rowKeys : keys) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 4, 0, 4);
            rowLayout.setLayoutParams(rowParams);

            for (String key : rowKeys) {
                Button keyBtn = new Button(this);
                keyBtn.setText(key);

                GradientDrawable keyShape = new GradientDrawable();
                keyShape.setShape(GradientDrawable.RECTANGLE);

                boolean isOk = key.equals("OK");
                if (isOk) {
                    keyBtn.setEnabled(false);
                    keyShape.setColor(Color.parseColor("#4a6278"));
                    okBtnRef[0] = keyBtn;
                } else {
                    keyShape.setColor(Color.parseColor("#34495e"));
                }

                keyShape.setCornerRadius(6f);
                keyBtn.setBackground(keyShape);
                keyBtn.setTextColor(Color.WHITE);
                keyBtn.setTextSize(20f);
                keyBtn.setPadding(16, 24, 16, 24);

                LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                keyParams.setMargins(4, 0, 4, 0);
                keyBtn.setLayoutParams(keyParams);

                keyBtn.setOnClickListener(v -> {
                    if (key.equals("⌫")) {
                        if (currentInput.length() > 0) {
                            currentInput.deleteCharAt(currentInput.length() - 1);
                        }
                    } else if (isOk) {
                        if (keyBtn.isEnabled()) {
                            handleAction(actionName);
                        }
                    } else {
                        currentInput.append(key);
                    }

                    customInputDisplay.setText(currentInput.toString());

                    try {
                        if (currentInput.length() == 0) {
                            setButtonState(okBtnRef[0], false);
                        } else {
                            int val = Integer.parseInt(currentInput.toString());
                            boolean valid = (val >= minVal && val <= maxVal);
                            setButtonState(okBtnRef[0], valid);
                        }
                    } catch (NumberFormatException e) {
                        setButtonState(okBtnRef[0], false);
                    }
                });

                rowLayout.addView(keyBtn);
            }
            keypadBox.addView(rowLayout);
        }
        buttonBox.addView(keypadBox);
    }

    private void setButtonState(Button b, boolean enabled) {
        b.setEnabled(enabled);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(enabled ? Color.parseColor("#34495e") : Color.parseColor("#4a6278"));
        shape.setCornerRadius(6f);
        b.setBackground(shape);
    }

    private void handleAction(String action) {
        SharedPreferences p = getProtectedPrefs();
        switch (action) {
            case "Продолжить": case "Continue":
                CryptoManager.putBoolean(p, CryptoManager.BFU_ALIAS, READ_INTRO, true);
                updateUI(); break;
            case "Включить спецвозможности": case "Enable Accessibility": case "Настройки спецвозможностей": case "Accessibility Settings":
                dialogShown = true;
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                break;
            case "Настройки приложения": case "App Settings":
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
                startActivity(intent);
                break;
            case "Дать права": case "Grant rights":
                Intent adminIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                adminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, MyDeviceAdminReceiver.class));
                startActivity(adminIntent);
                break;
            case "Сохранить": case "Save":
                if (currentInput.length() > 0) {
                    try {
                        int val = Integer.parseInt(currentInput.toString());
                        if (!p.contains(DURESS_LEN)) {
                            CryptoManager.putInt(p, CryptoManager.BFU_ALIAS, DURESS_LEN, val);
                        } else if (!p.contains(MAX_ATTEMPTS)) {
                            CryptoManager.putInt(p, CryptoManager.BFU_ALIAS, MAX_ATTEMPTS, val);
                        } else {
                            if (text.getText().toString().contains("длину") || text.getText().toString().contains("length")) {
                                CryptoManager.putInt(p, CryptoManager.BFU_ALIAS, DURESS_LEN, val);
                            } else {
                                CryptoManager.putInt(p, CryptoManager.BFU_ALIAS, MAX_ATTEMPTS, val);
                            }
                        }
                        updateUI();
                    } catch (Exception ignored) {}
                }
                break;            
            case "Изменить длину пароля блокировки экрана для сброса": case "Change screen lock password length for reset":
                render(isEn() ? TEXT_DURESS_LEN_EN : TEXT_DURESS_LEN);
                renderInputStep(isEn() ? "Save" : "Сохранить", 4, Integer.MAX_VALUE);
                break;
            case "Изменить количество попыток подбора пароля блокировки экрана для сброса": case "Change the number of screen lock password entry attempts for reset":
                render(isEn() ? TEXT_MAX_ATTEMPTS_EN : TEXT_MAX_ATTEMPTS);
                renderInputStep(isEn() ? "Save" : "Сохранить", 1, 5);
                break;
            case "Изменить пин-код приложения": case "Change app PIN":
                render(isEn() ? TEXT_SET_PIN_EN : TEXT_SET_PIN);
                renderPinInputStep(isEn() ? "Save PIN" : "Сохранить ПИН", 8, Integer.MAX_VALUE);
                break;            
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private boolean isAccessibilityEnabled() {
        String prefString = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (prefString == null || prefString.isEmpty()) return false;
        ComponentName target = new ComponentName(getPackageName(), MyAccessibilityService.class.getName());
        for (String s : prefString.split(":")) {
            ComponentName cn = ComponentName.unflattenFromString(s);
            if (target.equals(cn)) return true;
        }
        return false;
    }

    private boolean isAdmin() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isAdminActive(new ComponentName(this, MyDeviceAdminReceiver.class));
    }

    @Override
	protected void onDestroy() {		
		if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = null;
			
        super.onDestroy();		
    }	

	private boolean isServiceRunning() {
    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    if (manager == null) return false;

    String targetClassName = MyAccessibilityService.class.getName();

    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
        if (targetClassName.equals(service.service.getClassName())) {
            return true; 
        }
    }
    return false; 
	}

	private void showAccessibilityCrashAlert() {
	if (isServiceRunning()) return;	
    boolean isRu = "ru".equals(Locale.getDefault().getLanguage());

    dialog = new AlertDialog.Builder(this)
        .setTitle(isRu ? "Внимание" : "Attention")
        .setCancelable(false)
		.setMessage(isRu ? "Спецвозможности включены, но сервис не запущен. Возможно произошла ошибка, например система остановила его из-за нехватки оперативной памяти. Просьба отключить ползунок сервиса а потом снова включить." 
                         : "Accessibility features are enabled, but the service is not running. Perhaps an error occurred, for example, the system stopped it due to a lack of RAM. Please turn off the service slider and then turn it on again.")
        .setPositiveButton(isRu ? "ОК" : "OK", (d, w) -> 
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
        .create();

    if (dialog.getWindow() != null) {
        android.view.WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.gravity = android.view.Gravity.CENTER;
        lp.x = 0;
        lp.y = 0;
        dialog.getWindow().setAttributes(lp);
    }

    dialog.show(); }
   
    private static final String TEXT_INTRO = "\n\n\nПривет! Это приложение, которое сбрасывает телефон до заводких настроек и удаляет данные при вводе пароля блокировки экрана заданной длины для сброса или при превышении лимита неверных попыток разблокировки (обычно попытка неверная попытка засчитывается если введено 4 или более символов).\n\nКак это работает:\n Вы задаете длину пароля для сброса и максимальное количество неверных попыток (от 1 до 5). По умолчанию когда приложение только получило свои права (спецвозможности и админ) лимит неверных попыток держится на уровне 1. При вводе пароля обычной длины сервис спецвозможностей временно добавляет 2 попытки (вплоть до максимального лимита). При вводе длины для сброса, лимит остается равным 1 и если вы ввели неверный пароль, происходит сброс. Длина для сброса должна отличаться от длины вашего пароля. Приложение использует такую сложную тактику с выставлением лимитов чтобы минимизировать временное окно, когда защиту можно обойти. Проще говоря, при сбое в системе или случайной остановке сервиса спецвозможностей, с наибольшей вероятностью лимит будет оставаться равен 1му или 1му от текущего количества неверных попыток, оставляя защиту в силе.\n\nРекомендация: приложение поддерживает только один тип блокировки: Пароль. Перед настройкой приложения убедитесь что у вас этот тип блокировки, отключите другие типы блокировки, например графический ключ (иначе любая ошибка приведёт к сбросу данных). Также отключите разблокировку по биометрии и агентов доверия в настройках безопасности вашего телефона (ведь они могут мешать приложению осуществлять защиту).\n\nТакже важно сообщить что сброс по лимиту попыток не удаляет раздел FRP основного профиля, который хранит ID аккаунтов. Если хотите не оставлять следов от ваших Google аккаунтов, рекомендуется хранить их только в рабочих профилях, которые не могут быть завязаны на FRP. В остальных случаях будьте аккуратны и не привязывайте бекапы и важные данные к Google аккаунтам, также убедитесь что физического доступа к СИМ-карте недостаточно для получения контроля над ними, проще говоря не привязывайте Google аккаунты к номеру телефона. А ещё поставьте пин-код на сим-карту (это важно и для остальных данных вне зависимости от наличия FRP).\n\nЕсли предоставить этому приложению права Device Owner, оно отключит FRP.\n\nИсходный код:\nhttps://github.com/pofesk0/DuressUltimate\n\nСтраница на F-droid:\nhttps://f-droid.org/ru/packages/duress.ultimate\n";
	private static final String TEXT_INTRO_EN = "\n\n\nHello! This is an app that performs a factory reset and wipes all data when a screen lock password of the specified length for reset is entered or when the limit of failed unlock attempts is exceeded (typically, an incorrect attempt is counted if 4 or more characters are entered).\n\nHow it works:\n You set the password length for reset and the maximum number of failed attempts (1 to 5). By default, when the app has just received its permissions (accessibility and admin), the failed attempt limit is kept at 1. When entering a regular-length password, the accessibility service temporarily adds 2 attempts (up to the maximum limit). When entering the length for reset, the limit remains at 1, and if you enter an incorrect password, a reset occurs. The length for reset must differ from your actual password length. The app uses this complex limit-setting tactic to minimize the time window when protection could be bypassed. Simply put, during a system crash or accidental stoppage of the accessibility service, the limit is most likely to remain equal to 1 or 1 relative to the current number of failed attempts, keeping the protection active.\n\nRecommendation: The app supports only one lock type: Password. Before setting up the app, make sure you have this lock type, disable other lock types, such as pattern locks (otherwise any mistake will lead to a data reset). Also please disable biometric unlock and trust agents in your device security settings (because they can hinder the app in providing protection).\n\nIt is also important to inform that a reset by attempt limit does not delete the FRP section of the main profile, which stores account IDs. If you want not to leave traces of your Google accounts, it is recommended to store them only in work profiles, which cannot be linked to FRP. In other cases, be careful and do not link backups and important data to Google accounts, also make sure that physical access to the SIM card is not enough to gain control over them, simply put do not link Google accounts to a phone number. And also please set a PIN code for the SIM card (this is important also for the rest data regardless of the presence of FRP).\n\nIf you grant Device Owner rights to this app, it will disable FRP.\n\nSource code:\nhttps://github.com/pofesk0/DuressUltimate\n\nF-droid page:\nhttps://f-droid.org/ru/packages/duress.ultimate\n";
	
    private static final String TEXT_ACCESSIBILITY = "Теперь, дайте приложению Спецвозможности. Они нужны для определения длины паролей в полях ввода. Перейдите в настройки Спецвозможностей -> установленные приложения -> и включите их для DuressUltimate. Только после этого будет установлен лимит попыток.";
    private static final String TEXT_ACCESSIBILITY_EN = "Now, please grant to the app the Accessibility features. They are needed for the work of features for determining the passwords lengths in input fields. Go to Accessibility settings -> installed apps -> and enable them for DuressUltimate. Only after that will the attempt limit be set.";

    private static final String TEXT_RESTRICTED = "Вы пытались дать разрешение на спецвозможности, но у вас не получилось? Возможно это из-за того что система блокирует возможность активации таких сервисов называя это \"ограниченными настройками\".\n\nЕсли вам написали об этом при запросе разрешения то\nПерейдите в настройки приложения, нажмите на 3 точки в правом верхнем углу и разрешите их, затем заново перейдите в настройки спецвозможностей и произведите попытку активации. Если 3 точек нет, сделайте тоже самое пока они не появятся либо пока вы не активируете сервис.";
    private static final String TEXT_RESTRICTED_EN = "You tried to give accessibility permission, but you didn't succeed? Perhaps this is due to the fact that the system blocks the ability to activate such services, calling it \"restricted settings\".\n\nIf you were written about this when requesting permission then\nGo to the application settings, click on the 3 dots in the upper right corner and allow them, then go back to the accessibility settings and perform the activation attempt. If there are no 3 dots, do the same until they appear or until you activate the service.";

    private static final String TEXT_ADMIN = "Для начала использования этих функций сначала дайте приложению права администратора устройства для того чтобы оно могло стирать данные с телефона при вводе заданной вами длины пароля";
    private static final String TEXT_ADMIN_EN = "To start using these features first please grant the app device admin rights to allow it to wipe the phone data when you enter the password length configured by you";

    private static final String TEXT_DURESS_LEN = "Задайте длину пароля разблокировки экрана при вводе и отправке которой происходит сброс данных телефона (от 4 и более). Затем перейдите к следующему шагу.";
    private static final String TEXT_DURESS_LEN_EN = "Set the screen unlock password length upon entering and submitting which phone data will be wiped (from 4 or more). Then go to next step.";

    private static final String TOAST_ENABLED = "Опция успешно включена";
    private static final String TOAST_ENABLED_EN = "Option successfully enabled";

}
