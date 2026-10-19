package com.example.rokidkeyboardbridge;
public class NotificationDispatchGateTest{
 static int n;static void ok(boolean b){if(!b)throw new AssertionError(n);n++;}
 public static void main(String[] args){NotificationDispatchGate g=new NotificationDispatchGate();ok(!g.acknowledged());g.cancelWaiting();ok(!g.begin());g.finish(true);ok(!g.acknowledged());g=new NotificationDispatchGate();ok(g.begin());ok(!g.begin());ok(!g.acknowledged());g.cancelWaiting();g.finish(true);ok(g.acknowledged());g.finish(false);ok(g.acknowledged());g=new NotificationDispatchGate();ok(g.begin());g.finish(false);ok(!g.acknowledged());ok(!g.begin());ok(CodexNotificationPolicy.fresh(1000,301000));ok(!CodexNotificationPolicy.fresh(1000,301001));ok(!CodexNotificationPolicy.fresh(0,1000));ok(CodexNotificationPolicy.kind("question"));ok(!CodexNotificationPolicy.safe("sandbox_permissions: require_escalated"));System.out.println("PASS "+n+" notification admission checks");}
}
