package com.example.rokidkeyboardbridge;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.*;
public final class BridgeSecurityTest {
 static void check(boolean value,String name){if(!value)throw new AssertionError(name);}
 static SSLContext context;static String pin;
 static void handshake(String expected,boolean allowed)throws Exception{
  try(SSLServerSocket server=(SSLServerSocket)context.getServerSocketFactory().createServerSocket(0,1,java.net.InetAddress.getLoopbackAddress())){
   Thread worker=new Thread(()->{try(SSLSocket peer=(SSLSocket)server.accept()){peer.setSoTimeout(4000);peer.startHandshake();peer.getOutputStream().write(7);}catch(Exception rejected){}});worker.start();boolean accepted=false;
   try(SSLSocket client=(SSLSocket)BridgeCertificatePolicy.pinned(expected).createSocket("127.0.0.1",server.getLocalPort())){client.setSoTimeout(4000);client.startHandshake();accepted=client.getInputStream().read()==7;}catch(SSLException rejected){}
   worker.join(5000);check(!worker.isAlive(),"TLS test completed");check(accepted==allowed,"certificate pin enforcement");
  }
 }
 public static void main(String[] args)throws Exception{
  KeyStore ks=KeyStore.getInstance("PKCS12");try(InputStream in=Files.newInputStream(Path.of(args[0]))){ks.load(in,args[1].toCharArray());}KeyManagerFactory km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());km.init(ks,args[1].toCharArray());context=SSLContext.getInstance("TLS");context.init(km.getKeyManagers(),null,new SecureRandom());pin=BridgeCertificatePolicy.fingerprint(ks.getCertificate("fixture").getEncoded());
  String token="fixture-credential-not-a-real-secret";String proof=BridgeCertificatePolicy.proof(token,pin);
  if(args.length>2&&"serve".equals(args[2])){
   java.net.ServerSocket publicSocket=new java.net.ServerSocket(8765,4,java.net.InetAddress.getLoopbackAddress());publicSocket.setSoTimeout(60000);
   Thread discovery=new Thread(()->{try(java.net.ServerSocket server=publicSocket){for(int i=0;i<2;i++)try(java.net.Socket client=server.accept()){client.setSoTimeout(3000);BufferedReader reader=new BufferedReader(new InputStreamReader(client.getInputStream()));String line;boolean secret=false;while((line=reader.readLine())!=null&&!line.isEmpty())if(line.toLowerCase().contains("x-roki-token"))secret=true;check(!secret,"no credential in public discovery");respond(client,"{\"fingerprint\":\""+pin+"\",\"proof\":\""+proof+"\"}");}}catch(Exception error){throw new RuntimeException(error);}});discovery.setDaemon(true);discovery.start();
   try(SSLServerSocket server=(SSLServerSocket)context.getServerSocketFactory().createServerSocket(8766,4,java.net.InetAddress.getLoopbackAddress())){server.setSoTimeout(60000);System.out.println("READY local fixture");int successes=0;for(int i=0;i<8&&successes<2;i++)try(SSLSocket client=(SSLSocket)server.accept()){client.setSoTimeout(3000);try{client.startHandshake();BufferedReader reader=new BufferedReader(new InputStreamReader(client.getInputStream()));String line;boolean authorized=false,received=false;while((line=reader.readLine())!=null&&!line.isEmpty()){if(line.startsWith("GET "))received=true;if(line.equals("X-Roki-Token: "+token))authorized=true;}if(!received)continue;check(authorized,"token inside TLS");respond(client,"{\"ok\":true}");successes++;}catch(SSLException expected){}}}finally{publicSocket.close();}System.out.println("PASS local PC TLS fixture finished");return;
  }
  check(BridgeCertificatePolicy.verifies(token,pin,proof),"existing-token authenticated upgrade");check(!BridgeCertificatePolicy.verifies("wrong-fixture-token",pin,proof),"wrong token rejected");String wrong=(pin.charAt(0)=='0'?"1":"0")+pin.substring(1);check(!BridgeCertificatePolicy.verifies(token,wrong,proof),"substituted certificate proof rejected");try{BridgeCertificatePolicy.proof("",pin);throw new AssertionError("empty token");}catch(GeneralSecurityException expected){}
  handshake(pin,true);handshake(wrong,false);
  byte[] key=new byte[32];new SecureRandom().nextBytes(key);PhotoVaultCipher cipher=new PhotoVaultCipher(new SecretKeySpec(key,"AES"));byte[] jpeg={(byte)255,(byte)216,10,20,30,(byte)255,(byte)217};byte[] encrypted=cipher.encrypt(jpeg);check(Arrays.equals(cipher.decrypt(encrypted),jpeg),"photo round trip");check(!Arrays.equals(encrypted,cipher.encrypt(jpeg)),"unique encryption nonce");encrypted[encrypted.length-1]^=1;try{cipher.decrypt(encrypted);throw new AssertionError("tampered photo accepted");}catch(javax.crypto.AEADBadTagException expected){}
  PhotoVaultCipher other=new PhotoVaultCipher(new SecretKeySpec(new byte[32],"AES"));try{other.decrypt(cipher.encrypt(jpeg));throw new AssertionError("wrong key accepted");}catch(javax.crypto.AEADBadTagException expected){}
  Path root=Files.createTempDirectory("loki-vault-test");try{
   long now=System.currentTimeMillis();PhotoArchive legacy=new PhotoArchive(root.toFile());File old=legacy.save(jpeg,now);legacy.protect(now);
   PhotoArchive secured=new PhotoArchive(root.toFile(),cipher);secured.encryptLegacy();File converted=secured.entries()[0];check(!old.exists()&&converted.getName().endsWith(".gcm"),"legacy conversion");check(new File(converted.getPath()+".keep").exists(),"protection retained");check(Arrays.equals(secured.read(converted),jpeg),"legacy image preserved");secured.encryptLegacy();check(secured.entries().length==1,"migration idempotent");File saved=secured.save(jpeg,now+1);check(saved.getName().endsWith(".gcm"),"new pictures encrypted");check(Arrays.equals(secured.read(saved),jpeg),"new encrypted image readable");check(secured.stats()[2]==2,"future KEEP window retained");check(secured.stats()[1]==saved.length()+converted.length(),"quota uses encrypted actual bytes");
  }finally{try(java.util.stream.Stream<Path> paths=Files.walk(root)){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(IOException e){throw new RuntimeException(e);}});}}
  System.out.println("PASS token-authenticated certificate proof, correct and wrong pinned TLS handshakes, AES-GCM round trip/tamper/wrong-key, unique nonces, legacy migration/protection/idempotence and encrypted quota");
 }
 static void respond(java.net.Socket client,String body)throws IOException{byte[] bytes=body.getBytes(java.nio.charset.StandardCharsets.UTF_8);OutputStream out=client.getOutputStream();out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "+bytes.length+"\r\nConnection: close\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));out.write(bytes);out.flush();}
}
