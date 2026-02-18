package cm.aptoide.pt.smart.appfiltering;

import okhttp3.Interceptor;
import okhttp3.Response;
import android.util.Log;
import java.io.IOException;
import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.ResponseBody;


public class ApplicationRemoveWrapper implements Interceptor {
    private static final String LIST_APP = "/api/7/listApps";
    private static final String SEARCH_APP = "/api/7/listSearchApps";
    private static final String GET_APP = "/api/7/getApp";
    private RemoveHelper helper = new RemoveHelper();

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

    @Override public Response intercept(Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());
        //test123(originalResponse.toString());
        String path = chain.request().url().uri().getPath();
        //Log.d("zzz", "path is " + path);
        if (path.contains(LIST_APP) || path.contains(SEARCH_APP)) {
            String bodyString = originalResponse.body().string();
            test123(bodyString);
            MediaType contentType = originalResponse.body().contentType();
            JSONObject object = helper.filterResponse(bodyString, FilteringList.removingList);
            if (object != null) {
                Response r = new Response.Builder()
                        .body(ResponseBody.create(contentType, object.toString()))
                        .request(chain.request())
                        .protocol(originalResponse.protocol())
                        .code(originalResponse.code())
                        .message(originalResponse.message())
                        .build();
                return r;
            } else {
                return originalResponse;
            }
        }
        if (path.contains(GET_APP) ) {
            if(originalResponse != null && originalResponse.body() != null) {
                Log.d("zzz", "1");
                String bodyString = originalResponse.body().string();
                test123(bodyString);
                JSONObject jObject = null;
                try {
                    jObject = new JSONObject(bodyString);
                }
                catch (Exception e) {
                    jObject = null;
                }
                if(jObject != null) {
                    Response r = new Response.Builder()
                            .body(ResponseBody.create(originalResponse.body().contentType(), jObject.toString()))
                            .request(chain.request())
                            .protocol(originalResponse.protocol())
                            .code(originalResponse.code())
                            .message(originalResponse.message())
                            .build();
                    return r;
                }
                /*if(bodyString != null) {
                    Log.d("zzz", "2");
                    //test123(bodyString);
                }*/
            }
            return originalResponse;
        }
        return originalResponse;
    }
}
