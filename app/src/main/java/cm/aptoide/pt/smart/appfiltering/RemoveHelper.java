package cm.aptoide.pt.smart.appfiltering;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import android.util.Log;

class RemoveHelper {
    private static final String DATALIST = "datalist";
    private static final String LIST = "list";
    private static final String PACKAGE = "package";

    private void test123(String sb) {
        if (sb.length() > 4000) {
            Log.d("zzz", "sb.length = " + sb.length());
            int chunkCount = sb.length() / 4000;     // integer division
            for (int i = 0; i <= chunkCount; i++) {
                int max = 4000 * (i + 1);
                if (max >= sb.length()) {
                    Log.d("zzz", "chunk " + i + " of " + chunkCount + ":" + sb.substring(4000 * i));
                } else {
                    Log.d("zzz", "chunk " + i + " of " + chunkCount + ":" + sb.substring(4000 * i, max));
                }
            }
        } else {
            Log.v("zzz", sb.toString());
        }
    }

    @Nullable
    JSONObject filterResponse(String jsonResponse, List<AppToRemove> removeList) {
        try {
            Log.d("qqq", "Test1");
            JSONObject jObject = new JSONObject(jsonResponse);

            test123(jsonResponse.toString());

            JSONObject dataListObject = jObject.getJSONObject(DATALIST);
            JSONArray jArray = dataListObject.getJSONArray(LIST);
            JSONArray newJArray = new JSONArray();
            if (dataListObject != null ) {
                if (jArray != null) {
                    for (int i = 0; i < jArray.length(); i++) {
                        JSONObject application = jArray.getJSONObject(i);
                        if (application != null) {
                            String appPackage = application.getString(PACKAGE);
                            int indexToRemove = -1;
                            for (int j = 0; j < removeList.size(); j++) {
                                if (removeList.get(j).getAppPackage().equals(appPackage)) {
                                    //indexToRemove = i;
                                    Log.d("qqq", "Test - value is " + application.toString());
                                    break;
                                }
                            }
                            if (indexToRemove < 0) {
                                newJArray.put(application);
                            }
                        }
                    }
                    dataListObject.remove(LIST);
                    dataListObject.put(LIST, (Object) newJArray);
                    return jObject;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
} 
