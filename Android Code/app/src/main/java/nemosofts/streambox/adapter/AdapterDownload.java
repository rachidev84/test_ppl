package nemosofts.streambox.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.nemosofts.view.ImageHelperView;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

import nemosofts.streambox.R;
import nemosofts.streambox.dialog.DialogUtil;
import nemosofts.streambox.item.ItemVideoDownload;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.helper.DBHelper;

public class AdapterDownload extends RecyclerView.Adapter<AdapterDownload.MyViewHolder> {

    private final DBHelper dbHelper;
    private final Context context;
    private final List<ItemVideoDownload> arrayList;
    private final RecyclerItemClickListener listener;
    private final int columnWidth;
    private final int columnHeight;
    private final Boolean isTvBox;

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        private final View fd_movie_card;
        private final ImageHelperView iv_movie;
        private final TextView tv_movie_rating;
        private final TextView tv_movie_title;

        public MyViewHolder(View view) {
            super(view);
            fd_movie_card = view.findViewById(R.id.fd_movie_card);
            iv_movie = view.findViewById(R.id.iv_movie);
            tv_movie_rating = view.findViewById(R.id.tv_movie_rating);
            tv_movie_title = view.findViewById(R.id.tv_movie_title);
        }
    }

    public AdapterDownload(Context context, List<ItemVideoDownload> arrayList, RecyclerItemClickListener listener) {
        this.context = context;
        this.arrayList = arrayList;
        this.listener = listener;
        dbHelper = new DBHelper(context);
        columnWidth = ApplicationUtil.getColumnWidth(context,6, 0);
        columnHeight = (int) (columnWidth * 1.15);
        isTvBox  = ApplicationUtil.isTvBox(context);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View  itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_movie, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        holder.tv_movie_title.setText(arrayList.get(position).getName());
        holder.tv_movie_rating.setVisibility(View.GONE);

        holder.iv_movie.setScaleType(ImageView.ScaleType.CENTER_CROP);
        holder.iv_movie.setLayoutParams(new RelativeLayout.LayoutParams(columnWidth, columnHeight));
        holder.fd_movie_card.setLayoutParams(new RelativeLayout.LayoutParams(columnWidth, columnHeight));

        try {
            Picasso.get()
                    .load(arrayList.get(position).getStreamIcon().isEmpty() ? "null" : arrayList.get(position).getStreamIcon())
                    .resize(Boolean.TRUE.equals(isTvBox) ? columnWidth : 250, Boolean.TRUE.equals(isTvBox) ? columnHeight : 350)
                    .centerCrop()
                    .placeholder(R.color.bg_color_load)
                    .into(holder.iv_movie);
        } catch (Exception e) {
            e.printStackTrace();
        }

        holder.fd_movie_card.setOnLongClickListener(v -> {
            DialogUtil.DeleteDialog(context, () -> {
                try {
                    dbHelper.removeFromDownload(DBHelper.TABLE_DOWNLOAD_MOVIES, arrayList.get(holder.getAbsoluteAdapterPosition()).getStreamID());
                    final File file = new File(arrayList.get(holder.getAbsoluteAdapterPosition()).getVideoURL());
                    final File fileImage = new File(arrayList.get(holder.getAbsoluteAdapterPosition()).getStreamIcon());
                    if (file.exists()) {
                        file.delete();
                    }
                    if (fileImage.exists()) {
                        fileImage.delete();
                    }
                    arrayList.remove(holder.getAbsoluteAdapterPosition());
                    notifyItemRemoved(holder.getAbsoluteAdapterPosition());
                    listener.onDelete();
                    Toast.makeText(context, context.getString(R.string.file_deleted), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            return false;
        });
        holder.fd_movie_card.setOnClickListener(v -> listener.onClickListener(arrayList.get(holder.getAbsoluteAdapterPosition()), holder.getAbsoluteAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public interface RecyclerItemClickListener{
        void onClickListener(ItemVideoDownload itemVideo, int position);
        void onDelete();
    }
}
