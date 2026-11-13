#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <stdio.h>
#include <wchar.h>
static HMODULE g_self;
static wchar_t g_dir[MAX_PATH];
static void log_status(const char *message,DWORD value){
 wchar_t path[MAX_PATH];_snwprintf(path,MAX_PATH,L"%lsMilenium_Aura_Integracao.log",g_dir);path[MAX_PATH-1]=0;
 char text[256];snprintf(text,sizeof(text),"%s: %lu\r\n",message,(unsigned long)value);
 HANDLE h=CreateFileW(path,FILE_APPEND_DATA,FILE_SHARE_READ|FILE_SHARE_WRITE,NULL,OPEN_ALWAYS,FILE_ATTRIBUTE_NORMAL,NULL);
 if(h!=INVALID_HANDLE_VALUE){DWORD written;WriteFile(h,text,(DWORD)strlen(text),&written,NULL);CloseHandle(h);}
}
static int cape_ready(void){
 HANDLE map=OpenFileMappingW(FILE_MAP_READ,FALSE,L"Local\\CapasClassicLive_v1");if(!map)return 0;
 const volatile DWORD *view=(const volatile DWORD*)MapViewOfFile(map,FILE_MAP_READ,0,0,256);int ready=0;
 if(view){ready=view[0]==0x564c4343 && view[1]==2 && view[2]==GetCurrentProcessId() && view[4]==3;UnmapViewOfFile((const void*)view);}
 CloseHandle(map);return ready;
}
static DWORD WINAPI worker(void *unused){
 (void)unused;DWORD n=GetModuleFileNameW(g_self,g_dir,MAX_PATH);if(!n || n>=MAX_PATH)return 0;
 wchar_t *slash=wcsrchr(g_dir,L'\\');if(!slash)return 0;slash[1]=0;
 DWORD ticks=0;while(ticks<3000 && !cape_ready()){Sleep(100);ticks++;}
 if(!cape_ready())log_status("CapasClassic nao confirmou etapa 3 em 300 segundos; carregando nucleo original como fallback",1);
 log_status("Esperou CapasClassic antes de carregar o nucleo original (ms)",ticks*100);
 wchar_t path[MAX_PATH];_snwprintf(path,MAX_PATH,L"%lsL2modExCore.dll",g_dir);path[MAX_PATH-1]=0;
 HMODULE core=GetModuleHandleW(L"L2modExCore.dll");if(!core)core=LoadLibraryW(path);
 if(!core){log_status("Falha ao carregar L2modExCore original, erro",GetLastError());return 0;}
 log_status("L2modExCore original carregado",1);
 typedef int (__cdecl *StatusFn)(void);StatusFn status=(StatusFn)GetProcAddress(core,"ContornoDourado_Status");
 for(unsigned i=0;i<5 && status;i++){Sleep(1000);log_status("Status publicado pelo nucleo original (sem reinterpretar)",(DWORD)status());}
 return 0;
}
BOOL WINAPI DllMain(HINSTANCE module,DWORD reason,LPVOID reserved){
 (void)reserved;if(reason==DLL_PROCESS_ATTACH){g_self=module;DisableThreadLibraryCalls(module);HANDLE t=CreateThread(NULL,0,worker,NULL,0,NULL);if(t)CloseHandle(t);}return TRUE;
}
