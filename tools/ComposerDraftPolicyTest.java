package com.example.rokidgeminisecretary;
public final class ComposerDraftPolicyTest {
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError("case "+n);}
 public static void main(String[] args){
  ok(ComposerDraftPolicy.unchanged("送信した文","送信した文",5,5,2,2,false));
  ok(!ComposerDraftPolicy.unchanged("送信した文","追記した文",5,6,2,2,false));
  ok(!ComposerDraftPolicy.unchanged("送信した文","送信した文",5,7,2,2,false));
  ok(!ComposerDraftPolicy.unchanged("本文","本文",5,5,2,3,false));
  ok(!ComposerDraftPolicy.unchanged("本文","本文",5,5,2,2,true));
  ok(!ComposerDraftPolicy.unchanged("本文","本文 ",5,6,2,2,false));
  ok(!ComposerDraftPolicy.unchanged(null,"",0,0,0,0,false));
  ok(ComposerDraftPolicy.unchanged("","",0,0,0,0,false));
  ok(!ComposerDraftPolicy.unchanged("本文","本文",1,1,1,3,false));
  System.out.println("PASS "+n+" in-flight editing / ABA edit / attachment revision checks; no requests queued");
 }
}
