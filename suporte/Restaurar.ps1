param([switch]$SemPausa,[switch]$FalhaInstalacao)
$ErrorActionPreference='Stop'
try{
 $identity=[Security.Principal.WindowsIdentity]::GetCurrent();$principal=New-Object Security.Principal.WindowsPrincipal($identity)
 if(!$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){
  $arguments='-NoProfile -ExecutionPolicy Bypass -File "'+$PSCommandPath+'"';if($SemPausa){$arguments+=' -SemPausa'};if($FalhaInstalacao){$arguments+=' -FalhaInstalacao'}
  $child=Start-Process -FilePath powershell.exe -ArgumentList $arguments -Verb RunAs -Wait -PassThru;exit $child.ExitCode
 }
 if(Get-Process -Name L2 -ErrorAction SilentlyContinue){throw 'Feche o jogo antes de restaurar.'}
 $config=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'destino.json') -Raw | ConvertFrom-Json
 $root=(Resolve-Path -LiteralPath $config.cliente).Path
 $records=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'restaurar.json') -Raw | ConvertFrom-Json
 foreach($r in $records){
  $path=Join-Path $root $r.relative
  if($r.existed){
   $saved=Join-Path $PSScriptRoot $r.saved
   if(!(Test-Path -LiteralPath $saved -PathType Leaf)){throw "Backup ausente: $saved"}
   if($r.mode -eq 'Full' -and (Get-FileHash -LiteralPath $saved -Algorithm SHA256).Hash.ToLowerInvariant() -ne $r.before){throw "Backup alterado: $saved"}
   if($r.mode -eq 'Header'){
    if((Get-Item -LiteralPath $saved).Length -ne 64 -or (Get-FileHash -LiteralPath $saved -Algorithm SHA256).Hash.ToLowerInvariant() -ne $r.headerSha256){throw 'Cabecalho de backup invalido.'}
    if(!(Test-Path -LiteralPath $path)){throw ('O pacote que usa backup de cabecalho esta ausente: '+$path)}
   }
  }
  if(!$FalhaInstalacao -and $r.mode -ne 'Marker' -and (Test-Path -LiteralPath $path)){
   $hash=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
   if($hash -ne $r.after -and $hash -ne $r.before){throw "Arquivo alterado apos instalacao: $path. Restauracao interrompida antes de modificar o cliente."}
  }
 }
 foreach($r in $records){
  $path=Join-Path $root $r.relative
  if($r.existed){
   if($r.mode -eq 'Header'){
    $header=[IO.File]::ReadAllBytes((Join-Path $PSScriptRoot $r.saved))
    $stream=[IO.File]::Open($path,[IO.FileMode]::Open,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
    try{$stream.Position=0;$stream.Write($header,0,64);$stream.SetLength([long]$r.originalLength);$stream.Flush()}finally{$stream.Dispose()}
   }else{
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $path) | Out-Null
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot $r.saved) -Destination $path -Force
   }
   if((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant() -ne $r.before){throw "Falha ao conferir restauracao: $path"}
  }elseif(Test-Path -LiteralPath $path){Remove-Item -LiteralPath $path}
 }
 Write-Host 'CLIENTE RESTAURADO AO ESTADO ANTERIOR A ESTE KIT.' -ForegroundColor Green
 Write-Host 'O XML do servidor tem restauracao separada.'
}catch{
 Write-Host ('RESTAURACAO NAO CONCLUIDA: '+$_.Exception.Message) -ForegroundColor Red
 if(!$SemPausa){Read-Host 'Pressione Enter para fechar'}
 if($SemPausa){throw};exit 1
}
if(!$SemPausa){Read-Host 'Pressione Enter para fechar'}
