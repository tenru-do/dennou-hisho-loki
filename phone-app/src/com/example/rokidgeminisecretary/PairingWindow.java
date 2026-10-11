package com.example.rokidgeminisecretary;
/** One-shot admission based on elapsed time, independent of wall-clock changes. Admission is not approval. */
final class PairingWindow {
 private long until=-1;
 synchronized void open(long elapsed){until=elapsed+60000;}
 synchronized boolean claim(long elapsed,String code){if(code==null||!code.matches("[0-9]{6}")||until<0||elapsed>=until)return false;until=-1;return true;}
}
