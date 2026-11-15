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
struct State {u32 actor,last_draw,last_refresh; i32 yaw,last_x,down,drag,ready;float body_h,center_z;u32 race,sex,kind; i32 coll_off,focus_off;void *fn_prepare,*fn_pose,*event;u32 init; i32 equip_offsets[20]; u32 equip_init; float distance,center_y; u32 profile_since;};
struct State viewer_state;
/* IAT RVAs are inserted from the exact supported source DLL by the builder. */
#include "iat.h"
#define PTR(base,off,type) (*(type *)((u8*)(base)+(off)))
static int readable(u8 *base,const void *p,u32 n){return p && ((FRead)(base+0x1000))(p,n);}
static void logval(u8 *b,const u16 *s,u32 v){((FLog)(b+0x10cb))(s,v);}
static void *find(u8 *b,void *cls,const u16 *s){FFind f=PTR(b,0x17de4,FFind);return f?f(cls,(void*)-1,s,0):0;}
static void resolve(u8*b){
 struct State *s=&viewer_state;if(s->init)return;s->init=1;
 void *fc=find(b,PTR(b,0x17e24,void*),(const u16*)L"Core.Function");
 s->fn_prepare=fc?find(b,fc,(const u16*)L"Interface.HudPortraitPawn.PrepareClassicAppearance"):0;
 s->fn_pose=fc?find(b,fc,(const u16*)L"Interface.HudPortraitPawn.UpdateClassicPose"):0;
 s->event=PTR(b,IAT_GetProcAddress,FProc)(PTR(b,0x17dd4,void*),"?ProcessEvent@UObject@@UAEXPAVUFunction@@PAX1@Z");
 logval(b,(const u16*)L"Inventory Viewr R13 init",s->event && s->fn_prepare && s->fn_pose?1:0);
}
/* Only asset references and plain values are mirrored. Never share MeshInstances,
   actor ownership, physics state or live-world attachment arrays. */
