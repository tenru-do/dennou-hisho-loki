package com.example.rokidgeminisecretary;
public final class NavigationPowerPolicy {
 public static boolean relaxed(boolean active,String mode,double turnMetres,boolean fresh,boolean heading,double offRoute,boolean recovering){return active&&"walking".equals(mode)&&Double.isFinite(turnMetres)&&turnMetres>500&&fresh&&heading&&Double.isFinite(offRoute)&&offRoute>=0&&offRoute<=20&&!recovering;}
 private NavigationPowerPolicy(){}
}
