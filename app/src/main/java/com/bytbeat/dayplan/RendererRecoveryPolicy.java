package com.bytbeat.dayplan;

/** Bound repeated foreground crashes, not independent background process evictions. */
final class RendererRecoveryPolicy {
    private int foregroundCrashes;
    private long lastCrash=-1,readyAt=-1;
    void onReady(long now){readyAt=now;}
    boolean onExit(boolean crashed,boolean resumed,long now){
        if(readyAt>=0&&now-readyAt>=10_000)reset();
        readyAt=-1;
        if(!crashed||!resumed)return true;
        if(lastCrash<0||now-lastCrash>=30_000)foregroundCrashes=0;
        lastCrash=now;
        return ++foregroundCrashes<=2;
    }
    void reset(){foregroundCrashes=0;lastCrash=-1;readyAt=-1;}
}
