package com.example.rokidgeminisecretary;
import android.security.keystore.*;
import java.security.*;
import java.util.*;
import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
final class BridgeTlsServer {
 private static final String ALIAS="loki-bridge-tls-v2";
 static synchronized KeyStore keys()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(!ks.containsAlias(ALIAS)){KeyPairGenerator generator=KeyPairGenerator.getInstance("EC","AndroidKeyStore");generator.initialize(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_SIGN|KeyProperties.PURPOSE_VERIFY).setAlgorithmParameterSpec(new java.security.spec.ECGenParameterSpec("secp256r1")).setDigests(KeyProperties.DIGEST_NONE,KeyProperties.DIGEST_SHA256).setCertificateSubject(new X500Principal("CN=Loki bridge")).setCertificateSerialNumber(new java.math.BigInteger(120,new SecureRandom())).setCertificateNotBefore(new Date(System.currentTimeMillis()-86400000L)).setCertificateNotAfter(new Date(System.currentTimeMillis()+10L*365*86400000L)).build());generator.generateKeyPair();ks.load(null);}return ks;}
 static String fingerprint()throws Exception{return BridgeCertificatePolicy.fingerprint(keys().getCertificate(ALIAS).getEncoded());}
 static java.net.ServerSocket listen()throws Exception{KeyManagerFactory km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());km.init(keys(),null);SSLContext context=SSLContext.getInstance("TLS");KeyStore store=keys();X509ExtendedKeyManager pinned=new X509ExtendedKeyManager(){
 public String[] getClientAliases(String t,java.security.Principal[] i){return null;}
 public String chooseClientAlias(String[] t,java.security.Principal[] i,java.net.Socket s){return null;}
 public String[] getServerAliases(String t,java.security.Principal[] i){return "EC".equals(t)?new String[]{ALIAS}:null;}
 public String chooseServerAlias(String t,java.security.Principal[] i,java.net.Socket s){return "EC".equals(t)?ALIAS:null;}
 public String chooseEngineServerAlias(String t,java.security.Principal[] i,SSLEngine e){return "EC".equals(t)?ALIAS:null;}
 public java.security.cert.X509Certificate[] getCertificateChain(String a){try{java.security.cert.Certificate[] chain=store.getCertificateChain(ALIAS);java.security.cert.X509Certificate[] result=new java.security.cert.X509Certificate[chain.length];for(int n=0;n<chain.length;n++)result[n]=(java.security.cert.X509Certificate)chain[n];return result;}catch(Exception e){return null;}}
 public PrivateKey getPrivateKey(String a){try{return (PrivateKey)store.getKey(ALIAS,null);}catch(Exception e){return null;}}
 };context.init(new KeyManager[]{pinned},null,new SecureRandom());SSLServerSocket socket=(SSLServerSocket)context.getServerSocketFactory().createServerSocket(8766,20,java.net.InetAddress.getByName("0.0.0.0"));java.util.List<String> protocols=new ArrayList<>();for(String p:socket.getSupportedProtocols())if(p.equals("TLSv1.2")||p.equals("TLSv1.3"))protocols.add(p);socket.setEnabledProtocols(protocols.toArray(new String[0]));return socket;}
}
