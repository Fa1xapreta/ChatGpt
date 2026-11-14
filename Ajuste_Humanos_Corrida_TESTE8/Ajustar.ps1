param([ValidateSet('Aplicar','Desfazer')][string]$Acao='Aplicar',[string]$Cliente)
$ErrorActionPreference='Stop'
$originalSha='8a22d41ceaa78d52a10b856b923462eb8eb08d61b203d0d7904e464857ae988c'
$test7Sha='dcd14791f086d0f3053837f12dca8883ee0f2c1e6ea17ba68aab52d88d4ae170'
$test8Sha='75b4aa213a0d7164cb3b6486a512108627fb8a61239f73a33ea8201f6bc9b9a4'
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
   Add-Type -AssemblyName System.Windows.Forms;$dialog=New-Object System.Windows.Forms.FolderBrowserDialog;$dialog.Description='Escolha a pasta do jogo que contem system e Animations.'
   if($dialog.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK){exit 0};$Cliente=$dialog.SelectedPath
  }
 }
 $root=(Resolve-Path -LiteralPath $Cliente).Path;if((Split-Path -Leaf $root) -ieq 'system'){$root=Split-Path -Parent $root}
 if(!(Test-Path -LiteralPath (Join-Path $root 'system\L2.exe'))){throw 'Pasta do cliente incorreta.'}
 $path=Join-Path $root 'Animations\Fighter.ukx';$backup=Join-Path $root 'Milenium_Humanos_Backup_TESTE8';$savedBase=Join-Path $backup 'Fighter_original.ukx';$savedBefore=Join-Path $backup 'Fighter_antes_TESTE8.ukx'
 $current=$null;$hash=$null;if(Test-Path -LiteralPath $path){$current=[IO.File]::ReadAllBytes($path);$hash=Hash-Bytes $current}
 if($Acao -eq 'Desfazer'){
  if(!(Test-Path -LiteralPath $savedBase)){throw 'Backup original deste teste nao encontrado neste cliente.'}
  $bytes=[IO.File]::ReadAllBytes($savedBase);if((Hash-Bytes $bytes) -ne $originalSha){throw 'Backup original diferente do arquivo recebido. Nenhuma troca foi feita.'}
  if($null -ne $hash -and @($test8Sha,$test7Sha,$originalSha) -notcontains $hash){throw 'Fighter.ukx foi alterado por outra versao depois do teste. Nenhuma troca foi feita.'}
  [IO.File]::WriteAllBytes($path,$bytes)
  if((Hash-Bytes ([IO.File]::ReadAllBytes($path))) -ne $originalSha){throw 'Falha ao conferir restauracao.'}
  Write-Host 'ANIMACOES ORIGINAIS RESTAURADAS, SEM TESTE 7 E SEM TESTE 8.' -ForegroundColor Green
 }else{
  if($hash -eq $test8Sha){Write-Host 'TESTE 8 JA ESTA APLICADO.' -ForegroundColor Green;Read-Host 'Pressione Enter para fechar';exit 0}
  if(@($originalSha,$test7Sha) -notcontains $hash){throw 'Fighter.ukx diferente do original e do TESTE 7 analisados. Nenhuma troca foi feita.'}
  $spec=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'alteracao.json') -Raw | ConvertFrom-Json
  if($spec.source_sha256 -ne $originalSha -or $spec.test7_sha256 -ne $test7Sha -or $spec.target_sha256 -ne $test8Sha){throw 'Descritor do teste diferente do esperado.'}
  $baseBytes=[byte[]]$current.Clone()
  if($hash -eq $test7Sha){
   if(@($spec.remove_test7_changes).Count -ne 12){throw 'Descritor da retirada do TESTE 7 incompleto.'}
   foreach($p in $spec.remove_test7_changes){
    $offset=[int]$p.offset;$before=[byte[]]$p.before;$after=[byte[]]$p.after
    if($offset -lt 28 -or $offset+4 -gt $baseBytes.Length -or $before.Length -ne 4 -or $after.Length -ne 4){throw 'Trecho de restauracao invalido.'}
    for($i=0;$i -lt 4;$i++){if($baseBytes[$offset+$i] -ne $after[$i]){throw 'Bytes do TESTE 7 diferentes do esperado.'}}
    [Array]::Copy($before,0,$baseBytes,$offset,4)
   }
  }
  if((Hash-Bytes $baseBytes) -ne $originalSha){throw 'Nao foi possivel recuperar a base original em memoria.'}
  $header=[IO.File]::ReadAllBytes((Join-Path $PSScriptRoot 'cabecalho.bin'));$append=[IO.File]::ReadAllBytes((Join-Path $PSScriptRoot 'animacoes.bin'))
  if($header.Length -ne 92 -or (Hash-Bytes $header) -ne $spec.header_sha256 -or (Hash-Bytes $append) -ne $spec.append_sha256){throw 'Arquivos do teste incompletos ou alterados.'}
  $newBytes=New-Object byte[] ($baseBytes.Length+$append.Length)
  [Array]::Copy($baseBytes,0,$newBytes,0,$baseBytes.Length);[Array]::Copy($header,0,$newBytes,0,$header.Length);[Array]::Copy($append,0,$newBytes,$baseBytes.Length,$append.Length)
  if((Hash-Bytes $newBytes) -ne $test8Sha){throw 'Resultado diferente do ajuste esperado. Nenhuma troca foi feita.'}
  New-Item -ItemType Directory -Force -Path $backup | Out-Null
  if(Test-Path -LiteralPath $savedBase){if((Hash-Bytes ([IO.File]::ReadAllBytes($savedBase))) -ne $originalSha){throw 'Backup original existente foi alterado.'}}else{[IO.File]::WriteAllBytes($savedBase,$baseBytes)}
  if(Test-Path -LiteralPath $savedBefore){if(@($originalSha,$test7Sha) -notcontains (Hash-Bytes ([IO.File]::ReadAllBytes($savedBefore)))){throw 'Backup do estado encontrado foi alterado.'}}else{[IO.File]::WriteAllBytes($savedBefore,$current)}
  if((Hash-Bytes ([IO.File]::ReadAllBytes($savedBase))) -ne $originalSha){throw 'Falha ao conferir backup original.'}
  try{
   [IO.File]::WriteAllBytes($path,$newBytes)
   if((Hash-Bytes ([IO.File]::ReadAllBytes($path))) -ne $test8Sha){throw 'Falha ao conferir o ajuste gravado.'}
  }catch{[IO.File]::WriteAllBytes($path,$current);throw}
  Write-Host 'TESTE 8 APLICADO: posicao original do encaixe e inclinacao suave nas corridas humanas.' -ForegroundColor Green
  Write-Host 'O afastamento do TESTE 7 foi retirado. O resultado visual ainda precisa de confirmacao no jogo.'
  Write-Host "Backup: $backup"
  Write-Host 'DESFAZER.bat restaura a animacao original, sem TESTE 7/8. Aura e arquivos de outras racas foram preservados.'
 }
}catch{Write-Host ('NAO CONCLUIDO: '+$_.Exception.Message) -ForegroundColor Red;Read-Host 'Pressione Enter para fechar';exit 1}
Read-Host 'Pressione Enter para fechar'
