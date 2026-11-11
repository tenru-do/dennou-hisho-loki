package com.example.rokidkeyboardbridge;

/** Runtime key routing and reviewed source geometry; no Android rendering or user data. */
public final class RestKneeFallbackTest {
 private static int checks;
 private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 private static void close(float a,float b,String why){check(Math.abs(a-b)<.001f,why);}
 public static void main(String[] args){
  for(int key=0;key<18;key++)
   check(RestArtworkRenderer.runtimeKey(key)==(key>=14?9:key),"Only old knee keys use the ordinary master: "+key);
  for(int motion=0;motion<=7;motion++)for(int age=0;age<=12000;age+=37){
   int source=RestAnimationTimeline.frame(motion,age),prior=RestAnimationTimeline.previous(motion,age);
   int current=RestArtworkRenderer.runtimeKey(source),previous=RestArtworkRenderer.runtimeKey(prior);
   check(current>=0&&current<14&&previous>=0&&previous<14,"Renderer never indexes a removed knee bitmap");
   if(motion>=5){
    check(current==9&&previous==9,"Knee/return/fidget states use one master before alpha blending");
   }else{
    check(current==source&&previous==prior,"Other rest animation routes remain unchanged");
   }
  }
  EmbodiedScenePolicy state=new EmbodiedScenePolicy();
  state.user("寝たまま立膝にして",1000);
  check(state.restMotion(2000)==5&&state.correctedRest(),"Existing explicit knee state is retained");
  state.user("困った顔にして",3000);
  check(state.restFace()==2&&state.restMotion(3100)==5,"Expression selection survives the body fallback");
  state.user("寝たまま立膝のまま身じろぎして",4000);
  check(state.restMotion(4100)==7,"Finite upper-body fidget state remains available");
  check(RestArtworkRenderer.runtimeKey(RestAnimationTimeline.frame(7,10000))==9
          &&!RestAnimationTimeline.moving(7,10000),"Fidget finishes on the stable master without looping");
  state.user("身じろぎを止めて",5000);
  check(state.restMotion(5100)==5,"Stop exits the fidget state");
  state.user("寝たまま脚を伸ばして",6000);
  check(state.restMotion(6100)==6&&RestArtworkRenderer.runtimeKey(RestAnimationTimeline.frame(6,10000))==9,
          "The existing exit action still settles safely");
  state.user("立って",7000);check(!state.correctedRest(),"An explicit posture change exits normally");

  int[] xEdges={0,341,682,1024},yEdges={0,483,978,1536};
  int[] crown={11,11,15,8,27,11,8,9,8},heel={476,468,470,489,490,490,509,510,516};
  long area=0;
  for(int frame=0;frame<9;frame++){
   int[] box=RestArtworkLayout.bounds(frame);int column=frame%3,row=frame/3;
   check(box[0]==xEdges[column]&&box[1]==yEdges[row]
           &&box[2]==xEdges[column+1]-xEdges[column]&&box[3]==yEdges[row+1]-yEdges[row],
           "Motion crops follow actual empty source gutters, not head-cutting equal rows");
   area+=(long)box[2]*box[3];
   close(RestArtworkLayout.coordinate(frame,1,box[2],box[3]),crown[frame],"Reviewed crown source coordinate");
   close(RestArtworkLayout.coordinate(frame,2,box[2],box[3]),heel[frame],"Reviewed heel source coordinate");
   check(crown[frame]>=0&&heel[frame]<box[3],"Reviewed source head and heels fit within their own crop");
   for(int[] view:new int[][]{{240,520},{480,640},{320,900}}){
    float scale=RestArtworkLayout.scale(frame,box[3],view[0],view[1]);
    float top=CharacterViewport.headMargin(view[1])-crown[frame]*scale;
    check(scale>0&&Float.isFinite(scale)&&top+crown[frame]*scale>=CharacterViewport.headMargin(view[1])-.001f,
            "Updated source calibration retains the head in portrait viewports");
   }
  }
  check(area==1024L*1536,"Motion source crops partition the whole atlas without overlaps or gaps");
  for(int frame=10;frame<14;frame++){
   int[] box=RestArtworkLayout.bounds(frame);int index=frame-10;
   check(box[0]==index%2*512&&box[1]==index/2*768&&box[2]==512&&box[3]==768,
           "New wave source uses four equal 512x768 cells");
   close(RestArtworkLayout.coordinate(frame,0,512,768),254,"Wave keeps the same head center across all four frames");
   close(RestArtworkLayout.coordinate(frame,2,512,768),691,"Wave heel anchor is stable across both rows");
   int[] face=RestArtworkLayout.face(0);
   check(face[0]<254&&face[2]>254&&face[1]<=60&&face[3]>=94,"Master-aligned wave face patch contains the reviewed face");
  }
  System.out.println("PASS "+checks+" knee fallback, finite lifecycle, expression preservation and source crop assertions");
 }
}
