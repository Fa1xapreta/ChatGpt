param([string]$Cliente='C:\Program Files (x86)\LINEAGE ROYAL NOVO')
$ErrorActionPreference='Stop'
try{
 if(!(Test-Path -LiteralPath (Join-Path $Cliente 'system\L2.exe'))){
  Add-Type -AssemblyName System.Windows.Forms;$d=New-Object System.Windows.Forms.FolderBrowserDialog;$d.Description='Escolha a pasta do cliente em que esta testando a aura.'
  if($d.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK){exit 0};$Cliente=$d.SelectedPath
 }
 $root=(Resolve-Path -LiteralPath $Cliente).Path;if((Split-Path -Leaf $root) -ieq 'system'){$root=Split-Path -Parent $root}
 $lines=New-Object 'System.Collections.Generic.List[string]';$lines.Add('MILENIUM - DIAGNOSTICO AURA A1 - SOMENTE LEITURA');$lines.Add('Nenhum arquivo do jogo ou memoria foi alterado.');$lines.Add('Cliente selecionado: '+$root)
 foreach($name in @('D3DDrv.dll','L2modExCore.dll','CliExt.dll','MilAuraWait.dll','CapasClassic.dll','d3d9.dll','MileniumGraphics.dll')){
  $p=Join-Path $root ('system\'+$name)
  if(Test-Path -LiteralPath $p){$lines.Add($name+': '+(Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash.ToLowerInvariant())}else{$lines.Add($name+': AUSENTE')}
 }
 $games=@(Get-Process -Name L2 -ErrorAction SilentlyContinue)
 if(!$games.Count){$lines.Add('JOGO FECHADO. Para verificar modulos, repita com o personagem visivel no jogo.')}
 foreach($game in $games){
  $lines.Add('Processo L2 PID='+$game.Id)
  try{foreach($m in $game.Modules){if(@('L2modExCore.dll','CliExt.dll','MilAuraWait.dll','CapasClassic.dll','D3DDrv.dll','d3d9.dll','MileniumGraphics.dll') -contains $m.ModuleName){$lines.Add('CARREGADO: '+$m.ModuleName+'; caminho='+$m.FileName)}}}catch{$lines.Add('Nao foi possivel enumerar os modulos: '+$_.Exception.GetType().Name)}
 }
 foreach($name in @('ContornoDourado.log','Milenium_Aura_Integracao.log','Milenium_Capas_Integracao.log')){
  foreach($dir in @('system','')){
   $p=Join-Path (Join-Path $root $dir) $name
   if(Test-Path -LiteralPath $p){$lines.Add('');$lines.Add('LOG: '+$p+'; modificado='+((Get-Item -LiteralPath $p).LastWriteTime.ToString('o')));foreach($line in Get-Content -LiteralPath $p -Tail 100){$lines.Add([string]$line)}}
  }
 }
 $result=Join-Path $PSScriptRoot 'Resultado_Aura_A1.txt';$lines | Set-Content -LiteralPath $result -Encoding UTF8
 Write-Host "Envie este arquivo se a aura nao aparecer: $result" -ForegroundColor Green
}catch{Write-Host ('NAO CONCLUIDO: '+$_.Exception.Message) -ForegroundColor Red;Read-Host 'Pressione Enter para fechar';exit 1}
Read-Host 'Pressione Enter para fechar'
