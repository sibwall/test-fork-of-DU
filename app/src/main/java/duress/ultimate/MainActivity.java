package duress.ultimate;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS = "prefs";
    private static final String CE_PREFS = "ce_prefs";
    private static final String READ_INTRO = "read_intro";
    private static final String DURESS_LEN = "duress_len";
    private static final String MAX_ATTEMPTS = "max_attempts";
    private static final String APP_PIN_HASH = "app_pin_hash";
    private static final String APP_PIN_SALT = "app_pin_salt";
    private static final String CLOSE_WARNINGS = "close_warnings";

    private TextView text;
    private LinearLayout buttonBox;
    private TextView customInputDisplay;
    private StringBuilder currentInput = new StringBuilder();
    private boolean dialogShown = false;
    private boolean isPinAuthenticated = false;

    private void EnableComponent() {
        if (isComponentEnabled()) return;
        ComponentName componentName = new ComponentName(this, MyAccessibilityService.class);

        PackageManager packageManager = getPackageManager();
        packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );
    }

    private boolean isComponentEnabled() {
        ComponentName componentName = new ComponentName(this, MyAccessibilityService.class);
        PackageManager pm = getPackageManager();
        return pm.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    private boolean isEn() { return !Locale.getDefault().getLanguage().equals("ru"); }

    @Override
    protected void onCreate(Bundle b) {
        CryptoManager.initKeys();
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);       
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
        
        SharedPreferences ce = getCEPrefs();
        if (ce.contains(APP_PIN_HASH)) {
            isPinAuthenticated = false;
            render(isEn() ? "Enter PIN to access the application" : "Введите пин-код для доступа к приложению");
            renderPinInputStep("Auth PIN", 8, Integer.MAX_VALUE);
        } else {
            isPinAuthenticated = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (isPinAuthenticated) {
            updateUI();
        }
    }

    private SharedPreferences getProtectedPrefs() {
        return getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private SharedPreferences getCEPrefs() {
        return getApplicationContext().getSharedPreferences(CE_PREFS, MODE_PRIVATE);
    }

    private void updateUI() {
        SharedPreferences p = getProtectedPrefs();
        SharedPreferences ce = getCEPrefs();

        boolean readIntro = CryptoManager.getBoolean(p, CryptoManager.BFU_ALIAS, READ_INTRO, false);
        boolean admin = isAdmin();
        boolean accessibility = isAccessibilityEnabled();
        boolean hasDuressLen = p.contains(DURESS_LEN);
        boolean hasMaxAttempts = p.contains(MAX_ATTEMPTS);
        boolean hasPin = ce.contains(APP_PIN_HASH);

        if (!hasPin && isComponentEnabled()) {
            android.view.ViewGroup content = findViewById(android.R.id.content);
            ScrollView scrollView = (ScrollView) content.getChildAt(0);
            LinearLayout root = (LinearLayout) scrollView.getChildAt(0);
            root.setBackgroundColor(Color.parseColor("#7a1c1c"));
            text.setTextSize(24f);
            render(isEn() ? TEXT_ERROR_EN : TEXT_ERROR);
            return;
        }
        
        if (!readIntro) {
            render(isEn() ? TEXT_INTRO_EN : TEXT_INTRO);
            renderButtons(isEn() ? new String[]{"Continue"} : new String[]{"Продолжить"}, null, false);
            return;
        } 

        if (!hasDuressLen) {
            render(isEn() ? TEXT_DURESS_LEN_EN : TEXT_DURESS_LEN);
            renderInputStep(isEn() ? "Save" : "Сохранить", 4, Integer.MAX_VALUE);
            return;
        }
        if (!hasMaxAttempts) {
            render(isEn() ? TEXT_MAX_ATTEMPTS_EN : TEXT_MAX_ATTEMPTS);
            renderInputStep(isEn() ? "Save" : "Сохранить", 1, 5);
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
        
        if (!hasPin) {
            render(isEn() ? TEXT_SET_PIN_EN : TEXT_SET_PIN);
            renderPinInputStep(isEn() ? "Save PIN" : "Сохранить ПИН", 8, Integer.MAX_VALUE);
            return;
        }

        int duressLen = CryptoManager.getInt(p, CryptoManager.BFU_ALIAS, DURESS_LEN, 4);
        int maxAttempts = CryptoManager.getInt(p, CryptoManager.BFU_ALIAS, MAX_ATTEMPTS, 3);

        String infoText = isEn()
                ? "Screen lock password length after entering and submitting which phone data will be wiped: " + duressLen + "\n\nThe maximum number of failed screen unlock password entry attempts to wipe phone data: " + maxAttempts
                : "Длина пароля разблокировки экрана после ввода и отправки которой происходит сброс данных телефона: " + duressLen + "\n\nМаксимальное количество неверных попыток подбора пароля разблокировки экрана для сброса данных телефона: " + maxAttempts;

        render(infoText);
        renderMainSettingsMenu(isEn()
                ? new String[]{"Change screen lock password length for reset", "Change the number of screen lock password entry attempts for reset", "Change app PIN"}
                : new String[]{"Изменить длину пароля блокировки экрана для сброса", "Изменить количество попыток подбора пароля блокировки экрана для сброса", "Изменить пин-код приложения"});
    }

    private void render(String textValue) { text.setText(textValue); }

    private void renderMainSettingsMenu(String[] actions) {
        buttonBox.removeAllViews();

        SharedPreferences p = getProtectedPrefs();
        boolean isCloseWarningsEnabled = CryptoManager.getBoolean(p, CryptoManager.BFU_ALIAS, CLOSE_WARNINGS, true);

        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(isEn() ? TEXT_TOGGLE_CLOSE_WARNINGS_EN : TEXT_TOGGLE_CLOSE_WARNINGS);
        checkBox.setTextColor(Color.WHITE);
        checkBox.setTextSize(16f);
        checkBox.setChecked(isCloseWarningsEnabled);

        LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cbParams.setMargins(0, 0, 0, 32);
        checkBox.setLayoutParams(cbParams);

        checkBox.setOnClickListener(v -> {
            if (!checkBox.isChecked()) {
                checkBox.setChecked(true);
                render(isEn() ? TEXT_CONFIRM_DISABLE_WARNINGS_EN : TEXT_CONFIRM_DISABLE_WARNINGS);
                renderButtons(isEn()
                        ? new String[]{"Yes, disable closing of pop-up windows.", "No, keep closing of pop-up windows."}
                        : new String[]{"Да, отключить закрытие всплывающих окон.", "Нет, оставить закрытие всплывающих окон."}, null, false);
            } else {
                CryptoManager.putBoolean(p, CryptoManager.BFU_ALIAS, CLOSE_WARNINGS, true);
                Toast.makeText(MainActivity.this, isEn() ? TOAST_ENABLED_EN : TOAST_ENABLED, Toast.LENGTH_SHORT).show();
            }
        });

        buttonBox.addView(checkBox);

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

    private void renderPinInputStep(String actionName, int minLen, int maxLen) {
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
                        if (currentInput.length() < maxLen) {
                            currentInput.append(key);
                        }
                    }

                    StringBuilder masked = new StringBuilder();
                    for(int i = 0; i < currentInput.length(); i++) masked.append("•");
                    customInputDisplay.setText(masked.toString());

                    int len = currentInput.length();
                    setButtonState(okBtnRef[0], (len >= minLen && len <= maxLen));
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

    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    private String hashPin(String pin, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.decode(salt, Base64.NO_WRAP));
            byte[] hash = md.digest(pin.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
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
            case "Сохранить ПИН": case "Save PIN":
                if (currentInput.length() >= 8 && currentInput.length() <= Integer.MAX_VALUE) {                    
                    String pin = currentInput.toString();
                    String salt = generateSalt();
                    String hash = hashPin(pin, salt);                    
                    SharedPreferences ce = getCEPrefs();
                    CryptoManager.putString(ce, CryptoManager.CE_ALIAS, APP_PIN_SALT, salt);
                    CryptoManager.putString(ce, CryptoManager.CE_ALIAS, APP_PIN_HASH, hash);
                    EnableComponent();
                    updateUI();
                }
                break;
            case "Auth PIN":
                String pin = currentInput.toString();                
                SharedPreferences ce = getCEPrefs();
                String salt = CryptoManager.getString(ce, CryptoManager.CE_ALIAS, APP_PIN_SALT, "");
                String hash = CryptoManager.getString(ce, CryptoManager.CE_ALIAS, APP_PIN_HASH, "");
                if (hashPin(pin, salt).equals(hash)) {
                    isPinAuthenticated = true;
                    updateUI();
                } else {
                    currentInput.setLength(0);
                    if (customInputDisplay != null) customInputDisplay.setText("");
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
            case "Да, отключить закрытие всплывающих окон.": case "Yes, disable closing of pop-up windows.":
                CryptoManager.putBoolean(p, CryptoManager.BFU_ALIAS, CLOSE_WARNINGS, false);
                updateUI();
                break;
            case "Нет, оставить закрытие всплывающих окон.": case "No, keep closing of pop-up windows.":
                updateUI();
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

    private static final String TEXT_INTRO = ""
    private static final String TEXT_INTRO_EN = ""
    
    private static final String TEXT_ERROR = "Возникла ошибка безопасности. Вот она:\nпамять приложения была очищена пользователем или системой";
    private static final String TEXT_ERROR_EN = "A security error occurred. The error is:\nthe application memory was cleared by the user or system";

    private static final String TEXT_ACCESSIBILITY = "Теперь, дайте приложению Спецвозможности. Они нужны для определения длины паролей в полях ввода. Перейдите в настройки Спецвозможностей -> установленные приложения -> и включите их для DuressUltimate.";
    private static final String TEXT_ACCESSIBILITY_EN = "Now, please grant to the app the Accessibility features. They are needed for the work of features for determining the passwords lengths in input fields. Go to Accessibility settings -> installed apps -> and enable them for DuressUltimate.";

    private static final String TEXT_RESTRICTED = "Вы пытались дать разрешение на спецвозможности, но у вас не получилось? Возможно это из-за того что система блокирует возможность активации таких сервисов называя это \"ограниченными настройками\".\n\nЕсли вам написали об этом при запросе разрешения то\nПерейдите в настройки приложения, нажмите на 3 точки в правом верхнем углу и разрешите их, затем заново перейдите в настройки спецвозможностей и произведите попытку активации. Если 3 точек нет, сделайте тоже самое пока они не появятся либо пока вы не активируете сервис.";
    private static final String TEXT_RESTRICTED_EN = "You tried to give accessibility permission, but you didn't succeed? Perhaps this is due to the fact that the system blocks the ability to activate such services, calling it \"restricted settings\".\n\nIf you were written about this when requesting permission then\nGo to the application settings, click on the 3 dots in the upper right corner and allow them, then go back to the accessibility settings and perform the activation attempt. If there are no 3 dots, do the same until they appear or until you activate the service.";

    private static final String TEXT_ADMIN = "Для начала, дайте приложению права администратора устройства для того чтобы оно могло стирать данные с телефона при вводе заданной вами длины пароля";
    private static final String TEXT_ADMIN_EN = "For start, please grant the app device admin rights to allow it to wipe the phone data when you enter the password length configured by you";

    private static final String TEXT_DURESS_LEN = "Задайте длину пароля разблокировки экрана при вводе и отправке которой происходит сброс данных телефона (от 4 и более)";
    private static final String TEXT_DURESS_LEN_EN = "Set the screen unlock password length upon entering and submitting which phone data will be wiped (from 4 or more)";

    private static final String TEXT_MAX_ATTEMPTS = "Задайте максимальное количество неверных попыток подбора пароля разблокировки экрана для сброса данных телефона (от 1 до 5)";
    private static final String TEXT_MAX_ATTEMPTS_EN = "Set the maximum number of failed screen unlock password entry attempts to wipe phone data (from 1 to 5)";

    private static final String TEXT_SET_PIN = "Теперь рекомендуется установить пин-код на приложение (от 8 символов)";
    private static final String TEXT_SET_PIN_EN = "Now it is recommended to set a pin code for the app (8 or more characters)";

    private static final String TEXT_TOGGLE_CLOSE_WARNINGS = "Закрывать окна предупреждений об оставшихся попытках";
    private static final String TEXT_TOGGLE_CLOSE_WARNINGS_EN = "Close warning windows about remaining attempts";

    private static final String TEXT_CONFIRM_DISABLE_WARNINGS = "Вы хотите отключить закрытие всплывающих окон об оставшемся количестве попыток ввода пароля до сброса данных? Обычно вам не стоит этого делать, вы и так знаете сколько у вас всего попыток, а злоумышленникам эта информация может быть полезйней, чем вам. Выключайте это ТОЛЬКО ЕСЛИ опция работает со сбоями, наример происходят случайные нажатия в другие области экрана.";
    private static final String TEXT_CONFIRM_DISABLE_WARNINGS_EN = "Do you want to disable closing the pop-up windows about the remaining number of password entry attempts before data wipe? Usually, you should not do this; you already know how many attempts you have, and this information might be more useful to attackers than to you. Disable this ONLY IF the option is malfunctioning, for example, accidental taps occur in other areas of the screen.";

    private static final String BTN_YES_DISABLE = "Да, отключить закрытие всплывающих окон.";
    private static final String BTN_YES_DISABLE_EN = "Yes, disable closing of pop-up windows.";

    private static final String BTN_NO_KEEP = "Нет, оставить закрытие всплывающих окон.";
    private static final String BTN_NO_KEEP_EN = "No, keep closing of pop-up windows.";

    private static final String TOAST_ENABLED = "Опция успешно включена";
    private static final String TOAST_ENABLED_EN = "Option successfully enabled";
}
