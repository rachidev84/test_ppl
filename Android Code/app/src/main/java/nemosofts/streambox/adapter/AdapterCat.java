package nemosofts.streambox.adapter;

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

import java.util.ArrayList;
import java.util.List;

import nemosofts.streambox.R;
import nemosofts.streambox.item.ItemCat;

public class AdapterCat extends RecyclerView.Adapter<AdapterCat.ViewHolder> {

    private List<ItemCat> arrayList;
    private final List<ItemCat> filteredArrayList;
    private NameFilter filter;
    private final RecyclerItemClickListener listener;
    private boolean is_page = true;

    public static class ViewHolder extends RecyclerView.ViewHolder{

        private final ImageView iv_catch_up;
        private final TextView tvCategory;
        private final RelativeLayout rlCategory;

        public ViewHolder(View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_cat);
            rlCategory = itemView.findViewById(R.id.rl_cat);
            iv_catch_up = itemView.findViewById(R.id.iv_catch_up);
        }
    }

    public AdapterCat(boolean is_page, List<ItemCat> arrayList, RecyclerItemClickListener listener) {
        this.is_page = is_page;
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
        if (is_page){
            holder.iv_catch_up.setImageResource(R.drawable.ic_folders);
        }
        holder.rlCategory.setOnClickListener(v -> listener.onClickListener(getPosition(arrayList.get(position).getId())));
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    private int getPosition(String id) {
        for (int i = 0; i < filteredArrayList.size(); i++) {
            if (id.equals(filteredArrayList.get(i).getId())) {
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
                ArrayList<ItemCat> filteredItems = new ArrayList<>();
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
            ArrayList<ItemCat> filteredItems = (ArrayList<ItemCat>) results.values;
            arrayList = filteredItems;
            notifyDataSetChanged();
        }
    }

    public interface RecyclerItemClickListener{
        void onClickListener(int position);
    }
}
