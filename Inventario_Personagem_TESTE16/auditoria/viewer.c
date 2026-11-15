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
struct State {u32 actor,last_draw,last_refresh; i32 yaw,last_x,down,drag,ready;float body_h,center_z;u32 race,sex,kind; i32 coll_off,focus_off;void *fn_prepare,*fn_pose,*event;u32 init; i32 equip_offsets[23]; u32 equip_init; float distance,center_y; u32 profile_since; i32 mesh_off,turn_step;float center_x;void *match_bone,*bone_coords;u32 hold_since; i32 rotation_off,source_match;u32 current_mesh;};
struct State viewer_state;
/* IAT RVAs are inserted from the exact supported source DLL by the builder. */
#include "iat.h"
#define PTR(base,off,type) (*(type *)((u8*)(base)+(off)))
static int readable(u8 *base,const void *p,u32 n){return p && ((FRead)(base+0x1000))(p,n);}
static void logval(u8 *b,const u16 *s,u32 v){((FLog)(b+0x10cb))(s,v);}
static void *find(u8 *b,void *cls,const u16 *s){FFind f=PTR(b,0x17de4,FFind);return f?f(cls,(void*)-1,s,0):0;}
static void resolve(u8*b){
 struct State *s=&viewer_state;if(s->init)return;s->init=1;
 s->mesh_off=((FOffset)(b+0x141e))(PTR(b,0x17e2c,void*),(const u16*)L"Mesh",0);
 FProc proc=PTR(b,IAT_GetProcAddress,FProc);void*engine=PTR(b,0x17dd8,void*);
 s->match_bone=proc(engine,"?MatchRefBone@USkeletalMesh@@QAEHVFName@@@Z");
 s->bone_coords=proc(engine,"?GetBoneCoordsWithBoneIndex@AActor@@QAE?AVFCoords@@H@Z");
 s->rotation_off=((FOffset)(b+0x141e))(PTR(b,0x17e2c,void*),(const u16*)L"Rotation",0);
 const u16*names[3]={(const u16*)L"HeadBone",(const u16*)L"RightFootBone",(const u16*)L"LeftFootBone"};
 for(int i=0;i<3;i++)s->equip_offsets[20+i]=((FOffset)(b+0x141e))(PTR(b,0x17e30,void*),names[i],0);
}
/* Read the actual player's metadata. No appearance copy, forced animation,
   mesh scaling, position change or rotation change is applied to this Pawn. */
