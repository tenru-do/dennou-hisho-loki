package com.example.rokidkeyboardbridge;
/** Reserve distinct information, comment and operation bands. No data formatting or polling. */
final class FullBodyInfoLayout {
 static int band(int height,int minimum,int preferred,int controls,int footer,int comment,int top,int gap){int available=Math.max(1,height-controls-footer-comment-top-gap);return Math.max(1,Math.min(preferred,Math.min(available,Math.max(minimum,available))));}
 private FullBodyInfoLayout(){}
}
