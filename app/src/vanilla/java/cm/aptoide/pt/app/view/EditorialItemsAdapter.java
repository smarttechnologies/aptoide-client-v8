package cm.aptoide.pt.app.view;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import cm.aptoide.pt.R;
import java.util.List;

/**
 * Created by D01 on 28/08/2018.
 */

class EditorialItemsAdapter extends RecyclerView.Adapter<EditorialItemsViewHolder> {

  private List<EditorialContent> editorialItemList;

  public EditorialItemsAdapter(List<EditorialContent> editorialItemList) {
    this.editorialItemList = editorialItemList;
  }

  @Override public EditorialItemsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new EditorialItemsViewHolder(LayoutInflater.from(parent.getContext())
        .inflate(R.layout.editorial_item_layout, parent, false));
  }

  @Override
  public void onBindViewHolder(EditorialItemsViewHolder editorialItemsViewHolder, int position) {
    editorialItemsViewHolder.setVisibility(editorialItemList.get(position), position);
  }

  @Override public int getItemCount() {
    Log.d("TAG123", "size: " + editorialItemList.size());
    return editorialItemList.size();
  }

  public void update(List<EditorialContent> editorialItemList) {
    this.editorialItemList = editorialItemList;
    notifyDataSetChanged();
  }

  public void add(List<EditorialContent> editorialItemList) {
    this.editorialItemList.addAll(editorialItemList);
    notifyDataSetChanged();
  }

  public void remove(int itemPosition) {
    editorialItemList.remove(itemPosition);
    notifyItemRemoved(itemPosition);
  }
}