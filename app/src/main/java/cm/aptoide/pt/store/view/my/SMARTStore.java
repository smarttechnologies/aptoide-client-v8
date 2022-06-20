package cm.aptoide.pt.store.view.my;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public class SMARTStore {
    public static final String USE_RELEASE_APP_STORE_KEY = "Use Release App Store";

    private static final boolean DEBUG = "userdebug".equals(Build.TYPE);
    private static final String STORE_RELEASE_NAME = "smarttech-iq";
    private static final String STORE_DEBUG_NAME = "aptoide-test-store";
    private static final String DEFAULT_STORE_NAME = DEBUG ? STORE_DEBUG_NAME : STORE_RELEASE_NAME;

    public static final String STORE_COLOR = "red";

    private SMARTStore() {}

    public static String getStoreName(Context context) {
        if (context == null) {
            return DEFAULT_STORE_NAME;
        }
        return isReleaseAppStore(context) ? STORE_RELEASE_NAME : STORE_DEBUG_NAME;
    }

    private static boolean isReleaseAppStore(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                return Settings.Global.getInt(context.getContentResolver(), USE_RELEASE_APP_STORE_KEY) != 0;
            } catch (Settings.SettingNotFoundException e) {
                Settings.Global.putInt(context.getContentResolver(), USE_RELEASE_APP_STORE_KEY, !DEBUG ? 1 : 0);
            }
        }
        return !DEBUG;
    }
}
