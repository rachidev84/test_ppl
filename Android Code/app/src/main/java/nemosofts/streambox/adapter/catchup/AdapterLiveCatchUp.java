package nemosofts.streambox.adapter.catchup;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import nemosofts.streambox.R;
import nemosofts.streambox.item.ItemLive;

public class AdapterLiveCatchUp extends RecyclerView.Adapter<AdapterLiveCatchUp.ViewHolder> {

    private List<ItemLive> arrayList;
    private final List<ItemLive> filteredArrayList;
    private NameFilter filter;
    private final RecyclerItemClickListener listener;

    public static class ViewHolder extends RecyclerView.ViewHolder{

        private final TextView tvCategory;
        private final RelativeLayout rlCategory;
        private final ImageView iv_catch_up;

        public ViewHolder(View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_cat);
            rlCategory = itemView.findViewById(R.id.rl_cat);
            iv_catch_up = itemView.findViewById(R.id.iv_catch_up);
        }
    }

    public AdapterLiveCatchUp(List<ItemLive> arrayList, RecyclerItemClickListener listener) {
        this.arrayList = arrayList;
        this.filteredArrayList = arrayList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_cat_catch_up,parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvCategory.setText(arrayList.get(position).getName());
        try {
            Picasso.get()
                    .load(arrayList.get(position).getStreamIcon().isEmpty() ? "null" : arrayList.get(position).getStreamIcon())
                    .placeholder(R.drawable.bg_card_item_load)
                    .into(holder.iv_catch_up);
        } catch (Exception e) {
            e.printStackTrace();
        }

        holder.rlCategory.setOnClickListener(v -> listener.onClickListener(getPosition(arrayList.get(position).getStreamID())));
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    private int getPosition(String id) {
        for (int i = 0; i < filteredArrayList.size(); i++) {
            if (id.equals(filteredArrayList.get(i).getStreamID())) {
                return i;
            }
        }
        return -1; // Not found
    }

    public Filter getFilter() {
        if (filter == null) {
            filter = new NameFilter();
        }
        return filter;
    }

    private class NameFilter extends Filter {

        @NonNull
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            constraint = constraint.toString().toLowerCase();
            FilterResults result = new FilterResults();
            if (!constraint.toString().isEmpty()) {
                ArrayList<ItemLive> filteredItems = new ArrayList<>();
                for (int i = 0, l = filteredArrayList.size(); i < l; i++) {
                    String nameList = filteredArrayList.get(i).getName();
                    if (nameList.toLowerCase().contains(constraint))
                        filteredItems.add(filteredArrayList.get(i));
                }
                result.count = filteredItems.size();
                result.values = filteredItems;
            } else {
                synchronized (this) {
                    result.values = filteredArrayList;
                    result.count = filteredArrayList.size();
                }
            }
            return result;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        protected void publishResults(CharSequence constraint, @NonNull FilterResults results) {
            @SuppressWarnings("unchecked")
            ArrayList<ItemLive> filteredItems = (ArrayList<ItemLive>) results.values;
            arrayList = filteredItems;
            notifyDataSetChanged();
        }
    }

    public interface RecyclerItemClickListener{
        void onClickListener(int position);
    }
}
