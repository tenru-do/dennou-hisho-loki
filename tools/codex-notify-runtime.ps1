# Bounded tails and crash-safe state. No notification text in health records.
if(-not ('LokiNotifyRuntime' -as [type])){
Add-Type -TypeDefinition @'
using System;using System.IO;using System.Text;
public sealed class LokiNotifyChunk { public string Text="";public string Anchor="";public long Next;public bool Discarding;public bool Oversized;public bool Truncated; }
public static class LokiNotifyRuntime {
 static string Fingerprint(FileStream f,long end){long position=f.Position;try{int count=(int)Math.Min(64,end);f.Position=end-count;byte[] bytes=new byte[count];int n=f.Read(bytes,0,count);using(var sha=System.Security.Cryptography.SHA256.Create())return Convert.ToBase64String(sha.ComputeHash(bytes,0,n));}finally{f.Position=position;}}
 public static LokiNotifyChunk Read(string path,long offset,bool discarding,int limit){return Read(path,offset,discarding,limit,"");}
 public static LokiNotifyChunk Read(string path,long offset,bool discarding,int limit,string expectedAnchor){
  if(limit<32||limit>4194304)throw new ArgumentOutOfRangeException("limit");
  using(var f=new FileStream(path,FileMode.Open,FileAccess.Read,FileShare.ReadWrite|FileShare.Delete)){
   var result=new LokiNotifyChunk();if(offset>f.Length||(offset>0&&!String.IsNullOrEmpty(expectedAnchor)&&expectedAnchor!=Fingerprint(f,offset))){offset=0;discarding=false;result.Truncated=true;}
   f.Position=offset;byte[] data=new byte[(int)Math.Min(limit,f.Length-offset)];int n=0,k;while(n<data.Length&&(k=f.Read(data,n,data.Length-n))>0)n+=k;
   int begin=0;if(discarding){while(begin<n&&data[begin]!=10)begin++;if(begin==n){result.Next=offset+n;result.Discarding=true;result.Anchor=Fingerprint(f,result.Next);return result;}begin++;}
   int end=n-1;while(end>=begin&&data[end]!=10)end--;
   if(end<begin){result.Next=offset+begin;if(n-begin==limit){result.Next=offset+n;result.Discarding=true;result.Oversized=true;}result.Anchor=Fingerprint(f,result.Next);return result;}
   result.Text=Encoding.UTF8.GetString(data,begin,end-begin+1);result.Next=offset+end+1;result.Anchor=Fingerprint(f,result.Next);return result;
  }
 }
 public static void Atomic(string path,string text){
  string temp=path+".tmp";try{using(var f=new FileStream(temp,FileMode.Create,FileAccess.Write,FileShare.None)){byte[] b=new UTF8Encoding(false).GetBytes(text);f.Write(b,0,b.Length);f.Flush(true);}if(File.Exists(path))File.Replace(temp,path,path+".bak");else File.Move(temp,path);}finally{if(File.Exists(temp))File.Delete(temp);}
 }
}
'@
}
function Read-LokiNotifyState([string]$path,$fallback){
 if(!(Test-Path -LiteralPath $path)){return $fallback}
 try{$raw=Get-Content -LiteralPath $path -Raw -Encoding UTF8 -ErrorAction Stop;if([string]::IsNullOrWhiteSpace($raw) -or $raw.Trim() -eq 'null'){throw 'Empty notification state'};return ($raw|ConvertFrom-Json -ErrorAction Stop)}catch{
  if(Test-Path -LiteralPath ($path+'.bak')){try{$backupRaw=Get-Content -LiteralPath ($path+'.bak') -Raw -Encoding UTF8 -ErrorAction Stop;if([string]::IsNullOrWhiteSpace($backupRaw) -or $backupRaw.Trim() -eq 'null'){throw 'Empty backup state'};return ($backupRaw|ConvertFrom-Json -ErrorAction Stop)}catch{}}
  throw 'Notification state unreadable; preserving files instead of replacing with empty state'
 }
}
function Save-LokiNotifyState([string]$path,$value){[LokiNotifyRuntime]::Atomic($path,(ConvertTo-Json -InputObject $value -Depth 12 -Compress))}
function Get-LokiNotifyResumeDecision($health,[long]$now,[int]$expectedPid,[long]$startedAt,[long]$timeout=180000){
 if($null -eq $health -or [int]$health.pid -ne $expectedPid){return ($now-$startedAt -gt $timeout)}
 return ($now-[long]$health.at -gt $timeout)
}
function Write-LokiNotifyHealth([string]$path,[string]$phase,[int]$queued,[int]$files,[string]$errorType=''){
 Save-LokiNotifyState $path @{pid=$PID;at=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();phase=$phase;queued=$queued;files=$files;errorType=$errorType}
}


function Write-LokiNotifyEvent([string]$path,[string]$event,[int]$workerPid=0,[int]$exitCode=0){
 try{
  if((Test-Path -LiteralPath $path) -and (Get-Item -LiteralPath $path).Length -gt 1048576){[IO.File]::Copy($path,$path+'.previous',$true);[IO.File]::WriteAllText($path,'')}
  $line=ConvertTo-Json -InputObject @{at=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();event=$event;pid=$PID;workerPid=$workerPid;exitCode=$exitCode} -Compress
  $stream=[IO.File]::Open($path,'Append','Write','ReadWrite')
  try{$bytes=(New-Object Text.UTF8Encoding($false)).GetBytes($line+[char]10);$stream.Write($bytes,0,$bytes.Length);$stream.Flush($true)}finally{$stream.Dispose()}
 }catch{$script:notifyEventWriteFailed=$true}
}
