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
struct State {u32 actor,last_draw,last_refresh; i32 yaw,last_x,down,drag,ready;float body_h,center_z;u32 race,sex,kind; i32 coll_off,focus_off;void *fn_prepare,*fn_pose,*event;u32 init; i32 equip_offsets[23]; u32 equip_init; float distance,center_y; u32 profile_since; i32 mesh_off,turn_step;float center_x;void *match_bone,*bone_coords;u32 hold_since; i32 rotation_off,source_match;u32 current_mesh;void *pawn_class,*skeletal_class; i32 loading_off;u32 resolve_after;void*name_ctor; i32 attached_off,base_off; float zoom;};
struct State viewer_state;
/* IAT RVAs are inserted from the exact supported source DLL by the builder. */
#include "iat.h"
#define ENGINE_MODULE_RVA 0x17dd4
#define NWINDOW_MODULE_RVA 0x17dd8
typedef void* (__attribute__((stdcall)) *FModule)(const u16*);
#define PTR(base,off,type) (*(type *)((u8*)(base)+(off)))
static int readable(u8 *base,const void *p,u32 n){return p && ((FRead)(base+0x1000))(p,n);}
static void logval(u8 *b,const u16 *s,u32 v){((FLog)(b+0x10cb))(s,v);}
static void *find(u8 *b,void *cls,const u16 *s){FFind f=PTR(b,0x17de4,FFind);return f?f(cls,(void*)-1,s,0):0;}
static void resolve(u8*b){
 struct State*s=&viewer_state;if(s->init)return;
 u32 now=PTR(b,IAT_GetTickCount,FZero)();if(s->resolve_after && (i32)(now-s->resolve_after)<0)return;s->resolve_after=now+500;
 void*core=PTR(b,IAT_GetModuleHandleW,FModule)((const u16*)L"Core.dll");
 void*cc=PTR(b,0x17e24,void*);
 if(!cc){void*fn=PTR(b,IAT_GetProcAddress,FProc)(core,"?StaticClass@UClass@@SAPAV1@XZ");if(fn)cc=((void*(*)(void))fn)();}
 if(!cc)return;
 void*ac=find(b,cc,(const u16*)L"Engine.Actor");
 s->pawn_class=find(b,cc,(const u16*)L"Engine.Pawn");
 s->skeletal_class=find(b,cc,(const u16*)L"Engine.SkeletalMesh");
 if(!ac||!s->pawn_class||!s->skeletal_class)return;
 s->mesh_off=((FOffset)(b+0x141e))(ac,(const u16*)L"Mesh",0);
 s->attached_off=((FOffset)(b+0x141e))(ac,(const u16*)L"Attached",0);
 s->base_off=((FOffset)(b+0x141e))(ac,(const u16*)L"Base",0);
 s->rotation_off=((FOffset)(b+0x141e))(ac,(const u16*)L"Rotation",0);
 s->loading_off=((FOffset)(b+0x141e))(s->pawn_class,(const u16*)L"LoadingResourceRef",0);
 const u16*names[3]={(const u16*)L"HeadBone",(const u16*)L"RightFootBone",(const u16*)L"LeftFootBone"};
 for(int i=0;i<3;i++)s->equip_offsets[20+i]=((FOffset)(b+0x141e))(s->pawn_class,names[i],0);
 FProc proc=PTR(b,IAT_GetProcAddress,FProc);void*engine=PTR(b,ENGINE_MODULE_RVA,void*);
 s->name_ctor=proc(core,"??0FName@@QAE@PBGW4EFindName@@@Z");
 s->match_bone=proc(engine,"?MatchRefBone@USkeletalMesh@@QAEHVFName@@@Z");
 s->bone_coords=proc(engine,"?GetBoneCoordsWithBoneIndex@AActor@@QAE?AVFCoords@@H@Z");
 s->init=s->mesh_off>=0x34 && s->rotation_off>=0x34 && s->equip_offsets[20]>=0x34 && s->equip_offsets[21]>=0x34 && s->equip_offsets[22]>=0x34 && s->match_bone && s->bone_coords;
}
/* Read the actual player's metadata. No appearance copy, forced animation,
   mesh scaling, position change or rotation change is applied to this Pawn. */
