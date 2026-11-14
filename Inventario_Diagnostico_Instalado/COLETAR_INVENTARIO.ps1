$ErrorActionPreference = 'Stop'
$stage = Join-Path ([IO.Path]::GetTempPath()) ('MileniumInventario_' + [Guid]::NewGuid().ToString('N'))
[void](New-Item -ItemType Directory -Path $stage)
$rows = New-Object System.Collections.Generic.List[object]
try {
$system = $PSScriptRoot
$client = Split-Path -Parent $system
$wanted = @(
 @{Path=(Join-Path $system 'interface.u'); Relative='system/interface.u'},
 @{Path=(Join-Path $system 'interface.xdat'); Relative='system/interface.xdat'},
 @{Path=(Join-Path $system 'MileniumPortraitNative.dll'); Relative='system/MileniumPortraitNative.dll'}
)
foreach ($folder in @($system, (Join-Path $client 'SysTextures'))) {
 foreach ($name in @('L2Devil.utx','L2Devil.u')) {
  $full = Join-Path $folder $name
  $rel = (Split-Path -Leaf $folder) + '/' + $name
  $wanted += @{Path=$full;Relative=$rel}
 }
}
foreach ($entry in $wanted) {
 if (Test-Path -LiteralPath $entry.Path -PathType Leaf) {
  $file = Get-Item -LiteralPath $entry.Path
  $dest = Join-Path $stage $entry.Relative
  [void](New-Item -ItemType Directory -Path (Split-Path -Parent $dest) -Force)
  Copy-Item -LiteralPath $entry.Path -Destination $dest
  $hash = (Get-FileHash -LiteralPath $dest -Algorithm SHA256).Hash
  $rows.Add([PSCustomObject]@{File=$entry.Relative;Exists=$true;Bytes=$file.Length;SHA256=$hash})
  Write-Host ('Copiado: ' + $entry.Relative)
 } else {
  $rows.Add([PSCustomObject]@{File=$entry.Relative;Exists=$false})
 }
}
$rows | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $stage 'ARQUIVOS.json') -Encoding UTF8
Add-Type -AssemblyName System.IO.Compression.FileSystem
$desktop = [Environment]::GetFolderPath('Desktop')
if (!$desktop -or !(Test-Path -LiteralPath $desktop)) { $desktop = [IO.Path]::GetTempPath() }
$output = Join-Path $desktop ('Inventario_Instalado_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '.zip')
[IO.Compression.ZipFile]::CreateFromDirectory($stage,$output)
Write-Host ''
Write-Host 'Envie este ZIP no chat:' -ForegroundColor Green
Write-Host $output -ForegroundColor Green
} catch {
 Write-Host $_.Exception.Message -ForegroundColor Red
} finally {
 # Only the uniquely named staging directory created by this script is removed.
 if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
}
[void](Read-Host 'Pressione Enter para fechar')
