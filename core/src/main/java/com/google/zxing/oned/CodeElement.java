package com.google.zxing.oned;

public class CodeElement {
    private int iCode;
    private long lUnixTime;

    public String toString() {
        return "code=" + Integer.toString(iCode)
                + "|time=" + Long.toString(lUnixTime);
    }
    public CodeElement(int c, long t) {
        iCode = c;
        lUnixTime = t;
    }
    public int getCode() {
        return iCode;
    }
    public long getTime() {
        return lUnixTime;
    }
}
