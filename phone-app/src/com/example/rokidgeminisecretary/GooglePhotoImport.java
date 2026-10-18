package com.example.rokidgeminisecretary;
import java.io.*;import java.net.*;import javax.net.ssl.HttpsURLConnection;
/** Fetch only the Google image CDN URL explicitly dropped by an authenticated user. */
final class GooglePhotoImport {
 static URI validate(String raw)throws IOException {
  try{URI uri=new URI(raw);String host=uri.getHost();if(raw.length()>8192||!"https".equals(uri.getScheme())||host==null||!host.matches("lh[0-9]+\\.googleusercontent\\.com")||uri.getRawUserInfo()!=null||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getRawFragment()!=null||uri.getRawPath()==null||uri.getRawPath().length()<2)throw new IOException("Googleフォトの実画像URLではありません");return uri;}catch(URISyntaxException e){throw new IOException("画像URLを読み取れません");}
 }
 static byte[] read(String raw)throws Exception {
  URI uri=validate(raw);for(InetAddress ip:InetAddress.getAllByName(uri.getHost()))if(ip.isAnyLocalAddress()||ip.isLoopbackAddress()||ip.isLinkLocalAddress()||ip.isSiteLocalAddress()||ip.isMulticastAddress())throw new IOException("画像の接続先を確認できません");
  HttpsURLConnection connection=(HttpsURLConnection)uri.toURL().openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(10000);connection.setReadTimeout(15000);connection.setUseCaches(false);connection.setRequestProperty("Accept","image/*");
  try{int status=connection.getResponseCode();if(status!=200)throw new IOException("画像を取得できません（Google側 HTTP "+status+"）。Googleフォトで写真を開いて画像をコピーし、貼り付けてください");String type=connection.getContentType();if(type==null||!type.toLowerCase(java.util.Locale.ROOT).startsWith("image/"))throw new IOException("リンク先が画像ではありません");if(connection.getContentLengthLong()>ImageQuestions.MAX_SOURCE)throw new IOException("画像は20MB以内にしてください");return ImageQuestions.normalize(ImageQuestions.readBounded(connection.getInputStream()));}finally{connection.disconnect();}
 }
}
