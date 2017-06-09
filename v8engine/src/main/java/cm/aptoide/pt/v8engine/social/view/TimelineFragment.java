package cm.aptoide.pt.v8engine.social.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import cm.aptoide.pt.dataprovider.ws.v7.store.StoreContext;
import cm.aptoide.pt.networkclient.WebService;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.crashreports.CrashReport;
import cm.aptoide.pt.v8engine.link.LinksHandlerFactory;
import cm.aptoide.pt.v8engine.social.data.Article;
import cm.aptoide.pt.v8engine.social.data.CardTouchEvent;
import cm.aptoide.pt.v8engine.social.data.CardViewHolderFactory;
import cm.aptoide.pt.v8engine.social.data.SocialManager;
import cm.aptoide.pt.v8engine.social.data.SocialService;
import cm.aptoide.pt.v8engine.social.data.TimelineResponseCardMapper;
import cm.aptoide.pt.v8engine.social.presenter.TimelinePresenter;
import cm.aptoide.pt.v8engine.timeline.PackageRepository;
import cm.aptoide.pt.v8engine.util.DateCalculator;
import cm.aptoide.pt.v8engine.view.fragment.FragmentView;
import cm.aptoide.pt.v8engine.view.recycler.RecyclerViewPositionHelper;
import cm.aptoide.pt.v8engine.view.recycler.displayable.SpannableFactory;
import com.jakewharton.rxbinding.support.v4.widget.RxSwipeRefreshLayout;
import com.jakewharton.rxbinding.support.v7.widget.RxRecyclerView;
import com.jakewharton.rxbinding.view.RxView;
import java.util.Collections;
import java.util.List;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Created by jdandrade on 31/05/2017.
 */

public class TimelineFragment extends FragmentView implements TimelineView {

  public static final int LATEST_PACKAGES_COUNT = 20;
  public static final int RANDOM_PACKAGES_COUNT = 10;
  private static final String ACTION_KEY = "action";
  /**
   * The minimum number of items to have below your current scroll position before loading more.
   */
  private final int visibleThreshold = 5;
  private CardAdapter adapter;
  private PublishSubject<CardTouchEvent> articleSubject;
  private RecyclerView list;
  private ProgressBar progressBar;
  private SwipeRefreshLayout swipeRefreshLayout;
  private RecyclerViewPositionHelper helper;
  private View genericError;
  private View retryButton;

  public static Fragment newInstance(String action, Long userId, Long storeId,
      StoreContext storeContext) {
    final Bundle args = new Bundle();
    Fragment fragment = new TimelineFragment();
    args.putString(ACTION_KEY, action);
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    LinksHandlerFactory linksHandlerFactory = new LinksHandlerFactory(getContext());
    attachPresenter(new TimelinePresenter(this, new SocialManager(
        new SocialService(getArguments().getString(ACTION_KEY),
            ((V8Engine) getContext().getApplicationContext()).getBaseBodyInterceptorV7(),
            ((V8Engine) getContext().getApplicationContext()).getDefaultClient(),
            WebService.getDefaultConverter(),
            new PackageRepository(getContext().getPackageManager()), LATEST_PACKAGES_COUNT,
            RANDOM_PACKAGES_COUNT, new TimelineResponseCardMapper(), linksHandlerFactory, 20, 0,
            Integer.MAX_VALUE)), CrashReport.getInstance()), savedInstanceState);
    articleSubject = PublishSubject.create();
    adapter = new CardAdapter(Collections.emptyList(),
        new CardViewHolderFactory(articleSubject, new DateCalculator(), new SpannableFactory()));
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.fragment_timeline, container, false);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    genericError = view.findViewById(R.id.generic_error);
    retryButton = genericError.findViewById(R.id.retry);
    progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
    list = (RecyclerView) view.findViewById(R.id.fragment_cards_list);
    list.setAdapter(adapter);
    list.setLayoutManager(new LinearLayoutManager(getContext()));
    helper = RecyclerViewPositionHelper.createHelper(list);
    // Pull-to-refresh
    swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
    swipeRefreshLayout.setColorSchemeResources(R.color.default_progress_bar_color,
        R.color.default_color, R.color.default_progress_bar_color, R.color.default_color);

    //super needs to be call last because the presenter will try to access this local variables.
    super.onViewCreated(view, savedInstanceState);
  }

  @Override public void showCards(List<Article> cards) {
    adapter.updateCards(cards);
    genericError.setVisibility(View.GONE);
    progressBar.setVisibility(View.GONE);
    list.setVisibility(View.VISIBLE);
  }

  @Override public void showProgressIndicator() {
    list.setVisibility(View.GONE);
    genericError.setVisibility(View.GONE);
    progressBar.setVisibility(View.VISIBLE);
  }

  @Override public void hideProgressIndicator() {
    list.setVisibility(View.VISIBLE);
    swipeRefreshLayout.setVisibility(View.VISIBLE);
    progressBar.setVisibility(View.GONE);
  }

  @Override public void hideRefresh() {
    swipeRefreshLayout.setRefreshing(false);
  }

  @Override public void showMoreCards(List<Article> cards) {
    adapter.addCards(cards);
  }

  @Override public void showGenericError() {
    this.genericError.setVisibility(View.VISIBLE);
    this.list.setVisibility(View.GONE);
    this.progressBar.setVisibility(View.GONE);
    this.swipeRefreshLayout.setVisibility(View.GONE);
    if (this.swipeRefreshLayout.isRefreshing()) {
      this.swipeRefreshLayout.setRefreshing(false);
    }
  }

  @Override public rx.Observable<Void> refreshes() {
    return RxSwipeRefreshLayout.refreshes(swipeRefreshLayout);
  }

  @Override public Observable<Void> reachesBottom() {
    return RxRecyclerView.scrollEvents(list)
        .filter(event -> helper != null
            && event.view()
            .isAttachedToWindow()
            && (helper.getItemCount() - event.view()
            .getChildCount()) <= ((helper.findFirstVisibleItemPosition() == -1 ? 0
            : helper.findFirstVisibleItemPosition()) + visibleThreshold))
        .map(event -> null)
        .cast(Void.class);
  }

  @Override public Observable<CardTouchEvent> articleClicked() {
    return articleSubject;
  }

  @Override public Observable<Void> retry() {
    return RxView.clicks(retryButton);
  }
}
