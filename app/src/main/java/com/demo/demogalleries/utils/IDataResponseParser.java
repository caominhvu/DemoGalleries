package com.demo.demogalleries.utils;

import java.io.InputStream;

/**
 * Created by caominhvu on 5/17/15.
 */
public interface IDataResponseParser {
    public Object parseData(InputStream data, Error errCode, String errMsg);
}
