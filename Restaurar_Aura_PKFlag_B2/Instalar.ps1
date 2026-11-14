param([string]$Cliente)
$ErrorActionPreference='Stop'
function Sha($p){return (Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash.ToLowerInvariant()}
try{
 $identity=[Security.Principal.WindowsIdentity]::GetCurrent();$principal=New-Object Security.Principal.WindowsPrincipal($identity)
 if(!$principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){
  $arguments='-NoProfile -ExecutionPolicy Bypass -File "'+$PSCommandPath+'"';if($Cliente){$arguments+=' -Cliente "'+$Cliente+'"'}
  $child=Start-Process powershell.exe -ArgumentList $arguments -Verb RunAs -PassThru -Wait;exit $child.ExitCode
 }
 if(Get-Process -Name L2 -ErrorAction SilentlyContinue){throw 'Feche totalmente o jogo antes de instalar.'}
 if(!$Cliente){$Cliente='C:\Program Files (x86)\LINEAGE ROYAL NOVO';if(!(Test-Path -LiteralPath (Join-Path $Cliente 'system\L2.exe'))){Add-Type -AssemblyName System.Windows.Forms;$d=New-Object System.Windows.Forms.FolderBrowserDialog;$d.Description='Escolha a pasta do jogo que contem system.';if($d.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK){exit 0};$Cliente=$d.SelectedPath}}
 $root=(Resolve-Path -LiteralPath $Cliente).Path;if((Split-Path -Leaf $root) -ieq 'system'){$root=Split-Path -Parent $root}
 if(!(Test-Path -LiteralPath (Join-Path $root 'system\L2.exe'))){throw 'Pasta do cliente incorreta.'}
 $specPath=Join-Path $PSScriptRoot 'arquivos.json';if((Sha $specPath) -ne '19f19861e0218a0415b4540d52861125aadec9747173ad28d2494ad50e63249a'){throw 'Descritor do pacote alterado.'};$spec=Get-Content -LiteralPath $specPath -Raw | ConvertFrom-Json
 foreach($p in $spec.required.PSObject.Properties){$path=Join-Path $root ('system\'+$p.Name);if(!(Test-Path -LiteralPath $path) -or (Sha $path) -ne $p.Value){throw ('Componente diferente do analisado: '+$p.Name+'. Nenhuma troca foi feita.')}}
 $plan=New-Object 'System.Collections.Generic.List[object]'
 foreach($f in $spec.files){
  $payload=Join-Path $PSScriptRoot ('payload\'+$f.file);if((Sha $payload) -ne $f.sha256){throw ('Payload incompleto ou alterado: '+$f.file)}
  $path=Join-Path $root ('system\'+$f.file);$existed=Test-Path -LiteralPath $path;$before=$null;if($existed){$before=Sha $path;if(@($f.accepted) -notcontains $before){throw ('Versao diferente da analisada: '+$f.file)}}elseif(!$f.optional){throw ('Arquivo ausente: '+$f.file)}
  if($before -eq $f.sha256){continue}
  $plan.Add([pscustomobject]@{file=$f.file;existed=$existed;before=$before;after=$f.sha256;source=$payload})
 }
 if(!$plan.Count){Write-Host 'PONTE B2 E DRIVER ORIGINAL JA ESTAO INSTALADOS.' -ForegroundColor Green;Read-Host 'Pressione Enter para fechar';exit 0}
 $backup=Join-Path $root ('Milenium_Aura_Backup_B2_'+(Get-Date -Format 'yyyyMMdd_HHmmss_fff'));New-Item -ItemType Directory -Path $backup | Out-Null
 $records=New-Object 'System.Collections.Generic.List[object]'
 foreach($f in $plan){if($f.existed){Copy-Item -LiteralPath (Join-Path $root ('system\'+$f.file)) -Destination (Join-Path $backup $f.file);if((Sha (Join-Path $backup $f.file)) -ne $f.before){throw 'Falha ao conferir backup.'}};$records.Add([pscustomobject]@{file=$f.file;existed=$f.existed;before=$f.before;after=$f.after})}
 $records | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $backup 'restaurar.json') -Encoding UTF8
 [pscustomobject]@{cliente=$root} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $backup 'destino.json') -Encoding UTF8
 foreach($n in @('Restaurar.ps1','DESFAZER.bat')){Copy-Item -LiteralPath (Join-Path $PSScriptRoot $n) -Destination $backup}
 try{foreach($f in $plan){$path=Join-Path $root ('system\'+$f.file);Copy-Item -LiteralPath $f.source -Destination $path -Force;if((Sha $path) -ne $f.after){throw 'Falha ao conferir arquivo instalado.'}}}catch{$failure=$_;& (Join-Path $backup 'Restaurar.ps1') -SemPausa -FalhaInstalacao;throw $failure}
 Write-Host 'PONTE DA AURA PK/FLAG B2 INSTALADA.' -ForegroundColor Green
 Write-Host 'Abra o jogo, entre no personagem e teste ficando flag ou PK, como o efeito original.'
 Write-Host 'O Fighter.ukx, os ajustes de corrida e as capas nao foram alterados.'
 Write-Host "Backup e DESFAZER.bat: $backup"
 Write-Host 'Se nao aparecer, use VERIFICAR.bat com o jogo aberto e envie Resultado_Aura_B2.txt.'
}catch{Write-Host ('NAO CONCLUIDO: '+$_.Exception.Message) -ForegroundColor Red;Read-Host 'Pressione Enter para fechar';exit 1}
Read-Host 'Pressione Enter para fechar'