static const u16 *equip_names[20] = {
 (const u16*)L"RightHandMesh",(const u16*)L"LeftHandMesh",
 (const u16*)L"RightArmMesh",(const u16*)L"LeftArmMesh",
 (const u16*)L"CloakMesh",(const u16*)L"CloakCoverMesh",
 (const u16*)L"RightHandSkins",(const u16*)L"LeftHandSkins",
 (const u16*)L"RightArmSkins",(const u16*)L"LeftArmSkins",
 (const u16*)L"CloakSkins",(const u16*)L"WeaponScale",
 (const u16*)L"RightHandBone",(const u16*)L"LeftHandBone",
 (const u16*)L"RightArmBone",(const u16*)L"LeftArmBone",
 (const u16*)L"SpineBone",(const u16*)L"LowbodyBone",(const u16*)L"CapeBone",
 (const u16*)L"AttackItemEnchantedValue"
};
static int sync_equipment(u8*b,u8*actor,u8*source){
 struct State*s=&viewer_state;int changed=0;
 if(!s->equip_init){
  for(int i=0;i<20;i++)s->equip_offsets[i]=((FOffset)(b+0x141e))(PTR(b,0x17e30,void*),equip_names[i],0);
  s->equip_init=1;
 }
 for(int i=0;i<20;i++){
  int off=s->equip_offsets[i],count=(i==9?3:(i==10?2:1));
  if(off<0x34||off>0x4000||!readable(b,source+off,count*4)||!readable(b,actor+off,count*4))continue;
  for(int j=0;j<count;j++){
   u32 value=PTR(source,off+j*4,u32);
   if(PTR(actor,off+j*4,u32)!=value){PTR(actor,off+j*4,u32)=value;changed=1;}
  }
 }
 return changed;
}
static void refresh(u8*b,u8 *actor,u32 now){
 struct State *s=&viewer_state;
 if((u32)(now-s->last_refresh)<100)return;s->last_refresh=now;
 u8 *engine=PTR(b,0x17dd8,u8*);if(!readable(b,engine+0x3663bc,4))return;
 void *client=PTR(engine,0x3663bc,void*);if(!readable(b,client,4))return;
 u8 *info=((FInfo)(engine+0x14dd00))(client,0);if(!readable(b,info,0x314))return;
 int id=PTR(info,0x18,int);u8 *source=PTR(info,0x204,u8*);if(id<=0||source==actor||!readable(b,source,0x38))return;
 int result=((FCopy)(b+0x5e04))(actor,id);
 int equipment_changed=result>0?sync_equipment(b,actor,source):0;
 if(result==1 || equipment_changed){
  if(s->event && s->fn_prepare)((FEvent)s->event)(actor,s->fn_prepare,&source,0);
  logval(b,(const u16*)L"Inventory Viewr R13 equipment refreshed",1);
 }
 if(result>0 && s->event && s->fn_pose)((FEvent)s->event)(actor,s->fn_pose,0,0);
}
static int root_matches(u8*b,u8*ctx){
 if(!readable(b,ctx,12))return 0;u8 *w=PTR(ctx,4,u8*);if(!readable(b,w,0xbc))return 0;
 u8 *a=PTR(w,0xb8,u8*);if(!readable(b,a,16))return 0;
 u8 *c=PTR(a,12,u8*);if(!readable(b,c,8))return 0;
 return ((FName)(b+0x1f0b))(PTR(c,4,void*),(const u16*)L"MileniumInventoryPreviewWnd");
}
typedef u32 (__attribute__((stdcall)) *FModuleName)(void*,u16*,u32);
typedef void* (__attribute__((stdcall)) *FCreateFile)(const u16*,u32,u32,void*,u32,u32,void*);
typedef int (__attribute__((stdcall)) *FWrite)(void*,const void*,u32,u32*,void*);
typedef int (__attribute__((stdcall)) *FClose)(void*);
typedef int (*FPrint)(char*,u32,const char*,...);
static u32 diagnostic_lines,diagnostic_last;
static void diagnostic(u8*b,u8*actor,u8*fp){
 if(diagnostic_lines>=30)return;
 u32 tick=PTR(b,IAT_GetTickCount,FZero)();
 if(diagnostic_lines>=5 && !viewer_state.ready && (u32)(tick-diagnostic_last)<1000)return;
 diagnostic_last=tick;diagnostic_lines++;
 u16 path[320];u32 n=PTR(b,IAT_GetModuleFileNameW,FModuleName)(b,path,260);if(!n||n>=260)return;
 while(n&&path[n-1]!='\\'&&path[n-1]!='/')n--;
 const char*name="Inventory_Viewr_R13.log";for(u32 i=0;name[i];i++)path[n++]=name[i];path[n]=0;
 char line[384];Vec cam=PTR(fp,-0x54,Vec);i32 locoff=PTR(b,0x14550,i32);Vec loc=PTR(actor,locoff,Vec);
 int len=PTR(b,IAT__snprintf,FPrint)(line,383,"R13 frame=%u actor=%08X viewport=%dx%d loc=%.2f,%.2f,%.2f cam=%.2f,%.2f,%.2f yaw=%u ready=%d bboxZ=%.2f,%.2f bboxY=%.2f,%.2f\r\n",diagnostic_lines,(u32)actor,PTR(fp,-16,int),PTR(fp,-20,int),(double)loc.x,(double)loc.y,(double)loc.z,(double)cam.x,(double)cam.y,(double)cam.z,PTR(fp,-0x78,u32),viewer_state.ready,(double)PTR(fp,-0x68,float),(double)PTR(fp,-0x5c,float),(double)PTR(fp,-0x6c,float),(double)PTR(fp,-0x60,float));
 if(len<=0||len>=384)return;
 void*h=PTR(b,IAT_CreateFileW,FCreateFile)(path,4,3,0,4,0x80,0);if((u32)h==0xffffffff)return;
 u32 written=0;PTR(b,IAT_WriteFile,FWrite)(h,line,(u32)len,&written,0);PTR(b,IAT_CloseHandle,FClose)(h);
}

