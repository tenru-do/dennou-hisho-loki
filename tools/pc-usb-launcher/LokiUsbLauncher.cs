using System;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Windows.Forms;
[assembly:System.Reflection.AssemblyTitle("ロキをUSBで開く")]
[assembly:System.Reflection.AssemblyDescription("ロキUSB接続・文字コード事前検査付き")]
class LokiUsbLauncher {
 static readonly string Script=Path.Combine(Application.StartupPath,"Start-Loki-USB.ps1");
 static void ValidateScript(string path){
  byte[] bytes=File.ReadAllBytes(path);
  if(bytes.Length<3||bytes[0]!=0xef||bytes[1]!=0xbb||bytes[2]!=0xbf)
   throw new InvalidDataException("起動スクリプトの文字コードが不正です。BOM付きUTF-8で保存し直す必要があります。USB接続の問題ではありません。");
  new UTF8Encoding(false,true).GetString(bytes,3,bytes.Length-3);
 }
 static int SelfTest(){
  string root=Path.Combine(Path.GetTempPath(),"LokiLauncherTest-"+Guid.NewGuid().ToString("N"));Directory.CreateDirectory(root);
  try {
   string file=Path.Combine(root,"fixture.ps1");string body="Write-Output '接続確認'";
   File.WriteAllText(file,body,new UTF8Encoding(true));ValidateScript(file);
   File.WriteAllText(file,body,new UTF8Encoding(false));bool rejected=false;try{ValidateScript(file);}catch(InvalidDataException){rejected=true;}if(!rejected)return 2;
   File.WriteAllBytes(file,new byte[]{0xef,0xbb,0xbf,0xff});rejected=false;try{ValidateScript(file);}catch(DecoderFallbackException){rejected=true;}if(!rejected)return 3;
   return 0;
  }finally {File.Delete(Path.Combine(root,"fixture.ps1"));Directory.Delete(root);}
 }
 [STAThread] static int Main(string[] args){
  if(args.Length==1&&args[0]=="--self-test")return SelfTest();
  string stage="起動スクリプトの確認";
  try {
   ValidateScript(Script);stage="USB接続スクリプトの実行";
   var info=new ProcessStartInfo(@"C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe", "-NoProfile -ExecutionPolicy Bypass -File \""+Script+"\"");
   info.UseShellExecute=false;info.CreateNoWindow=true;info.WindowStyle=ProcessWindowStyle.Hidden;info.RedirectStandardError=true;info.StandardErrorEncoding=Encoding.Default;
   using(var process=Process.Start(info)){
    var errorTask=process.StandardError.ReadToEndAsync();process.WaitForExit();string error=errorTask.Wait(500)?errorTask.Result:"詳細を取得できませんでした。";
    if(process.ExitCode!=0){error=error.Trim();if(error.Length>1200)error=error.Substring(0,1200);throw new ApplicationException("接続処理が終了コード "+process.ExitCode+" で停止しました。\n"+error);}
   }
   return 0;
  }catch(Exception error){MessageBox.Show(stage+"で停止しました。\n\n"+error.Message,"ロキ USB接続",MessageBoxButtons.OK,MessageBoxIcon.Error);return 1;}
 }
}