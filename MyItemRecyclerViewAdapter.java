package edu.vassar.cmpu203.myfirstapplication.View;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import edu.vassar.cmpu203.myfirstapplication.View.placeholder.PlaceholderContent.PlaceholderItem;
import edu.vassar.cmpu203.myfirstapplication.databinding.FragmentViewFavoritesBinding;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PlaceholderItem}.
 * TODO: Replace the implementation with code for your data type.
 */

public class MyItemRecyclerViewAdapter extends RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder> {

    private final List<PlaceholderItem> mValues;

    /**
     * Constructor for the adapter.
     * @param items
     */
    public MyItemRecyclerViewAdapter(List<PlaceholderItem> items) {
        mValues = items;
    }

    /**
     * Creates a new ViewHolder which inflates the layout and binds it to the View Favorites Fragment.
     * @param parent   The ViewGroup to which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return ViewHolder
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentViewFavoritesBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    /**
     * Updates the Text of items in the ViewHolder.
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(mValues.get(position).id);
        holder.mContentView.setText(mValues.get(position).content);
    }

    /**
     * Returns the number of items in mValues.
     * @return size of mValues
     */
    @Override
    public int getItemCount() {
        return mValues.size();
    }

    /**
     * ViewHolder class. Extends RecyclerView.ViewHolder.
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mIdView;
        public final TextView mContentView;
        public PlaceholderItem mItem;

        /**
         * Constructor for the ViewHolder. Binds itemNumber and content to the View Favorites Fragment.
         * @param binding
         */
        public ViewHolder(FragmentViewFavoritesBinding binding) {
            super(binding.getRoot());
            mIdView = binding.itemNumber;
            mContentView = binding.content;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}