from pathlib import Path
import sys,struct,json,math
R=Path(__file__).resolve().parent;P=R.parent;sys.path[:0]=[str(P/'test-vendor'),'/workspace/work/cloak-audit/vendor'];import pefile
from unicorn import *
from unicorn.x86_const import *
meta=json.loads((R/'metadata.json').read_text());results=[]
for base in [0x10000000,0x10100000,0x10340000,0x33000000,0x6fba0000,0x500000]:
 for race,height in [(0,75.0),(4,55.0),(3,89.0)]:
  p=pefile.PE(str(P/'TESTE16/system/MileniumPortraitNative.dll'));p.relocate_image(base);u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(base,0x188000);u.mem_write(base,p.get_memory_mapped_image());u.mem_map(0x2000000,0x100000);u.mem_map(0xf000000,0x10000);u.mem_map(0x40000000,0x380000)
  def put(a,v):u.mem_write(a,struct.pack('<I',v&0xffffffff))
  def get(a):return struct.unpack('<I',u.mem_read(a,4))[0]
  def fput(a,v):u.mem_write(a,struct.pack('<f',v))
  def fg(a):return struct.unpack('<f',u.mem_read(a,4))[0]
  def wide(a):
   text=[]
   while (c:=struct.unpack('<H',u.mem_read(a,2))[0]):text.append(chr(c));a+=2
   return ''.join(text)
  actor=0x2002000;ctx=0x2001000;info=0x2004000;fp=0xf008000;mesh=0x200d500
  for a,v in [(ctx+4,0x2005000),(ctx+8,actor),(0x20050b8,0x2006000),(0x200600c,0x2007000),(0x2007004,0x2008000),(base+0x17dd8,0x40000000),(base+0x14550,0x100),(0x403663bc,0x2009000),(info+0x18,100),(info+0x204,actor),(info+0x4c,race),(info+0x50,0),(info+0x54,1),(actor+0x140,mesh),(actor+0x440,101),(actor+0x450,102),(actor+0x460,103),(fp+8,ctx),(fp-8,50),(fp-12,50),(fp-16,270),(fp-20,429)]:put(a,v)
  for off,v in [(0x100,100),(0x104,200),(0x108,300)]:fput(actor+off,v)
  clock={'now':1000,'x':100,'y':100,'down':False,'root':True};stubs={};counter=[0]
  def stub(name,pop=0):
   address=0x2020000+counter[0]*16;counter[0]+=1;stubs[address]=(name,pop);return address
  needed={'GetTickCount':0,'GetProcAddress':8,'GetForegroundWindow':0,'GetWindowThreadProcessId':8,'GetCurrentProcessId':0,'GetCursorPos':4,'GetAsyncKeyState':4,'ScreenToClient':8,'GetModuleFileNameW':12,'CreateFileW':28,'WriteFile':20,'CloseHandle':4,'_snprintf':0}
  for d in p.DIRECTORY_ENTRY_IMPORT:
   for imp in d.imports:
    if imp.name and imp.name.decode() in needed:put(imp.address,stub(imp.name.decode(),needed[imp.name.decode()]))
  match=stub('match',4);bone=stub('bone',8)
  hooks={base+0x1000:('read',0),base+0x141e:('offset',0),base+0x1f0b:('name',0),base+0x10cb:('log',0),base+0x5e04:('FORBIDDEN_COPY',0),0x4014dd00:('info',0),**stubs}
  def hook(uc,address,size,ud):
   if address not in hooks:return
   name,pop=hooks[address];sp=uc.reg_read(UC_X86_REG_ESP);arg=lambda i:get(sp+4+4*i);ret=1
   if name=='read':ret=int((0x2000000<=arg(0)<0x2100000)or(0x40000000<=arg(0)<0x40380000))
   elif name=='offset':ret={'Mesh':0x140,'Rotation':0x180,'HeadBone':0x440,'RightFootBone':0x450,'LeftFootBone':0x460}[wide(arg(1))]
   elif name=='GetProcAddress':
    text=bytes(uc.mem_read(arg(1),120)).split(b'\0')[0]
    assert b'ProcessEvent' not in text,'Must not invoke forced-pose events on the real actor'
    ret=match if b'MatchRefBone' in text else bone if b'GetBoneCoordsWithBoneIndex' in text else 0
   elif name=='match':assert uc.reg_read(UC_X86_REG_ECX)==mesh;ret={101:0,102:1,103:2}[arg(0)]
   elif name=='bone':
    assert uc.reg_read(UC_X86_REG_ECX)==actor
    x,y,z=fg(actor+0x100),fg(actor+0x104),fg(actor+0x108);yaw=get(actor+0x184)*2*math.pi/65536
    lx,ly,lz=[(2,1,height-5),(0,-4,-4),(0,4,-5)][arg(1)]
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
   for off,v in [(-0x54,180),(-0x50,200),(-0x4c,320)]:fput(fp+off,v)
   put(fp-0x78,32768);put(fp-0x58,0)
   original=bytes(u.mem_read(actor,0x600));sp=0xf00f000;put(sp,0x202f000);put(sp+4,base);put(sp+8,fp);u.reg_write(UC_X86_REG_ESP,sp)
   u.emu_start(base+meta['symbols']['viewer_tick']-0x10000000,0x202f000,count=12000)
   assert u.reg_read(UC_X86_REG_EIP)==0x202f000
   assert bytes(u.mem_read(actor,0x600))==original,'World actor changed'
   assert get(ctx+8)==actor
  run();clock['now']=1300;run();initial=[fg(fp-0x54+i*4) for i in range(3)];assert get(fp-0x78)==32768
  distance=initial[0]-101;foot=214.5+(initial[2]-294)*270/(2*0.22169466)/distance;assert abs(foot-429*.835)<0.03,foot
  # Source item/animation state is already the renderer's input; no copied proxy.
  put(actor+0x300,0x12345678);put(actor+0x500,0x87654321);put(actor+0x510,25);clock['now']=1400;run();assert [fg(fp-0x54+i*4) for i in range(3)]==initial
  # Real movement and facing do not turn the preview sideways: camera follows heading.
  put(actor+0x184,16384);fput(actor+0x100,110);fput(actor+0x104,205);clock['now']=1410;run();assert get(fp-0x78)==49152;assert abs(fg(fp-0x54)-109.5)<0.02 and abs(fg(fp-0x50)-(206+distance))<0.03
  clock.update(now=1420,down=True,x=230,y=450);run();assert get(fp-0x78)==51882
  clock.update(now=1430,down=False);run()
  # Reset front after reopening, then exercise exact 24-click wrap.
  clock.update(now=2100,down=False);run();assert get(fp-0x78)==49152
  for i in range(24):
   clock.update(now=2110+i*20,down=True,x=230,y=450);run();clock.update(now=2120+i*20,down=False);run()
  assert get(fp-0x78)==49152
  # Holding preserves the accelerated control, and never changes world facing.
  clock.update(now=2600,down=True,x=230,y=450);run();start_yaw=get(fp-0x78)
  for tick in range(2610,2910,10):clock.update(now=tick);run()
  assert get(fp-0x78)!=start_yaw and get(actor+0x184)==16384
  clock.update(now=2920,down=False);run()
  before=bytes(u.mem_read(actor,0x600));clock['root']=False;run();assert bytes(u.mem_read(actor,0x600))==before
  # Detour and ABI after the real PE relocation, preserving world state and registers.
  clock['root']=True;sp=0xf00f000;u.reg_write(UC_X86_REG_ESP,sp);u.reg_write(UC_X86_REG_EBP,fp);u.reg_write(UC_X86_REG_EBX,12345);u.emu_start(base+0x4a20,base+0x4a26,count=15000)
  assert u.reg_read(UC_X86_REG_EIP)==base+0x4a26 and u.reg_read(UC_X86_REG_EAX)==ctx+8 and u.reg_read(UC_X86_REG_EBX)==12345 and u.reg_read(UC_X86_REG_ESP)==sp
  assert bytes(u.mem_read(actor,0x600))==before
  results.append({'base':hex(base),'race_fixture':race,'simulated_body_height':height,'source_is_rendered_actor':True,'world_actor_unchanged':True,'no_appearance_copy_or_pose_override':True,'front_relative_to_actor_facing':True,'feet_pixel':foot,'initial_camera':initial,'arrow_rotation':True,'actual_relocated_detour':True})
(P/'TESTE16/VALIDACAO.json').write_text(json.dumps({'windows_game_run':False,'engine_calls_mocked':True,'results':results},indent=2));print('PASS',len(results),'real-actor cases across 6 relocated bases')
