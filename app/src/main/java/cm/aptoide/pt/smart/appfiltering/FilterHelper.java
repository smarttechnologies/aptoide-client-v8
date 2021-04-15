package cm.aptoide.pt.smart.appfiltering;

import androidx.annotation.Nullable;

import cm.aptoide.pt.smart.appfiltering.data.GetAppResponse;
import cm.aptoide.pt.smart.appfiltering.data.Version;
import cm.aptoide.pt.smart.appfiltering.data.Versions;
import cm.aptoide.pt.smart.appfiltering.data.File;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

public class FilterHelper {
    private static final String APK_LINK = "https://pool.apk.aptoide.com/";

    @Nullable
    public JSONObject filterResponse(String jsonResponse, List<AppToRemove> filterList) {
        Gson gson = new Gson();
        GetAppResponse response = gson.fromJson(jsonResponse, GetAppResponse.class);
        response.getNodes().setVersions(removeVersions(response.getNodes().getVersions(), filterList));
        rebuildLatestVersionIfNeeded(response, filterList);
        try {
            JSONObject ob = new JSONObject(gson.toJson(response));
            return ob;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public JSONObject filterVersionRequest(String jsonResponse, List<AppToRemove> filterList) {
        Gson gson = new Gson();
        Versions response = gson.fromJson(jsonResponse, Versions.class);

        response = removeVersions(response, filterList);
        try {
            JSONObject ob = new JSONObject(gson.toJson(response));
            return ob;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void rebuildLatestVersionIfNeeded(GetAppResponse response, List<AppToRemove> filterList) {
        boolean shouldRebuild = shouldRebuildLatestVersion(getLatestVersion(response), filterList);
        if (shouldRebuild) {
            List<Version> versions = response.getNodes().getVersions().getList();
            if (!versions.isEmpty()) {
                Version maxVersion = versions.get(0);
                for (Version version : versions) {
                    if (version.getFile().getVercode() > maxVersion.getFile().getVercode() ) {
                        maxVersion = version;
                    }
                }
                rebuildLatestVersion(response, maxVersion);
            }
        }
    }

    private void rebuildLatestVersion(GetAppResponse response, Version version) {
        String apkLink = buildLinkForTheSpecificVersion(response, version);
        File apkFile = response.getNodes().getMeta().getData().getFile();
        apkFile.setVername(version.getFile().getVername());
        apkFile.setVercode(version.getFile().getVercode());
        apkFile.setPath(apkLink);
        apkFile.setMd5sum(version.getFile().getMd5sum());
    }



    private String getLatestVersion(GetAppResponse response) {
        return response.getNodes().getMeta().getData().getFile().getVername();
    }

    private boolean shouldRebuildLatestVersion(String latestVersion, List<AppToRemove> filterList) {
        boolean result = false;
        for(AppToRemove app: filterList) {
            if (latestVersion.equals(app.getVersion())) {
                result = true;
                break;
            }
        }

        return result;
    }

    private String buildLinkForTheSpecificVersion(GetAppResponse response, Version version) {
        String link = APK_LINK  + version.getStore().getName()
                + "/" + version.getPackage().replace(".", "-") +
                "-" + version.getFile().getVercode() + "-" +
                version.getId() + "-" + version.getFile().getMd5sum()
                + ".apk";
        return link;
    }



    private Versions removeVersions(Versions versions, List<AppToRemove> appsToRemove) {
        for (AppToRemove app: appsToRemove) {
            removeVersion(versions, app.getVersion());
        }
        return versions;
    }

    private void removeVersion(Versions versions, String version) {
        List<Version> versionsList = versions.getList();
        Iterator<Version> iterator = versionsList.listIterator();

        while (iterator.hasNext()) {
            if (iterator.next().getFile().getVername().equals(version)) {
                iterator.remove();
                break;
            }
        }
    }
}
