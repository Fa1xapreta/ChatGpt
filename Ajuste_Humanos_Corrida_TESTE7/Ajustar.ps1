param([ValidateSet('Aplicar','Desfazer')][string]$Acao='Aplicar',[string]$Cliente)
$ErrorActionPreference='Stop'
$originalSha='8a22d41ceaa78d52a10b856b923462eb8eb08d61b203d0d7904e464857ae988c'
$patchedSha='dcd14791f086d0f3053837f12dca8883ee0f2c1e6ea17ba68aab52d88d4ae170'
function Hash-Bytes([byte[]]$bytes){$s=[Security.Cryptography.SHA256]::Create();try{return ([BitConverter]::ToString($s.ComputeHash($bytes))).Replace('-','').ToLowerInvariant()}finally{$s.Dispose()}}
try{
 if($Acao -eq 'Aplicar'){throw 'TESTE 7 RETIRADO: afasta o encaixe nos ombros. Use o TESTE 8 ou DESFAZER.bat para restaurar.'}
 $identity=[Security.Principal.WindowsIdentity]::GetCurrent();$principal=New-Object Security.Principal.WindowsPrincipal($identity)
 if(!$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){
  $arguments='-NoProfile -ExecutionPolicy Bypass -File "'+$PSCommandPath+'" -Acao '+$Acao;if($Cliente){$arguments+=' -Cliente "'+$Cliente+'"'}
  $child=Start-Process powershell.exe -ArgumentList $arguments -Verb RunAs -PassThru -Wait;exit $child.ExitCode
 }
 if(Get-Process -Name L2 -ErrorAction SilentlyContinue){throw 'Feche totalmente o jogo antes de aplicar ou desfazer.'}
 if(!$Cliente){
  $Cliente='C:\Program Files (x86)\LINEAGE ROYAL NOVO'
  if(!(Test-Path -LiteralPath (Join-Path $Cliente 'system\L2.exe'))){
   Add-Type -AssemblyName System.Windows.Forms;$dialog=New-Object System.Windows.Forms.FolderBrowserDialog;$dialog.Description='Escolha a pasta do jogo que contem system e Animations.'
   if($dialog.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK){exit 0};$Cliente=$dialog.SelectedPath
  }
 }
 $root=(Resolve-Path -LiteralPath $Cliente).Path;if((Split-Path -Leaf $root) -ieq 'system'){$root=Split-Path -Parent $root}
 if(!(Test-Path -LiteralPath (Join-Path $root 'system\L2.exe'))){throw 'Pasta do cliente incorreta.'}
 $path=Join-Path $root 'Animations\Fighter.ukx';$backup=Join-Path $root 'Milenium_Humanos_Backup_TESTE7';$saved=Join-Path $backup 'Fighter.ukx'
 $current=$null;$hash=$null;if(Test-Path -LiteralPath $path){$current=[IO.File]::ReadAllBytes($path);$hash=Hash-Bytes $current}
 if($Acao -eq 'Desfazer'){
  if(!(Test-Path -LiteralPath $saved)){throw 'Backup do TESTE 7 nao encontrado neste cliente.'}
  $bytes=[IO.File]::ReadAllBytes($saved);if((Hash-Bytes $bytes) -ne $originalSha){throw 'Backup diferente do original recebido. Nenhuma troca foi feita.'}
  if($null -ne $hash -and $hash -ne $patchedSha -and $hash -ne $originalSha){throw 'Fighter.ukx foi alterado depois deste teste. Nenhuma troca foi feita.'}
  [IO.File]::WriteAllBytes($path,$bytes)
  if((Hash-Bytes ([IO.File]::ReadAllBytes($path))) -ne $originalSha){throw 'Falha ao conferir restauracao.'}
  Write-Host 'TESTE 7 DESFEITO. Fighter.ukx original restaurado.' -ForegroundColor Green
 }else{
  if($hash -eq $patchedSha){Write-Host 'TESTE 7 JA ESTA APLICADO.' -ForegroundColor Green;Read-Host 'Pressione Enter para fechar';exit 0}
  if($hash -ne $originalSha){throw 'Fighter.ukx diferente do arquivo que voce enviou. Nenhuma troca foi feita.'}
  $spec=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'alteracao.json') -Raw | ConvertFrom-Json
  if($spec.source_sha256 -ne $originalSha -or $spec.patched_sha256 -ne $patchedSha -or @($spec.changes).Count -ne 12){throw 'Descritor do teste diferente do esperado.'}
  $newBytes=[byte[]]$current.Clone();$seen=@{}
  foreach($patch in $spec.changes){
   $offset=[int]$patch.offset;$before=[byte[]]$patch.before;$after=[byte[]]$patch.after
   if($offset -lt 28 -or $offset+4 -gt $newBytes.Length -or $before.Length -ne 4 -or $after.Length -ne 4 -or $seen.ContainsKey($offset)){throw 'Trecho de alteracao invalido.'}
   $seen[$offset]=$true
   for($i=0;$i -lt 4;$i++){if($current[$offset+$i] -ne $before[$i]){throw 'Bytes de origem diferentes do esperado.'}}
   [Array]::Copy($after,0,$newBytes,$offset,4)
  }
  if((Hash-Bytes $newBytes) -ne $patchedSha){throw 'Resultado diferente do ajuste esperado. Nenhuma troca foi feita.'}
  New-Item -ItemType Directory -Force -Path $backup | Out-Null
  if(Test-Path -LiteralPath $saved){if((Hash-Bytes ([IO.File]::ReadAllBytes($saved))) -ne $originalSha){throw 'Backup existente diferente do original.'}}
  else{[IO.File]::WriteAllBytes($saved,$current)}
  if((Hash-Bytes ([IO.File]::ReadAllBytes($saved))) -ne $originalSha){throw 'Falha ao conferir backup.'}
  try{
   [IO.File]::WriteAllBytes($path,$newBytes)
   if((Hash-Bytes ([IO.File]::ReadAllBytes($path))) -ne $patchedSha){throw 'Falha ao conferir o ajuste gravado.'}
  }catch{[IO.File]::WriteAllBytes($path,$current);throw}
  Write-Host 'TESTE 7 APLICADO: ancora de capa nas corridas dos humanos fighter.' -ForegroundColor Green
  Write-Host 'Teste a Freya 31394 correndo com o humano masculino e feminino. O resultado visual ainda precisa ser confirmado.'
  Write-Host 'O ajuste e de corpo: afeta todas as capas desses humanos durante Run.'
  Write-Host "Backup: $backup"
  Write-Host 'Se nao melhorar, feche o jogo e execute DESFAZER.bat deste teste.'
 }
}catch{Write-Host ('NAO CONCLUIDO: '+$_.Exception.Message) -ForegroundColor Red;Read-Host 'Pressione Enter para fechar';exit 1}
Read-Host 'Pressione Enter para fechar'
