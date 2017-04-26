package com.example.user.translator.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.user.translator.ConstData;
import com.example.user.translator.models.DetectModel;
import com.example.user.translator.models.HistoryItemModel;
import com.example.user.translator.models.TranslateModel;
import com.example.user.translator.R;
import com.example.user.translator.YandexTranslateApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class TranslateFragment extends Fragment {
    //    Предыдущие позиции языков
    private int prevSpinnerToPos;
    private int prevSpinnerFromPos;
    private boolean autoTranslating;
    private boolean autoDetecting;

    private static YandexTranslateApi yandexTranslateApi;
    private Retrofit retrofit;

    private TextView toTranslate;
    private TextView textTranslated;
    private TextView fromTranslate;
    private TextView yandexTranslate;

    private CardView fromTranslateForm;

    private ArrayAdapter<CharSequence> adapter;
    private Spinner spinnerToTranslate, spinnerFromTranslate;
    private ImageButton swapLanguagesButton;
    private ImageButton clearText;
    private ImageButton copyText;
    private ImageButton addToFav;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_translate, container, false);

        Toolbar myToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(myToolbar);

        toTranslate = (TextView) view.findViewById(R.id.toTranslate);
        addToFav = (ImageButton) view.findViewById(R.id.btn_add_to_fav);
        fromTranslate = (TextView) view.findViewById(R.id.fromTranslate);
        textTranslated = (TextView) view.findViewById(R.id.textTranslated);
        copyText = (ImageButton) view.findViewById(R.id.btn_copy_translated);
        yandexTranslate = (TextView) view.findViewById(R.id.yandexTranslate);
        copyText = (ImageButton) view.findViewById(R.id.btn_copy_translated);
        yandexTranslate = (TextView) view.findViewById(R.id.yandexTranslate);
        clearText = (ImageButton) view.findViewById(R.id.btn_translate_clear);
        fromTranslateForm = (CardView) view.findViewById(R.id.from_translate_cw);
        spinnerToTranslate = (Spinner) view.findViewById(R.id.spinner_to_translate);
        spinnerFromTranslate = (Spinner) view.findViewById(R.id.spinner_from_translate);
        swapLanguagesButton = (ImageButton) view.findViewById(R.id.switch_language_button);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            yandexTranslate.setText(Html.fromHtml("<a href=\"https://translate.yandex.ru\">Переведено сервисом «Яндекс.Переводчик»</a>", Html.FROM_HTML_MODE_LEGACY));
        } else {
            yandexTranslate.setText(Html.fromHtml("<a href=\"https://translate.yandex.ru\">Переведено сервисом «Яндекс.Переводчик»</a>"));
        }
        yandexTranslate.setMovementMethod(LinkMovementMethod.getInstance());

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getActivity().setTitle(getString(R.string.title_translator));

        setTranslatedFormInvisible();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        autoDetecting = prefs.getBoolean("auto_detect_language", true);
        autoTranslating = prefs.getBoolean("instant_translation", false);

        retrofit = new Retrofit.Builder()
                .baseUrl("https://translate.yandex.net")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        yandexTranslateApi = retrofit.create(YandexTranslateApi.class);

        adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.languages_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFromTranslate.setAdapter(adapter);
        spinnerToTranslate.setAdapter(adapter);
        restoreSwitchLanguageState();

        toTranslate.setHorizontallyScrolling(false);
        toTranslate.setMinLines(4);
        toTranslate.setMaxLines(1000);

//        Смена яызка
        swapLanguagesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = spinnerToTranslate.getSelectedItemPosition();
                spinnerToTranslate.setSelection(spinnerFromTranslate.getSelectedItemPosition());
                spinnerFromTranslate.setSelection(index);
                savePrevSpinnerPos();

            }
        });

        clearText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toTranslate.setText("");
                setTranslatedFormInvisible();
            }
        });

        copyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Translated text", fromTranslate.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Перевод скопирован", Toast.LENGTH_SHORT).show();
            }
        });

        toTranslate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (toTranslate.getText().toString().trim().length() > 0) {
                        setTranslatedFormVisible();
                        if (autoDetecting) {
                            detectLanguageAndTranslate(toTranslate.getText().toString());
                        } else {
                            translate(toTranslate.getText().toString());
                        }
                    }
                }
                return false;
            }
        });