static void refresh(u8*b,u8 *actor,u32 now){
 struct State*s=&viewer_state;
 if((u32)(now-s->last_refresh)<100)return;s->last_refresh=now;
 u8*engine=PTR(b,0x17dd8,u8*);if(!readable(b,engine+0x3663bc,4))return;
 void*client=PTR(engine,0x3663bc,void*);if(!readable(b,client,4))return;
 u8*info=((FInfo)(engine+0x14dd00))(client,0);if(!readable(b,info,0x314))return;
 s->source_match=PTR(info,0x204,u8*)==actor;
 if(!s->source_match)return;
 u32 race=PTR(info,0x4c,u32),sex=PTR(info,0x50,u32),kind=PTR(info,0x54,u32);
 if(s->race!=race||s->sex!=sex||s->kind!=kind){s->race=race;s->sex=sex;s->kind=kind;s->ready=0;s->profile_since=now;}
}
static void trig_yaw(u32 yaw,float*co,float*si){
 float angle=(float)(yaw&65535u)*0.00009587379924f,c,t;
 __asm__ __volatile__("fsincos":"=t"(c),"=u"(t):"0"(angle));*co=c;*si=t;
}
static int root_matches(u8*b,u8*ctx){
 if(!readable(b,ctx,12))return 0;u8 *w=PTR(ctx,4,u8*);if(!readable(b,w,0xbc))return 0;
 u8 *a=PTR(w,0xb8,u8*);if(!readable(b,a,16))return 0;
 u8 *c=PTR(a,12,u8*);if(!readable(b,c,8))return 0;
 return ((FName)(b+0x1f0b))(PTR(c,4,void*),(const u16*)L"MileniumInventoryPreviewWnd");
}
typedef int (__attribute__((thiscall)) *FMatchBone)(void*,u32);
typedef struct {Vec origin,x,y,z;} Coords;
typedef Coords* (__attribute__((thiscall)) *FBoneCoords)(void*,Coords*,int);
struct BodySample {Vec head,left,right; int hi,li,ri,source;u32 mesh;};
static struct BodySample body_sample;
static int body_bounds(u8*b,u8*actor,Vec loc,float*miny,float*maxy,float*minz,float*maxz,float*cx){
 struct State*s=&viewer_state;body_sample.source=0;
 if(s->mesh_off<0x34||!s->match_bone||!s->bone_coords||!readable(b,actor+s->mesh_off,4))return 0;
 void*mesh=PTR(actor,s->mesh_off,void*);if(!readable(b,mesh,0x50))return 0;body_sample.mesh=(u32)mesh;
 int indices[3];for(int i=0;i<3;i++){
  int off=s->equip_offsets[20+i];if(off<0x34||off>0x4000||!readable(b,actor+off,4))return 0;
  u32 bone=PTR(actor,off,u32);if(!bone)return 0;
  indices[i]=((FMatchBone)s->match_bone)(mesh,bone);if(indices[i]<0||indices[i]>2048)return 0;
 }
 body_sample.hi=indices[0];body_sample.ri=indices[1];body_sample.li=indices[2];
 Coords c[3];for(int i=0;i<3;i++){
  float*zero=(float*)&c[i];for(int j=0;j<12;j++)zero[j]=0;
  ((FBoneCoords)s->bone_coords)(actor,&c[i],indices[i]);
 }
 Vec head=c[0].origin,right=c[1].origin,left=c[2].origin;
 body_sample.head=head;body_sample.right=right;body_sample.left=left;
 float foot=left.z<right.z?left.z:right.z;float height=head.z-foot;
 if(!(height>15.0f&&height<300.0f&&head.z-loc.z>-100.0f&&head.z-loc.z<400.0f&&foot-loc.z>-150.0f&&foot-loc.z<150.0f))return 0;
 float middle_y=(head.y+(right.y+left.y)*0.5f)*0.5f;
 if(!(middle_y-loc.y>-200.0f&&middle_y-loc.y<200.0f))return 0;
 *minz=foot-1.0f;*maxz=head.z+height*0.12f;
 *miny=middle_y-height*0.32f;*maxy=middle_y+height*0.32f;
 *cx=(head.x+(right.x+left.x)*0.5f)*0.5f-loc.x;
 body_sample.source=1;return 1;
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
 const char*name="Inventory_Viewr_R16.log";for(u32 i=0;name[i];i++)path[n++]=name[i];path[n]=0;
 char line[384];Vec cam=PTR(fp,-0x54,Vec);i32 locoff=PTR(b,0x14550,i32);Vec loc=PTR(actor,locoff,Vec);
 int len=PTR(b,IAT__snprintf,FPrint)(line,383,"R16 frame=%u actor=%08X viewport=%dx%d loc=%.2f,%.2f,%.2f cam=%.2f,%.2f,%.2f yaw=%u ready=%d bboxZ=%.2f,%.2f bboxY=%.2f,%.2f body=%d headZ=%.2f feetZ=%.2f,%.2f xy=%d,%d live=%d\r\n",diagnostic_lines,(u32)actor,PTR(fp,-16,int),PTR(fp,-20,int),(double)loc.x,(double)loc.y,(double)loc.z,(double)cam.x,(double)cam.y,(double)cam.z,PTR(fp,-0x78,u32),viewer_state.ready,(double)PTR(fp,-0x68,float),(double)PTR(fp,-0x5c,float),(double)PTR(fp,-0x6c,float),(double)PTR(fp,-0x60,float),body_sample.source,(double)body_sample.head.z,(double)body_sample.left.z,(double)body_sample.right.z,PTR(fp,-8,int),PTR(fp,-12,int),viewer_state.source_match);
 if(len<=0||len>=384)return;
 void*h=PTR(b,IAT_CreateFileW,FCreateFile)(path,4,3,0,4,0x80,0);if((u32)h==0xffffffff)return;
 u32 written=0;PTR(b,IAT_WriteFile,FWrite)(h,line,(u32)len,&written,0);PTR(b,IAT_CloseHandle,FClose)(h);
}