void viewer_tick(u8 *b,u8 *fp){
 struct State *s=&viewer_state;u8 *ctx=PTR(fp,8,u8*);if(!root_matches(b,ctx))return;
 u8 *actor=PTR(ctx,8,u8*);if(!readable(b,actor,0x38))return;
 resolve(b);u32 now=PTR(b,IAT_GetTickCount,FZero)();
 if(s->actor!=(u32)actor){s->actor=(u32)actor;s->ready=0;s->yaw=0;s->drag=0;s->down=0;s->last_refresh=now-100;s->profile_since=now;}
 if((u32)(now-s->last_draw)>500){s->drag=0;s->down=1;}
 u32 elapsed=now-s->last_draw;if(elapsed>100)elapsed=100;
 s->last_draw=now;refresh(b,actor,now);
 if(PTR(ctx,8,u8*)!=actor||!readable(b,actor,0x38))return;
 u32 race=PTR(b,0x18080,u32),sex=PTR(b,0x18084,u32),kind=PTR(b,0x18088,u32);
 if(s->race!=race||s->sex!=sex||s->kind!=kind){s->race=race;s->sex=sex;s->kind=kind;s->ready=0;s->profile_since=now;}
 i32 loc_off=PTR(b,0x14550,i32);if(loc_off<0x34||loc_off>0x4000||!readable(b,actor+loc_off,12))return;
 Vec loc=PTR(actor,loc_off,Vec);
 i32 x=PTR(fp,-8,i32),y=PTR(fp,-12,i32),w=PTR(fp,-16,i32),h=PTR(fp,-20,i32);if(w<32||h<128||w>2048||h>2048)return;
 if(!s->ready && (u32)(now-s->profile_since)>=300){
  /* The original renderer validated this WORLD-space FBox before using it.
     Do not derive full-body framing from a head bone or collision capsule. */
  float miny=PTR(fp,-0x6c,float),maxy=PTR(fp,-0x60,float);
  float minz=PTR(fp,-0x68,float),maxz=PTR(fp,-0x5c,float);
  float height=maxz-minz,width=maxy-miny;
  if((PTR(fp,-0x58,u32)&255) && height>20.0f && height<1000.0f && width>1.0f && width<2000.0f && minz-loc.z>-500.0f && maxz-loc.z<1000.0f){
   float fit_height=height*0.5f*(float)w/(0.3152988f*0.62f*(float)(h-64));
   float fit_width=width*0.5f*(float)w/(0.3152988f*(float)(w-32));
   s->distance=fit_height>fit_width?fit_height:fit_width;
   s->center_y=(miny+maxy)*0.5f-loc.y;
   s->center_z=minz-loc.z+(0.835f-0.5f)*(float)h*2.0f*0.3152988f*s->distance/(float)w;
   s->body_h=height*0.5f;s->ready=1;
  }
 }
 /* Until resources have settled, preserve the original renderer's fitted view. */
 if(!s->ready){diagnostic(b,actor,fp);return;}
 Point pt;void *hwnd=PTR(b,IAT_GetForegroundWindow,FFore)();u32 pid=0;
 int valid=hwnd && PTR(b,IAT_GetWindowThreadProcessId,FPid)(hwnd,&pid) && pid==PTR(b,IAT_GetCurrentProcessId,FZero)();
 int down=valid && (PTR(b,IAT_GetAsyncKeyState,FAsync)(1)&0x8000)!=0;
 if(valid && PTR(b,IAT_GetCursorPos,FPoint)(&pt) && PTR(b,IAT_ScreenToClient,FScreen)(hwnd,&pt)){
  /* These rectangles are the actual 64x32 XDAT buttons, local to the viewport. */
  int side=0;
  if(pt.y>=y+389 && pt.y<y+421){
   if(pt.x>=x+55 && pt.x<x+119)side=-1;
   if(pt.x>=x+151 && pt.x<x+215)side=1;
  }
  if(down && !s->down && side){s->drag=side;s->yaw=(s->yaw+side*2048)&65535;}
  else if(down && s->drag && side==s->drag)s->yaw=(s->yaw+side*(i32)elapsed*24)&65535;
 }else{s->drag=0;}
 if(!down)s->drag=0;s->down=down;
 float distance=s->distance;
 float angle=(float)s->yaw*0.00009587379924f;float co,si;
 __asm__ __volatile__("fsincos":"=t"(co),"=u"(si):"0"(angle));
 Vec *cam=(Vec*)(fp-0x54);cam->x=loc.x+distance*co;cam->y=loc.y+s->center_y+distance*si;cam->z=loc.z+s->center_z;
 PTR(fp,-0x7c,i32)=0;PTR(fp,-0x78,i32)=(s->yaw+32768)&65535;PTR(fp,-0x74,i32)=0;
 diagnostic(b,actor,fp);
}
