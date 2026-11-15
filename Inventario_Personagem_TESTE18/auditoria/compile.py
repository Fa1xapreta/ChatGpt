from pathlib import Path
import sys,subprocess,json
R=Path(__file__).resolve().parent;sys.path.insert(0,'/workspace/work/cloak-audit/vendor');import pefile
p=pefile.PE(str(R.parent/'TESTE5/system/MileniumPortraitNative.dll'));iat={i.name.decode():i.address-p.OPTIONAL_HEADER.ImageBase for d in p.DIRECTORY_ENTRY_IMPORT for i in d.imports if i.name}
needed=['GetTickCount','GetProcAddress','GetForegroundWindow','GetWindowThreadProcessId','GetCurrentProcessId','GetCursorPos','GetAsyncKeyState','ScreenToClient','GetModuleFileNameW','CreateFileW','WriteFile','CloseHandle','_snprintf','GetCurrentThreadId','VirtualProtect','GetCurrentProcess','FlushInstructionCache']
(R/'iat.h').write_text('\n'.join('#define IAT_'+n+' '+hex(iat[n]) for n in needed)+'\n')
(R/'link.ld').write_text('SECTIONS { . = 0x10182000; .text : { *(.text*) } . = 0x10184000; .rodata : { *(.rodata*) } . = 0x10185000; .data : { *(.data*) } .bss : { *(.bss*) *(COMMON) } /DISCARD/ : { *(.comment) *(.note*) *(.eh_frame*) } }')
cmd=['gcc','-m32','-O2','-ffreestanding','-fno-pic','-fno-pie','-fno-stack-protector','-fno-asynchronous-unwind-tables','-fshort-wchar','-mno-sse','-mpreferred-stack-boundary=2','-mincoming-stack-boundary=2','-Wall','-Wextra','-c','viewer.c','-o','viewer.o']
subprocess.run(cmd,cwd=R,check=True);subprocess.run(['ld','-melf_i386','--emit-relocs','-T','link.ld','viewer.o','-o','viewer.elf'],cwd=R,check=True)
print('Compiled viewer')
