package com.example.rokidkeyboardbridge;
public class CoolSmileGeometryTest {
 public static void main(String[] args){int count=0;double den=150*150+24*24,expectedU=(-36*150+140*24)/den,expectedV=(150*140+24*36)/den;
 for(int id:CoolSmileFeatures.POSES){if(!CoolSmilePolicy.supports(id))continue;float[] f=CoolSmileFeatures.forPose(id),o=FullBodyRealisticFeatures.ALL[id];double dx=f[8]-f[2],dy=f[9]-f[3],d=dx*dx+dy*dy,mx=f[14]-(f[2]+f[8])*.5,my=f[15]-(f[3]+f[9])*.5;double u=(mx*dx+my*dy)/d,v=(dx*my-dy*mx)/d;
 if(Math.abs(u-expectedU)>.00001||Math.abs(v-expectedV)>.00001)throw new AssertionError("face shear "+id);
 if(f[0]!=o[0]||f[1]!=o[1]||Math.abs(f[2]-o[2])>.0001||Math.abs(f[3]-o[3])>.0001||Math.abs(f[8]-o[8])>.0001||Math.abs(f[9]-o[9])>.0001)throw new AssertionError("anchors "+id);
 double eyeAngle=Math.toDegrees(Math.atan2(dy,dx));if(Math.abs(f[17]-eyeAngle-CoolSmilePolicy.mouthEyeAngle())>.0001)throw new AssertionError("mouth angle "+id);
 if(Math.abs(f[18]/Math.sqrt(d)-9/Math.sqrt(den))>.00001)throw new AssertionError("mouth height "+id);if(!CoolSmilePolicy.protectsTransition(id,4)||!CoolSmilePolicy.protectsTransition(4,id))throw new AssertionError("transition endpoint protection "+id);count++;
 }System.out.println("PASS "+count+" faces: invariant aspect and mouth position relative to eyes; separate mouth angle/height; fixed body anchors");
 }
}
