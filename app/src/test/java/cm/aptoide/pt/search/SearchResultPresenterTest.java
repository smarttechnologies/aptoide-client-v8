package cm.aptoide.pt.search;

import androidx.appcompat.widget.SearchView;
import androidx.core.util.Pair;
import cm.aptoide.pt.R;
import cm.aptoide.pt.bottomNavigation.BottomNavigationItem;
import cm.aptoide.pt.bottomNavigation.BottomNavigationMapper;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.download.view.DownloadViewActionPresenter;
import cm.aptoide.pt.home.AptoideBottomNavigator;
import cm.aptoide.pt.presenter.View;
import cm.aptoide.pt.search.analytics.SearchAnalytics;
import cm.aptoide.pt.search.model.SearchAdResult;
import cm.aptoide.pt.search.model.SearchAdResultWrapper;
import cm.aptoide.pt.search.model.SearchAppResult;
import cm.aptoide.pt.search.model.SearchAppResultWrapper;
import cm.aptoide.pt.search.model.SearchQueryModel;
import cm.aptoide.pt.search.model.Suggestion;
import cm.aptoide.pt.search.suggestions.SearchQueryEvent;
import cm.aptoide.pt.search.suggestions.SearchSuggestionManager;
import cm.aptoide.pt.search.suggestions.TrendingManager;
import cm.aptoide.pt.search.view.SearchResultFragment;
import cm.aptoide.pt.search.view.SearchResultPresenter;
import cm.aptoide.pt.search.view.SearchResultView;
import com.jakewharton.rxbinding.support.v7.widget.SearchViewQueryTextEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by D01 on 26/04/18.
 */

public class SearchResultPresenterTest {

  private static final Integer MENU_ITEM_ID_TEST = R.id.action_search;
  private PublishSubject<View.LifecycleEvent> lifecycleEvent;
  private SearchResultPresenter presenter;
  @Mock private SearchView searchView;
  @Mock private SearchResultFragment searchResultView;
  @Mock private SearchAnalytics searchAnalytics;
  @Mock private SearchNavigator searchNavigator;
  @Mock private CrashReport crashReport;
  @Mock private SearchManager searchManager;
  @Mock private TrendingManager trendingManager;
  @Mock private SearchSuggestionManager searchSuggestionManager;
  @Mock private AptoideBottomNavigator aptoideBottomNavigator;
  @Mock private BottomNavigationMapper bottomNavigationMapper;
  @Mock private Suggestion suggestion;
  @Mock private SearchQueryEvent searchQueryEvent;
  @Mock private SearchResultView.Model searchResultModel;
  @Mock private SearchAdResult searchAdResult;
  @Mock private SearchAppResult searchAppResult;
  @Mock private DownloadViewActionPresenter downloadViewActionPresenter;

  private SearchAppResultWrapper searchAppResultWrapper;
  private SearchAdResultWrapper searchAdResultWrapper;

  @Before public void setupSearchResultPresenter() {
    MockitoAnnotations.initMocks(this);

    lifecycleEvent = PublishSubject.create();

    searchAdResultWrapper = new SearchAdResultWrapper(searchAdResult, 1);
    searchAppResultWrapper = new SearchAppResultWrapper("", searchAppResult, 1);

    presenter =
        new SearchResultPresenter(searchResultView, searchAnalytics, searchNavigator, crashReport,
            Schedulers.immediate(), searchManager, trendingManager, searchSuggestionManager,
            aptoideBottomNavigator, bottomNavigationMapper, Schedulers.immediate(),
            downloadViewActionPresenter);
    //simulate view lifecycle event
    when(searchResultView.getLifecycleEvent()).thenReturn(lifecycleEvent);
  }

  @Test public void getTrendingOnStartTest() {
    presenter.getTrendingOnStart();

    List<Suggestion> suggestionList = new ArrayList<>();
    suggestionList.add(suggestion);
    //When the user goes to the search view
    when(searchResultView.searchSetup()).thenReturn(Observable.just(null));
    //It should request a trending sugestion list
    when(trendingManager.getTrendingListSuggestions()).thenReturn(Single.just(suggestionList));

    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);

