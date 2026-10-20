package com.example.rokidgeminisecretary;
/** Revisions detect edits even when the visible text changes back to the same value. */
final class ComposerDraftPolicy {
 static boolean unchanged(String before,String now,long beforeRevision,long nowRevision,long beforeAttachment,long nowAttachment,boolean busy){return !busy&&before!=null&&before.equals(now)&&beforeRevision==nowRevision&&beforeAttachment==nowAttachment;}
 private ComposerDraftPolicy(){}
}
