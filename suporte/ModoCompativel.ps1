$ErrorActionPreference='Stop'
try{
 $identity=[Security.Principal.WindowsIdentity]::GetCurrent();$principal=New-Object Security.Principal.WindowsPrincipal($identity)
 if(!$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){
  $child=Start-Process powershell.exe -ArgumentList ('-NoProfile -ExecutionPolicy Bypass -File "'+$PSCommandPath+'"') -Verb RunAs -PassThru -Wait;exit $child.ExitCode
 }
 if(Get-Process -Name L2 -ErrorAction SilentlyContinue){throw 'Feche o jogo antes de mudar o modo grafico.'}
 $config=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'destino.json') -Raw | ConvertFrom-Json
 $root=(Resolve-Path -LiteralPath $config.cliente).Path
 $journalPath=Join-Path $PSScriptRoot 'restaurar.json';$journal=Get-Content -LiteralPath $journalPath -Raw | ConvertFrom-Json
 $entries=@(@{name='d3d9.dll';source='d3d9_modo_capas.dll';expected=$config.fallbackD3d9},@{name='CliExt.dll';source='CliExt_modo_capas.dll';expected=$config.fallbackCliExt})
 foreach($entry in $entries){
  $relative='system\'+$entry.name;$path=Join-Path $root $relative;$r=@($journal | Where-Object {$_.relative -eq $relative})
  if($r.Count -ne 1){throw 'Journal deste componente nao localizado. Use a restauracao completa.'}
  $hash=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
  $fallback=(Get-FileHash -LiteralPath (Join-Path $PSScriptRoot $entry.source) -Algorithm SHA256).Hash.ToLowerInvariant()
  if($fallback -ne $entry.expected){throw 'Recurso do modo compativel alterado.'}
  if($hash -ne $r[0].after -and $hash -ne $fallback){throw ('Arquivo grafico mudou depois da instalacao: '+$entry.name)}
 }
 $previous=@{};foreach($entry in $entries){$previous[$entry.name]=[IO.File]::ReadAllBytes((Join-Path $root ('system\'+$entry.name)))}
 $journalBytes=[IO.File]::ReadAllBytes($journalPath)
 try{
 foreach($entry in $entries){
  $relative='system\'+$entry.name;$path=Join-Path $root $relative
  Copy-Item -LiteralPath (Join-Path $PSScriptRoot $entry.source) -Destination $path -Force
  $hash=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
  $r=@($journal | Where-Object {$_.relative -eq $relative});$r[0].after=$hash
 }
 $journal | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $journalPath -Encoding UTF8
 }catch{foreach($entry in $entries){[IO.File]::WriteAllBytes((Join-Path $root ('system\'+$entry.name)),$previous[$entry.name])};[IO.File]::WriteAllBytes($journalPath,$journalBytes);throw}
 Write-Host 'MODO COMPATIVEL APLICADO. As capas e os DATs permanecem; a ponte grafica nova foi retirada.' -ForegroundColor Green
 Write-Host 'Cores/nitidez do MileniumFX podem voltar a ficar ausentes neste modo. Ele serve para recuperar a abertura do jogo se a integracao nova falhar.'
}catch{Write-Host ('NAO CONCLUIDO: '+$_.Exception.Message) -ForegroundColor Red;Read-Host 'Pressione Enter para fechar';exit 1}
Read-Host 'Pressione Enter para fechar'
