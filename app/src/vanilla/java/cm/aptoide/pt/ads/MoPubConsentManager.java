package cm.aptoide.pt.ads;

import rx.Single;

public class MoPubConsentManager implements MoPubConsentDialogManager, MoPubConsentDialogView {


  public MoPubConsentManager() {
  }

  @Override public void showConsentDialog() {
  }

  @Override public Single<Boolean> shouldShowConsentDialog() {
    return Single.just(false);
  }
}
