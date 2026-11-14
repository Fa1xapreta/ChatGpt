param([switch]$SemPausa,[switch]$FalhaInstalacao,[string]$Cliente)
$ErrorActionPreference='Stop'
function Sha($p){return (Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash.ToLowerInvariant()}
try{
 $identity=[Security.Principal.WindowsIdentity]::GetCurrent();$principal=New-Object Security.Principal.WindowsPrincipal($identity)
 if(!$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){
  $arguments='-NoProfile -ExecutionPolicy Bypass -File "'+$PSCommandPath+'"';if($SemPausa){$arguments+=' -SemPausa'};if($FalhaInstalacao){$arguments+=' -FalhaInstalacao'}
  if($Cliente){$arguments+=' -Cliente "'+$Cliente+'"'}
  $child=Start-Process powershell.exe -ArgumentList $arguments -Verb RunAs -PassThru -Wait;exit $child.ExitCode
 }
 if(Get-Process -Name L2 -ErrorAction SilentlyContinue){throw 'Feche o jogo antes de desfazer.'}
 if(!(Test-Path -LiteralPath (Join-Path $PSScriptRoot 'destino.json'))){
  if(!$Cliente){$Cliente='C:\Program Files (x86)\LINEAGE ROYAL NOVO'}
  if(!(Test-Path -LiteralPath (Join-Path $Cliente 'system\L2.exe'))){Add-Type -AssemblyName System.Windows.Forms;$d=New-Object System.Windows.Forms.FolderBrowserDialog;$d.Description='Escolha o cliente onde instalou a ponte B2.';if($d.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK){exit 0};$Cliente=$d.SelectedPath}
  $clientRoot=(Resolve-Path -LiteralPath $Cliente).Path;if((Split-Path -Leaf $clientRoot) -ieq 'system'){$clientRoot=Split-Path -Parent $clientRoot}
  $last=Get-ChildItem -LiteralPath $clientRoot -Directory -Filter 'Milenium_Aura_Backup_B2_*' | Sort-Object Name -Descending | Select-Object -First 1
  if(!$last){throw 'Backup B2 nao encontrado neste cliente.'}
  $where=Get-Content -LiteralPath (Join-Path $last.FullName 'destino.json') -Raw | ConvertFrom-Json
  if(![string]::Equals([IO.Path]::GetFullPath($where.cliente),[IO.Path]::GetFullPath($clientRoot),[StringComparison]::OrdinalIgnoreCase)){throw 'Backup pertence a outro cliente.'}
  & (Join-Path $last.FullName 'Restaurar.ps1') -SemPausa:$SemPausa -FalhaInstalacao:$FalhaInstalacao
  return
 }
 $config=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'destino.json') -Raw | ConvertFrom-Json;$root=(Resolve-Path -LiteralPath $config.cliente).Path;$records=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'restaurar.json') -Raw | ConvertFrom-Json
 foreach($r in $records){if($r.existed -and (Sha (Join-Path $PSScriptRoot $r.file)) -ne $r.before){throw 'Backup foi alterado.'};$path=Join-Path $root ('system\'+$r.file);if(!$FalhaInstalacao -and (Test-Path -LiteralPath $path)){if(@($r.before,$r.after) -notcontains (Sha $path)){throw ('Arquivo mudou depois da instalacao: '+$r.file)}}}
 foreach($r in $records){$path=Join-Path $root ('system\'+$r.file);if($r.existed){Copy-Item -LiteralPath (Join-Path $PSScriptRoot $r.file) -Destination $path -Force;if((Sha $path) -ne $r.before){throw 'Falha ao conferir restauracao.'}}elseif(Test-Path -LiteralPath $path){Remove-Item -LiteralPath $path}}
 Write-Host 'B2 DESFEITO. Estado anterior a instalacao restaurado.' -ForegroundColor Green
}catch{Write-Host ('RESTAURACAO NAO CONCLUIDA: '+$_.Exception.Message) -ForegroundColor Red;if($SemPausa){throw};Read-Host 'Pressione Enter para fechar';exit 1}
if(!$SemPausa){Read-Host 'Pressione Enter para fechar'}
