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
import java.util.List;
import java.util.Map;

public class FavoriteFragment extends Fragment {
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    List<HistoryItemModel> favData = new ArrayList<>();
    private List<Integer> itemsToRemove = new ArrayList<>();

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.item_delete, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Предупреждение")
                    .setMessage("Вы действитьльно хотите очистить избранное?")
                    .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
//                            Очищаем все избранное
                            SharedPreferences prefs = getActivity()
                                    .getSharedPreferences(ConstData.CACHE_FAV_NAME, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.clear();
                            editor.apply();
//                            Изменяем "избранность" в истории
                            for (HistoryItemModel item : favData) {
                                item.setMarkedFav(!item.isMarkedFav());
                                String name = item.getTextFrom() + item.getTextTo() + item.getLang();
                                prefs = getActivity()
                                        .getSharedPreferences(ConstData.CACHE_HISTORY_NAME, Context.MODE_PRIVATE);
                                editor = prefs.edit();
                                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                                String json = gson.toJson(item);
//                                Проверяем случай когда осталось в избранном, а из истории удалено
                                if (!prefs.getString(name, "NotExist").equals("NotExist")) {
                                    editor.putString(name, json);
                                }

                                editor.apply();
                            }
                            favData.clear();
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
        View view = inflater.inflate(R.layout.fragment_favorite, container, false);

        Toolbar myToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(myToolbar);
        setHasOptionsMenu(true);
        initRecyclerView(view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(getString(R.string.title_favorite));
    }

    @Override
    public void onPause() {
        super.onPause();
//        Пробегаемся по itemsToRemove и удалем не нужное. В changeFav() описно подробнее
        for (int position : itemsToRemove) {
            HistoryItemModel item = favData.get(position);
            String name = item.getTextFrom() + item.getTextTo() + item.getLang();
            SharedPreferences prefs = getActivity()
                    .getSharedPreferences(ConstData.CACHE_FAV_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.remove(name);
            editor.apply();

            prefs = getActivity()
                    .getSharedPreferences(ConstData.CACHE_HISTORY_NAME, Context.MODE_PRIVATE);
            editor = prefs.edit();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(item);

            if (!prefs.getString(name, "NotExist").equals("NotExist")) {
                editor.putString(name, json);
            }

            editor.apply();
        }
    }

    private void initRecyclerView(View view) {
        recyclerView = (RecyclerView) view.findViewById(R.id.fav_list);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        favData = loadCacheData();
        adapter = new FavoriteApapter(favData, new HistoryAdapter.OnFavClickListener() {
            @Override
            public void onFavClick(int position) {
                changeFav(position, true);
            }
        });
        recyclerView.setAdapter(adapter);

//        Удаление свайпом
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                changeFav(viewHolder.getAdapterPosition(), false);
                adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

    }
//    Те элементы которые смахиваются с экрана удаляются сразу, а вот на сердечко пользователь может
//    нажать сколько угодно раз, и значит не понятно удалять или нет, но если пользователь выходит
//    из фрагмента то очевидно, что где пропали сердечки, те элементы и надо удалить в onPause()
    private void changeFav(int position, boolean clicked) {
        if (favData.isEmpty())
            return;
        HistoryItemModel item = favData.get(position);
//        Будем запоминать те элементы, которые нужно будет удалить в onPause()? в itemsToRemove
        if (clicked && itemsToRemove.contains(position)) {
//            поставили сердечко
            itemsToRemove.remove((Integer) position);
            item.setMarkedFav(true);
        } else if (clicked) {
//            убрали сердечко
            itemsToRemove.add(position);
            item.setMarkedFav(false);
        } else {
//            свайпнули
            itemsToRemove.remove((Integer) position);
            favData.remove(item);
            item.setMarkedFav(false);
            String name = item.getTextFrom() + item.getTextTo() + item.getLang();
            SharedPreferences prefs = getActivity()
                    .getSharedPreferences(ConstData.CACHE_FAV_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
//            Удалили из кэша избранного
            editor.remove(name);
            editor.apply();

            prefs = getActivity()
                    .getSharedPreferences(ConstData.CACHE_HISTORY_NAME, Context.MODE_PRIVATE);
            editor = prefs.edit();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(item);
//            Обновили сердечко в кэше истории, при условии, что мы не удалили этот запрос из истории
            if (!prefs.getString(name, "NotExist").equals("NotExist")) {
                editor.putString(name, json);
            }

            editor.apply();
        }
    }

//    Загружаем элементы из кэша избранного
    private List<HistoryItemModel> loadCacheData() {
        Map<String, String> allEntries = (Map<String, String>) getContext()
                .getSharedPreferences(ConstData.CACHE_FAV_NAME, Context.MODE_PRIVATE).getAll();
        List<HistoryItemModel> list = new ArrayList<>();
        Gson gson = new Gson();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String json = entry.getValue().toString();
            HistoryItemModel historyItemModel = gson.fromJson(json, HistoryItemModel.class);
            list.add(new HistoryItemModel(historyItemModel.getLang(),
                    historyItemModel.getTextTo(), historyItemModel.getTextFrom(),
                    historyItemModel.getDate(), historyItemModel.isMarkedFav()));
        }
        Collections.sort(list);
        return list;
    }
}
