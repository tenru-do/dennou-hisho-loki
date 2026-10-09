# Scoped certificate validation. Never disable validation globally or fall back to HTTP.
if (-not ('LokiSecureBridge' -as [type])) {
$bridgeSource = @'

using System;
using System.Net;
using System.IO;
using System.Text;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
public static class LokiSecureBridge {
 public static string Hash(byte[] b) { using(var h=SHA256.Create())return BitConverter.ToString(h.ComputeHash(b)).Replace("-", "").ToLowerInvariant(); }
 public static bool Proof(string token,string pin,string proof) { using(var h=new HMACSHA256(Encoding.UTF8.GetBytes(token))){byte[] a=h.ComputeHash(Encoding.ASCII.GetBytes("LOKI-TLS-v1:"+pin));byte[] b;try{b=Convert.FromBase64String(proof);}catch{return false;}int d=a.Length^b.Length;for(int i=0;i<a.Length;i++)d|=a[i]^(i<b.Length?b[i]:0);return d==0;} }
 public static HttpWebRequest Create(string address,string pin) {
  if(pin==null||pin.Length!=64)throw new Exception("Phone certificate fingerprint required");
  var r=(HttpWebRequest)WebRequest.Create(address);if(r.Address.Scheme!="https")throw new Exception("TLS required");r.AllowAutoRedirect=false;
  r.ServerCertificateValidationCallback=(sender,cert,chain,errors)=>{if(cert==null)return false;var c=new X509Certificate2(cert);return Hash(c.RawData)==pin && c.NotBefore.ToUniversalTime()<=DateTime.UtcNow && c.NotAfter.ToUniversalTime()>=DateTime.UtcNow;};
  ServicePointManager.SecurityProtocol=SecurityProtocolType.Tls12;
  return r;
 }
 public static string Discovery(string address) {var r=(HttpWebRequest)WebRequest.Create(address);r.AllowAutoRedirect=false;r.Timeout=2000;using(var response=r.GetResponse())using(var stream=response.GetResponseStream())using(var reader=new StreamReader(stream)){char[] b=new char[16385];int n=0,k;while(n<b.Length&&(k=reader.Read(b,n,b.Length-n))>0)n+=k;if(n>16384)throw new Exception("Certificate proof too large");return new string(b,0,n);} }
}
'@
if($PSVersionTable.PSVersion.Major -ge 6){$bridgeSource="#pragma warning disable SYSLIB0014`n"+$bridgeSource}
Add-Type -TypeDefinition $bridgeSource
}
$script:LokiTlsPins=@{}
$script:LokiPairPins=@{}
function New-LokiSecureRequest([string]$Address,[string]$Token='', [string]$Fingerprint='') {
    $uri=[uri]$Address
    if($uri.Port -notin @(8765,8766,18765,18766)){throw 'Expected Loki bridge port'}
    $discoveryPort=if($uri.Port -ge 18000){18765}else{8765}
    $securePort=if($uri.Port -ge 18000){18766}else{8766}
    $pin=$Fingerprint.Replace(':','').Replace(' ','').Trim().ToLowerInvariant()
    if(!$pin -and $Token.Length -ge 16){
        $cacheKey=$uri.Host+':'+[LokiSecureBridge]::Hash([Text.Encoding]::UTF8.GetBytes($Token))
        if($script:LokiTlsPins.ContainsKey($cacheKey)){$pin=$script:LokiTlsPins[$cacheKey]}
        else{
            $discovery=[UriBuilder]::new('http',$uri.Host,$discoveryPort,'/tls-auth')
            $proof=[LokiSecureBridge]::Discovery($discovery.Uri.AbsoluteUri)|ConvertFrom-Json
            $pin=[string]$proof.fingerprint
            if($pin -notmatch '^[a-f0-9]{64}$' -or ![LokiSecureBridge]::Proof($Token,$pin,[string]$proof.proof)){throw 'Phone TLS authentication failed; HTTP fallback is disabled'}
            $script:LokiTlsPins[$cacheKey]=$pin
        }
    }
    if($pin -notmatch '^[a-f0-9]{64}$'){throw 'Enter the SHA256 displayed on the phone when pairing'}
    $target=[UriBuilder]::new($uri);$target.Scheme='https';$target.Port=$securePort
    return [LokiSecureBridge]::Create($target.Uri.AbsoluteUri,$pin)
}
function Invoke-LokiSecureRest {
    param([Parameter(Mandatory=$true)][string]$Uri,[string]$Method='Get',$Headers=@{},[string]$ContentType='application/json; charset=utf-8',$Body,[int]$TimeoutSec=5,[string]$Fingerprint='')
    $token=if($Headers -and $Headers.ContainsKey('X-Roki-Token')){[string]$Headers['X-Roki-Token']}else{''}
    $pair=([uri]$Uri).AbsolutePath -in @('/pair','/pc_pair')
    if($pair -and !$Fingerprint -and $token.Length -lt 16){
        if($script:LokiPairPins.ContainsKey(([uri]$Uri).Host)){$Fingerprint=$script:LokiPairPins[([uri]$Uri).Host]}
        elseif('System.Windows.Forms.Form' -as [type]){Add-Type -AssemblyName Microsoft.VisualBasic;$Fingerprint=[Microsoft.VisualBasic.Interaction]::InputBox('スマホのペアリング画面の証明書SHA256を入力してください','スマホの証明書確認','')}
        else{$Fingerprint=Read-Host 'スマホのペアリング画面の証明書SHA256'}
        if($Fingerprint){$script:LokiPairPins[([uri]$Uri).Host]=$Fingerprint}
    }
    $request=New-LokiSecureRequest $Uri $token $Fingerprint
    $request.Timeout=1000*$(if($pair){35}else{$TimeoutSec});$request.ReadWriteTimeout=$request.Timeout
    $request.Method=$Method.ToUpperInvariant()
    foreach($key in $Headers.Keys){$request.Headers.Add($key,[string]$Headers[$key])}
    if($pair){$rng=[Security.Cryptography.RandomNumberGenerator]::Create();$random=New-Object byte[] 4;$rng.GetBytes($random);$rng.Dispose();$code=('{0:D6}' -f ([BitConverter]::ToUInt32($random,0)%1000000));$request.Headers.Add('X-Roki-Pair-Code',$code);Write-Host "スマホで接続コード $code を確認・許可してください";if('System.Windows.Forms.Form' -as [type]){[Windows.Forms.MessageBox]::Show("接続コード: $code`nこの画面を閉じ、スマホで同じコードの接続を許可してください。",'接続確認')|Out-Null}}
    if($null -ne $Body){$request.ContentType=$ContentType;$bytes=if($Body -is [byte[]]){$Body}else{[Text.Encoding]::UTF8.GetBytes([string]$Body)};$request.ContentLength=$bytes.Length;$out=$request.GetRequestStream();try{$out.Write($bytes,0,$bytes.Length)}finally{$out.Dispose()}}
    $response=$request.GetResponse();$reader=[IO.StreamReader]::new($response.GetResponseStream(),[Text.Encoding]::UTF8)
    try{return ($reader.ReadToEnd()|ConvertFrom-Json)}finally{$reader.Dispose();$response.Dispose()}
}
