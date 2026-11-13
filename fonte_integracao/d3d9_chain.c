#define WIN32_LEAN_AND_MEAN
#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0600
#endif
#include <windows.h>
#include <stdio.h>
#include <stdarg.h>
#include <wchar.h>
#include "chain_core.h"

typedef void *(WINAPI *Factory9)(UINT);
typedef HRESULT (WINAPI *FactoryEx)(UINT,void**);
static HMODULE g_self,g_backend,g_capes;
static Factory9 g_factory;
static FactoryEx g_factory_ex;
static INIT_ONCE g_init=INIT_ONCE_STATIC_INIT;
static LONG g_sync_errors;
extern int __cdecl milenium_guard_sync(void (__cdecl *callback)(void));
static wchar_t g_directory[MAX_PATH];
static void log_line(const char *format,...){
 wchar_t path[MAX_PATH];
 if(!g_directory[0])return;
 _snwprintf(path,MAX_PATH,L"%lsMilenium_Capas_Integracao.log",g_directory);path[MAX_PATH-1]=0;
 char text[512];va_list args;va_start(args,format);vsnprintf(text,sizeof(text)-3,format,args);va_end(args);text[sizeof(text)-3]=0;strcat(text,"\r\n");
 HANDLE file=CreateFileW(path,FILE_APPEND_DATA,FILE_SHARE_READ|FILE_SHARE_WRITE,NULL,OPEN_ALWAYS,FILE_ATTRIBUTE_NORMAL,NULL);
 if(file!=INVALID_HANDLE_VALUE){DWORD written;WriteFile(file,text,(DWORD)strlen(text),&written,NULL);CloseHandle(file);}
}
static void chain_on_present(void){InterlockedExchange((LONG*)&g_last_present,(LONG)GetTickCount());}
static void chain_note_device(int attached){log_line("CreateDevice backend=MileniumGraphics; sincronizacao=%s",attached?"instalada":"falha de memoria");}
__attribute__((noinline)) static void sync_capes(void){
 /* The reference proxy calls CCD3DSync immediately BEFORE device.EndScene.
  * Preserve that position; it is deliberately not called after Present. */
 if(!g_capes)return;
 typedef void (__cdecl *CapaSync)(void);
 CapaSync fn=(CapaSync)GetProcAddress(g_capes,"CCD3DSync");
 if(fn){
  if(milenium_guard_sync(fn)){
   if(InterlockedIncrement(&g_sync_errors)<=3)log_line("CCD3DSync gerou excecao; frame ignorado");
  }
 }
}
static BOOL CALLBACK initialize(PINIT_ONCE once,PVOID unused,PVOID *context){
 (void)once;(void)unused;(void)context;
 wchar_t path[MAX_PATH];
 DWORD n=GetModuleFileNameW(g_self,g_directory,MAX_PATH);
 if(!n || n>=MAX_PATH)return FALSE;
 wchar_t *slash=wcsrchr(g_directory,L'\\');if(!slash)return FALSE;slash[1]=0;
 log_line("Milenium graphics/capas chain v1; processo=%lu",GetCurrentProcessId());
 _snwprintf(path,MAX_PATH,L"%lsMileniumGraphics.dll",g_directory);path[MAX_PATH-1]=0;
 g_backend=LoadLibraryW(path);
 if(!g_backend){log_line("MileniumGraphics nao carregou; erro=%lu; usando DirectX do Windows",GetLastError());
  UINT size=GetSystemDirectoryW(path,MAX_PATH);
  if(size && size<MAX_PATH-12){wcscat(path,L"\\d3d9.dll");g_backend=LoadLibraryW(path);}
 }
 if(g_backend){g_factory=(Factory9)GetProcAddress(g_backend,"Direct3DCreate9");g_factory_ex=(FactoryEx)GetProcAddress(g_backend,"Direct3DCreate9Ex");}
 _snwprintf(path,MAX_PATH,L"%lsCapasClassic.dll",g_directory);path[MAX_PATH-1]=0;
 g_capes=GetModuleHandleW(L"CapasClassic.dll");if(!g_capes)g_capes=LoadLibraryW(path);
 if(g_capes && GetProcAddress(g_capes,"CCD3DSync")){g_sync=sync_capes;log_line("CapasClassic carregada; CCD3DSync disponivel; inicializacao nativa ainda depende do proprio motor");}
 else log_line("CapasClassic/CCD3DSync indisponivel; erro=%lu",GetLastError());
 log_line("CliExt=%s; L2modExCore=%s; os arquivos originais desses componentes foram preservados",GetModuleHandleW(L"CliExt.dll")?"carregado":"ainda nao carregado",GetModuleHandleW(L"L2modExCore.dll")?"carregado":"ainda nao carregado");
 return TRUE;
}
__declspec(dllexport) void *WINAPI Direct3DCreate9(UINT version){
 if(!InitOnceExecuteOnce(&g_init,initialize,NULL,NULL) || !g_factory)return NULL;
 void *root=g_factory(version);
 if(root && !attach_interface(root,1))log_line("Falha ao ligar CreateDevice da interface raiz");
 return root;
}
__declspec(dllexport) HRESULT WINAPI Direct3DCreate9Ex(UINT version,void **out){
 /* Client under analysis imports Direct3DCreate9, not Ex. Preserve Ex
  * as a backend passthrough rather than claiming untested Ex interception. */
 if(!InitOnceExecuteOnce(&g_init,initialize,NULL,NULL) || !g_factory_ex)return (HRESULT)0x80004001;
 log_line("Direct3DCreate9Ex solicitado: encaminhamento sem sincronizacao adicional (caminho nao usado pelo cliente analisado)");
 return g_factory_ex(version,out);
}
__declspec(dllexport) DWORD WINAPI HangGetPresentAgeMs(void){DWORD tick=g_last_present;return tick?GetTickCount()-tick:0;}
BOOL WINAPI DllMain(HINSTANCE module,DWORD reason,LPVOID reserved){
 (void)reserved;if(reason==DLL_PROCESS_ATTACH){g_self=module;DisableThreadLibraryCalls(module);}return TRUE;
}
