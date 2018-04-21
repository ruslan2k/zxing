package com.google.zxing.oned;

public class Singleton {

  private static Singleton INSTANCE = null;

  // other instance variables can be here
  private int counter;

  private Singleton() {
    counter = 0;
  };

  public static synchronized Singleton getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new Singleton();
    }
    return(INSTANCE);
  }
  // other instance methods can follow

  public void incCounter() {
    counter ++;
  }
  public int getCounter() {
    return counter;
  }
}
