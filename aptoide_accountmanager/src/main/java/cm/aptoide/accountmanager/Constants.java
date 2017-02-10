package cm.aptoide.accountmanager;

/**
 * Created by trinkes on 4/18/16.
 */
public class Constants {

  /**
   * Account type id
   */
  public static final String ACCOUNT_TYPE =
      cm.aptoide.pt.preferences.Application.getConfiguration().getAccountType();
  public static final String IS_LOCALYTICS_ENABLE_KEY = "IS_LOCALYTICS_ENABLE_KEY";
  public static final String IS_LOCALYTICS_FIRST_SESSION = "IS_LOCALYTICS_FIRST_SESSION";
  public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
  public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
  public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
  public final static String ARG_OPTIONS_BUNDLE = "BE";
  /**
   * Auth token types
   */
  public static final String AUTHTOKEN_TYPE_FULL_ACCESS_LABEL = "Full access to an Aptoide " + "account";
  public static final String AUTHTOKEN_TYPE_READ_ONLY_LABEL =
      "Read only access to an Aptoide " + "account";
  public static final String AUTHTOKEN_TYPE_FULL_ACCESS = "Full access";
  public static final String AUTHTOKEN_TYPE_READ_ONLY = "Read only";
}
