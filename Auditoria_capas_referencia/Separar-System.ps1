param([Parameter(Mandatory=$true)][string]$System,[string]$Destino=".\System_separada")
$ErrorActionPreference="Stop"
$System=(Resolve-Path -LiteralPath $System).Path
$rows=Import-Csv -LiteralPath (Join-Path $PSScriptRoot "inventario_completo.csv")
foreach($r in $rows) {
 $src=Join-Path $System $r.arquivo
 if(!(Test-Path -LiteralPath $src)){Write-Warning "Ausente: $($r.arquivo)";continue}
 if((Get-FileHash -LiteralPath $src -Algorithm SHA256).Hash.ToLower() -ne $r.sha256){Write-Warning "Hash diferente; nao copiado: $($r.arquivo)";continue}
 $dir=Join-Path $Destino $r.categoria
 New-Item -ItemType Directory -Force -Path $dir | Out-Null
 Copy-Item -LiteralPath $src -Destination $dir
}
Write-Host "Separacao concluida. Os originais nao foram alterados."
