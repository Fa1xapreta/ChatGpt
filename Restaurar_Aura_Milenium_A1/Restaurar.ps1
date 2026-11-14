param([ValidateSet('Aplicar','Desfazer')][string]$Acao='Aplicar',[string]$Cliente)
$ErrorActionPreference='Stop'
$originalSha='2b496d235cca529b6180588a987ca0059600a2323193d96e87b90014105be0c2'
$patchedSha='f670f30ce22973a36e8fec49c3536c9ed05a49fe47b9205a778230a1c17e79e2'
function Hash-Bytes([byte[]]$bytes){$s=[Security.Cryptography.SHA256]::Create();try{return ([BitConverter]::ToString($s.ComputeHash($bytes))).Replace('-','').ToLowerInvariant()}finally{$s.Dispose()}}
try{
 $identity=[Security.Principal.WindowsIdentity]::GetCurrent();$principal=New-Object Security.Principal.WindowsPrincipal($identity)
 if(!$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){
  $arguments='-NoProfile -ExecutionPolicy Bypass -File "'+$PSCommandPath+'" -Acao '+$Acao;if($Cliente){$arguments+=' -Cliente "'+$Cliente+'"'}
  $child=Start-Process powershell.exe -ArgumentList $arguments -Verb RunAs -PassThru -Wait;exit $child.ExitCode
 }
 if(Get-Process -Name L2 -ErrorAction SilentlyContinue){throw 'Feche totalmente o jogo antes de aplicar ou desfazer.'}
 if(!$Cliente){
  $Cliente='C:\Program Files (x86)\LINEAGE ROYAL NOVO'
  if(!(Test-Path -LiteralPath (Join-Path $Cliente 'system\L2.exe'))){
   Add-Type -AssemblyName System.Windows.Forms;$dialog=New-Object System.Windows.Forms.FolderBrowserDialog;$dialog.Description='Escolha a pasta do jogo que contem system.'
   if($dialog.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK){exit 0};$Cliente=$dialog.SelectedPath
  }
 }
 $root=(Resolve-Path -LiteralPath $Cliente).Path;if((Split-Path -Leaf $root) -ieq 'system'){$root=Split-Path -Parent $root}
 if(!(Test-Path -LiteralPath (Join-Path $root 'system\L2.exe'))){throw 'Pasta do cliente incorreta.'}
 $path=Join-Path $root 'system\D3DDrv.dll';$backup=Join-Path $root 'Milenium_Aura_Backup_A1';$saved=Join-Path $backup 'D3DDrv.dll'
 $current=$null;$hash=$null;if(Test-Path -LiteralPath $path){$current=[IO.File]::ReadAllBytes($path);$hash=Hash-Bytes $current}
 if($Acao -eq 'Desfazer'){
  if(!(Test-Path -LiteralPath $saved)){throw 'Backup da restauracao A1 nao encontrado neste cliente.'}
  $bytes=[IO.File]::ReadAllBytes($saved);if((Hash-Bytes $bytes) -ne $originalSha){throw 'Backup diferente do driver anterior conhecido. Nenhuma troca foi feita.'}
  if($null -ne $hash -and $hash -ne $patchedSha -and $hash -ne $originalSha){throw 'D3DDrv.dll foi alterado depois deste teste. Nenhuma troca foi feita.'}
  [IO.File]::WriteAllBytes($path,$bytes)
  if((Hash-Bytes ([IO.File]::ReadAllBytes($path))) -ne $originalSha){throw 'Falha ao conferir restauracao.'}
  Write-Host 'RESTAURACAO A1 DESFEITA. Driver anterior do modelo restaurado. A aura pode voltar a ficar desativada.' -ForegroundColor Green
 }else{
  if($hash -eq $patchedSha){Write-Host 'O DRIVER ORIGINAL JA ESTA INSTALADO. Abra o jogo e pressione F10; se a aura nao voltar, execute VERIFICAR_AURA.bat.' -ForegroundColor Green;Read-Host 'Pressione Enter para fechar';exit 0}
  if($hash -ne $originalSha){throw 'D3DDrv.dll diferente das versoes analisadas. Nenhuma troca foi feita.'}
  $newBytes=[IO.File]::ReadAllBytes((Join-Path $PSScriptRoot 'original\D3DDrv.dll'))
  if((Hash-Bytes $newBytes) -ne $patchedSha){throw 'Resultado diferente do ajuste esperado. Nenhuma troca foi feita.'}
  New-Item -ItemType Directory -Force -Path $backup | Out-Null
  if(Test-Path -LiteralPath $saved){if((Hash-Bytes ([IO.File]::ReadAllBytes($saved))) -ne $originalSha){throw 'Backup existente diferente do original.'}}
  else{[IO.File]::WriteAllBytes($saved,$current)}
  if((Hash-Bytes ([IO.File]::ReadAllBytes($saved))) -ne $originalSha){throw 'Falha ao conferir backup.'}
  try{
   [IO.File]::WriteAllBytes($path,$newBytes)
   if((Hash-Bytes ([IO.File]::ReadAllBytes($path))) -ne $patchedSha){throw 'Falha ao conferir o ajuste gravado.'}
  }catch{[IO.File]::WriteAllBytes($path,$current);throw}
  Write-Host 'DRIVER ORIGINAL RESTAURADO PARA COMPATIBILIDADE COM SUA AURA.' -ForegroundColor Green
  Write-Host 'Abra o jogo, entre no personagem e pressione somente F10 para ligar a aura.'
  Write-Host 'O ajuste de corrida TESTE 7 foi preservado. Se a aura nao voltar, execute VERIFICAR_AURA.bat com o jogo aberto.'
  Write-Host "Backup: $backup"
  Write-Host 'Para retornar ao driver anterior, feche o jogo e execute DESFAZER.bat desta pasta.'
 }
}catch{Write-Host ('NAO CONCLUIDO: '+$_.Exception.Message) -ForegroundColor Red;Read-Host 'Pressione Enter para fechar';exit 1}
Read-Host 'Pressione Enter para fechar'
