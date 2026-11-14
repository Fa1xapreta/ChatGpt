typedef unsigned int u32;
typedef int i32;
typedef unsigned short u16;
typedef unsigned char u8;
typedef struct {i32 x,y;} Point;
typedef struct {float x,y,z;} Vec;
typedef int (__attribute__((stdcall)) *FPoint)(Point*);
typedef int (__attribute__((stdcall)) *FAsync)(int);
typedef void* (__attribute__((stdcall)) *FFore)(void);
typedef int (__attribute__((stdcall)) *FScreen)(void*,Point*);
typedef u32 (__attribute__((stdcall)) *FPid)(void*,u32*);
typedef u32 (__attribute__((stdcall)) *FZero)(void);
typedef void* (__attribute__((stdcall)) *FProc)(void*,const char*);
typedef int (*FRead)(const void*,u32);
typedef int (*FName)(void*,const u16*);
typedef i32 (*FOffset)(void*,const u16*,void*);
typedef void* (*FFind)(void*,void*,const u16*,int);
typedef void* (__attribute__((fastcall)) *FInfo)(void*,void*);
typedef int (*FCopy)(void*,int);
typedef void (__attribute__((thiscall)) *FEvent)(void*,void*,void*,void*);
typedef void (*FLog)(const u16*,u32);
struct State {u32 actor,last_draw,last_refresh; i32 yaw,last_x,down,drag,ready;float body_h,center_z;u32 race,sex,kind; i32 coll_off,focus_off;void *fn_prepare,*fn_pose,*event;u32 init;};
struct State viewer_state;
/* IAT RVAs are inserted from the exact supported source DLL by the builder. */
#include "iat.h"
#define PTR(base,off,type) (*(type *)((u8*)(base)+(off)))
static int readable(u8 *base,const void *p,u32 n){return p && ((FRead)(base+0x1000))(p,n);}
static void logval(u8 *b,const u16 *s,u32 v){((FLog)(b+0x10cb))(s,v);}
static void *find(u8 *b,void *cls,const u16 *s){FFind f=PTR(b,0x17de4,FFind);return f?f(cls,(void*)-1,s,0):0;}
static void resolve(u8*b){
 struct State *s=&viewer_state;if(s->init)return;s->init=1;
 s->coll_off=((FOffset)(b+0x141e))(PTR(b,0x17e2c,void*),(const u16*)L"CollisionHeight",0);
 void *pc=find(b,PTR(b,0x17e24,void*),(const u16*)L"Interface.HudPortraitPawn");
 s->focus_off=pc?((FOffset)(b+0x141e))(pc,(const u16*)L"PortraitFocus",0):-1;
 void *fc=find(b,PTR(b,0x17e24,void*),(const u16*)L"Core.Function");
 s->fn_prepare=fc?find(b,fc,(const u16*)L"Interface.HudPortraitPawn.PrepareClassicAppearance"):0;
 s->fn_pose=fc?find(b,fc,(const u16*)L"Interface.HudPortraitPawn.UpdateClassicPose"):0;
 s->event=PTR(b,IAT_GetProcAddress,FProc)(PTR(b,0x17dd4,void*),"?ProcessEvent@UObject@@UAEXPAVUFunction@@PAX1@Z");
 logval(b,(const u16*)L"Inventory Viewr R6 init",s->event && s->fn_prepare && s->fn_pose?1:0);
}
static void refresh(u8*b,u8 *actor,u32 now){
 struct State *s=&viewer_state;
 if((u32)(now-s->last_refresh)<100)return;s->last_refresh=now;
 u8 *engine=PTR(b,0x17dd8,u8*);if(!readable(b,engine+0x3663bc,4))return;
 void *client=PTR(engine,0x3663bc,void*);if(!readable(b,client,4))return;
 u8 *info=((FInfo)(engine+0x14dd00))(client,0);if(!readable(b,info,0x314))return;
 int id=PTR(info,0x18,int);u8 *source=PTR(info,0x204,u8*);if(id<=0||source==actor||!readable(b,source,0x38))return;
 int result=((FCopy)(b+0x5e04))(actor,id);
 if(result==1){
  if(s->event && s->fn_prepare)((FEvent)s->event)(actor,s->fn_prepare,&source,0);
  logval(b,(const u16*)L"Inventory Viewr R6 equipment refreshed",1);
 }
 if(result>0 && s->event && s->fn_pose)((FEvent)s->event)(actor,s->fn_pose,0,0);
}
static int root_matches(u8*b,u8*ctx){
 if(!readable(b,ctx,12))return 0;u8 *w=PTR(ctx,4,u8*);if(!readable(b,w,0xbc))return 0;
 u8 *a=PTR(w,0xb8,u8*);if(!readable(b,a,16))return 0;
 u8 *c=PTR(a,12,u8*);if(!readable(b,c,8))return 0;
 return ((FName)(b+0x1f0b))(PTR(c,4,void*),(const u16*)L"MileniumInventoryPreviewWnd");
}
void viewer_tick(u8 *b,u8 *fp){
 struct State *s=&viewer_state;u8 *ctx=PTR(fp,8,u8*);if(!root_matches(b,ctx))return;
 u8 *actor=PTR(ctx,8,u8*);if(!readable(b,actor,0x38))return;
 resolve(b);u32 now=PTR(b,IAT_GetTickCount,FZero)();
 if(s->actor!=(u32)actor){s->actor=(u32)actor;s->ready=0;s->yaw=0;s->drag=0;s->down=0;s->last_refresh=now-100;}
 if((u32)(now-s->last_draw)>500){s->drag=0;s->down=1;s->yaw=0;}
 s->last_draw=now;refresh(b,actor,now);
 if(PTR(ctx,8,u8*)!=actor||!readable(b,actor,0x38))return;
 u32 race=PTR(b,0x18080,u32),sex=PTR(b,0x18084,u32),kind=PTR(b,0x18088,u32);
 if(s->race!=race||s->sex!=sex||s->kind!=kind){s->race=race;s->sex=sex;s->kind=kind;s->ready=0;}
 i32 loc_off=PTR(b,0x14550,i32);if(loc_off<0x34||loc_off>0x4000||!readable(b,actor+loc_off,12))return;
 Vec loc=PTR(actor,loc_off,Vec);if(!s->ready){
  float h=23.0f;
  if(s->coll_off>0&&readable(b,actor+s->coll_off,4)){float v=PTR(actor,s->coll_off,float);if(v>5.0f&&v<200.0f)h=v;}
  float center=0.0f;
  if(s->focus_off>0&&readable(b,actor+s->focus_off,12)){float head=PTR(actor,s->focus_off+8,float)-loc.z;if(head>5&&head<200)center=head-0.85f*h;}
  s->body_h=h;s->center_z=center;s->ready=1;
 }
 i32 x=PTR(fp,-8,i32),y=PTR(fp,-12,i32),w=PTR(fp,-16,i32),h=PTR(fp,-20,i32);if(w<32||h<128||w>2048||h>2048)return;
 Point pt;void *hwnd=PTR(b,IAT_GetForegroundWindow,FFore)();u32 pid=0;
 int valid=hwnd && PTR(b,IAT_GetWindowThreadProcessId,FPid)(hwnd,&pid) && pid==PTR(b,IAT_GetCurrentProcessId,FZero)();
 int down=valid && (PTR(b,IAT_GetAsyncKeyState,FAsync)(1)&0x8000)!=0;
 if(valid && PTR(b,IAT_GetCursorPos,FPoint)(&pt) && PTR(b,IAT_ScreenToClient,FScreen)(hwnd,&pt)){
  if(down && !s->down && pt.x>=x&&pt.x<x+w&&pt.y>=y+24&&pt.y<y+h-24){s->drag=1;s->last_x=pt.x;}
  if(down&&s->drag){i32 dx=pt.x-s->last_x;if(dx>-2000&&dx<2000)s->yaw=(s->yaw-dx*128)&65535;s->last_x=pt.x;}
 }else{s->drag=0;}
 if(!down)s->drag=0;s->down=down;
 float distance=s->body_h*(float)w/(0.3152988f*0.90f*(float)(h-64));
 float angle=(float)s->yaw*0.00009587379924f;float co,si;
 __asm__ __volatile__("fsincos":"=t"(co),"=u"(si):"0"(angle));
 Vec *cam=(Vec*)(fp-0x54);cam->x=loc.x+distance*co;cam->y=loc.y+distance*si;cam->z=loc.z+s->center_z;
 PTR(fp,-0x7c,i32)=0;PTR(fp,-0x78,i32)=(s->yaw+32768)&65535;PTR(fp,-0x74,i32)=0;
}
