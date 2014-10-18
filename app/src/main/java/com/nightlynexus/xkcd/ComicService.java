package com.nightlynexus.xkcd;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;

public interface ComicService {

    @GET("/{number}/info.0.json")
    void getComic(@Path("number") int number, Callback<Comic> cb);

    @GET("/info.0.json")
    void getComicLatest(Callback<Comic> cb);
}
