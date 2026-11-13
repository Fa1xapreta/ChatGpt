param([string]$Cliente)
$ErrorActionPreference='Stop'
$kit=$PSScriptRoot
$kitId='Milenium_Capas_Completo_TESTE_2'
function Get-Sha([string]$Path){return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()}
function Assert-File([string]$Path,[string]$Sha){
 if(!(Test-Path -LiteralPath $Path -PathType Leaf)){throw "Arquivo ausente: $Path"}
 if((Get-Sha $Path) -ne $Sha){throw "Arquivo incompleto ou diferente: $Path"}
}
function Elevate-IfNeeded {
 $identity=[Security.Principal.WindowsIdentity]::GetCurrent()
 $principal=New-Object Security.Principal.WindowsPrincipal($identity)
 if(!$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){
  $arguments='-NoProfile -ExecutionPolicy Bypass -File "'+$PSCommandPath+'"'
  if($Cliente){$arguments+=' -Cliente "'+$Cliente+'"'}
  $process=Start-Process -FilePath 'powershell.exe' -ArgumentList $arguments -Verb RunAs -PassThru -Wait
  exit $process.ExitCode
 }
}
try {
 Elevate-IfNeeded
 if(Get-Process -Name L2 -ErrorAction SilentlyContinue){throw 'Feche todas as janelas do jogo e clique novamente em INSTALAR.bat.'}
 if(!$Cliente){
  $known='C:\Program Files (x86)\LINEAGE ROYAL NOVO'
  if(Test-Path -LiteralPath (Join-Path $known 'system\L2.exe')){$Cliente=$known}
  else{
   Add-Type -AssemblyName System.Windows.Forms
   $dialog=New-Object System.Windows.Forms.FolderBrowserDialog
   $dialog.Description='Escolha a pasta do seu jogo (a pasta que contem system, Animations e SysTextures).'
   if($dialog.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK){Write-Host 'Instalacao cancelada; nenhum arquivo alterado.';exit 0}
   $Cliente=$dialog.SelectedPath
  }
 }
 $root=(Resolve-Path -LiteralPath $Cliente).Path
 if((Split-Path -Leaf $root) -ieq 'system'){$root=Split-Path -Parent $root}
 if(!(Test-Path -LiteralPath (Join-Path $root 'system\L2.exe'))){throw 'A pasta escolhida nao contem system\L2.exe.'}
 if($kit.StartsWith($root.TrimEnd('\')+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Extraia o kit fora da pasta do jogo, por exemplo na Area de Trabalho, e execute INSTALAR.bat dali.'}
 if(Test-Path -LiteralPath (Join-Path $root 'Milenium_Capas_TESTE_2_instalado.json')){throw 'Este kit ja foi instalado nesta pasta. Para repetir, use RESTAURAR.bat no backup antes de instalar novamente.'}
 Write-Host "Cliente identificado: $root"
 Write-Host 'Conferindo todos os arquivos do kit. Aguarde...'
 $manifest=Get-Content -LiteralPath (Join-Path $kit 'arquivos_sha256.json') -Raw | ConvertFrom-Json
 $checked=0
 foreach($entry in $manifest){
  $checked++
  Write-Progress -Activity 'Conferindo o kit completo' -Status $entry.file -PercentComplete ([int](100*$checked/@($manifest).Count))
  Assert-File (Join-Path $kit $entry.file) $entry.sha256
 }
 Write-Progress -Activity 'Conferindo o kit completo' -Completed
 $accepted=Get-Content -LiteralPath (Join-Path $kit 'cliente_aceito.json') -Raw | ConvertFrom-Json
 foreach($property in $accepted.PSObject.Properties){
  $path=Join-Path $root ('system\'+$property.Name)
  if(!(Test-Path -LiteralPath $path -PathType Leaf)){throw "Falta no cliente: $path"}
  if(@($property.Value) -notcontains (Get-Sha $path)){throw "Versao diferente da analisada: $path. Nenhuma substituicao foi feita."}
 }
 $plan=New-Object 'System.Collections.Generic.List[object]'
 foreach($entry in $manifest | Where-Object {$_.file.StartsWith('cliente/')} ){
  $relative=$entry.file.Substring(8).Replace('/','\')
  $plan.Add([pscustomobject]@{relative=$relative;source=(Join-Path $kit $entry.file);sha256=$entry.sha256})
 }
 foreach($relative in @('system\dsetup_base.dll','system\essence_capes.bin','system\essence_cloth.bin','system\essence_fx.bin','Animations\DynastyCloak.ukx')){
  if(Test-Path -LiteralPath (Join-Path $root $relative)){$plan.Add([pscustomobject]@{relative=$relative;source=$null;sha256=$null})}
 }
 $stamp=Get-Date -Format 'yyyyMMdd_HHmmss_fff'
 $backup=Join-Path $root ('Milenium_Capas_Backup_'+$stamp)
 New-Item -ItemType Directory -Path $backup | Out-Null
 $journal=New-Object 'System.Collections.Generic.List[object]'
 Write-Host 'Guardando o estado atual e preparando restauracao...'
 foreach($entry in $plan){
  $destination=Join-Path $root $entry.relative
  $existed=Test-Path -LiteralPath $destination -PathType Leaf
  $record=[pscustomobject]@{relative=$entry.relative;existed=$existed;savedRelative=$null;originalSha256=$null;foundSha256=$null}
  if($existed){
   $foundHash=Get-Sha $destination
   $found=Join-Path $backup ('estado_encontrado\'+$entry.relative)
   New-Item -ItemType Directory -Force -Path (Split-Path -Parent $found) | Out-Null
   Copy-Item -LiteralPath $destination -Destination $found
   Assert-File $found $foundHash
   $record.foundSha256=$foundHash
   $record.savedRelative='estado_encontrado\'+$entry.relative
   $record.originalSha256=$foundHash
   # Repair the already-manually-copied TESTE_1 files when restoring.
   $baseline=Join-Path $kit ('originais\'+$entry.relative)
   if(Test-Path -LiteralPath $baseline -PathType Leaf){
    $savedRelative='restauracao_original\'+$entry.relative
    $saved=Join-Path $backup $savedRelative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $saved) | Out-Null
    Copy-Item -LiteralPath $baseline -Destination $saved
    $baselineHash=Get-Sha $baseline;Assert-File $saved $baselineHash
    $record.savedRelative=$savedRelative;$record.originalSha256=$baselineHash
   }
  }
  $journal.Add($record)
 }
 $journal.Add([pscustomobject]@{relative='Milenium_Capas_TESTE_2_instalado.json';existed=$false;savedRelative=$null;originalSha256=$null;foundSha256=$null})
 $journal | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $backup 'restaurar.json') -Encoding UTF8
 [pscustomobject]@{cliente=$root;kit=$kitId} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $backup 'destino.json') -Encoding UTF8
 Copy-Item -LiteralPath (Join-Path $kit 'suporte\Restaurar.ps1') -Destination $backup
 Copy-Item -LiteralPath (Join-Path $kit 'suporte\RESTAURAR.bat') -Destination $backup
 Write-Host 'Instalando o motor e os recursos de capas...'
 try{
  foreach($entry in $plan){
   $destination=Join-Path $root $entry.relative
   if($null -eq $entry.source){Remove-Item -LiteralPath $destination;continue}
   New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
   Copy-Item -LiteralPath $entry.source -Destination $destination -Force
   Assert-File $destination $entry.sha256
  }
  [pscustomobject]@{kit=$kitId;backup=$backup;data=(Get-Date).ToString('o')} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $root 'Milenium_Capas_TESTE_2_instalado.json') -Encoding UTF8
 }catch{
  $installError=$_
  Write-Host 'A instalacao falhou. Restaurando os arquivos...'
  & (Join-Path $backup 'Restaurar.ps1') -SemPausa
  throw $installError
 }
 Write-Host ''
 Write-Host 'CLIENTE PREPARADO PARA TESTE.' -ForegroundColor Green
 Write-Host "Backup e restauracao: $backup"
 Write-Host 'As montarias e a interface nao foram substituidas.'
 Write-Host 'Servidor: use o XML da pasta servidor. Se ja aplicou a classe Java do TESTE_1, mantenha-a.'
 Write-Host 'Primeiros itens para testar: 31382 (Draconic) e 31383 (Vesper).'
 Write-Host 'O funcionamento do motor/licenca ainda precisa ser confirmado dentro do jogo.'
}catch{
 Write-Host ''
 Write-Host ('NAO CONCLUIDO: '+$_.Exception.Message) -ForegroundColor Red
 Read-Host 'Pressione Enter para fechar'
 exit 1
}
Read-Host 'Pressione Enter para fechar'
