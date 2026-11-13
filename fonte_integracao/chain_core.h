/* Milenium graphics/cloak interop. Own code: MIT license (see LICENSE.txt).
 * Clones only the vtable of interfaces returned by the backend factory.
 * No patch is made to the CapasClassic or MileniumFX binary.
 */
#ifndef MILENIUM_CHAIN_CORE_H
#define MILENIUM_CHAIN_CORE_H
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#ifdef _WIN32
#include <windows.h>
#define CALL __stdcall
#else
#define CALL
#ifndef HRESULT
 typedef int32_t HRESULT;
 typedef uint32_t ULONG;
 typedef uint32_t UINT;
 typedef uint32_t DWORD;
 typedef void *HWND;
#endif
#endif

typedef HRESULT (CALL *CreateDeviceFn)(void*,UINT,int,HWND,DWORD,void*,void**);
typedef HRESULT (CALL *EndSceneFn)(void*);
typedef HRESULT (CALL *PresentFn)(void*,const void*,const void*,HWND,const void*);
typedef ULONG (CALL *ReleaseFn)(void*);
typedef void (*SyncFn)(void);
typedef struct ChainRecord {
 void *object;
 void **original;
 void **shadow;
 unsigned count;
 int kind; /* 1=root, 2=device */
 struct ChainRecord *next;
} ChainRecord;
static ChainRecord *g_records;
static SyncFn g_sync;
#ifdef _WIN32
static SRWLOCK g_record_lock=SRWLOCK_INIT;
static void lock_records(void){AcquireSRWLockExclusive(&g_record_lock);}
static void unlock_records(void){ReleaseSRWLockExclusive(&g_record_lock);}
static DWORD g_last_present;
static _Thread_local int g_inside_sync;
#else
static void lock_records(void){}
static void unlock_records(void){}
static uint32_t g_last_present;
static _Thread_local int g_inside_sync;
#endif
static void chain_on_present(void);
static void chain_note_device(int attached);
static ChainRecord *find_record(void *object){
 ChainRecord *r=g_records;
 while(r && r->object!=object)r=r->next;
 return r;
}
static void *original_method(void *object,unsigned index){
 void *result=NULL;
 lock_records();
 ChainRecord *r=find_record(object);
 if(r && index<r->count)result=r->original[index];
 unlock_records();
 return result;
}
static ULONG CALL chain_release(void *object){
 ReleaseFn original=(ReleaseFn)original_method(object,2);
 if(!original)return 0;
 ULONG refs=original(object);
 if(refs==0){
  lock_records();
  ChainRecord **link=&g_records;
  while(*link && (*link)->object!=object)link=&(*link)->next;
  if(*link){ChainRecord *dead=*link;*link=dead->next;free(dead->shadow);free(dead);}
  unlock_records();
 }
 return refs;
}
static HRESULT CALL chain_end_scene(void *object){
 EndSceneFn original=(EndSceneFn)original_method(object,42);
 if(!original)return (HRESULT)0x80004005;
 if(g_sync && !g_inside_sync){
  g_inside_sync=1;
  g_sync();
  g_inside_sync=0;
 }
 return original(object);
}
static HRESULT CALL chain_present(void *object,const void *source,const void *dest,HWND window,const void *dirty){
 PresentFn original=(PresentFn)original_method(object,17);
 if(!original)return (HRESULT)0x80004005;
 chain_on_present();
 return original(object,source,dest,window,dirty);
}
static int attach_interface(void *object,int kind);
static HRESULT CALL chain_create_device(void *object,UINT adapter,int type,HWND window,DWORD flags,void *params,void **out){
 CreateDeviceFn original=(CreateDeviceFn)original_method(object,16);
 if(!original)return (HRESULT)0x80004005;
 HRESULT hr=original(object,adapter,type,window,flags,params,out);
 if(hr>=0 && out && *out){int attached=attach_interface(*out,2);chain_note_device(attached);}
 return hr;
}
static int attach_interface(void *object,int kind){
 if(!object || (kind!=1 && kind!=2))return 0;
 unsigned count=kind==1?17:119;
 lock_records();
 if(find_record(object)){unlock_records();return 1;}
 ChainRecord *r=(ChainRecord*)calloc(1,sizeof(*r));
 if(!r){unlock_records();return 0;}
 r->shadow=(void**)malloc(count*sizeof(void*));
 if(!r->shadow){free(r);unlock_records();return 0;}
 r->object=object;r->original=*(void***)object;r->count=count;r->kind=kind;
 memcpy(r->shadow,r->original,count*sizeof(void*));
 r->shadow[2]=(void*)chain_release;
 if(kind==1)r->shadow[16]=(void*)chain_create_device;
 else{r->shadow[17]=(void*)chain_present;r->shadow[42]=(void*)chain_end_scene;}
 r->next=g_records;g_records=r;
#ifdef _WIN32
 InterlockedExchangePointer((void *volatile*)object,r->shadow);
#else
 *(void***)object=r->shadow;
#endif
 unlock_records();
 return 1;
}
#endif