//        Для мгновенного перевода и определения языка
        toTranslate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(final Editable editable) {
                if (editable.toString().trim().length() > 0) {
                    setClearTextButtonVisible();
                    if (autoTranslating) {
                        if (fromTranslate.getVisibility() == View.GONE) {
                            fromTranslate.setText("");
                            setTranslatedFormVisible();
                        }
                        if (autoDetecting) {
                            detectLanguageAndTranslate(editable.toString());
                        } else {
                            translate(editable.toString());
                        }
                    }
                } else {
                    setTranslatedFormInvisible();
                }
            }
        });

        spinnerToTranslate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Проверка, чтобы языки поменялись местами при установке одинаковых
                if (spinnerToTranslate.getSelectedItemPosition() == spinnerFromTranslate.getSelectedItemPosition()) {
                    spinnerToTranslate.setSelection(spinnerFromTranslate.getSelectedItemPosition());
                    spinnerFromTranslate.setSelection(prevSpinnerToPos);
                    savePrevSpinnerPos();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerFromTranslate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Проверка, чтобы языки поменялись местами при установке одинаковых
                if (spinnerFromTranslate.getSelectedItemPosition() == spinnerToTranslate.getSelectedItemPosition()) {
                    spinnerFromTranslate.setSelection(spinnerToTranslate.getSelectedItemPosition());
                    spinnerToTranslate.setSelection(prevSpinnerFromPos);
                    savePrevSpinnerPos();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    //    Сохраниение положения спинеров
    @Override
    public void onStop() {
        super.onStop();
        saveSwitchLanguageState();
    }

    private void translate(@NonNull final String text) {
        if (isNetworkAvailable()) {
//            Запрос к Я.Переводчику
            String languages = ConstData.LANGUAGES.get(spinnerToTranslate.getSelectedItem().toString())
                    + "-" + ConstData.LANGUAGES.get(spinnerFromTranslate.getSelectedItem().toString());
            Map<String, String> keys = new HashMap<>();
            keys.put("key", ConstData.KEY);
            keys.put("text", text);
            keys.put("lang", languages);

            TranslateFragment.getApi().translate(keys).enqueue(new Callback<TranslateModel>() {
                @Override
                public void onResponse(Call<TranslateModel> call, final retrofit2.Response<TranslateModel> response) {
                    if (response.isSuccessful()) {
                        fromTranslate.setText(response.body().getText().get(0));

                        String name = text.trim() + fromTranslate.getText().toString().trim()
                                + response.body().getLang();
//                        Если такой перевод добавлен в избранное, то отметим это
                        addToFav.setSelected(isResponseAlreadyLiked(name));
//                        Добавим перевед в избранное нажатием
                        addToFav.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                addToFav.setSelected(!addToFav.isSelected());
                                saveResponseToFav(response.body(), text);
                            }
                        });
//                        Сохраним запрос для истории
                        saveResponse(response.body(), text);
                    } else if (!response.isSuccessful()) {
                        Toast.makeText(getContext(), "Error: " + String.valueOf(response.code()),
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<TranslateModel> call, Throwable t) {

                }
            });
        } else {
            Toast.makeText(getContext(), "Отсутствует подключение к интернету", Toast.LENGTH_SHORT)
                    .show();
            setTranslatedFormInvisible();
            setClearTextButtonVisible();
        }
    }

    //    Автоопределение языка
    private void detectLanguageAndTranslate(@NonNull final String text) {
        if (isNetworkAvailable()) {
            Map<String, String> keys = new HashMap<>();
            keys.put("key", ConstData.KEY);
            keys.put("text", text);

            TranslateFragment.getApi().detectLang(keys).enqueue(new Callback<DetectModel>() {
                @Override
                public void onResponse(Call<DetectModel> call, retrofit2.Response<DetectModel> response) {
                    if (response.isSuccessful() && response.body().getLang().length() > 0) {
                        int langId = adapter
                                .getPosition(ConstData.getKeyByValue(response.body().getLang()));
//                        Если вдруг введенный язык совпадает с переведенным, то изменим переведенный на предыдущий
                        if (langId == spinnerFromTranslate.getSelectedItemPosition()) {
                            spinnerFromTranslate.setSelection(spinnerToTranslate.getSelectedItemPosition());
                        }

                        spinnerToTranslate.setSelection(langId);
                        savePrevSpinnerPos();
                        translate(text);
                    } else {
                        Toast.makeText(getContext(), "Error: " + String.valueOf(response.code()), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<DetectModel> call, Throwable t) {

                }
            });
        } else {
            Toast.makeText(getContext(), "Отсутствует подключение к интернету", Toast.LENGTH_SHORT)
                    .show();
            setTranslatedFormInvisible();
            setClearTextButtonVisible();
        }
    }

    public static YandexTranslateApi getApi() {
        return yandexTranslateApi;
    }

//    Сохранение положения спинеров
    private void saveSwitchLanguageState() {
        SharedPreferences prefs = getActivity().getSharedPreferences(ConstData.SPINNERS_STATE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(ConstData.SPINNER_TO_TRANSLATE, spinnerToTranslate.getSelectedItemPosition());
        editor.putInt(ConstData.SPINNER_FROM_TRANSLATE, spinnerFromTranslate.getSelectedItemPosition());
        editor.apply();
    }
//    Восстановление положения спинеров
    private void restoreSwitchLanguageState() {
        SharedPreferences prefs = getActivity().getSharedPreferences(ConstData.SPINNERS_STATE_NAME, Context.MODE_PRIVATE);
        int pos1 = prefs.getInt(ConstData.SPINNER_TO_TRANSLATE, 0);
        int pos2 = prefs.getInt(ConstData.SPINNER_FROM_TRANSLATE, 1);
        spinnerToTranslate.setSelection(pos1);
        spinnerFromTranslate.setSelection(pos2);
        prevSpinnerToPos = pos1;
        prevSpinnerFromPos = pos2;
    }
    //    Сохранение предыдущего положения спинеров
    private void savePrevSpinnerPos() {
        prevSpinnerToPos = spinnerToTranslate.getSelectedItemPosition();
        prevSpinnerFromPos = spinnerFromTranslate.getSelectedItemPosition();
    }

    private void setTranslatedFormVisible() {
        clearText.setVisibility(View.VISIBLE);
        textTranslated.setVisibility(View.VISIBLE);
        fromTranslate.setVisibility(View.VISIBLE);
        yandexTranslate.setVisibility(View.VISIBLE);
        fromTranslateForm.setVisibility(View.VISIBLE);
    }

    private void setClearTextButtonVisible() {
        clearText.setVisibility(View.VISIBLE);
    }

    private void setTranslatedFormInvisible() {
        fromTranslate.setText("");
        clearText.setVisibility(View.GONE);
        textTranslated.setVisibility(View.GONE);
        fromTranslate.setVisibility(View.GONE);
        yandexTranslate.setVisibility(View.GONE);
        fromTranslateForm.setVisibility(View.GONE);
    }

//    Храним избранное и историю в разных местах, то есть при удалении истории избранное сохранится
    private void saveResponse(TranslateModel object, String fromText) {
        String name = fromText.trim() + object.getText().get(0).trim() + object.getLang();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        HistoryItemModel historyItem = new HistoryItemModel(object.getLang(),
                object.getText().get(0).trim(), fromText.trim(), new Date().getTime(), isResponseAlreadyLiked(name));
        String json = gson.toJson(historyItem);

        SharedPreferences prefs = getActivity().getSharedPreferences(ConstData.CACHE_HISTORY_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(name, json);
        editor.apply();
    }

    public void saveResponseToFav(TranslateModel object, String fromText) {
        String name = fromText.trim() + object.getText().get(0).trim() + object.getLang();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        HistoryItemModel favoriteItemModel = new HistoryItemModel(object.getLang(),
                object.getText().get(0).trim(), fromText.trim(), new Date().getTime(), addToFav.isSelected());
        String json = gson.toJson(favoriteItemModel);

        SharedPreferences prefs = getActivity().getSharedPreferences(ConstData.CACHE_FAV_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(name, json);
        editor.apply();
//        Сохранили в избранное, добавим в историю. Но мы итак сохраняем запрос в translate(), но есть одно но
//        тот запрос всегда не будет избранным, потому что пользователь не успеет нажать на кнопку,
//        а здесь как раз по нажатию на кнопку мы обновим пункт в истории и сделаем его с пометкой избранное
        saveResponse(object, fromText);
    }

//    Проверка наличия перевода в избранном
    private boolean isResponseAlreadyLiked(String name) {
        Map<String, String> allEntries = (Map<String, String>) getActivity()
                .getSharedPreferences(ConstData.CACHE_FAV_NAME, Context.MODE_PRIVATE).getAll();
        return allEntries.containsKey(name);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
