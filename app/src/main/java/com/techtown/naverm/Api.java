package com.techtown.naverm;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface Api {

    @Headers("Accept: application/json")
    @GET("/706265636c6b7373363342726c7768/json/ListConstructionWorkService/1/5")
    Call<ApiResult> getPlacesByGeo(@Query("LAT") double LAT, @Query("LNG") double LNG);
}
