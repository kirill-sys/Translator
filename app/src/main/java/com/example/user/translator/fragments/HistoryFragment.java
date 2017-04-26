package com.example.user.translator.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.user.translator.ConstData;
import com.example.user.translator.adapters.FavoriteApapter;
import com.example.user.translator.adapters.HistoryAdapter;
import com.example.user.translator.R;
import com.example.user.translator.models.HistoryItemModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class HistoryFragment extends Fragment {
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    List<HistoryItemModel> historyData = new ArrayList<>();

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.item_delete, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_delete) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Предупреждение")
                    .setMessage("Вы действитьльно хотите очистить историю?")
                    .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
//                            Очищаем все избранное
                            SharedPreferences prefs = getActivity()
                                    .getSharedPreferences(ConstData.CACHE_HISTORY_NAME, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.clear();
                            editor.apply();
                            historyData.clear();
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton("Отмена", null).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        Toolbar myToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(myToolbar);
        recyclerView = (RecyclerView) view.findViewById(R.id.history_list);
        setHasOptionsMenu(true);
        initRecyclerView(view);
        return view;
    }

    private void initRecyclerView(View view) {
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        historyData = loadCacheData();
        adapter = new HistoryAdapter(historyData, new FavoriteApapter.OnFavClickListener() {
            @Override
            public void onFavClick(int position) {
                markAsFav(position);
            }
        });
        recyclerView.setAdapter(adapter);

//        Удаление свайпом
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                removeItemFromHistory(viewHolder.getAdapterPosition());
                adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

//        Добавление "полосок" между элементами
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(), layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    private void removeItemFromHistory(int adapterPosition) {
        HistoryItemModel item = historyData.get(adapterPosition);
        historyData.remove(adapterPosition);
        String name = item.getTextFrom() + item.getTextTo() + item.getLang();

        SharedPreferences prefs = getActivity().getSharedPreferences(ConstData.CACHE_HISTORY_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(name);
        editor.apply();
    }

    private void markAsFav(int position) {
//        Изменяем в истории
        HistoryItemModel item = historyData.get(position);
        item.setMarkedFav(!item.isMarkedFav());

        String name = item.getTextFrom() + item.getTextTo() + item.getLang();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(item);

        SharedPreferences prefs = getActivity().getSharedPreferences(ConstData.CACHE_HISTORY_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(name, json);
        editor.apply();

//        Добавляем/удаляем в избранное
        addItemToFav(item, name);
    }

    public void addItemToFav(HistoryItemModel item, String name) {
        SharedPreferences prefs = getActivity().getSharedPreferences(ConstData.CACHE_FAV_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (!item.isMarkedFav()) {
            editor.remove(name);
            editor.apply();
            return;
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        HistoryItemModel favoriteItemModel =
                new HistoryItemModel(item.getLang(), item.getTextTo(), item.getTextFrom(), new Date().getTime(), item.isMarkedFav());
        String json = gson.toJson(favoriteItemModel);

        editor.putString(name, json);
        editor.apply();
    }

//    Загрузка истории из кэша
    private List<HistoryItemModel> loadCacheData() {
        Map<String, String> allEntries = (Map<String, String>) getContext().getSharedPreferences(ConstData.CACHE_HISTORY_NAME, Context.MODE_PRIVATE).getAll();
        List<HistoryItemModel> list = new ArrayList<>();
        Gson gson = new Gson();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String json = entry.getValue().toString();
            HistoryItemModel historyItem = gson.fromJson(json, HistoryItemModel.class);
            list.add(new HistoryItemModel(historyItem.getLang(), historyItem.getTextTo(), historyItem.getTextFrom(), historyItem.getDate(), historyItem.isMarkedFav()));
        }
        Collections.sort(list);
        return list;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(getString(R.string.title_history));
    }
}
