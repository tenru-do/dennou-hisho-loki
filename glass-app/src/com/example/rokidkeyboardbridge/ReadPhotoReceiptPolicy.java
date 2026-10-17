package com.example.rokidkeyboardbridge;
final class ReadPhotoReceiptPolicy {
 static boolean verified(boolean ok,boolean complete,boolean stored,boolean deleted,boolean gallerySaved,String id,String expectedId,String sha,String expectedSha,int total,int expectedTotal){
  return ok&&complete&&(deleted||stored&&gallerySaved)&&expectedId.equals(id)&&expectedSha.equals(sha)&&total==expectedTotal;
 }
}
