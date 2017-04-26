package com.example.user.translator;

import com.example.user.translator.models.DetectModel;
import com.example.user.translator.models.TranslateModel;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface YandexTranslateApi {
    @FormUrlEncoded
    @POST("/api/v1.5/tr.json/translate")
    Call<TranslateModel> translate(@FieldMap Map<String, String> keys);

    @FormUrlEncoded
    @POST("/api/v1.5/tr.json/detect")
    Call<DetectModel> detectLang(@FieldMap Map<String, String> keys);
}
