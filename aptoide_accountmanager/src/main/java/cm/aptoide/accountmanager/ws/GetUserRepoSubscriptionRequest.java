/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 24/05/2016.
 */

package cm.aptoide.accountmanager.ws;

import android.content.Context;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.accountmanager.ws.responses.GetUserRepoSubscription;
import cm.aptoide.pt.networkclient.util.HashMapNotNull;
import cm.aptoide.pt.preferences.Application;
import rx.Observable;

/**
 * Created by rmateus on 16-02-2015.
 */
public class GetUserRepoSubscriptionRequest extends v3accountManager<GetUserRepoSubscription> {

  private AptoideAccountManager accountManager;

  GetUserRepoSubscriptionRequest(AptoideAccountManager accountManager) {
    super(accountManager);
    this.accountManager = accountManager;
  }

  public static GetUserRepoSubscriptionRequest of(Context context) {
    return new GetUserRepoSubscriptionRequest(AptoideAccountManager.getInstance(context,
        Application.getConfiguration()));
  }

  @Override protected Observable<GetUserRepoSubscription> loadDataFromNetwork(Interfaces interfaces,
      boolean bypassCache) {
    HashMapNotNull<String, String> parameters = new HashMapNotNull<>();

    parameters.put("mode", "json");
    parameters.put("access_token", accountManager.getAccessToken());

    return interfaces.getUserRepos(parameters);
  }
}
