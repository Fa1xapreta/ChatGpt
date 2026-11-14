param([string]$Cliente='C:\Program Files (x86)\LINEAGE ROYAL NOVO')
$ErrorActionPreference='Stop'
try {
 if (!(Test-Path -LiteralPath (Join-Path $Cliente 'system\interface.u'))) {
  Add-Type -AssemblyName System.Windows.Forms
  $d=New-Object System.Windows.Forms.FolderBrowserDialog
  $d.Description='Selecione a pasta raiz do cliente (ou sua pasta system)'
  if($d.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK){exit 0}
  $Cliente=$d.SelectedPath
 }
 $root=(Resolve-Path -LiteralPath $Cliente).Path
 if((Split-Path -Leaf $root) -ieq 'system'){$root=Split-Path -Parent $root}
 $lines=New-Object 'System.Collections.Generic.List[string]'
 $lines.Add('MILENIUM INVENTARIO 3D TESTE 1 - SOMENTE LEITURA')
 $lines.Add('Cliente: '+$root)
 foreach($name in @('interface.u','interface.xdat','MileniumPortraitNative.dll','L2UI_CH3.u')) {
  $p=Join-Path $root ('system\'+$name)
  if(Test-Path -LiteralPath $p){$lines.Add($name+': '+(Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash.ToLowerInvariant())}
  else{$lines.Add($name+': AUSENTE')}
 }
 foreach($dir in @('system','')) {
  $p=Join-Path (Join-Path $root $dir) 'MileniumPortrait.log'
  if(Test-Path -LiteralPath $p){$lines.Add('');$lines.Add('LOG: '+$p);foreach($line in Get-Content -LiteralPath $p -Tail 100){$lines.Add([string]$line)}}
 }
 $out=Join-Path $PSScriptRoot 'Resultado_Inventario_T1.txt'
 $lines | Set-Content -LiteralPath $out -Encoding UTF8
 Write-Host "Diagnostico salvo em: $out" -ForegroundColor Green
} catch { Write-Host ('Falha de leitura: '+$_.Exception.Message) -ForegroundColor Red }
Read-Host 'Pressione Enter para fechar'
