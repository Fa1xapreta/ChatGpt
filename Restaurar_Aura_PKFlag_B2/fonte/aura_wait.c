#define WIN32_LEAN_AND_MEAN
#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0600
#endif
#include <windows.h>
#include <stdint.h>
#include <stdio.h>
#include <wchar.h>
#include <string.h>
#include "binding.h"
static HMODULE g_self;
static wchar_t g_dir[MAX_PATH];
static volatile LONG g_bound_logged,g_bad_logged;
static void log_value(const char *message,uintptr_t value){
 if(!g_dir[0])return;
 wchar_t path[MAX_PATH];_snwprintf(path,MAX_PATH,L"%lsMilenium_Aura_PKFlag_B2.log",g_dir);path[MAX_PATH-1]=0;
 char text[512];snprintf(text,sizeof(text),"PID=%lu %s: 0x%08lx\r\n",(unsigned long)GetCurrentProcessId(),message,(unsigned long)value);
 HANDLE h=CreateFileW(path,FILE_APPEND_DATA,FILE_SHARE_READ|FILE_SHARE_WRITE,NULL,OPEN_ALWAYS,FILE_ATTRIBUTE_NORMAL,NULL);
 if(h!=INVALID_HANDLE_VALUE){DWORD n;WriteFile(h,text,(DWORD)strlen(text),&n,NULL);CloseHandle(h);}
}
static int cape_ready(void){
 HANDLE map=OpenFileMappingW(FILE_MAP_READ,FALSE,L"Local\\CapasClassicLive_v1");if(!map)return 0;
 const volatile DWORD *p=(const volatile DWORD*)MapViewOfFile(map,FILE_MAP_READ,0,0,256);int ready=0;
 if(p){ready=p[0]==0x564c4343 && p[1]==2 && p[2]==GetCurrentProcessId() && p[4]==3;UnmapViewOfFile((const void*)p);}CloseHandle(map);return ready;
}
static BYTE *known_cliext(void){
 BYTE *b=(BYTE*)GetModuleHandleW(L"CliExt.dll");if(!b)return NULL;
 IMAGE_DOS_HEADER *dos=(IMAGE_DOS_HEADER*)b;if(dos->e_magic!=IMAGE_DOS_SIGNATURE || dos->e_lfanew<0x40 || dos->e_lfanew>0x1000)return NULL;
 IMAGE_NT_HEADERS32 *nt=(IMAGE_NT_HEADERS32*)(b+dos->e_lfanew);
 if(nt->Signature!=IMAGE_NT_SIGNATURE || nt->FileHeader.Machine!=IMAGE_FILE_MACHINE_I386 || nt->OptionalHeader.Magic!=IMAGE_NT_OPTIONAL_HDR32_MAGIC || nt->OptionalHeader.SizeOfImage<0x8000)return NULL;
 static const BYTE prefix[]={0x55,0x53,0x57,0x56,0x83,0xec,0x08};
 if(memcmp(b+0x1162,prefix,sizeof(prefix)) || b[0x1174]!=0xa3 || *(DWORD*)(b+0x1175)!=(DWORD)(uintptr_t)(b+0x4044))return NULL;
 if(b[0x13f3]!=0x8b || b[0x13f4]!=0x35 || *(DWORD*)(b+0x13f5)!=(DWORD)(uintptr_t)(b+0x4044))return NULL;
 MEMORY_BASIC_INFORMATION mbi;if(!VirtualQuery(b+0x4044,&mbi,sizeof(mbi)) || mbi.State!=MEM_COMMIT || (mbi.Protect&PAGE_GUARD))return NULL;
 DWORD prot=mbi.Protect&0xff;if(prot!=PAGE_READWRITE && prot!=PAGE_EXECUTE_READWRITE && prot!=PAGE_WRITECOPY && prot!=PAGE_EXECUTE_WRITECOPY)return NULL;return b;
}
static int bind_core(HMODULE core){
 BYTE *b=known_cliext();if(!b){if(!InterlockedCompareExchange(&g_bad_logged,1,0))log_value("CliExt fora do perfil; sem publicar pronto",0);return 0;}
 PVOID volatile *slot=(PVOID volatile*)(b+0x4044);uintptr_t current=(uintptr_t)InterlockedCompareExchangePointer(slot,NULL,NULL);
 if(binding_replace_allowed(current,(uintptr_t)g_self,(uintptr_t)core)){InterlockedCompareExchangePointer(slot,(PVOID)core,(PVOID)g_self);current=(uintptr_t)InterlockedCompareExchangePointer(slot,NULL,NULL);}
 if(!binding_ready(current,(uintptr_t)g_self,(uintptr_t)core))return 0;
 if(!InterlockedCompareExchange(&g_bound_logged,1,0))log_value("CliExt aponta para L2modExCore REAL; identificador corrigido",current);return 1;
}
__declspec(dllexport) int __cdecl ContornoDourado_Status(void){
 HMODULE c=GetModuleHandleW(L"L2modExCore.dll");if(!c || !bind_core(c))return 0;
 typedef int (__cdecl *Fn)(void);Fn f=(Fn)GetProcAddress(c,"ContornoDourado_Status");return f?f():0;
}
__declspec(dllexport) uintptr_t __cdecl ContornoDourado_Versao(void){
 HMODULE c=GetModuleHandleW(L"L2modExCore.dll");if(!c)return 0;
 typedef uintptr_t (__cdecl *Fn)(void);Fn f=(Fn)GetProcAddress(c,"ContornoDourado_Versao");return f?f():0;
}
static DWORD WINAPI worker(void *unused){
 (void)unused;DWORD n=GetModuleFileNameW(g_self,g_dir,MAX_PATH);if(!n || n>=MAX_PATH)return 0;wchar_t *slash=wcsrchr(g_dir,L'\\');if(!slash)return 0;slash[1]=0;
 HMODULE pinned=NULL;if(!GetModuleHandleExW(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS|GET_MODULE_HANDLE_EX_FLAG_PIN,(LPCWSTR)(uintptr_t)&worker,&pinned)){log_value("Falha ao fixar carregador",GetLastError());return 0;}
 log_value("Ponte PK/Flag B2 iniciada",2);DWORD ticks=0;while(ticks<3000 && !cape_ready()){Sleep(100);ticks++;}log_value("Espera por CapasClassic em ms",ticks*100);
 wchar_t path[MAX_PATH];_snwprintf(path,MAX_PATH,L"%lsL2modExCore.dll",g_dir);path[MAX_PATH-1]=0;HMODULE core=GetModuleHandleW(L"L2modExCore.dll");if(!core)core=LoadLibraryW(path);
 if(!core){log_value("Falha ao carregar L2modExCore original",GetLastError());return 0;}log_value("L2modExCore original carregado",(uintptr_t)core);
 DWORD previous[8];memset(previous,0xff,sizeof(previous));
 for(;;){
  int bound=bind_core(core);BYTE *b=known_cliext();typedef int (__cdecl *Fn)(void);Fn status=(Fn)GetProcAddress(core,"ContornoDourado_Status");DWORD now[8]={(DWORD)bound,status?(DWORD)status():0,0,0,0,0,0,0};
  if(b){now[2]=*(volatile DWORD*)(b+0x4014);now[3]=*(volatile DWORD*)(b+0x4018);now[4]=*(volatile DWORD*)(b+0x401c);now[5]=*(volatile DWORD*)(b+0x4004);now[6]=*(volatile DWORD*)(b+0x404c);now[7]=*(volatile DWORD*)(b+0x5864);}
  if(memcmp(previous,now,sizeof(now))){const char *labels[]={"identificador_real_vinculado","status_nucleo","CliExt_inicializado_u4014","hook_cor_u4018","hook_remotos_u401c","bruto_u4004","bruto_u404c","bruto_u5864"};for(unsigned i=0;i<8;i++)log_value(labels[i],now[i]);memcpy(previous,now,sizeof(now));}
  Sleep(1000);
 }
 return 0;
}
BOOL WINAPI DllMain(HINSTANCE module,DWORD reason,LPVOID reserved){(void)reserved;if(reason==DLL_PROCESS_ATTACH){g_self=module;DisableThreadLibraryCalls(module);HANDLE t=CreateThread(NULL,0,worker,NULL,0,NULL);if(t)CloseHandle(t);}return TRUE;}