    //Then it should display it
    verify(searchResultView).setTrendingList(suggestionList);
  }

  @Test public void handleToolbarClickTest() {
    presenter.handleToolbarClick();

    //When the user clicks on the search icon in the toolbar
    when(searchResultView.toolbarClick()).thenReturn(Observable.just(null));
    when(searchResultView.shouldFocusInSearchBar()).thenReturn(false);

    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);

    //Then it should send the necessary analytics and focus in the searchBar
    verify(searchAnalytics).searchStart(any(), anyBoolean());
    verify(searchResultView).focusInSearchBar();
  }

  @Test public void handleSearchMenuItemClickTest() {
    presenter.handleSearchMenuItemClick();

    //When the user clicks on the search icon in the toolbar and has something already written
    when(searchResultView.searchMenuItemClick()).thenReturn(Observable.just(null));
    when(searchResultView.shouldFocusInSearchBar()).thenReturn(false);

    lifecycleEvent.onNext(View.LifecycleEvent.RESUME);

    //Then it should send the necessary analytics and focus in the searchBar
    verify(searchAnalytics).searchStart(any(), anyBoolean());
    verify(searchResultView).focusInSearchBar();
  }

  @Test public void focusInSearchBarTest() {
    presenter.focusInSearchBar();

    //When the user goes to the search view
    when(searchResultView.searchSetup()).thenReturn(Observable.just(null));
    when(searchResultView.shouldFocusInSearchBar()).thenReturn(true);

    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);

    //Then it should focus on the search bar
    verify(searchResultView).focusInSearchBar();
  }

  @Test public void handleSuggestionClickedTest() {
    presenter.handleSuggestionClicked();

    //When the user clicks on a sugestion
    when(searchResultView.listenToSuggestionClick()).thenReturn(
        Observable.just(new Pair<>("a", searchQueryEvent)));
    when(searchQueryEvent.hasQuery()).thenReturn(true);
    when(searchQueryEvent.isSubmitted()).thenReturn(true);
    when(searchQueryEvent.getQuery()).thenReturn("anyString");

    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);

    //Then it should make the search view disappear and navigate to correspondent result
    verify(searchResultView).collapseSearchBar(anyBoolean());
    verify(searchResultView).hideSuggestionsViews();
    verify(searchNavigator).navigate(any(SearchQueryModel.class));
  }

  @Test public void handleFragmentRestorationVisibilityTest() {
    presenter.handleFragmentRestorationVisibility();

    when(searchResultView.searchSetup()).thenReturn(Observable.just(null));
    when(searchResultView.shouldFocusInSearchBar()).thenReturn(false);
    when(searchResultView.shouldShowSuggestions()).thenReturn(true);

    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);

    verify(searchResultView).setVisibilityOnRestore();
  }

  @Test public void handleClickToOpenAppViewFromItemTest() {
    presenter.handleClickToOpenAppViewFromItem();

    //When the user clicks on an item from the search result list
    when(searchResultView.getViewModel()).thenReturn(searchResultModel);
    when(searchResultView.onViewItemClicked()).thenReturn(Observable.just(searchAppResultWrapper));
    when(searchAppResult.getPackageName()).thenReturn("random");
    when(searchAppResult.getAppId()).thenReturn((long) 0);
    when(searchAppResult.getStoreName()).thenReturn("random");
    when(searchResultModel.getSearchQueryModel()).thenReturn(new SearchQueryModel("non-empty"));
    when(searchResultModel.getStoreTheme()).thenReturn("non-empty");
    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);

    //It should send the necessary analytics and navigate to the app's App view
    //verify(searchManager).recordAbTestAction();
    verify(searchAnalytics).searchAppClick(new SearchQueryModel("non-empty"), "random", 1, false);
    verify(searchNavigator).goToAppView(anyLong(), eq("random"), anyString(), eq("random"));
  }

  @Test public void handleSuggestionQueryTextSubmittedTest() {
    presenter.handleQueryTextSubmitted();

    //When the user clicks on one of the suggestions
    when(searchResultView.searchSetup()).thenReturn(Observable.just(null));
    when(searchResultView.onQueryTextChanged()).thenReturn(Observable.just(searchQueryEvent));
    when(searchQueryEvent.hasQuery()).thenReturn(true);
    when(searchQueryEvent.isSubmitted()).thenReturn(true);
    when(searchQueryEvent.isSuggestion()).thenReturn(true);
    when(searchQueryEvent.getQuery()).thenReturn("anyString");

    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);

    //Then it should navigate to the results and send the necessary analytics
    verify(searchResultView).collapseSearchBar(false);
    verify(searchResultView).hideSuggestionsViews();
    verify(searchAnalytics).search(any(SearchQueryModel.class));
    verify(searchNavigator).navigate(any(SearchQueryModel.class));
  }

  @Test public void handleNoSuggestionQueryTextSubmittedTest() {
    presenter.handleQueryTextSubmitted();

    //When the user submits a query and it's not a suggestion
    when(searchResultView.searchSetup()).thenReturn(Observable.just(null));
    when(searchResultView.onQueryTextChanged()).thenReturn(Observable.just(searchQueryEvent));
    when(searchQueryEvent.hasQuery()).thenReturn(true);
    when(searchQueryEvent.isSubmitted()).thenReturn(true);
    when(searchQueryEvent.isSuggestion()).thenReturn(false);
    when(searchQueryEvent.getQuery()).thenReturn("anyString");

    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);

    //Then it should navigate to the results and send the necessary analytics
    verify(searchResultView).collapseSearchBar(false);
    verify(searchResultView).hideSuggestionsViews();
    verify(searchNavigator).navigate(any(SearchQueryModel.class));
    verify(searchAnalytics).search(any(SearchQueryModel.class));
  }

  @Test public void handleQueryTextChangedTest() {
    presenter.handleQueryTextChanged();

    //When the query from the user changes
    when(searchResultView.searchSetup()).thenReturn(Observable.just(null));
    when(searchResultView.onQueryTextChanged()).thenReturn(Observable.just(searchQueryEvent));
    when(searchQueryEvent.hasQuery()).thenReturn(true);
    when(searchQueryEvent.isSubmitted()).thenReturn(false);
    when(searchQueryEvent.getQuery()).thenReturn("non-empty");
    List<String> suggestionList = new ArrayList<>();
    suggestionList.add("non-empty");
    when(searchSuggestionManager.getSuggestionsForApp("non-empty")).thenReturn(
        Single.just(suggestionList));

    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);

    //It should refresh the list of suggestions
    verify(searchResultView).setUnsubmittedQuery("non-empty");
    verify(searchResultView).setSuggestionsList(any());
    verify(searchResultView).toggleSuggestionsView();
  }

  @Test public void handleQueryTextCleanedTest() {
    presenter.handleQueryTextCleaned();

    //When the user clears what he/she was searching
    when(searchResultView.onQueryTextChanged()).thenReturn(Observable.just(searchQueryEvent));
    when(searchQueryEvent.hasQuery()).thenReturn(false);
    when(searchResultView.isSearchViewExpanded()).thenReturn(true);

    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);

    //Then it should show the trending suggestions
    verify(searchResultView).clearUnsubmittedQuery();
    verify(searchResultView).toggleTrendingView();
  }

  @Test public void handleClickOnBottomNavWithResultsTest() {
    presenter.handleClickOnBottomNavWithResults();

    //When the user clicks the BottomNavigation search item and has results
    when(aptoideBottomNavigator.navigationEvent()).thenReturn(Observable.just(MENU_ITEM_ID_TEST));
    when(bottomNavigationMapper.mapItemClicked(MENU_ITEM_ID_TEST)).thenReturn(
        BottomNavigationItem.SEARCH);
    when(searchResultView.hasResults()).thenReturn(true);

    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);

    //Then it should navigate to the top
    verify(searchResultView).scrollToTop();
  }

  @Test public void handleClickOnBottomNavWithoutResultsTest() {
    presenter.handleClickOnBottomNavWithoutResults();

    //When the user clicks the BottomNavigation search item and has no results
    when(aptoideBottomNavigator.navigationEvent()).thenReturn(Observable.just(MENU_ITEM_ID_TEST));
    when(bottomNavigationMapper.mapItemClicked(MENU_ITEM_ID_TEST)).thenReturn(
        BottomNavigationItem.SEARCH);
    when(searchResultView.hasResults()).thenReturn(false);

    lifecycleEvent.onNext(View.LifecycleEvent.CREATE);

    //Then it should focus in the search bar
    verify(searchResultView).focusInSearchBar();
  }

  @Test public void listenToSearchQueriesTest() {
    presenter.listenToSearchQueries();

    when(searchResultView.searchSetup()).thenReturn(Observable.just(null));
    SearchViewQueryTextEvent searchViewQueryTextEvent =
        SearchViewQueryTextEvent.create(searchView, "", true);
    when(searchResultView.queryChanged()).thenReturn(Observable.just(searchViewQueryTextEvent));

    lifecycleEvent.onNext(View.LifecycleEvent.RESUME);

    verify(searchResultView).queryEvent(any());
  }
}
