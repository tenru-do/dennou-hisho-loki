package com.example.rokidgeminisecretary;
public final class GooglePhotoImportPolicyTest {
 public static void main(String[] args)throws Exception{
  for(String value:new String[]{"https://lh3.googleusercontent.com/pw/synthetic-image=w1000","https://lh1.googleusercontent.com:443/a"})GooglePhotoImport.validate(value);
  for(String value:new String[]{"http://lh3.googleusercontent.com/a","https://127.0.0.1/a","https://localhost/a","https://lh3.googleusercontent.com.attacker.invalid/a","https://user@lh3.googleusercontent.com/a","https://lh3.googleusercontent.com:444/a","https://lh3.googleusercontent.com/a#fragment","https://lh3.googleusercontent.com/","https://lh3.googleusercontent.com/"+"a".repeat(8200)}){
   try{GooglePhotoImport.validate(value);throw new AssertionError("accepted unsafe URL");}catch(java.io.IOException expected){}
  }
  System.out.println("PASS Google image CDN allowlist, HTTPS, credentials, port, fragment and length restrictions; no network request");
 }
}
