package cm.aptoide.pt.editorialList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import cm.aptoide.analytics.implementation.navigation.ScreenTagHistory;
import cm.aptoide.aptoideviews.errors.ErrorView;
import cm.aptoide.aptoideviews.skeleton.Skeleton;
import cm.aptoide.aptoideviews.skeleton.SkeletonUtils;
import cm.aptoide.pt.R;
import cm.aptoide.pt.bonus.BonusAppcModel;
import cm.aptoide.pt.bottomNavigation.BottomNavigationActivity;
import cm.aptoide.pt.bottomNavigation.BottomNavigationItem;
import cm.aptoide.pt.editorial.CaptionBackgroundPainter;
import cm.aptoide.pt.editorial.EditorialFragment;
import cm.aptoide.pt.home.bundles.base.HomeEvent;
import cm.aptoide.pt.home.bundles.editorial.EditorialBundleViewHolder;
import cm.aptoide.pt.home.bundles.editorial.EditorialHomeEvent;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.reactions.ReactionsHomeEvent;
import cm.aptoide.pt.themes.ThemeManager;
import cm.aptoide.pt.utils.design.ShowMessage;
import cm.aptoide.pt.view.fragment.NavigationTrackFragment;
import com.google.android.material.snackbar.Snackbar;
import com.jakewharton.rxbinding.support.v4.widget.RxSwipeRefreshLayout;
import com.jakewharton.rxbinding.support.v7.widget.RxRecyclerView;
import com.jakewharton.rxbinding.view.RxView;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

public class EditorialListFragment extends NavigationTrackFragment implements EditorialListView {

  private static final String TAG = EditorialFragment.class.getName();

  /**
   * The minimum number of items to have below your current scroll position before loading more.
   */
  private static final int VISIBLE_THRESHOLD = 1;
  private static final BottomNavigationItem BOTTOM_NAVIGATION_ITEM = BottomNavigationItem.CURATION;
  @Inject public EditorialListPresenter presenter;
  @Inject CaptionBackgroundPainter captionBackgroundPainter;
  @Inject ThemeManager themeAttributeProvider;
  private BottomNavigationActivity bottomNavigationActivity;
  private RecyclerView editorialList;
  private EditorialListAdapter adapter;
  private PublishSubject<HomeEvent> uiEventsListener;
  private PublishSubject<Void> snackListener;
  private ScrollControlLinearLayoutManager layoutManager;
  private SwipeRefreshLayout swipeRefreshLayout;

  private ErrorView errorView;
  private ProgressBar progressBar;
  private ImageView userAvatar;

  private Skeleton skeleton;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getFragmentComponent(savedInstanceState).inject(this);
    uiEventsListener = PublishSubject.create();
    snackListener = PublishSubject.create();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (bottomNavigationActivity != null) {
      bottomNavigationActivity.requestFocus(BOTTOM_NAVIGATION_ITEM);
    }
    userAvatar = view.findViewById(R.id.user_actionbar_icon);
    layoutManager = new ScrollControlLinearLayoutManager(getContext());
    adapter = new EditorialListAdapter(new ArrayList<>(), new ProgressCard(), uiEventsListener,
        captionBackgroundPainter, themeAttributeProvider);
    editorialList = view.findViewById(R.id.editorial_list);
    editorialList.setLayoutManager(layoutManager);
    editorialList.setAdapter(adapter);
    editorialList.getItemAnimator()
        .setChangeDuration(0);

    swipeRefreshLayout = view.findViewById(R.id.refresh_layout);

    errorView = view.findViewById(R.id.error_view);
    progressBar = view.findViewById(R.id.progress_bar);

