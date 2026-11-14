$ErrorActionPreference = 'Stop'
$report = New-Object System.Collections.Generic.List[string]
function Report($s) { Write-Host $s; $report.Add([string]$s) }
try {
Report ('Processo 32 bits: ' + (-not [Environment]::Is64BitProcess))
$dll = Join-Path $PSScriptRoot 'MileniumPortraitNative.dll'
Report ('Arquivo: ' + $dll)
Report ('Existe: ' + (Test-Path -LiteralPath $dll))
if (!(Test-Path -LiteralPath $dll)) { throw 'Coloque os dois arquivos VERIFICAR_DLL dentro da system do jogo.' }
Report ('SHA256: ' + (Get-FileHash -LiteralPath $dll -Algorithm SHA256).Hash)
Add-Type @'
using System;
using System.Runtime.InteropServices;
public class NativeCheck {
 [DllImport("kernel32.dll", CharSet=CharSet.Unicode, SetLastError=true)] public static extern IntPtr LoadLibraryEx(string path, IntPtr reserved, uint flags);
 [DllImport("kernel32.dll", CharSet=CharSet.Ansi, ExactSpelling=true, SetLastError=true)] public static extern IntPtr GetProcAddress(IntPtr module, string name);
 [DllImport("kernel32.dll")] public static extern bool FreeLibrary(IntPtr module);
}
'@
# Maps for export inspection only. Does not execute the DLL's DllMain.
$module = [NativeCheck]::LoadLibraryEx($dll, [IntPtr]::Zero, 1)
$err = [Runtime.InteropServices.Marshal]::GetLastWin32Error()
Report ('LoadLibraryEx: ' + $module + ' / codigo Windows: ' + $err)
if ($module -eq [IntPtr]::Zero) { Report ((New-Object ComponentModel.Win32Exception($err)).Message) }
else {
$entry = [NativeCheck]::GetProcAddress($module, 'intUMobPreviewNativeexecReleasePreview')
Report ('ReleasePreview encontrada: ' + ($entry -ne [IntPtr]::Zero))
[void][NativeCheck]::FreeLibrary($module)
}
} catch { Report $_.Exception.Message }
$report | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'Resultado_DLL.txt') -Encoding UTF8
Read-Host 'Resultado_DLL.txt salvo. Pressione Enter para fechar'
