package com.example.kim_pc.groupproject.Utils;

import com.example.kim_pc.groupproject.Retrofit.MovieShopAPI;
import com.example.kim_pc.groupproject.Retrofit.RetrofitClient;

public class Common {
    private static final String BASE_URL = "http://10.0.2.2/project/";

    public static MovieShopAPI getAPI()
    {
        return RetrofitClient.getClient(BASE_URL).create(MovieShopAPI.class);
    }
}
