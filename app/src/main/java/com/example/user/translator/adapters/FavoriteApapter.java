package com.example.user.translator.adapters;

import com.example.user.translator.models.HistoryItemModel;

import java.util.List;


public class FavoriteApapter extends HistoryAdapter {
    public FavoriteApapter(List<HistoryItemModel> data, OnFavClickListener listener) {
        super(data, listener);
    }
}