    skeleton =
        SkeletonUtils.applySkeleton(editorialList, R.layout.editorial_list_action_item_skeleton, 4);
    attachPresenter(presenter);
  }

  @Override public ScreenTagHistory getHistoryTracker() {
    return ScreenTagHistory.Builder.build(this.getClass()
        .getSimpleName());
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof BottomNavigationActivity) {
      bottomNavigationActivity = ((BottomNavigationActivity) activity);
    }
  }

  @Override public void onDestroy() {
    uiEventsListener = null;
    snackListener = null;
    super.onDestroy();
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.fragment_editorial_list, container, false);
  }

  @Override public Observable<EditorialHomeEvent> editorialCardClicked() {
    return uiEventsListener.filter(homeEvent -> homeEvent.getType()
        .equals(HomeEvent.Type.EDITORIAL))
        .cast(EditorialHomeEvent.class);
  }

  @Override public Observable<EditorialHomeEvent> reactionsButtonClicked() {
    return uiEventsListener.filter(homeEvent -> homeEvent.getType()
        .equals(HomeEvent.Type.REACT_SINGLE_PRESS))
        .cast(EditorialHomeEvent.class);
  }

  @Override public void showLoading() {
    errorView.setVisibility(View.GONE);
    skeleton.showSkeleton();
  }

  @Override public void hideLoading() {
    errorView.setVisibility(View.GONE);
    skeleton.showOriginal();
    swipeRefreshLayout.setVisibility(View.VISIBLE);
  }

  @Override public void showGenericError() {
    errorView.setError(ErrorView.Error.GENERIC);
    errorView.setVisibility(View.VISIBLE);
    editorialList.setVisibility(View.GONE);
    progressBar.setVisibility(View.GONE);
    if (this.swipeRefreshLayout.isRefreshing()) {
      this.swipeRefreshLayout.setRefreshing(false);
    }
  }

  @Override public void showNetworkError() {
    errorView.setError(ErrorView.Error.NO_NETWORK);
    errorView.setVisibility(View.VISIBLE);
    this.editorialList.setVisibility(View.GONE);
    this.progressBar.setVisibility(View.GONE);
    if (this.swipeRefreshLayout.isRefreshing()) {
      this.swipeRefreshLayout.setRefreshing(false);
    }
  }

  @Override public Observable<Void> retryClicked() {
    return errorView.retryClick();
  }

  @Override public Observable<Void> refreshes() {
    return RxSwipeRefreshLayout.refreshes(swipeRefreshLayout);
  }

  @Override public void hideRefresh() {
    swipeRefreshLayout.setRefreshing(false);
  }

  @Override public Observable<Void> imageClick() {
    return RxView.clicks(userAvatar);
  }

  @Override public void showAvatar() {
    userAvatar.setVisibility(View.VISIBLE);
  }

  @Override public void setDefaultUserImage() {
    ImageLoader.with(getContext())
        .loadUsingCircleTransform(R.drawable.ic_account_circle, userAvatar);
  }

  @Override public void setUserImage(String userAvatarUrl) {
    ImageLoader.with(getContext())
        .loadWithShadowCircleTransformWithPlaceholder(userAvatarUrl, userAvatar,
            R.drawable.ic_account_circle);
  }

  @Override public Observable<Object> reachesBottom() {
    return RxRecyclerView.scrollEvents(editorialList)
        .map(scroll -> isEndReached())
        .distinctUntilChanged()
        .filter(isEnd -> isEnd)
        .cast(Object.class);
  }

  @Override
  public void populateView(List<CurationCard> curationCards, BonusAppcModel bonusAppcModel) {
    editorialList.setVisibility(View.VISIBLE);
    adapter.add(curationCards, bonusAppcModel);
  }

  @Override public Observable<EditorialListEvent> visibleCards() {
    return RxRecyclerView.scrollEvents(editorialList)
        .subscribeOn(AndroidSchedulers.mainThread())
        .map(recyclerViewScrollEvent -> layoutManager.findFirstVisibleItemPosition())
        .filter(position -> position != RecyclerView.NO_POSITION)
        .distinctUntilChanged()
        .map(visibleItem -> new EditorialListEvent(adapter.getCard(visibleItem)
            .getId(), visibleItem));
  }

  @Override public void showLoadMore() {
    if (adapter != null) {
      adapter.addLoadMore();
    }
  }

  @Override public void hideLoadMore() {
    if (adapter != null) {
      adapter.removeLoadMore();
    }
  }

  @Override public void update(List<CurationCard> curationCards) {
    editorialList.setVisibility(View.VISIBLE);
    if (adapter != null) {
      adapter.update(curationCards);
    }
  }

  @Override public Observable<ReactionsHomeEvent> reactionClicked() {
    return uiEventsListener.filter(homeEvent -> homeEvent.getType()
        .equals(HomeEvent.Type.REACTION))
        .cast(ReactionsHomeEvent.class);
  }

  @Override public Observable<EditorialHomeEvent> reactionButtonLongPress() {
    return uiEventsListener.filter(homeEvent -> homeEvent.getType()
        .equals(HomeEvent.Type.REACT_LONG_PRESS))
        .cast(EditorialHomeEvent.class)
        .map(event -> {
          setScrollEnabled(false);
          return event;
        });
  }

  @Override public void showReactionsPopup(String cardId, String groupId, int bundlePosition) {
    EditorialBundleViewHolder editorialBundleViewHolder =
        getViewHolderForAdapterPosition(bundlePosition);
    if (editorialBundleViewHolder != null) {
      editorialBundleViewHolder.showReactions(cardId, groupId, bundlePosition);
    }
  }

  @Override public void showLogInDialog() {
    ShowMessage.asSnack(getActivity(), R.string.editorial_reactions_login_short, R.string.login,
        snackView -> snackListener.onNext(null), Snackbar.LENGTH_SHORT);
  }

  @Override public Observable<Void> snackLogInClick() {
    return snackListener;
  }

  @Override public void showGenericErrorToast() {
    Snackbar.make(getView(), getString(R.string.error_occured), Snackbar.LENGTH_LONG)
        .show();
  }

  @Override public void updateEditorialCard(CurationCard curationCard) {
    adapter.updateEditorialCard(curationCard);
  }

  @Override public void showNetworkErrorToast() {
    Snackbar.make(getView(), getString(R.string.connection_error), Snackbar.LENGTH_LONG)
        .show();
  }

  @Override public void setScrollEnabled(Boolean flag) {
    layoutManager.setScrollEnabled(flag);
  }

  @Override public Observable<EditorialHomeEvent> onPopupDismiss() {
    return uiEventsListener.filter(homeEvent -> homeEvent.getType()
        .equals(HomeEvent.Type.POPUP_DISMISS))
        .cast(EditorialHomeEvent.class);
  }

  private boolean isEndReached() {
    return layoutManager.getItemCount() - layoutManager.findLastVisibleItemPosition()
        <= VISIBLE_THRESHOLD;
  }

  private EditorialBundleViewHolder getViewHolderForAdapterPosition(int placeHolderPosition) {
    if (placeHolderPosition != -1) {
      EditorialBundleViewHolder placeHolderViewHolder =
          ((EditorialBundleViewHolder) editorialList.findViewHolderForAdapterPosition(
              placeHolderPosition));
      if (placeHolderViewHolder == null) {
        Log.e(TAG, "Unable to find editorialBundleViewHolder");
      }
      return placeHolderViewHolder;
    }
    return null;
  }

  @Override public void onDetach() {
    bottomNavigationActivity = null;
    super.onDetach();
  }

  @Override public void onDestroyView() {
    editorialList = null;
    adapter = null;
    layoutManager = null;
    progressBar = null;
    errorView = null;
    userAvatar = null;
    swipeRefreshLayout = null;
    super.onDestroyView();
  }
}
