/* Isolated x86 SEH guard, compiled with the MSVC C ABI. */
typedef void (__cdecl *SyncCallback)(void);
__declspec(noinline) int __cdecl milenium_guard_sync(SyncCallback callback){
 __try {callback();return 0;}
 __except(1){return 1;}
}
