package com.demo.demogalleries.utils;

/**
 * Created by caominhvu on 2/26/16.
 */
public enum Error {
    NONE(0, "No Error"),
    NETWORK(410, "Network Error: Please check your network state"),
    SERVER_COMM(500, "Server Error: Please check your server state");

    private final int mErrCode;
    private final String mErrMsg;
    Error(int errCode, String errMsg) {
        this.mErrCode = errCode;
        this.mErrMsg = errMsg;
    }
    public int getCode() {
        return mErrCode;
    }
    public String getMsg() {
        return mErrMsg;
    }
}
