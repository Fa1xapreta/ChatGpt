param([ValidateSet('Aplicar','Desfazer')][string]$Acao='Aplicar',[string]$Cliente)
$ErrorActionPreference='Stop'
$originalSha='20250ed27749bc3e5efee5f307dce3590c982d21d14b3bbbae1e4c747919f3d0'
$patchedSha='80478436ae8811f67436c82d16c17dc89968f2b2a714045ea0da423ebbc5ec25'
function Hash-Bytes([byte[]]$bytes){$s=[Security.Cryptography.SHA256]::Create();try{return ([BitConverter]::ToString($s.ComputeHash($bytes))).Replace('-','').ToLowerInvariant()}finally{$s.Dispose()}}
try{
 $identity=[Security.Principal.WindowsIdentity]::GetCurrent();$principal=New-Object Security.Principal.WindowsPrincipal($identity)
 if(!$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){
  $arguments='-NoProfile -ExecutionPolicy Bypass -File "'+$PSCommandPath+'" -Acao '+$Acao;if($Cliente){$arguments+=' -Cliente "'+$Cliente+'"'}
  $child=Start-Process powershell.exe -ArgumentList $arguments -Verb RunAs -PassThru -Wait;exit $child.ExitCode
 }
 if(Get-Process -Name L2 -ErrorAction SilentlyContinue){throw 'Feche o jogo antes de aplicar ou desfazer.'}
 if(!$Cliente){
  $Cliente='C:\Program Files (x86)\LINEAGE ROYAL NOVO'
  if(!(Test-Path -LiteralPath (Join-Path $Cliente 'system\L2.exe'))){
   Add-Type -AssemblyName System.Windows.Forms;$dialog=New-Object System.Windows.Forms.FolderBrowserDialog;$dialog.Description='Escolha a pasta do jogo que contem system.'
   if($dialog.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK){exit 0};$Cliente=$dialog.SelectedPath
  }
 }
 $root=(Resolve-Path -LiteralPath $Cliente).Path;if((Split-Path -Leaf $root) -ieq 'system'){$root=Split-Path -Parent $root}
 if(!(Test-Path -LiteralPath (Join-Path $root 'system\L2.exe'))){throw 'Pasta do cliente incorreta.'}
 $path=Join-Path $root 'system\CapasClassic.dll';$backup=Join-Path $root 'Milenium_Corrida_Backup_TESTE6';$saved=Join-Path $backup 'CapasClassic.dll'
 $current=$null;$hash=$null;if(Test-Path -LiteralPath $path){$current=[IO.File]::ReadAllBytes($path);$hash=Hash-Bytes $current}
 if($Acao -eq 'Desfazer'){
  if(!(Test-Path -LiteralPath $saved)){throw 'Backup deste teste nao encontrado neste cliente.'}
  $bytes=[IO.File]::ReadAllBytes($saved);if((Hash-Bytes $bytes) -ne $originalSha){throw 'Backup diferente do original conhecido. Nenhuma troca foi feita.'}
  if($null -ne $hash -and $hash -ne $patchedSha -and $hash -ne $originalSha){throw 'CapasClassic foi alterada depois deste teste. Nenhuma troca foi feita.'}
  [IO.File]::WriteAllBytes($path,$bytes)
  if((Hash-Bytes ([IO.File]::ReadAllBytes($path))) -ne $originalSha){throw 'Falha ao conferir restauracao.'}
  Write-Host 'TESTE 6 DESFEITO. Motor de capas restaurado ao original deste teste.' -ForegroundColor Green
 }else{
  if($hash -eq $patchedSha){Write-Host 'TESTE 6 JA ESTA APLICADO.' -ForegroundColor Green;Read-Host 'Pressione Enter para fechar';exit 0}
  if($hash -ne $originalSha){throw 'Versao de CapasClassic diferente da analisada. Nenhuma troca foi feita.'}
  $offset=306624
  if([BitConverter]::ToDouble($current,$offset) -ne -50.0){throw 'Parametro de origem diferente do esperado.'}
  $newBytes=[byte[]]$current.Clone();[Array]::Copy([BitConverter]::GetBytes([double]-65.0),0,$newBytes,$offset,8)
  if((Hash-Bytes $newBytes) -ne $patchedSha){throw 'Resultado diferente do ajuste esperado. Nenhuma troca foi feita.'}
  New-Item -ItemType Directory -Force -Path $backup | Out-Null
  if(Test-Path -LiteralPath $saved){if((Hash-Bytes ([IO.File]::ReadAllBytes($saved))) -ne $originalSha){throw 'Backup existente diferente do original.'}}
  else{[IO.File]::WriteAllBytes($saved,$current)}
  if((Hash-Bytes ([IO.File]::ReadAllBytes($saved))) -ne $originalSha){throw 'Falha ao conferir backup.'}
  try{
   [IO.File]::WriteAllBytes($path,$newBytes)
   if((Hash-Bytes ([IO.File]::ReadAllBytes($path))) -ne $patchedSha){throw 'Falha ao conferir o ajuste gravado.'}
  }catch{[IO.File]::WriteAllBytes($path,$current);throw}
  Write-Host 'TESTE 6 APLICADO: parametro de movimento do humano fighter masculino.' -ForegroundColor Green
  Write-Host 'Teste a Freya 31394 correndo. O resultado visual ainda precisa ser confirmado.'
  Write-Host 'Afeta outras capas do mesmo corpo. Os campos de montaria e os outros corpos foram preservados.'
  Write-Host 'Use DESFAZER.bat deste kit para voltar ao estado anterior.'
 }
}catch{Write-Host ('NAO CONCLUIDO: '+$_.Exception.Message) -ForegroundColor Red;Read-Host 'Pressione Enter para fechar';exit 1}
Read-Host 'Pressione Enter para fechar'
