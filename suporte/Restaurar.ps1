param([switch]$SemPausa)
$ErrorActionPreference='Stop'
try{
 $identity=[Security.Principal.WindowsIdentity]::GetCurrent()
 $principal=New-Object Security.Principal.WindowsPrincipal($identity)
 if(!$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){
  $arguments='-NoProfile -ExecutionPolicy Bypass -File "'+$PSCommandPath+'"'
  if($SemPausa){$arguments+=' -SemPausa'}
  $process=Start-Process -FilePath 'powershell.exe' -ArgumentList $arguments -Verb RunAs -Wait -PassThru
  exit $process.ExitCode
 }
 if(Get-Process -Name L2 -ErrorAction SilentlyContinue){throw 'Feche o jogo antes de restaurar.'}
 $config=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'destino.json') -Raw | ConvertFrom-Json
 $root=(Resolve-Path -LiteralPath $config.cliente).Path
 $records=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'restaurar.json') -Raw | ConvertFrom-Json
 foreach($record in $records){if($record.existed){
  $source=Join-Path $PSScriptRoot $record.savedRelative
  if(!(Test-Path -LiteralPath $source -PathType Leaf)){throw "Backup ausente: $source"}
  if((Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant() -ne $record.originalSha256){throw "Backup alterado: $source"}
 }}
 foreach($record in $records){
  $destination=Join-Path $root $record.relative
  if($record.existed){
   New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
   Copy-Item -LiteralPath (Join-Path $PSScriptRoot $record.savedRelative) -Destination $destination -Force
   if((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant() -ne $record.originalSha256){throw "Falha ao restaurar: $destination"}
  }elseif(Test-Path -LiteralPath $destination){Remove-Item -LiteralPath $destination}
 }
 Write-Host 'Cliente restaurado. Os tres arquivos do TESTE_1 voltaram a versao original enviada.' -ForegroundColor Green
 Write-Host 'Servidor: a restauracao do XML/Java deve ser feita separadamente, caso necessario.'
}catch{
 Write-Host ('RESTAURACAO NAO CONCLUIDA: '+$_.Exception.Message) -ForegroundColor Red
 if(!$SemPausa){Read-Host 'Pressione Enter para fechar'}
 if($SemPausa){throw}
 exit 1
}
if(!$SemPausa){Read-Host 'Pressione Enter para fechar'}
