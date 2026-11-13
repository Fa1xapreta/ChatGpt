#include <stdlib.h>
#define assert(x) do { if(!(x)){fprintf(stderr,"FAIL line %d: %s\n",__LINE__,#x);abort();} } while(0)
#include <stdio.h>
#include "chain_core.h"
typedef struct Obj {void **vtable;unsigned refs;} Obj;
static void *root_table[17],*device_table[119];
static Obj root,device;
static int sync_calls,scene_calls,present_calls,device_notice;
static void chain_on_present(void){present_calls++;}
static void chain_note_device(int attached){device_notice=attached;}
static ULONG CALL backend_release(void *p){return --((Obj*)p)->refs;}
static HRESULT CALL backend_scene(void *p){assert(p==&device);assert(sync_calls==scene_calls+1);scene_calls++;return 123;}
static HRESULT CALL backend_present(void *p,const void*a,const void*b,HWND c,const void*d){(void)a;(void)b;(void)c;(void)d;assert(p==&device);return 456;}
static void sync_callback(void){sync_calls++;}
static HRESULT CALL backend_create(void *p,UINT a,int t,HWND w,DWORD f,void *pp,void **out){(void)a;(void)t;(void)w;(void)f;(void)pp;assert(p==&root);*out=&device;return 0;}
int main(void){
 root_table[2]=(void*)backend_release;root_table[16]=(void*)backend_create;
 device_table[2]=(void*)backend_release;device_table[17]=(void*)backend_present;device_table[42]=(void*)backend_scene;
 root.vtable=root_table;root.refs=1;device.vtable=device_table;device.refs=1;g_sync=sync_callback;
 assert(attach_interface(&root,1));assert(attach_interface(&root,1));assert(root.vtable!=root_table);
 assert(root.vtable[4]==root_table[4]);void *out=NULL;
 assert(((CreateDeviceFn)root.vtable[16])(&root,0,0,NULL,0,NULL,&out)==0);assert(out==&device);assert(device_notice==1);
 assert(device.vtable[3]==device_table[3]);assert(((EndSceneFn)device.vtable[42])(&device)==123);assert(sync_calls==1 && scene_calls==1);
 assert(((PresentFn)device.vtable[17])(&device,NULL,NULL,NULL,NULL)==456);assert(present_calls==1);
 assert(((ReleaseFn)device.vtable[2])(&device)==0);assert(find_record(&device)==NULL);
 assert(((ReleaseFn)root.vtable[2])(&root)==0);assert(g_records==NULL);
 puts("PASS: root/device hooks, forwarding, sync before backend EndScene, unmodified slots, release cleanup");return 0;
}
