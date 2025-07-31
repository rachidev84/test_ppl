package nemosofts.streambox.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import nemosofts.streambox.R;
import nemosofts.streambox.item.ItemCat;
import nemosofts.streambox.util.ApplicationUtil;

public class AdapterCategory extends RecyclerView.Adapter<AdapterCategory.ViewHolder> {

    private final Context context;
    private List<ItemCat> arrayList;
    private final List<ItemCat> filteredArrayList;
    private final RecyclerItemClickListener listener;
    private int row_index = -1;
    private NameFilter filter;
    private final Boolean isTvBox;
    private Boolean isRequestFocus = true;

    public static class ViewHolder extends RecyclerView.ViewHolder{

        private final TextView tv_cat_mob, tv_cat_tv;
        private final View vw_cat_mob, vw_cat_tv;

        public ViewHolder(View itemView) {
            super(itemView);
            tv_cat_mob = itemView.findViewById(R.id.tv_cat_mob);
            vw_cat_mob = itemView.findViewById(R.id.vw_cat_mob);

            tv_cat_tv = itemView.findViewById(R.id.tv_cat_tv);
            vw_cat_tv = itemView.findViewById(R.id.vw_cat_tv);
        }
    }

    public AdapterCategory(Context context,List<ItemCat> arrayList, RecyclerItemClickListener listener) {
        this.context = context;
        this.arrayList = arrayList;
        this.filteredArrayList = arrayList;
        this.listener = listener;
        isTvBox  = ApplicationUtil.isTvBox(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_category,parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ItemCat currentItem = arrayList.get(position);

        holder.tv_cat_tv.setVisibility(isTvBox ? View.VISIBLE : View.GONE);
        holder.tv_cat_mob.setVisibility(isTvBox ? View.GONE : View.VISIBLE);

        holder.tv_cat_tv.setText(currentItem.getName());
        holder.tv_cat_tv.setOnClickListener(v -> {
            row_index = position;
            notifyDataSetChanged(); // Highlight selected item
            listener.onClickListener(getPosition(currentItem.getId()));
        });

        holder.tv_cat_mob.setText(currentItem.getName());
        holder.tv_cat_mob.setOnClickListener(v -> {
            row_index = position;
            notifyDataSetChanged(); // Highlight selected item
            listener.onClickListener(getPosition(currentItem.getId()));
        });

        if (row_index == position) {
            holder.tv_cat_mob.setTextColor(ContextCompat.getColor(context, R.color.color_select));
            holder.tv_cat_tv.setTextColor(ContextCompat.getColor(context, R.color.color_select));
            if (isTvBox) {
                if (isRequestFocus){
                    holder.tv_cat_tv.requestFocus();
                }

                holder.vw_cat_mob.setVisibility(View.GONE);
                holder.vw_cat_tv.setVisibility(View.VISIBLE);
            } else {
                holder.vw_cat_mob.setVisibility(View.VISIBLE);
                holder.vw_cat_tv.setVisibility(View.GONE);
            }
        } else {
            holder.tv_cat_mob.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.vw_cat_mob.setVisibility(View.GONE);

            holder.tv_cat_tv.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.vw_cat_tv.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public interface RecyclerItemClickListener{
        void onClickListener(int position);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void select(int position) {
        row_index = position;
        notifyDataSetChanged();
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
                isRequestFocus = false;
            } else {
                synchronized (this) {
                    result.values = filteredArrayList;
                    result.count = filteredArrayList.size();
                }
                isRequestFocus = true;
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
}