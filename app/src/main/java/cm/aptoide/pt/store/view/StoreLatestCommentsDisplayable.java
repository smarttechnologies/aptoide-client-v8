package cm.aptoide.pt.store.view;

import cm.aptoide.pt.R;
import cm.aptoide.pt.dataprovider.model.v7.Comment;
import cm.aptoide.pt.themes.ThemeManager;
import cm.aptoide.pt.view.recycler.displayable.Displayable;
import java.util.Collections;
import java.util.List;

public class StoreLatestCommentsDisplayable extends Displayable {

  private final long storeId;
  private final List<Comment> comments;
  private String storeName;
  private ThemeManager themeManager;

  public StoreLatestCommentsDisplayable() {
    this.storeId = -1;
    this.comments = Collections.emptyList();
  }

  public StoreLatestCommentsDisplayable(long storeId, String storeName, List<Comment> comments,
      ThemeManager themeManager) {
    this.storeId = storeId;
    this.storeName = storeName;
    this.comments = comments;
    this.themeManager = themeManager;
  }

  public List<Comment> getComments() {
    return comments;
  }

  public long getStoreId() {
    return storeId;
  }

  @Override protected Configs getConfig() {
    return new Configs(1, true);
  }

  @Override public int getViewLayout() {
    return R.layout.displayable_grid_latest_store_comments;
  }

  public String getStoreName() {
    return storeName;
  }

  public ThemeManager getThemeManager() {
    return themeManager;
  }
}
