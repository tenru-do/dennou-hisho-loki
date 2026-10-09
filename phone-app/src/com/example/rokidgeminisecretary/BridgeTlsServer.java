package com.example.rokidgeminisecretary;
import android.security.keystore.*;
import java.security.*;
import java.util.*;
import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
final class BridgeTlsServer {
 private static final String ALIAS="loki-bridge-tls-v1";
 static synchronized KeyStore keys()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(!ks.containsAlias(ALIAS)){KeyPairGenerator generator=KeyPairGenerator.getInstance("EC","AndroidKeyStore");generator.initialize(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_SIGN|KeyProperties.PURPOSE_VERIFY).setAlgorithmParameterSpec(new java.security.spec.ECGenParameterSpec("secp256r1")).setDigests(KeyProperties.DIGEST_SHA256).setCertificateSubject(new X500Principal("CN=Loki bridge")).setCertificateSerialNumber(new java.math.BigInteger(120,new SecureRandom())).setCertificateNotBefore(new Date(System.currentTimeMillis()-86400000L)).setCertificateNotAfter(new Date(System.currentTimeMillis()+10L*365*86400000L)).build());generator.generateKeyPair();ks.load(null);}return ks;}
 static String fingerprint()throws Exception{return BridgeCertificatePolicy.fingerprint(keys().getCertificate(ALIAS).getEncoded());}
 static java.net.ServerSocket listen()throws Exception{KeyManagerFactory km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());km.init(keys(),null);SSLContext context=SSLContext.getInstance("TLS");context.init(km.getKeyManagers(),null,new SecureRandom());SSLServerSocket socket=(SSLServerSocket)context.getServerSocketFactory().createServerSocket(8766,20,java.net.InetAddress.getByName("0.0.0.0"));java.util.List<String> protocols=new ArrayList<>();for(String p:socket.getSupportedProtocols())if(p.equals("TLSv1.2")||p.equals("TLSv1.3"))protocols.add(p);socket.setEnabledProtocols(protocols.toArray(new String[0]));return socket;}
}
