package com.example.rokidkeyboardbridge;
final class MeetingReceiptPolicy{static boolean verified(boolean ok,boolean complete,String id,String expectedId,int parts,int expectedParts,long bytes,long expectedBytes){return ok&&complete&&expectedId.equals(id)&&parts==expectedParts&&bytes==expectedBytes;}private MeetingReceiptPolicy(){}}
