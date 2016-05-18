package com.demo.demogalleries;

/**
 * Created by caominhvu on 2/26/16.
 */
public interface IHttpClientReq {
    String KEY = "3JI21VDqbN7qFTfeW1eijrUAPzEKwpmzwdnflM7u";
    String SERVER_URL = "https://api.500px.com/v1";
    void getPhotosOfCategory(String category);
    void getPhotoDetail(String id);
}
