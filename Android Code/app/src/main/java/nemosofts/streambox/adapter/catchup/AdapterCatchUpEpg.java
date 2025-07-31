package nemosofts.streambox.adapter.catchup;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import nemosofts.streambox.R;
import nemosofts.streambox.item.ItemEpgFull;
import nemosofts.streambox.util.ApplicationUtil;

public class AdapterCatchUpEpg extends RecyclerView.Adapter<AdapterCatchUpEpg.ViewHolder> {

    private final List<ItemEpgFull> arrayList;
    private final RecyclerItemClickListener listener;
    private final Boolean is_12h;

    public static class ViewHolder extends RecyclerView.ViewHolder{

        private final TextView tv_epg_title, tv_epg_date, tv_active, tv_none;
        private final LinearLayout ll_epg_full;

        public ViewHolder(View itemView) {
            super(itemView);
            tv_epg_title = itemView.findViewById(R.id.tv_epg_title);
            tv_epg_date = itemView.findViewById(R.id.tv_epg_date);
            ll_epg_full = itemView.findViewById(R.id.ll_epg_full);
            tv_active = itemView.findViewById(R.id.tv_active);
            tv_none = itemView.findViewById(R.id.tv_none);
        }
    }

    public AdapterCatchUpEpg(Boolean is_12h, List<ItemEpgFull> arrayList, RecyclerItemClickListener listener) {
        this.arrayList = arrayList;
        this.listener = listener;
        this.is_12h = is_12h;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_tab_epg_full,parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tv_active.setVisibility(arrayList.get(position).getHasArchive() == 1 ? View.VISIBLE : View.GONE);
        holder.tv_none.setVisibility(arrayList.get(position).getHasArchive() == 1 ? View.GONE : View.VISIBLE);
        holder.tv_epg_title.setText(ApplicationUtil.decodeBase64(arrayList.get(position).getTitle()));
        holder.tv_epg_date.setText(ApplicationUtil.getTimestamp(arrayList.get(position).getStartTimestamp(), is_12h) + " - " + ApplicationUtil.getTimestamp(arrayList.get(position).getStopTimestamp(), is_12h));
        holder.ll_epg_full.setOnClickListener(v -> listener.onClickListener(arrayList.get(holder.getAbsoluteAdapterPosition()), holder.getAbsoluteAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public interface RecyclerItemClickListener{
        void onClickListener(ItemEpgFull item, int position);
    }
}
