package com.example.user.translator.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.user.translator.models.HistoryItemModel;
import com.example.user.translator.R;

import java.util.List;

//  Адаптер для истории, для избранного он аналогичен
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<HistoryItemModel> data;
    FavoriteApapter.OnFavClickListener onFavClickListener;

    public HistoryAdapter(List<HistoryItemModel> data, FavoriteApapter.OnFavClickListener onFavClickListener) {
        this.data = data;
        this.onFavClickListener = onFavClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item, parent, false);
        return new ViewHolder(view, onFavClickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.from.setText(data.get(position).getTextFrom());
        holder.to.setText(data.get(position).getTextTo());
        holder.lang.setText(data.get(position).getLang());
        holder.fav.setSelected(data.get(position).isMarkedFav());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView from;
        public TextView to;
        public TextView lang;
        public ImageButton fav;

        public ViewHolder(View view, FavoriteApapter.OnFavClickListener listener) {
            super(view);
            from = (TextView) view.findViewById(R.id.history_from);
            to = (TextView) view.findViewById(R.id.history_to);
            lang = (TextView) view.findViewById(R.id.history_lang);
            fav = (ImageButton) view.findViewById(R.id.history_fav);

            setFavListener(listener);
        }

        private void setFavListener(final FavoriteApapter.OnFavClickListener listener) {
            fav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        fav.setSelected(!fav.isSelected());
                        listener.onFavClick(getAdapterPosition());
                    }
                }
            });
        }

    }

    public interface OnFavClickListener {
        void onFavClick(int position);
    }

}
