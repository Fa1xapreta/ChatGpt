from pathlib import Path
import sys,struct,json,math
R=Path(__file__).resolve().parent;P=R.parent;sys.path[:0]=[str(P/'test-vendor'),'/workspace/work/cloak-audit/vendor'];import pefile
from unicorn import *
from unicorn.x86_const import *
meta=json.loads((R/'metadata.json').read_text());results=[]
for base in [0x10000000,0x10100000,0x10340000,0x33000000,0x6fba0000,0x500000]:
 for sample in json.loads((R/'log_samples.json').read_text()):
  race,height=4,sample['z'][1]-sample['z'][0]
  p=pefile.PE(str(P/'TESTE20/system/MileniumPortraitNative.dll'));p.relocate_image(base);u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(base,0x190000);u.mem_write(base,p.get_memory_mapped_image());u.mem_map(0x2000000,0x100000);u.mem_map(0xf000000,0x10000);u.mem_map(0x40000000,0x380000);u.mem_map(0x50000000,0x380000)
  def put(a,v):u.mem_write(a,struct.pack('<I',v&0xffffffff))
  def get(a):return struct.unpack('<I',u.mem_read(a,4))[0]
  def fput(a,v):u.mem_write(a,struct.pack('<f',v))
  def fg(a):return struct.unpack('<f',u.mem_read(a,4))[0]
  def wide(a):
   text=[]
   while (c:=struct.unpack('<H',u.mem_read(a,2))[0]):text.append(chr(c));a+=2
   return ''.join(text)
  actor=0x2002000;ctx=0x2001000;info=0x2004000;fp=0xf008000;mesh=0x200d500
  for a,v in [(ctx+4,0x2005000),(ctx+8,actor),(0x20050b8,0x2006000),(0x200600c,0x2007000),(0x2007004,0x2008000),(base+0x17dd4,0x50000000),(base+0x17dd8,0x40000000),(base+0x17e24,0),(base+0x14550,0x100),(0x403663bc,0x2009000),(info+0x18,100),(info+0x204,actor),(info+0x4c,race),(info+0x50,0),(info+0x54,1),(actor+0x140,mesh),(actor+0x440,0),(actor+0x450,0),(actor+0x460,0),(fp+8,ctx),(fp-8,50),(fp-12,50),(fp-16,270),(fp-20,429)]:put(a,v)
  for off,v in [(0x100,100),(0x104,200),(0x108,300)]:fput(actor+off,v)
  clock={'now':1000,'x':100,'y':100,'down':False,'root':True,'classes_ready':False,'thread':77,'skeletal':True};stubs={};counter=[0]
  def stub(name,pop=0):
   address=0x2020000+counter[0]*16;counter[0]+=1;stubs[address]=(name,pop);return address
  needed={'GetTickCount':0,'GetProcAddress':8,'GetForegroundWindow':0,'GetWindowThreadProcessId':8,'GetCurrentProcessId':0,'GetCursorPos':4,'GetAsyncKeyState':4,'ScreenToClient':8,'GetModuleFileNameW':12,'CreateFileW':28,'WriteFile':20,'CloseHandle':4,'_snprintf':0,'GetCurrentThreadId':0,'VirtualProtect':16,'GetCurrentProcess':0,'FlushInstructionCache':12,'CallWindowProcW':20,'GetWindowLongW':8,'SetWindowLongW':12,'GetModuleHandleW':4}
  for d in p.DIRECTORY_ENTRY_IMPORT:
   for imp in d.imports:
    if imp.name and imp.name.decode() in needed:put(imp.address,stub(imp.name.decode(),needed[imp.name.decode()]))
  match=stub('match',4);bone=stub('bone',8);find=stub('find');make_name=stub('make_name',8);static_class=stub('static_class');put(base+0x17de4,find)
  assert get(base+0x17e30)==0 # A new account has never initialized the optional HUD-Pawn class.
  u.mem_map(0,0x1000);put(0,0x12345678)
  export=0x50010100;caption_target=0x5024e640;u.mem_write(export,b'\xe9'+struct.pack('<i',caption_target-export-5));u.mem_write(caption_target,bytes.fromhex('6aff64a100000000')+b'\x90'*16)
  hooks={base+0x1000:('read',0),base+0x141e:('offset',0),base+0x135a:('isa',0),base+0x1f0b:('name',0),base+0x10cb:('log',0),base+0x5e04:('FORBIDDEN_COPY',0),0x4014dd00:('info',0),**stubs}
  naming={0:['Bip01 Head','Bip01 R Foot','Bip01 L Foot'],4:['Bip01_head','bip01_r_foot','bip01_l_foot'],3:['Head','R_Foot','L_Foot']}[race]
  caption_calls=[];bone_calls=[]
  def hook(uc,address,size,ud):
   if address==caption_target+8:
    sp=uc.reg_read(UC_X86_REG_ESP);assert get(sp)==0xffffffff;caption_calls.append(True);uc.reg_write(UC_X86_REG_EIP,get(sp+4));uc.reg_write(UC_X86_REG_ESP,sp+48);return
   if address not in hooks:return
   name,pop=hooks[address];sp=uc.reg_read(UC_X86_REG_ESP);arg=lambda i:get(sp+4+4*i);ret=1
   if name=='read':ret=int((0x2000000<=arg(0)<0x2100000)or(0x40000000<=arg(0)<0x40380000)or(0x50000000<=arg(0)<0x50380000))
   elif name=='offset':
    field=wide(arg(1));assert arg(0)==(0x200c100 if field in ['Mesh','Rotation','Attached','Base'] else 0x200c200),'Property owner must be a resolved class, never null'
    ret={'Mesh':0x140,'Rotation':0x180,'Attached':0x480,'Base':0x490,'LoadingResourceRef':0x470,'HeadBone':0x440,'RightFootBone':0x450,'LeftFootBone':0x460}[field]
   elif name=='find':
    text=wide(arg(2));ret={'Engine.Actor':0x200c100,'Engine.Pawn':0x200c200,'Engine.SkeletalMesh':0x200c300}.get(text,0) if clock['classes_ready'] else 0
   elif name=='isa':assert arg(1)==0x200c300;ret=int(clock['skeletal'])
   elif name=='SetWindowLongW':ret=0x12340000
   elif name=='CallWindowProcW':ret=99
   elif name=='GetWindowLongW':ret=0
   elif name=='GetCurrentThreadId':ret=clock['thread']
   elif name=='VirtualProtect':put(arg(3),0x20);ret=1
   elif name=='GetCurrentProcess':ret=0xffffffff
   elif name=='FlushInstructionCache':ret=1
   elif name=='GetModuleHandleW':assert wide(arg(0))=='Core.dll';ret=0x60000000
   elif name=='GetProcAddress':
    text=bytes(uc.mem_read(arg(1),120)).split(b'\0')[0]
    assert arg(0)==(0x60000000 if b'StaticClass@UClass' in text or text.startswith(b'??0FName') else 0x50000000),(text,hex(arg(0)))
    assert b'ProcessEvent' not in text,'Must not invoke forced-pose events on the real actor'
    ret=match if b'MatchRefBone' in text else bone if b'GetBoneCoordsWithBoneIndex' in text else static_class if b'StaticClass@UClass' in text else make_name if text.startswith(b'??0FName') else 0
   elif name=='static_class':ret=0x200c000
   elif name=='make_name':
    assert arg(1)==0;value=(101+naming.index(wide(arg(0)))) if wide(arg(0)) in naming else 0;put(uc.reg_read(UC_X86_REG_ECX),value);ret=uc.reg_read(UC_X86_REG_ECX)
   elif name=='match':assert uc.reg_read(UC_X86_REG_ECX)==mesh;ret={101:6,102:9,103:15}[arg(0)]
   elif name=='bone':
    bone_calls.append(True)
    assert uc.reg_read(UC_X86_REG_ECX)==actor
    x,y,z=fg(actor+0x100),fg(actor+0x104),fg(actor+0x108);yaw=get(actor+0x184)*2*math.pi/65536
    lx,ly,lz={6:(2,1,height-5),9:(0,-4,-4),15:(0,4,-5)}[arg(1)]
    origin=(x+lx*math.cos(yaw)-ly*math.sin(yaw),y+lx*math.sin(yaw)+ly*math.cos(yaw),z+lz)
    uc.mem_write(arg(0),struct.pack('<12f',*origin,*([0]*9)));ret=arg(0)
   elif name=='info':ret=info
   elif name=='name':ret=int(clock['root'])
   elif name=='GetTickCount':ret=clock['now']
   elif name=='GetForegroundWindow':ret=55
   elif name=='GetWindowThreadProcessId':put(arg(1),77)
   elif name=='GetCurrentProcessId':ret=77
   elif name=='GetAsyncKeyState':ret=0x8000 if clock['down'] else 0
   elif name=='GetCursorPos':put(arg(0),clock['x']);put(arg(0)+4,clock['y'])
   elif name=='GetModuleFileNameW':ret=0
   elif name=='FORBIDDEN_COPY':raise AssertionError('Appearance-copy helper was called for the world actor')
   uc.reg_write(UC_X86_REG_EAX,ret);uc.reg_write(UC_X86_REG_ESP,sp+4+pop);uc.reg_write(UC_X86_REG_EIP,get(sp))
  u.hook_add(UC_HOOK_CODE,hook)
  def run():
   # The original renderer prepares its fitted view before the extension runs.
   for off,v in zip([-0x54,-0x50,-0x4c],[sample['loc'][0]+114.77,sample['loc'][1],sample['loc'][2]+45]):fput(fp+off,v)
   put(fp-0x78,32768);put(fp-0x58,0)
   for off,v in zip([-0x68,-0x5c,-0x6c,-0x60],sample['z']+sample['y']):fput(fp+off,v)
   original=bytes(u.mem_read(actor,0x600));sp=0xf00f000;put(sp,0x202f000);put(sp+4,base);put(sp+8,fp);u.reg_write(UC_X86_REG_ESP,sp)
   u.emu_start(base+meta['symbols']['viewer_tick']-0x10000000,0x202f000,count=12000)
   assert u.reg_read(UC_X86_REG_EIP)==0x202f000
   assert bytes(u.mem_read(actor,0x600))==original,'World actor changed'
   assert get(ctx+8)==actor
   sp=0xf00f000;u.reg_write(UC_X86_REG_ESP,sp);u.reg_write(UC_X86_REG_EBP,fp);u.emu_start(base+0x4cb5,base+0x4cbb,count=5000);assert u.reg_read(UC_X86_REG_ESP)==sp
  for i,v in enumerate(sample['loc']):fput(actor+0x100+i*4,v)
  clock.update(now=1600,classes_ready=True);run()
  cam=[fg(fp-0x54+i*4) for i in range(3)]
  ready=get(base+meta['symbols']['viewer_state']-0x10000000+28)
  expected=height>25
  assert bool(ready)==expected,(sample,ready,cam)
  if expected:
   distance=cam[0]-sample['loc'][0]
   foot=214.5+(cam[2]-sample['z'][0])*270/(2*0.22169466*distance)
   head=214.5+(cam[2]-sample['z'][1])*270/(2*0.22169466*distance)
   assert abs(foot-429*.835)<0.2,(foot,cam)
   assert 20<head<200,(head,cam)
   assert not bone_calls
  else:foot=head=None
  results.append({'base':hex(base),'source_log_sample':sample,'skeletal_exports_available':True,'fbox_flag_zero':True,'framing_ready':bool(ready),'feet_pixel':foot,'top_pixel':head,'camera':cam,'loading_partial_box_rejected':not expected})
(P/'TESTE20/CAMERA_WITH_ENGINE_APIS.json').write_text(json.dumps({'windows_game_run':False,'engine_api_mocked':True,'measured_bounds_from_user_log':True,'results':results},indent=2));print('PASS',len(results),'cached camera cases with skeletal APIs available; approved profile retained')
