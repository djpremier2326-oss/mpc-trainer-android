package com.bytbeat.dayplan;
import org.junit.Test;
import static org.junit.Assert.*;
public class RendererRecoveryPolicyTest {
    @Test public void repeatedSystemEvictionsAlwaysRecover(){
        RendererRecoveryPolicy p=new RendererRecoveryPolicy();
        for(int i=0;i<20;i++)assertTrue(p.onExit(false,false,i*1000));
        assertTrue(p.onExit(false,true,21_000));
    }
    @Test public void backgroundCrashesDoNotExhaustForegroundRecovery(){
        RendererRecoveryPolicy p=new RendererRecoveryPolicy();
        for(int i=0;i<20;i++)assertTrue(p.onExit(true,false,i*1000));
        assertTrue(p.onExit(true,true,21_000));
    }
    @Test public void stablePageResetsCrashBudget(){
        RendererRecoveryPolicy p=new RendererRecoveryPolicy();
        assertTrue(p.onExit(true,true,0));p.onReady(100);
        assertTrue(p.onExit(true,true,10_100));p.onReady(10_200);
        assertTrue(p.onExit(true,true,20_200));
    }
    @Test public void rapidActualCrashesStillStopLoop(){
        RendererRecoveryPolicy p=new RendererRecoveryPolicy();
        assertTrue(p.onExit(true,true,0));p.onReady(100);
        assertTrue(p.onExit(true,true,1000));p.onReady(1100);
        assertFalse(p.onExit(true,true,2000));
        p.reset();assertTrue(p.onExit(true,true,2100));
    }
    @Test public void separateCrashWindowRecovers(){
        RendererRecoveryPolicy p=new RendererRecoveryPolicy();
        assertTrue(p.onExit(true,true,0));assertTrue(p.onExit(true,true,1000));
        assertTrue(p.onExit(true,true,31_000));
    }
}
