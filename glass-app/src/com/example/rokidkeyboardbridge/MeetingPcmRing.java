package com.example.rokidkeyboardbridge;
/** Bounded observation window; callers receive a copy and never block the recorder. */
final class MeetingPcmRing {
    private final byte[] data=new byte[128000];private int next,count;
    synchronized void put(byte[] b,int n){for(int i=0;i<n;i++){data[next]=b[i];next=(next+1)%data.length;}count=Math.min(data.length,count+n);}
    synchronized byte[] snapshot(){byte[] b=new byte[count];int start=(next-count+data.length)%data.length;for(int i=0;i<count;i++)b[i]=data[(start+i)%data.length];return b;}
    synchronized int size(){return count;}
}
