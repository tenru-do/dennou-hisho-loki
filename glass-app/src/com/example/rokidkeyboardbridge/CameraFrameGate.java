package com.example.rokidkeyboardbridge;
/** Camera-handler confined: vendor JPEG delivery may precede capture completion. */
final class CameraFrameGate {
 private boolean inFlight,pending;
 boolean request(){if(inFlight){pending=true;return false;}inFlight=true;return true;}
 boolean complete(){inFlight=false;boolean next=pending;pending=false;return next;}
}
