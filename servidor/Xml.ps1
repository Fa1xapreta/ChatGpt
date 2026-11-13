param([ValidateSet('Aplicar','Desfazer')][string]$Acao='Aplicar',[string]$Itens)
$ErrorActionPreference='Stop'
function Hash-File($path){return (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()}
function Hash-Bytes([byte[]]$bytes){$sha=[Security.Cryptography.SHA256]::Create();try{return ([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}}
function Load-Xml($path){$d=New-Object Xml.XmlDocument;$d.PreserveWhitespace=$true;$d.XmlResolver=$null;$d.Load($path);return ,$d}
function Item-Nodes($d){return @($d.SelectNodes("/*[local-name()='list']/*[local-name()='item']"))}
function Known-Cape($item){
 if($item.GetAttribute('type') -ine 'Armor'){return $false}
 if(@($item.ChildNodes | Where-Object {$_.NodeType -eq [Xml.XmlNodeType]::Element -and $_.LocalName -notin @('set','for')}).Count){return $false}
 $wanted=@{default_action='equip';bodypart='back';crystal_type='NONE';armor_type='NONE';weight='100';price='10000';is_enchantable='false';is_oly_restricted='true'}
 $sets=@($item.SelectNodes('set'));if($sets.Count -ne $wanted.Count){return $false}
 $seen=@{};foreach($s in $sets){$name=$s.GetAttribute('name');if(!$wanted.ContainsKey($name) -or $seen.ContainsKey($name) -or $s.GetAttribute('val') -ine $wanted[$name]){return $false};$seen[$name]=$true}
 $for=@($item.SelectNodes('for'));if($for.Count -ne 1){return $false}
 $stats=@($for[0].ChildNodes | Where-Object {$_.NodeType -eq [Xml.XmlNodeType]::Element});if($stats.Count -ne 2){return $false}
 $seen=@{};foreach($s in $stats){
  $stat=$s.GetAttribute('stat');if($s.LocalName -ne 'mul' -or $stat -notin @('maxHp','maxCp') -or $seen.ContainsKey($stat) -or $s.GetAttribute('order') -ine '0x30'){return $false}
  try{$value=[double]::Parse($s.GetAttribute('val'),[Globalization.CultureInfo]::InvariantCulture)}catch{return $false}
  if($value -ne 1.05){return $false};$seen[$stat]=$true
 }
 return $true
}
function Save-XmlBytes($doc){
 $settings=New-Object Xml.XmlWriterSettings;$settings.Encoding=New-Object Text.UTF8Encoding($false);$settings.Indent=$false;$settings.NewLineHandling=[Xml.NewLineHandling]::None
 $memory=New-Object IO.MemoryStream;$writer=[Xml.XmlWriter]::Create($memory,$settings)
 try{$doc.Save($writer);$writer.Flush();return ,$memory.ToArray()}finally{$writer.Dispose();$memory.Dispose()}
}
function Elevate-IfNeeded{
 $identity=[Security.Principal.WindowsIdentity]::GetCurrent();$principal=New-Object Security.Principal.WindowsPrincipal($identity)
 if(!$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){
  $args='-NoProfile -ExecutionPolicy Bypass -File "'+$PSCommandPath+'" -Acao '+$Acao;if($Itens){$args+=' -Itens "'+$Itens+'"'}
  $child=Start-Process powershell.exe -ArgumentList $args -Verb RunAs -PassThru -Wait;exit $child.ExitCode
 }
}
try{
 Elevate-IfNeeded
 $latest=Join-Path $PSScriptRoot 'ultimo_backup_servidor.json'
 if($Acao -eq 'Desfazer'){
  if(!(Test-Path -LiteralPath $latest)){throw 'Nenhum backup de XML deste kit foi registrado.'}
  $pointer=Get-Content -LiteralPath $latest -Raw | ConvertFrom-Json;$backup=$pointer.backup
  $records=Get-Content -LiteralPath (Join-Path $backup 'restaurar.json') -Raw | ConvertFrom-Json
  foreach($r in $records){
   if($r.existed -and (Hash-File (Join-Path $backup $r.saved)) -ne $r.before){throw 'Backup de XML alterado.'}
   if(Test-Path -LiteralPath $r.path){$hash=Hash-File $r.path;if($hash -ne $r.after -and $hash -ne $r.before){throw ('XML alterado depois da instalacao: '+$r.path)}}
  }
  foreach($r in $records){
   if($r.existed){Copy-Item -LiteralPath (Join-Path $backup $r.saved) -Destination $r.path -Force;if((Hash-File $r.path) -ne $r.before){throw 'Falha ao conferir XML restaurado.'}}
   elseif(Test-Path -LiteralPath $r.path){Remove-Item -LiteralPath $r.path}
  }
  Write-Host 'XMLS RESTAURADOS AO ESTADO ANTERIOR A ESTE KIT.' -ForegroundColor Green
 }else{
  if(!$Itens){
   Add-Type -AssemblyName System.Windows.Forms;$dialog=New-Object System.Windows.Forms.FolderBrowserDialog
   $dialog.Description='Escolha a pasta items dos XMLs de itens do gameserver.'
   if($dialog.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK){exit 0};$Itens=$dialog.SelectedPath
  }
  $root=(Resolve-Path -LiteralPath $Itens).Path
  if((Split-Path -Leaf $root) -ine 'items'){throw 'Escolha a pasta chamada items onde estao os XMLs de itens do servidor.'}
  if($PSScriptRoot.StartsWith($root.TrimEnd('\')+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Extraia o kit fora da pasta items antes de executar este botao.'}
  $source=Join-Path $PSScriptRoot 'itens\Milenium_Capas_70.xml';$newDoc=Load-Xml $source;$newItems=Item-Nodes $newDoc
  if($newItems.Count -ne 70){throw 'XML do kit diferente do esperado.'}
  $ids=@{};foreach($item in $newItems){$id=$item.GetAttribute('id');if($ids.ContainsKey($id) -or !(Known-Cape $item)){throw 'XML do kit invalido ou com IDs repetidos.'};$ids[$id]=$true}
  $plan=@{}
  foreach($file in Get-ChildItem -LiteralPath $root -Filter '*.xml' -File -Recurse){
   $doc=Load-Xml $file.FullName;$all=Item-Nodes $doc;$hits=@($all | Where-Object {$ids.ContainsKey($_.GetAttribute('id'))})
   if(!$hits.Count){continue}
   foreach($hit in $hits){if(!(Known-Cape $hit)){throw ('ID '+$hit.GetAttribute('id')+' ja existe com definicao diferente em '+$file.FullName+'. Nenhum XML foi modificado.')}}
   $untouched=@($all | Where-Object {!$ids.ContainsKey($_.GetAttribute('id'))} | ForEach-Object {$_.OuterXml})
   foreach($hit in $hits){$null=$hit.ParentNode.RemoveChild($hit)}
   $remaining=Item-Nodes $doc;$same=@($remaining | ForEach-Object {$_.OuterXml})
   if($same.Count -ne $untouched.Count -or ($same.Count -gt 0 -and (Compare-Object $same $untouched))){throw 'Falha ao preservar as definicoes nao relacionadas as capas.'}
   $bytes=$null;if($remaining.Count){$bytes=Save-XmlBytes $doc}
   $plan[$file.FullName]=[pscustomobject]@{path=$file.FullName;bytes=$bytes;before=(Hash-File $file.FullName)}
  }
  $target=Join-Path $root 'Milenium_Capas_70.xml';$bytes=[IO.File]::ReadAllBytes($source);$wanted=Hash-Bytes $bytes
  $targetBefore=$null;if(Test-Path -LiteralPath $target){$targetBefore=Hash-File $target;foreach($node in (Item-Nodes (Load-Xml $target))){if(!$ids.ContainsKey($node.GetAttribute('id'))){throw 'O XML de destino contem outros itens. Nenhum XML foi modificado.'}}}
  if($targetBefore -eq $wanted){$plan.Remove($target)}else{$plan[$target]=[pscustomobject]@{path=$target;bytes=$bytes;before=$targetBefore}}
  if(!$plan.Count){Write-Host 'OS 70 ITENS JA ESTAO ORGANIZADOS, SEM DUPLICATAS ENCONTRADAS.' -ForegroundColor Green;Read-Host 'Pressione Enter para fechar';exit 0}
  $backup=Join-Path $PSScriptRoot ('backup_xml_'+(Get-Date -Format 'yyyyMMdd_HHmmss_fff'));New-Item -ItemType Directory -Path $backup | Out-Null
  $records=New-Object 'System.Collections.Generic.List[object]';$index=0
  foreach($change in $plan.Values){
   $index++;$exists=Test-Path -LiteralPath $change.path;$saved=$null
   if($exists){$saved=([string]$index)+'.xml.bak';Copy-Item -LiteralPath $change.path -Destination (Join-Path $backup $saved);if((Hash-File (Join-Path $backup $saved)) -ne $change.before){throw 'Falha ao conferir backup de XML.'}}
   $after=$null;if($null -ne $change.bytes){$after=Hash-Bytes $change.bytes}
   $records.Add([pscustomobject]@{path=$change.path;existed=$exists;before=$change.before;after=$after;saved=$saved})
  }
  $records | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $backup 'restaurar.json') -Encoding UTF8
  [pscustomobject]@{backup=$backup} | ConvertTo-Json | Set-Content -LiteralPath $latest -Encoding UTF8
  try{
   foreach($change in $plan.Values){if($null -eq $change.bytes){Remove-Item -LiteralPath $change.path}else{[IO.File]::WriteAllBytes($change.path,$change.bytes)}}
   $counts=@{};foreach($file in Get-ChildItem -LiteralPath $root -Filter '*.xml' -File -Recurse){$d=Load-Xml $file.FullName;foreach($item in (Item-Nodes $d)){$id=$item.GetAttribute('id');if($ids.ContainsKey($id)){if(!$counts.ContainsKey($id)){$counts[$id]=0};$counts[$id]++}}}
   foreach($id in $ids.Keys){if($counts[$id] -ne 1){throw ('Conferencia de ID duplicado/ausente falhou: '+$id)}}
  }catch{
   foreach($r in $records){if($r.existed){Copy-Item -LiteralPath (Join-Path $backup $r.saved) -Destination $r.path -Force}elseif(Test-Path -LiteralPath $r.path){Remove-Item -LiteralPath $r.path}}
   throw
  }
  Write-Host 'XML ORGANIZADO: 70 ITENS, SEM DUPLICATAS DOS IDS DO KIT.' -ForegroundColor Green
  Write-Host '44 possuem malhas para 14 corpos; 26 sao de corpos especificos, conforme o catalogo.'
  Write-Host "Backup: $backup"
  Write-Host 'Reinicie seu gameserver pelo procedimento habitual para carregar os itens. O script nao reinicia o servidor.'
 }
}catch{Write-Host ('NAO CONCLUIDO: '+$_.Exception.Message) -ForegroundColor Red;Read-Host 'Pressione Enter para fechar';exit 1}
Read-Host 'Pressione Enter para fechar'