static void refresh(u8*b,u8 *actor,u32 now){
 struct State*s=&viewer_state;
 if((u32)(now-s->last_refresh)<100)return;s->last_refresh=now;s->source_match=0;
 u8*engine=PTR(b,NWINDOW_MODULE_RVA,u8*);if(!readable(b,engine+0x3663bc,4))return;
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
typedef void* (__attribute__((thiscall)) *FMakeName)(u32*,const u16*,int);
typedef int (__attribute__((thiscall)) *FMatchBone)(void*,u32);
typedef struct {Vec origin,x,y,z;} Coords;
typedef Coords* (__attribute__((thiscall)) *FBoneCoords)(void*,Coords*,int);
struct BodySample {Vec head,left,right; int hi,li,ri,source;u32 mesh;};
static struct BodySample body_sample;
static int body_bounds(u8*b,u8*actor,Vec loc,float*miny,float*maxy,float*minz,float*maxz,float*cx){
 struct State*s=&viewer_state;body_sample.source=-1;
 if(!s->init||s->mesh_off<0x34||!s->match_bone||!s->bone_coords||!readable(b,actor+s->mesh_off,4))return 0;
 body_sample.source=-2;void*mesh=PTR(actor,s->mesh_off,void*);if(!readable(b,mesh,0x50))return 0;body_sample.mesh=(u32)mesh;
 body_sample.source=-3;
 if(!((int(*)(void*,void*))(b+0x135a))(mesh,s->skeletal_class))return 0;
 body_sample.source=-4;
 if(s->loading_off>=0x34 && s->loading_off<0x4000 && readable(b,actor+s->loading_off,4) && PTR(actor,s->loading_off,int)>0)return 0;
 const u16*candidates[3][3]={
  {(const u16*)L"Bip01_head",(const u16*)L"Bip01 Head",(const u16*)L"Head"},
  {(const u16*)L"bip01_r_foot",(const u16*)L"Bip01 R Foot",(const u16*)L"R_Foot"},
  {(const u16*)L"bip01_l_foot",(const u16*)L"Bip01 L Foot",(const u16*)L"L_Foot"}
 };
 int indices[3];for(int i=0;i<3;i++){
  body_sample.source=-5;int off=s->equip_offsets[20+i];if(off<0x34||off>0x4000||!readable(b,actor+off,4))return 0;
  u32 bone=PTR(actor,off,u32);int index=bone?((FMatchBone)s->match_bone)(mesh,bone):-1;
  /* Some real Pawns leave the script bone-name fields at None. Resolve the
     standard biped names already present in the mesh's name table instead. */
  if(index<=0 && s->name_ctor)for(int c=0;c<3;c++){
   u32 name=0;((FMakeName)s->name_ctor)(&name,candidates[i][c],0);
   if(name)index=((FMatchBone)s->match_bone)(mesh,name);
   if(index>0)break;
  }
  body_sample.source=-10-i;if(index<=0||index>2048)return 0;indices[i]=index;
 }
 body_sample.hi=indices[0];body_sample.ri=indices[1];body_sample.li=indices[2];
 Coords c[3];for(int i=0;i<3;i++){
  float*zero=(float*)&c[i];for(int j=0;j<12;j++)zero[j]=0;
  ((FBoneCoords)s->bone_coords)(actor,&c[i],indices[i]);
 }
 Vec head=c[0].origin,right=c[1].origin,left=c[2].origin;
 body_sample.head=head;body_sample.right=right;body_sample.left=left;
 body_sample.source=-20;float foot=left.z<right.z?left.z:right.z;float height=head.z-foot;
 if(!(height>15.0f&&height<300.0f&&head.z-loc.z>-100.0f&&head.z-loc.z<400.0f&&foot-loc.z>-150.0f&&foot-loc.z<150.0f))return 0;
 float middle_y=(head.y+(right.y+left.y)*0.5f)*0.5f;
 if(!(middle_y-loc.y>-200.0f&&middle_y-loc.y<200.0f))return 0;
 *minz=foot-1.0f;*maxz=head.z+height*0.12f;
 *miny=middle_y-height*0.32f;*maxy=middle_y+height*0.32f;
 *cx=(head.x+(right.x+left.x)*0.5f)*0.5f-loc.x;
 body_sample.source=1;return 1;
}

typedef u32 (__attribute__((stdcall)) *FThread)(void);
typedef int (__attribute__((stdcall)) *FProtect)(void*,u32,u32,u32*);
typedef void* (__attribute__((stdcall)) *FProcess)(void);
typedef int (__attribute__((stdcall)) *FFlush)(void*,const void*,u32);
static u8*caption_base;static u8*active_context;static u32 caption_thread,caption_scope;static int caption_attempted,caption_status;static u32 caption_hits;
extern u8 render_entry_trampoline[],caption_trampoline[];
__asm__(".pushsection .text\n.p2align 4\n.global render_entry_trampoline\nrender_entry_trampoline:\n.fill 32,1,0x90\n.p2align 4\n.global caption_trampoline\ncaption_trampoline:\n.fill 32,1,0x90\n.popsection\n");
int preview_caption_block(void){return caption_scope && caption_base && PTR(caption_base,IAT_GetCurrentThreadId,FThread)()==caption_thread;}
int preview_nested_block(u8*context){
 if(preview_caption_block() && context==active_context)return 1;
 u8*b=caption_base;if(!b||!root_matches(b,context))return 0;
 if(!readable(b,context,12))return 1;
 u8*engine=PTR(b,NWINDOW_MODULE_RVA,u8*);if(!readable(b,engine+0x3663bc,4))return 1;
 void*client=PTR(engine,0x3663bc,void*);if(!readable(b,client,4))return 1;
 u8*info=((FInfo)(engine+0x14dd00))(client,0);
 if(!readable(b,info,0x314))return 1;
 /* Reject a stale binding before the original renderer dereferences its Pawn. */
 return PTR(info,0x204,u8*)!=PTR(context,8,u8*);
}
void preview_finish(u8*context){if(preview_caption_block() && context==active_context){caption_scope=0;active_context=0;}}
int preview_caption_filter(void){int block=preview_caption_block();if(block)caption_hits++;return block;}
__attribute__((naked)) void caption_gate(void){__asm__ __volatile__(
 "pushfl\n pushal\n call preview_caption_filter\n test %eax,%eax\n jz 1f\n popal\n popfl\n ret $40\n1: popal\n popfl\n jmp caption_trampoline\n");}
__attribute__((naked)) void render_entry_gate(void){__asm__ __volatile__(
 "pushfl\n pushal\n push 40(%esp)\n call preview_nested_block\n add $4,%esp\n test %eax,%eax\n jz 1f\n popal\n popfl\n mov $1,%eax\n ret\n1: popal\n popfl\n jmp render_entry_trampoline\n");}
static int patch_bytes(u8*b,u8*destination,const u8*bytes,u32 count){
 u32 old=0,restore=0;if(!PTR(b,IAT_VirtualProtect,FProtect)(destination,count,0x40,&old))return 0;
 for(u32 i=0;i<count;i++)destination[i]=bytes[i];
 PTR(b,IAT_VirtualProtect,FProtect)(destination,count,old,&restore);
 PTR(b,IAT_FlushInstructionCache,FFlush)(PTR(b,IAT_GetCurrentProcess,FProcess)(),destination,count);return 1;
}

#include "engine_patterns.h"
static u8* checked_engine(u8*b,u32 rva,const u8*sig,const u8*mask){
 u8*e=PTR(b,ENGINE_MODULE_RVA,u8*)+rva;if(!readable(b,e,24))return 0;
 for(int i=0;i<24;i++)if(mask[i] && e[i]!=sig[i])return 0;return e;
}
typedef int (__attribute__((stdcall)) *FWndProc)(void*,u32,u32,u32);
typedef int (__attribute__((stdcall)) *FCallWnd)(void*,void*,u32,u32,u32);
typedef i32 (__attribute__((stdcall)) *FSetWnd)(void*,i32,i32);
typedef i32 (__attribute__((stdcall)) *FGetWnd)(void*,i32);
static u8*wheel_context;static void*wheel_hwnd;static void*wheel_previous;static u8*wheel_base;static int wheel_x,wheel_y,wheel_w,wheel_h,wheel_remainder,wheel_status;
static int __attribute__((stdcall)) preview_wndproc(void*hwnd,u32 msg,u32 wp,u32 lp){
 u8*b=wheel_base;struct State*s=&viewer_state;
 if(b && msg==0x20a && hwnd==wheel_hwnd && s->ready && readable(b,wheel_context,12) && PTR(wheel_context,4,void*) && PTR(wheel_context,8,u32)==s->actor && (u32)(PTR(b,IAT_GetTickCount,FZero)()-s->last_draw)<200){
  Point pt={(short)(lp&65535),(short)(lp>>16)};
  if(PTR(b,IAT_ScreenToClient,FScreen)(hwnd,&pt) && pt.x>=wheel_x && pt.x<wheel_x+wheel_w && pt.y>=wheel_y && pt.y<wheel_y+wheel_h){
   wheel_remainder+=(short)(wp>>16);
   while(wheel_remainder>=120){s->zoom*=0.90f;wheel_remainder-=120;}
   while(wheel_remainder<=-120){s->zoom/=0.90f;wheel_remainder+=120;}
   if(s->zoom<0.60f)s->zoom=0.60f;if(s->zoom>1.60f)s->zoom=1.60f;
   return 0;
  }
 }
 return b&&wheel_previous?PTR(b,IAT_CallWindowProcW,FCallWnd)(wheel_previous,hwnd,msg,wp,lp):0;
}
static void wheel_update(u8*b,void*hwnd,int x,int y,int w,int h){
 wheel_base=b;wheel_x=x;wheel_y=y;wheel_w=w;wheel_h=h;
 if(!hwnd||wheel_hwnd==hwnd)return;
 if(wheel_hwnd && wheel_previous){
  /* Do not unlink a newer subclass installed by another module. */
  if((u32)PTR(b,IAT_GetWindowLongW,FGetWnd)(wheel_hwnd,-4)!=(u32)preview_wndproc)return;
  PTR(b,IAT_SetWindowLongW,FSetWnd)(wheel_hwnd,-4,(i32)wheel_previous);
 }
 i32 old=PTR(b,IAT_SetWindowLongW,FSetWnd)(hwnd,-4,(i32)preview_wndproc);
 if(old){wheel_hwnd=hwnd;wheel_previous=(void*)old;wheel_status=1;}else wheel_status=-1;
}
/* Draw attached mesh actors in the same actor-scene pass, rather than copying
   their mesh or altering the original world attachment/animation. */
extern u8 dynamic_trampoline[];
__asm__(".pushsection .text\n.p2align 4\n.global dynamic_trampoline\ndynamic_trampoline:\n.fill 32,1,0x90\n.popsection\n");
typedef void (__attribute__((thiscall)) *FDynamicRender)(void*,void*,void*,void*,void*);
typedef void* (__attribute__((thiscall)) *FRenderData)(void*);
static u32 attachment_frames;static FRenderData render_data;static int attachment_status,attachment_count,attachment_drawn,attachment_busy;
void __attribute__((thiscall)) preview_dynamic(void*self,void*scene,void*lights,void*projectors,void*ri){
 ((FDynamicRender)dynamic_trampoline)(self,scene,lights,projectors,ri);
 u8*b=caption_base;struct State*s=&viewer_state;
 if(!b||!preview_caption_block()||attachment_busy||!readable(b,self,4)||PTR(self,0,u32)!=s->actor)return;
 attachment_frames++;attachment_count=0;attachment_drawn=0;
 u8*pawn=(u8*)s->actor;int off=s->attached_off;
 if(off<0x34||off>0x4000||!readable(b,pawn+off,12))return;
 u8**items=PTR(pawn,off,u8**);int n=PTR(pawn,off+4,int),cap=PTR(pawn,off+8,int);
 if(n<0||n>32||cap<n||cap>4096||!readable(b,items,n*4))return;
 attachment_count=n;attachment_busy=1;u8*seen[32];int used=0;
 for(int i=0;i<n;i++){
  u8*a=items[i];if(a==pawn||!readable(b,a,0x400))continue;
  int duplicate=0;for(int j=0;j<used;j++)if(seen[j]==a)duplicate=1;if(duplicate)continue;seen[used++]=a;
  if(s->base_off<0x34||s->base_off>0x4000||!readable(b,a+s->base_off,4)||PTR(a,s->base_off,u8*)!=pawn)continue;
  if(s->mesh_off<0x34||s->mesh_off>0x4000||!readable(b,a+s->mesh_off,4)||!PTR(a,s->mesh_off,void*))continue;
  if(!render_data)continue;void*d=render_data(a);if(!readable(b,d,4)||PTR(d,0,u8*)!=a)continue;
  ((FDynamicRender)dynamic_trampoline)(d,scene,lights,projectors,ri);attachment_drawn++;
 }
 attachment_busy=0;
}
static void attachments_install(u8*b){
 if(attachment_status)return;
 u8*target=checked_engine(b,ENGINE_DYNAMIC_RVA,engine_dynamic_sig,engine_dynamic_mask);
 u8*data=checked_engine(b,ENGINE_RENDERDATA_RVA,engine_renderdata_sig,engine_renderdata_mask);
 if(!target||!data){attachment_status=-1;return;}
 u8 tramp[13];for(int i=0;i<8;i++)tramp[i]=target[i];tramp[8]=0xe9;*(u32*)(tramp+9)=(u32)(target+8)-(u32)(dynamic_trampoline+13);
 if(!patch_bytes(b,dynamic_trampoline,tramp,13)){attachment_status=-2;return;}
 u8 jump[8]={0xe9,0,0,0,0,0x90,0x90,0x90};*(u32*)(jump+1)=(u32)preview_dynamic-(u32)(target+5);
 render_data=(FRenderData)data;attachment_status=patch_bytes(b,target,jump,8)?1:-2;
}
static void caption_begin(u8*b,u8*context){
 caption_base=b;
 if(!caption_attempted){
  caption_attempted=1;u8*engine=PTR(b,ENGINE_MODULE_RVA,u8*);
  u8*entry=PTR(b,IAT_GetProcAddress,FProc)(engine,"?DrawTargetName@UCanvas@@UAEXPAVFLevelSceneNode@@PAVFRenderInterface@@VFVector@@KPAUUser@@W4TargetRenderType@@W4L2FontType@@K@Z");
  if(!entry)entry=checked_engine(b,ENGINE_CAPTION_RVA,engine_caption_sig,engine_caption_mask);
  if(readable(b,entry,8)){
   u8*target=entry;if(target[0]==0xe9)target+=5+*(i32*)(target+1);
   const u8 prefix[8]={0x6a,0xff,0x64,0xa1,0,0,0,0};int valid=readable(b,target,8) && target>=engine && target<engine+0x2000000;
   if(valid)for(int i=0;i<8;i++)if(target[i]!=prefix[i])valid=0;
   if(valid){
    u8 trampoline[13];for(int i=0;i<8;i++)trampoline[i]=target[i];trampoline[8]=0xe9;*(u32*)(trampoline+9)=(u32)(target+8)-(u32)(caption_trampoline+13);
    if(patch_bytes(b,caption_trampoline,trampoline,13)){
     u8 jump[8]={0xe9,0,0,0,0,0x90,0x90,0x90};*(u32*)(jump+1)=(u32)caption_gate-(u32)(target+5);
     caption_status=patch_bytes(b,target,jump,8)?1:-3;
    }else caption_status=-3;
   }else caption_status=-2;
  }else caption_status=-1;
 }
 attachments_install(b);
 caption_thread=PTR(b,IAT_GetCurrentThreadId,FThread)();active_context=context;caption_scope=1;
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
 while(n&&path[n-1]!=92&&path[n-1]!='/')n--;
 const char*name="Inventory_Viewr_R20.log";for(u32 i=0;name[i];i++)path[n++]=name[i];path[n]=0;
 char line[640];Vec cam=PTR(fp,-0x54,Vec);i32 locoff=PTR(b,0x14550,i32);Vec loc=PTR(actor,locoff,Vec);
 int len=PTR(b,IAT__snprintf,FPrint)(line,639,"R20 frame=%u actor=%08X viewport=%dx%d loc=%.2f,%.2f,%.2f cam=%.2f,%.2f,%.2f yaw=%u ready=%d bboxZ=%.2f,%.2f bboxY=%.2f,%.2f body=%d headZ=%.2f feetZ=%.2f,%.2f xy=%d,%d live=%d init=%u boneOff=%d,%d,%d caption=%d boxFlag=%u meshOff=%d rotOff=%d match=%08X coords=%08X attach=%d,%d,%d mods=%08X,%08X attachedOff=%d baseOff=%d wheel=%d zoom=%.2f nameCalls=%u attachPasses=%u\r\n",diagnostic_lines,(u32)actor,PTR(fp,-16,int),PTR(fp,-20,int),(double)loc.x,(double)loc.y,(double)loc.z,(double)cam.x,(double)cam.y,(double)cam.z,PTR(fp,-0x78,u32),viewer_state.ready,(double)PTR(fp,-0x68,float),(double)PTR(fp,-0x5c,float),(double)PTR(fp,-0x6c,float),(double)PTR(fp,-0x60,float),body_sample.source,(double)body_sample.head.z,(double)body_sample.left.z,(double)body_sample.right.z,PTR(fp,-8,int),PTR(fp,-12,int),viewer_state.source_match,viewer_state.init,viewer_state.equip_offsets[20],viewer_state.equip_offsets[21],viewer_state.equip_offsets[22],caption_status,PTR(fp,-0x58,u32)&255,viewer_state.mesh_off,viewer_state.rotation_off,(u32)viewer_state.match_bone,(u32)viewer_state.bone_coords,attachment_status,attachment_count,attachment_drawn,PTR(b,ENGINE_MODULE_RVA,u32),PTR(b,NWINDOW_MODULE_RVA,u32),viewer_state.attached_off,viewer_state.base_off,wheel_status,(double)viewer_state.zoom,caption_hits,attachment_frames);
 if(len<=0||len>=640)return;
 void*h=PTR(b,IAT_CreateFileW,FCreateFile)(path,4,3,0,4,0x80,0);if((u32)h==0xffffffff)return;
 u32 written=0;PTR(b,IAT_WriteFile,FWrite)(h,line,(u32)len,&written,0);PTR(b,IAT_CloseHandle,FClose)(h);
}

void viewer_tick(u8 *b,u8 *fp){
 struct State *s=&viewer_state;u8 *ctx=PTR(fp,8,u8*);if(!root_matches(b,ctx))return;
 u8 *actor=PTR(ctx,8,u8*);if(!readable(b,actor,0x38))return;
 caption_begin(b,ctx);resolve(b);u32 now=PTR(b,IAT_GetTickCount,FZero)();
 if(s->actor!=(u32)actor){s->actor=(u32)actor;s->ready=0;s->yaw=0;s->turn_step=0;s->drag=0;s->down=0;s->last_refresh=now-100;s->profile_since=now;s->source_match=0;s->current_mesh=0;s->zoom=1.0f;wheel_remainder=0;diagnostic_lines=0;diagnostic_last=0;}
 if((u32)(now-s->last_draw)>500){s->drag=0;s->down=1;s->turn_step=0;s->yaw=0;s->ready=0;s->zoom=1.0f;wheel_remainder=0;s->profile_since=now;diagnostic_lines=0;diagnostic_last=0;}
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
 if(valid){wheel_context=ctx;wheel_update(b,hwnd,x,y,w,h);}
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
  /* Prefer head/feet when available; otherwise validate cached world-space
     geometry independently of the optional skeletal APIs and FBox flag. */
  float miny=PTR(fp,-0x6c,float),maxy=PTR(fp,-0x60,float);
  float minz=PTR(fp,-0x68,float),maxz=PTR(fp,-0x5c,float);
  float center_x=0;
  int bones=0;
  float height=maxz-minz,width=maxy-miny;
  /* The live client supplies numeric cached bounds even when FBox.IsValid
     is clear. Never gate this independent profile on skeletal API resolution.
     Accept only a plausible body-sized box surrounding this actor's location;
     reject NaNs, partial loading boxes and stale boxes from another location. */
  int cached_body=height>25.0f && height<250.0f && width>5.0f && width<300.0f && minz-loc.z>-160.0f && minz-loc.z<30.0f && maxz-loc.z>5.0f && maxz-loc.z<200.0f && (miny+maxy)*0.5f-loc.y>-150.0f && (miny+maxy)*0.5f-loc.y<150.0f;
  if(!cached_body){
   bones=body_bounds(b,actor,loc,&miny,&maxy,&minz,&maxz,&center_x);
   height=maxz-minz;width=maxy-miny;
  }
  if((bones || cached_body) && height>20.0f && height<1000.0f && width>1.0f && width<2000.0f && minz-loc.z>-500.0f && maxz-loc.z<1000.0f){
   float fit_height=height*0.5f*(float)w/(0.22169466f*0.77f*(float)(h-64));
   float fit_width=width*0.5f*(float)w/(0.22169466f*(float)(w-32));
   s->distance=fit_height>fit_width?fit_height:fit_width;
   float center_y=(miny+maxy)*0.5f-loc.y;
   s->center_x=center_x*baseco+center_y*basesi;s->center_y=-center_x*basesi+center_y*baseco;
   s->center_z=minz-loc.z+(0.835f-0.5f)*(float)h*2.0f*0.22169466f*s->distance/(float)w;
   s->body_h=height*0.5f;s->ready=1;
   if(!bones)body_sample.source=(PTR(fp,-0x58,u32)&255)?2:3;
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
 float distance=s->distance*s->zoom;
 float co,si;trig_yaw(actor_yaw+s->yaw,&co,&si);
 Vec *cam=(Vec*)(fp-0x54);cam->x=loc.x+s->center_x*baseco-s->center_y*basesi+distance*co;cam->y=loc.y+s->center_x*basesi+s->center_y*baseco+distance*si;cam->z=loc.z+s->center_z+(distance-s->distance)*(0.835f-0.5f)*(float)h*2.0f*0.22169466f/(float)w;
 PTR(fp,-0x7c,i32)=0;PTR(fp,-0x78,i32)=(actor_yaw+s->yaw+32768)&65535;PTR(fp,-0x74,i32)=0;
 diagnostic(b,actor,fp);
}
