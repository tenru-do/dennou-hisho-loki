function Start-LokiCodexRpc {
 $found=Get-Command codex -ErrorAction SilentlyContinue
 $cli=if($found){$found.Source}else{Get-ChildItem (Join-Path $env:LOCALAPPDATA 'OpenAI/Codex/bin/*/codex.exe') -ErrorAction SilentlyContinue|Sort-Object LastWriteTime -Descending|Select-Object -First 1 -ExpandProperty FullName}
 if(!$cli){throw 'Installed Codex CLI not found'}
 $info=[Diagnostics.ProcessStartInfo]::new($cli,'app-server --listen stdio://')
 $info.UseShellExecute=$false;$info.CreateNoWindow=$true
 $info.RedirectStandardInput=$true;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
 $info.StandardOutputEncoding=[Text.UTF8Encoding]::new($false)
 $script:LokiRpcProcess=[Diagnostics.Process]::Start($info);$script:LokiRpcProcess.BeginErrorReadLine();$script:LokiRpcId=0
 Invoke-LokiCodexRpc 'initialize' @{clientInfo=@{name='loki_instruction_bridge';version='0.1'};capabilities=@{experimentalApi=$true}}|Out-Null
 $script:LokiRpcProcess.StandardInput.WriteLine('{"method":"initialized","params":{}}');$script:LokiRpcProcess.StandardInput.Flush()
}
function Invoke-LokiCodexRpc([string]$method,$parameters) {
 if(!$script:LokiRpcProcess -or $script:LokiRpcProcess.HasExited){throw 'Codex RPC disconnected'}
 $id=++$script:LokiRpcId
 $line=@{id=$id;method=$method;params=$parameters}|ConvertTo-Json -Depth 30 -Compress
 # ASCII wire data avoids Windows stdin code-page changes; JSON preserves Unicode.
 $line=[regex]::Replace($line,'[^\x00-\x7f]',{param($m)'\u'+('{0:x4}' -f [int][char]$m.Value)})
 $script:LokiRpcProcess.StandardInput.WriteLine($line);$script:LokiRpcProcess.StandardInput.Flush()
 $deadline=[DateTime]::UtcNow.AddSeconds(12)
 while([DateTime]::UtcNow -lt $deadline){
  $read=$script:LokiRpcProcess.StandardOutput.ReadLineAsync()
  if(!$read.Wait([Math]::Max(1,[int]($deadline-[DateTime]::UtcNow).TotalMilliseconds))){throw 'Codex RPC response unknown; do not automatically resend'}
  if($null -eq $read.Result){throw 'Codex RPC ended'}
  $reply=$read.Result|ConvertFrom-Json
  if($reply.PSObject.Properties['method']){continue}
  if($reply.id -ne $id){continue}
  if($reply.error){throw ('Codex RPC rejected: '+[string]$reply.error.code)}
  return $reply.result
 }
 throw 'Codex RPC timed out'
}
function Stop-LokiCodexRpc {
 if($script:LokiRpcProcess){if(!$script:LokiRpcProcess.HasExited){$script:LokiRpcProcess.Kill()};$script:LokiRpcProcess.Dispose();$script:LokiRpcProcess=$null}
}
