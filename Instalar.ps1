param([string]$Cliente)
$ErrorActionPreference='Stop'
$kit=$PSScriptRoot
$kitId='Milenium_Capas_Integrado_70_TESTE5'
function Get-Sha([string]$Path){return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()}
function Assert-File([string]$Path,[string]$Sha){if(!(Test-Path -LiteralPath $Path -PathType Leaf)){throw "Arquivo ausente: $Path"};if((Get-Sha $Path) -ne $Sha){throw "Arquivo incompleto ou diferente: $Path"}}
function Elevate-IfNeeded {
 $identity=[Security.Principal.WindowsIdentity]::GetCurrent();$principal=New-Object Security.Principal.WindowsPrincipal($identity)
 if(!$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){
  $arguments='-NoProfile -ExecutionPolicy Bypass -File "'+$PSCommandPath+'"';if($Cliente){$arguments+=' -Cliente "'+$Cliente+'"'}
  $child=Start-Process -FilePath powershell.exe -ArgumentList $arguments -Verb RunAs -PassThru -Wait;exit $child.ExitCode
 }
}
function Apply-Alias($Path,$Patch){
 Assert-File $Path $Patch.before
 $header=[IO.File]::ReadAllBytes((Join-Path $kit ('alias-patches\'+$Patch.header_file)))
 $append=[IO.File]::ReadAllBytes((Join-Path $kit ('alias-patches\'+$Patch.append_file)))
 if($header.Length -ne 64){throw 'Cabecalho de alias invalido.'}
 $stream=[IO.File]::Open($Path,[IO.FileMode]::Open,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
 try{
  if($stream.Length -ne $Patch.original_bytes){throw 'Comprimento inesperado no pacote de origem.'}
  $stream.Position=0;$stream.Write($header,0,$header.Length)
  $stream.Position=$Patch.original_bytes;$stream.Write($append,0,$append.Length);$stream.Flush()
 }finally{$stream.Dispose()}
 Assert-File $Path $Patch.after
}
try{
 Elevate-IfNeeded
 if(Get-Process -Name L2 -ErrorAction SilentlyContinue){throw 'Feche o jogo antes de instalar.'}
 if(!$Cliente){
  $known='C:\Program Files (x86)\LINEAGE ROYAL NOVO'
  if(Test-Path -LiteralPath (Join-Path $known 'system\L2.exe')){$Cliente=$known}else{
   Add-Type -AssemblyName System.Windows.Forms;$dialog=New-Object System.Windows.Forms.FolderBrowserDialog
   $dialog.Description='Escolha a pasta do jogo, que contem system.'
   if($dialog.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK){exit 0};$Cliente=$dialog.SelectedPath
  }
 }
 $root=(Resolve-Path -LiteralPath $Cliente).Path;if((Split-Path -Leaf $root) -ieq 'system'){$root=Split-Path -Parent $root}
 if(!(Test-Path -LiteralPath (Join-Path $root 'system\L2.exe'))){throw 'Pasta do cliente incorreta.'}
 if($kit.StartsWith($root.TrimEnd('\')+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Extraia o kit fora da pasta do jogo, por exemplo na Area de Trabalho.'}
 if(Test-Path -LiteralPath (Join-Path $root 'Milenium_Capas_Integrado_instalado.json')){throw 'Este kit ja foi instalado. Para repetir, use primeiro a restauracao do backup desta versao.'}
 Write-Host "Cliente: $root"
 Write-Host 'Conferindo os arquivos do kit completo...'
 $manifest=Get-Content -LiteralPath (Join-Path $kit 'arquivos_sha256.json') -Raw | ConvertFrom-Json
 $n=0;foreach($entry in $manifest){$n++;Write-Progress -Activity 'Conferindo o kit' -Status $entry.file -PercentComplete ([int](100*$n/@($manifest).Count));Assert-File (Join-Path $kit $entry.file) $entry.sha256};Write-Progress -Activity 'Conferindo o kit' -Completed
 $accepted=Get-Content -LiteralPath (Join-Path $kit 'cliente_aceito.json') -Raw | ConvertFrom-Json
 foreach($property in $accepted.PSObject.Properties){
  $path=Join-Path $root ('system\'+$property.Name)
  if(!(Test-Path -LiteralPath $path -PathType Leaf)){throw "Falta no cliente: $path"}
  if(@($property.Value) -notcontains (Get-Sha $path)){throw "Versao de base diferente da analisada: $path. Nenhuma substituicao foi feita."}
 }
 $patches=Get-Content -LiteralPath (Join-Path $kit 'alias-patches\patches.json') -Raw | ConvertFrom-Json
 $patchByFile=@{};foreach($patch in $patches){$patchByFile[$patch.file]=$patch}
 $plan=New-Object 'System.Collections.Generic.List[object]'
 foreach($entry in $manifest | Where-Object {$_.file.StartsWith('cliente/')} ){
  $relative=$entry.file.Substring(8).Replace('/','\');$path=Join-Path $root $relative
  $filename=Split-Path -Leaf $relative
  if($patchByFile.ContainsKey($filename)){continue}
  # Keep the actual brightness/color preset and already-approved mount tuning.
  if(($filename -ieq 'MileniumFX.ini' -or $filename -ieq 'CapasClassic.ini') -and (Test-Path -LiteralPath $path)){continue}
  if((Test-Path -LiteralPath $path -PathType Leaf) -and (Get-Sha $path) -eq $entry.sha256){continue}
  $plan.Add([pscustomobject]@{relative=$relative;kind='Copy';source=(Join-Path $kit $entry.file);after=$entry.sha256;patch=$null})
 }
 foreach($patch in $patches){
  $relative='system\'+$patch.file;$path=Join-Path $root $relative
  $exists=Test-Path -LiteralPath $path -PathType Leaf;$hash=$null;if($exists){$hash=Get-Sha $path}
  if($hash -eq $patch.after){continue}
  $kind='CopyAlias';if($hash -eq $patch.before){$kind='Alias'}
  $plan.Add([pscustomobject]@{relative=$relative;kind=$kind;source=(Join-Path $kit ('cliente\system\'+$patch.file));after=$patch.after;patch=$patch})
 }
 foreach($relative in @('system\dsetup_base.dll','system\essence_capes.bin','system\essence_cloth.bin','system\essence_fx.bin','Animations\DynastyCloak.ukx')){
  if(Test-Path -LiteralPath (Join-Path $root $relative)){$plan.Add([pscustomobject]@{relative=$relative;kind='Remove';source=$null;after=$null;patch=$null})}
 }
 $stamp=Get-Date -Format 'yyyyMMdd_HHmmss_fff';$backup=Join-Path $root ('Milenium_Integrado_Backup_'+$stamp)
 New-Item -ItemType Directory -Path $backup | Out-Null
 $journal=New-Object 'System.Collections.Generic.List[object]'
 Write-Host 'Guardando somente os arquivos que precisam mudar...'
 foreach($entry in $plan){
  $path=Join-Path $root $entry.relative;$exists=Test-Path -LiteralPath $path -PathType Leaf
  $record=[pscustomobject]@{relative=$entry.relative;existed=$exists;before=$null;after=$entry.after;mode='Full';saved=$null;originalLength=0;headerSha256=$null}
  if($exists){
   $record.before=Get-Sha $path
   if($entry.kind -eq 'Alias'){
    $record.mode='Header';$record.originalLength=(Get-Item -LiteralPath $path).Length
    $record.saved='cabecalhos\'+$entry.patch.file+'.bin';$saved=Join-Path $backup $record.saved
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $saved) | Out-Null
    $stream=[IO.File]::OpenRead($path);$header=New-Object byte[] 64
    try{if($stream.Read($header,0,64) -ne 64){throw 'Falha ao ler cabecalho de backup.'}}finally{$stream.Dispose()}
    [IO.File]::WriteAllBytes($saved,$header);$record.headerSha256=Get-Sha $saved
   }else{
    $record.saved='arquivos\'+$entry.relative;$saved=Join-Path $backup $record.saved
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $saved) | Out-Null
    Copy-Item -LiteralPath $path -Destination $saved;Assert-File $saved $record.before
   }
  }
  $journal.Add($record)
 }
 $journal.Add([pscustomobject]@{relative='Milenium_Capas_Integrado_instalado.json';existed=$false;before=$null;after=$null;mode='Marker';saved=$null;originalLength=0;headerSha256=$null})
 $journal | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $backup 'restaurar.json') -Encoding UTF8
 [pscustomobject]@{cliente=$root;kit=$kitId;fallbackD3d9=(Get-Sha (Join-Path $kit 'suporte\d3d9_modo_capas.dll'));fallbackCliExt=(Get-Sha (Join-Path $kit 'suporte\CliExt_modo_capas.dll'))} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $backup 'destino.json') -Encoding UTF8
 foreach($name in @('Restaurar.ps1','RESTAURAR.bat','MODO_COMPATIVEL.bat','ModoCompativel.ps1','d3d9_modo_capas.dll','CliExt_modo_capas.dll')){Copy-Item -LiteralPath (Join-Path $kit ('suporte\'+$name)) -Destination $backup}
 try{
  $n=0
  foreach($entry in $plan){
   $n++;Write-Progress -Activity 'Instalando integracao' -Status $entry.relative -PercentComplete ([int](100*$n/$plan.Count))
   $path=Join-Path $root $entry.relative
   if($entry.kind -eq 'Remove'){Remove-Item -LiteralPath $path;continue}
   New-Item -ItemType Directory -Force -Path (Split-Path -Parent $path) | Out-Null
   if($entry.kind -eq 'Copy' -or $entry.kind -eq 'CopyAlias'){Copy-Item -LiteralPath $entry.source -Destination $path -Force}
   if($entry.kind -eq 'Alias' -or $entry.kind -eq 'CopyAlias'){Apply-Alias $path $entry.patch}
   Assert-File $path $entry.after
  }
  Write-Progress -Activity 'Instalando integracao' -Completed
  [pscustomobject]@{kit=$kitId;backup=$backup;data=(Get-Date).ToString('o');catalogo=70;graficos='Integrado'} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $root 'Milenium_Capas_Integrado_instalado.json') -Encoding UTF8
 }catch{
  $failure=$_;Write-Host 'Falha de instalacao. Restaurando o estado encontrado...'
  & (Join-Path $backup 'Restaurar.ps1') -SemPausa -FalhaInstalacao
  throw $failure
 }
 Write-Host ''
 Write-Host 'CLIENTE INTEGRADO PREPARADO PARA TESTE.' -ForegroundColor Green
 Write-Host 'Catalogo: 70 itens; 44 universais e 26 de corpos especificos. Veja CATALOGO_CAPAS.csv.'
 Write-Host 'MileniumFX original ligado por uma ponte nova com as capas. Integracao ainda precisa ser confirmada no jogo.'
 Write-Host "Backup e modo compativel: $backup"
 Write-Host 'Servidor: execute INSTALAR_XML.bat para trocar o XML antigo de 25 pelo catalogo de 70.'
 Write-Host 'Java: se ja aplicou o gerenciador do kit anterior, ele permanece igual.'
}catch{Write-Host ('NAO CONCLUIDO: '+$_.Exception.Message) -ForegroundColor Red;Read-Host 'Pressione Enter para fechar';exit 1}
Read-Host 'Pressione Enter para fechar'
