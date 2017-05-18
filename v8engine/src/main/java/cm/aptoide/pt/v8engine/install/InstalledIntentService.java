package cm.aptoide.pt.v8engine.install;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.database.accessors.AccessorFactory;
import cm.aptoide.pt.database.accessors.StoredMinimalAdAccessor;
import cm.aptoide.pt.database.realm.Installed;
import cm.aptoide.pt.database.realm.Rollback;
import cm.aptoide.pt.database.realm.StoredMinimalAd;
import cm.aptoide.pt.database.realm.Update;
import cm.aptoide.pt.dataprovider.util.DataproviderUtils;
import cm.aptoide.pt.dataprovider.ws.v7.analyticsbody.DownloadInstallAnalyticsBaseBody;
import cm.aptoide.pt.downloadmanager.AptoideDownloadManager;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.networkclient.WebService;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.v8engine.InstallManager;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.ads.AdsRepository;
import cm.aptoide.pt.v8engine.analytics.Analytics;
import cm.aptoide.pt.v8engine.crashreports.CrashReport;
import cm.aptoide.pt.v8engine.download.InstallEvent;
import cm.aptoide.pt.v8engine.install.rollback.RollbackRepository;
import cm.aptoide.pt.v8engine.install.root.RootShell;
import cm.aptoide.pt.v8engine.repository.RepositoryFactory;
import cm.aptoide.pt.v8engine.updates.UpdateRepository;
import cm.aptoide.pt.v8engine.util.referrer.ReferrerUtils;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Completable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class InstalledIntentService extends IntentService {

  private static final String TAG = InstalledIntentService.class.getName();

  private AdsRepository adsRepository;
  private RollbackRepository repository;
  private UpdateRepository updatesRepository;
  private CompositeSubscription subscriptions;
  private Analytics analytics;
  private OkHttpClient httpClient;
  private Converter.Factory converterFactory;
  private InstallManager installManager;

  public InstalledIntentService() {
    super("InstalledIntentService");
  }

  @Override public void onCreate() {
    super.onCreate();
    final AptoideAccountManager accountManager =
        ((V8Engine) getApplicationContext()).getAccountManager();
    httpClient = ((V8Engine) getApplicationContext()).getDefaultClient();
    converterFactory = WebService.getDefaultConverter();
    adsRepository =
        new AdsRepository(((V8Engine) getApplicationContext()).getIdsRepository(), accountManager,
            httpClient, converterFactory, V8Engine.getQManager());
    repository = RepositoryFactory.getRollbackRepository();
    updatesRepository = RepositoryFactory.getUpdateRepository(this);

    subscriptions = new CompositeSubscription();
    analytics = Analytics.getInstance();
    final AptoideDownloadManager downloadManager =
        ((V8Engine) getApplicationContext()).getDownloadManager();
    Installer installer = new InstallerFactory().create(this, InstallerFactory.ROLLBACK);
    installManager = new InstallManager(downloadManager, installer);
  }

  @Override protected void onHandleIntent(Intent intent) {
    if (intent != null) {
      final String action = intent.getAction();
      final String packageName = intent.getData()
          .getEncodedSchemeSpecificPart();

      confirmAction(packageName, action);

      if (!TextUtils.equals(action, Intent.ACTION_PACKAGE_REPLACED) && intent.getBooleanExtra(
          Intent.EXTRA_REPLACING, false)) {
        // do nothing if its a replacement ongoing. we are only interested in
        // already replaced apps
        return;
      }

      switch (action) {
        case Intent.ACTION_PACKAGE_ADDED:
          onPackageAdded(packageName);
          break;
        case Intent.ACTION_PACKAGE_REPLACED:
          onPackageReplaced(packageName);
          break;
        case Intent.ACTION_PACKAGE_REMOVED:
          onPackageRemoved(packageName);
          break;
      }
    }
  }

  private void confirmAction(String packageName, String action) {
    repository.getNotConfirmedRollback(packageName)
        .first()
        .filter(rollback -> shouldConfirmRollback(rollback, action))
        .subscribe(rollback -> {
          repository.confirmRollback(rollback);
        }, throwable -> throwable.printStackTrace());
  }

  protected void onPackageAdded(String packageName) {
    Logger.d(TAG, "Package added: " + packageName);

    PackageInfo packageInfo = databaseOnPackageAdded(packageName);
    checkAndBroadcastReferrer(packageName);
    sendInstallEvent(packageName, packageInfo);
  }

  protected void onPackageReplaced(String packageName) {
    Logger.d(TAG, "Packaged replaced: " + packageName);
    PackageInfo packageInfo = databaseOnPackageReplaced(packageName);
    sendInstallEvent(packageName, packageInfo);
  }

  protected void onPackageRemoved(String packageName) {
    Logger.d(TAG, "Packaged removed: " + packageName);
    databaseOnPackageRemoved(packageName);
  }

  private boolean shouldConfirmRollback(Rollback rollback, String action) {
    return rollback != null && ((rollback.getAction()
        .equals(Rollback.Action.INSTALL.name()) && action.equals(Intent.ACTION_PACKAGE_ADDED))
        || (rollback.getAction()
        .equals(Rollback.Action.UNINSTALL.name()) && action.equals(Intent.ACTION_PACKAGE_REMOVED))
        || (rollback.getAction()
        .equals(Rollback.Action.UPDATE.name()) && action.equals(Intent.ACTION_PACKAGE_REPLACED))
        || (rollback.getAction()
        .equals(Rollback.Action.DOWNGRADE.name()) && action.equals(Intent.ACTION_PACKAGE_ADDED)));
  }

  private PackageInfo databaseOnPackageAdded(String packageName) {
    PackageInfo packageInfo = AptoideUtils.SystemU.getPackageInfo(packageName);

    if (checkAndLogNullPackageInfo(packageInfo, packageName)) {
      return packageInfo;
    }
    Installed installed = new Installed(packageInfo);
    installManager.onAppInstalled(installed)
        .subscribe(() -> {
        }, throwable -> CrashReport.getInstance()
            .log(throwable));
    return packageInfo;
  }

  private void checkAndBroadcastReferrer(String packageName) {
    StoredMinimalAdAccessor storedMinimalAdAccessor =
        AccessorFactory.getAccessorFor(StoredMinimalAd.class);
    Subscription unManagedSubscription = storedMinimalAdAccessor.get(packageName)
        .flatMapCompletable(storeMinimalAd -> {
          if (storeMinimalAd != null) {
            return knockCpi(packageName, storedMinimalAdAccessor, storeMinimalAd);
          } else {
            return extractReferrer(packageName);
          }
        })
        .subscribe(__ -> { /* do nothing */ }, err -> {
          CrashReport.getInstance()
              .log(err);
        });

    subscriptions.add(unManagedSubscription);
  }

  private void sendInstallEvent(String packageName, PackageInfo packageInfo) {
    if (packageInfo != null) {
      InstallEvent event =
          (InstallEvent) analytics.get(packageName + packageInfo.versionCode, InstallEvent.class);
      if (event != null) {
        event.setPhoneRooted(RootShell.isRootAvailable());
        event.setResultStatus(DownloadInstallAnalyticsBaseBody.ResultStatus.SUCC);
        analytics.sendEvent(event);
        return;
      }

      CrashReport.getInstance()
          .log(new NullPointerException("Event is null."));
      return;
    }

    // information about the package is null so we don't broadcast an event
    CrashReport.getInstance()
        .log(new NullPointerException("PackageInfo is null."));
  }

  private PackageInfo databaseOnPackageReplaced(String packageName) {
    final Update update = updatesRepository.get(packageName)
        .first()
        .doOnError(throwable -> {
          CrashReport.getInstance()
              .log(throwable);
        })
        .onErrorReturn(throwable -> null)
        .toBlocking()
        .first();

    if (update != null && update.getPackageName() != null && update.getTrustedBadge() != null) {
      Analytics.ApplicationInstall.replaced(packageName, update.getTrustedBadge());
    }

    PackageInfo packageInfo = AptoideUtils.SystemU.getPackageInfo(packageName);

    if (checkAndLogNullPackageInfo(packageInfo, packageName)) {
      return packageInfo;
    }

    installManager.onUpdateConfirmed(new Installed(packageInfo))
        .andThen(updatesRepository.remove(update))
        .subscribe(() -> Logger.d(TAG, "databaseOnPackageReplaced: " + packageName),
            throwable -> CrashReport.getInstance()
                .log(throwable));
    return packageInfo;
  }

  private void databaseOnPackageRemoved(String packageName) {
    installManager.onAppRemoved(packageName)
        .andThen(Completable.fromAction(() -> updatesRepository.remove(packageName)))
        .subscribe(() -> Logger.d(TAG, "databaseOnPackageRemoved: " + packageName),
            throwable -> CrashReport.getInstance()
                .log(throwable));
  }

  /**
   * @param packageInfo packageInfo.
   *
   * @return true if packageInfo is null, false otherwise.
   */
  private boolean checkAndLogNullPackageInfo(PackageInfo packageInfo, String packageName) {
    if (packageInfo == null) {
      CrashReport.getInstance()
          .log(new IllegalArgumentException("PackageName null for package " + packageName));
      return true;
    } else {
      return false;
    }
  }

  private Completable knockCpi(String packageName, StoredMinimalAdAccessor storedMinimalAdAccessor,
      StoredMinimalAd storeMinimalAd) {
    return Completable.fromCallable(() -> {
      ReferrerUtils.broadcastReferrer(packageName, storeMinimalAd.getReferrer());
      DataproviderUtils.AdNetworksUtils.knockCpi(storeMinimalAd);
      storedMinimalAdAccessor.remove(storeMinimalAd);
      return null;
    });
  }

  @NonNull private Completable extractReferrer(String packageName) {
    return adsRepository.getAdsFromSecondInstall(packageName)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(minimalAd -> ReferrerUtils.extractReferrer(minimalAd, ReferrerUtils.RETRIES, true,
            adsRepository, httpClient, converterFactory))
        .toCompletable();
  }
}
