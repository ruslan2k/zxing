package com.google.zxing.oned;

import com.sun.org.apache.bcel.internal.classfile.Code;
import com.google.zxing.oned.CodeElement;

public class Counter {
    private static Counter INSTANCE = null;
    private int SIZE = 10;
    private int counter;
    private CodeElement[] cListArray;

    private Counter() {
        counter = 0;
        cListArray = new CodeElement[SIZE];
    }
    public static synchronized Counter getInstance() {
      if (INSTANCE == null) {
        INSTANCE = new Counter();
      }
      return(INSTANCE);
    }
    // other instance methods can follow
    public void addCode(int iCode) {
        int i = counter % SIZE;
        long unixTime = System.currentTimeMillis() / 1000L;
        cListArray[i] = new CodeElement(iCode, unixTime);
        counter ++;
    }
    public boolean isCodeValid(int iCode) {
        if (counter < SIZE) {
            return false;
        }
        int iIndex = counter % SIZE;
        long lStartTime = cListArray[iIndex].getTime();
        long lCurrentTime = System.currentTimeMillis() / 1000L;
        long lDelta = lCurrentTime - lStartTime;
        if (lDelta > 2) { return false; }
        int iEquals = 0;
        for (int i = 0; i < SIZE; i ++) {
            if (iCode == cListArray[i].getCode()) { iEquals ++; }
        }
        return (iEquals >= (SIZE - 1));
    }
    public void print() {
        for (int i = 0; i < SIZE; i ++) {
            System.out.print(i);
            System.out.println(cListArray[i]);
        }
    }
    public int getSize(){
        return cListArray.length;
    }
    //public void incCounter() {
    //  counter ++;
    //}
    public int getCounter() {
      return counter;
    }
}
