param([string]$ThreadId='', [string]$ExpectedTitle='')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'codex-stdio-rpc.ps1')
try{
 Start-LokiCodexRpc
 if(!$ThreadId){
  $items=@((Invoke-LokiCodexRpc 'thread/list' @{limit=100;archived=$false}).data|Where-Object {$_.name -and !$_.parentThreadId -and ([string]$_.source -notmatch 'subagent|review')})
  Add-Type -AssemblyName System.Windows.Forms
  $form=[Windows.Forms.Form]::new();$form.Text='ロキからのCodex指示の送信先';$form.Width=650;$form.Height=210
  $label=[Windows.Forms.Label]::new();$label.Text='Codexで開いている会話とタイトルが一致するものを選んでください。切替時は登録し直してください。';$label.SetBounds(15,15,600,45)
  $choice=[Windows.Forms.ComboBox]::new();$choice.DropDownStyle='DropDownList';$choice.SetBounds(15,65,600,28)
  foreach($item in $items){[void]$choice.Items.Add([string]$item.name)}
  $button=[Windows.Forms.Button]::new();$button.Text='この会話を送信先に登録';$button.SetBounds(350,110,250,30)
  $button.Add_Click({if($choice.SelectedIndex -ge 0){$form.DialogResult=[Windows.Forms.DialogResult]::OK;$form.Close()}})
  $form.Controls.AddRange(@($label,$choice,$button));if($form.ShowDialog() -ne 'OK'){exit 0}
  $ThreadId=[string]$items[$choice.SelectedIndex].id
 }
 $thread=(Invoke-LokiCodexRpc 'thread/read' @{threadId=$ThreadId;includeTurns=$false}).thread
 if($thread.id -ne $ThreadId -or !$thread.name -or ($ExpectedTitle -and $thread.name -ne $ExpectedTitle)){throw 'Conversation title/id mismatch'}
 $data=@{threadId=$ThreadId;title=[string]$thread.name;registeredAt=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()}|ConvertTo-Json
 [IO.File]::WriteAllText((Join-Path $PSScriptRoot 'codex-instruction-target.local.json'),$data,[Text.UTF8Encoding]::new($false))
 Write-Output ('登録しました: '+$thread.name)
}finally{Stop-LokiCodexRpc}
