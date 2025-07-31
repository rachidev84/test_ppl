package nemosofts.streambox.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import nemosofts.streambox.R;
import nemosofts.streambox.item.ItemRadioButton;
import nemosofts.streambox.util.ApplicationUtil;

public class AdapterRadioButton extends RecyclerView.Adapter<AdapterRadioButton.ViewHolder> {

    private final List<ItemRadioButton> arrayList;
    private int row_index_id = 0;
    private final Boolean isTvBox;

    public static class ViewHolder extends RecyclerView.ViewHolder{

        private final RadioButton rd_item;

        public ViewHolder(View itemView) {
            super(itemView);
            rd_item = itemView.findViewById(R.id.rd_item);
        }
    }

    public AdapterRadioButton(Context context, List<ItemRadioButton> arrayList, int position) {
        this.arrayList = arrayList;
        this.row_index_id = position;
        isTvBox  = ApplicationUtil.isTvBox(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_radio_button,parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemRadioButton currentItem = arrayList.get(position);

        holder.rd_item.setText(currentItem.getName());

        holder.rd_item.setChecked(row_index_id == currentItem.getId());
        if (isTvBox && row_index_id == currentItem.getId()){
            holder.rd_item.requestFocus();
        }

        holder.rd_item.setOnClickListener(v -> {
            row_index_id = arrayList.get(holder.getAbsoluteAdapterPosition()).getId();
            notifyDataSetChanged();
        });
    }

    public int getData() {
       return row_index_id;
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

}