void viewer_tick(u8 *b,u8 *fp){
 struct State *s=&viewer_state;u8 *ctx=PTR(fp,8,u8*);if(!root_matches(b,ctx))return;
 u8 *actor=PTR(ctx,8,u8*);if(!readable(b,actor,0x38))return;
 resolve(b);u32 now=PTR(b,IAT_GetTickCount,FZero)();
 if(s->actor!=(u32)actor){s->actor=(u32)actor;s->ready=0;s->yaw=0;s->turn_step=0;s->drag=0;s->down=0;s->last_refresh=now-100;s->profile_since=now;s->source_match=0;s->current_mesh=0;}
 if((u32)(now-s->last_draw)>500){s->drag=0;s->down=1;s->turn_step=0;s->yaw=0;}
 u32 elapsed=now-s->last_draw;if(elapsed>100)elapsed=100;
 s->last_draw=now;refresh(b,actor,now);
 if(PTR(ctx,8,u8*)!=actor||!readable(b,actor,0x38))return;
 if(!s->source_match){diagnostic(b,actor,fp);return;}
 if(s->mesh_off>=0x34 && readable(b,actor+s->mesh_off,4)){
  u32 mesh=PTR(actor,s->mesh_off,u32);if(mesh!=s->current_mesh){s->current_mesh=mesh;s->ready=0;s->profile_since=now;}
 }
 u32 actor_yaw=0;
 if(s->rotation_off>=0x34 && s->rotation_off<0x4000 && readable(b,actor+s->rotation_off,12))actor_yaw=PTR(actor,s->rotation_off+4,u32)&65535u;
 float baseco,basesi;trig_yaw(actor_yaw,&baseco,&basesi);
 i32 loc_off=PTR(b,0x14550,i32);if(loc_off<0x34||loc_off>0x4000||!readable(b,actor+loc_off,12))return;
 Vec loc=PTR(actor,loc_off,Vec);
 i32 x=PTR(fp,-8,i32),y=PTR(fp,-12,i32),w=PTR(fp,-16,i32),h=PTR(fp,-20,i32);if(w<32||h<128||w>2048||h>2048)return;
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
  if(down && !s->down && side){
   s->drag=side;s->hold_since=now;
   s->turn_step=(s->turn_step+side*1500+36000)%36000;
   s->yaw=(u32)s->turn_step*65536u/36000u;
  }else if(down && s->drag==side && side && (u32)(now-s->hold_since)>=300){
   int rate=(u32)(now-s->hold_since)>=900?18:12;
   s->turn_step=(s->turn_step+side*(i32)elapsed*rate+36000)%36000;
   s->yaw=(u32)s->turn_step*65536u/36000u;
  }
  if(!side || side!=s->drag)s->drag=0;
 }else{s->drag=0;}
 if(!down)s->drag=0;s->down=down;
 if(!s->ready){
  /* The original renderer validated this WORLD-space FBox before using it.
     Do not derive full-body framing from a head bone or collision capsule. */
  float miny=PTR(fp,-0x6c,float),maxy=PTR(fp,-0x60,float);
  float minz=PTR(fp,-0x68,float),maxz=PTR(fp,-0x5c,float);
  float center_x=0;
  int bones=body_bounds(b,actor,loc,&miny,&maxy,&minz,&maxz,&center_x);
  float height=maxz-minz,width=maxy-miny;
  if((bones||(PTR(fp,-0x58,u32)&255)) && height>20.0f && height<1000.0f && width>1.0f && width<2000.0f && minz-loc.z>-500.0f && maxz-loc.z<1000.0f){
   float fit_height=height*0.5f*(float)w/(0.22169466f*0.77f*(float)(h-64));
   float fit_width=width*0.5f*(float)w/(0.22169466f*(float)(w-32));
   s->distance=fit_height>fit_width?fit_height:fit_width;
   float center_y=(miny+maxy)*0.5f-loc.y;
   s->center_x=center_x*baseco+center_y*basesi;s->center_y=-center_x*basesi+center_y*baseco;
   s->center_z=minz-loc.z+(0.835f-0.5f)*(float)h*2.0f*0.22169466f*s->distance/(float)w;
   s->body_h=height*0.5f;s->ready=1;
   if(!bones)body_sample.source=2;
  }
 }
 /* Input is processed even while the geometry profile is still loading. */
 if(!s->ready){
  /* Rotate the engine-fitted view too: input must never depend on pose readiness. */
  Vec*camera=(Vec*)(fp-0x54);float radius=camera->x-loc.x;
  if(radius>5.0f && radius<2000.0f){
   float co,si;trig_yaw(actor_yaw+s->yaw,&co,&si);
   camera->x=loc.x+radius*co;camera->y=camera->y+radius*si;
   PTR(fp,-0x7c,i32)=0;PTR(fp,-0x78,i32)=(actor_yaw+s->yaw+32768)&65535;PTR(fp,-0x74,i32)=0;
  }
  diagnostic(b,actor,fp);return;
 }
 float distance=s->distance;
 float co,si;trig_yaw(actor_yaw+s->yaw,&co,&si);
 Vec *cam=(Vec*)(fp-0x54);cam->x=loc.x+s->center_x*baseco-s->center_y*basesi+distance*co;cam->y=loc.y+s->center_x*basesi+s->center_y*baseco+distance*si;cam->z=loc.z+s->center_z;
 PTR(fp,-0x7c,i32)=0;PTR(fp,-0x78,i32)=(actor_yaw+s->yaw+32768)&65535;PTR(fp,-0x74,i32)=0;
 diagnostic(b,actor,fp);
}
