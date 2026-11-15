from pathlib import Path
import sys,struct,json
R=Path(__file__).resolve().parent;P=R.parent;sys.path[:0]=[str(P/'test-vendor'),'/workspace/work/cloak-audit/vendor'];import pefile
from unicorn import *
from unicorn.x86_const import *
meta=json.loads((R/'metadata.json').read_text());results=[]
for base in [0x10000000,0x10100000,0x10340000,0x33000000,0x6fba0000,0x500000]:
 p=pefile.PE(str(P/'TESTE15/system/MileniumPortraitNative.dll'));p.relocate_image(base);u=Uc(UC_ARCH_X86,UC_MODE_32);u.mem_map(base,0x188000);u.mem_write(base,p.get_memory_mapped_image());u.mem_map(0x2000000,0x100000);u.mem_map(0xf000000,0x10000);u.mem_map(0x40000000,0x380000)
 def put(a,v):u.mem_write(a,struct.pack('<I',v&0xffffffff))
 def get(a):return struct.unpack('<I',u.mem_read(a,4))[0]
 def fput(a,v):u.mem_write(a,struct.pack('<f',v))
 def fg(a):return struct.unpack('<f',u.mem_read(a,4))[0]
 def wide(a):
  r=[]
  while getword:=struct.unpack('<H',u.mem_read(a,2))[0]:r.append(chr(getword));a+=2
  return ''.join(r)
 ctx=0x2001000;actor=0x2002000;world=0x2003000;info=0x2004000;fp=0xf008000
 for a,v in [(ctx+4,0x2005000),(ctx+8,actor),(0x20050b8,0x2006000),(0x200600c,0x2007000),(0x2007004,0x2008000),(base+0x17dd8,0x40000000),(base+0x14550,0x100),(0x403663bc,0x2009000),(info+0x18,100),(info+0x204,world),(fp+8,ctx),(fp-8,50),(fp-12,50),(fp-16,270),(fp-20,429)]:put(a,v)
 for off,v in [(0x100,100),(0x104,200),(0x108,300),(0x120,23),(0x208,340)]:fput(actor+off,v)
 clock={'now':1000,'x':100,'y':100,'down':0,'copy':1,'root':True};calls=[];stubs={};n=[0]
 def stub(name,pop=0):
  a=0x2020000+n[0]*16;n[0]+=1;stubs[a]=(name,pop);return a
 for d in p.DIRECTORY_ENTRY_IMPORT:
  for imp in d.imports:
   if imp.name:
    name=imp.name.decode();pop={'GetTickCount':0,'GetProcAddress':8,'GetForegroundWindow':0,'GetWindowThreadProcessId':8,'GetCurrentProcessId':0,'GetCursorPos':4,'GetAsyncKeyState':4,'ScreenToClient':8,'GetModuleFileNameW':12,'CreateFileW':28,'WriteFile':20,'CloseHandle':4,'_snprintf':0}.get(name)
    if pop is not None:put(imp.address,stub(name,pop))
 find=stub('find');event=stub('event',12);match=stub('match',4);bone=stub('bone',8);put(base+0x17de4,find)
 hooks={base+0x1000:('read',0),base+0x141e:('offset',0),base+0x1f0b:('name',0),base+0x10cb:('log',0),base+0x5e04:('copy',0),0x4014dd00:('info',0),**stubs}
 def hook(uc,addr,size,ud):
  if addr not in hooks:return
  name,pop=hooks[addr];sp=uc.reg_read(UC_X86_REG_ESP);args=lambda i:get(sp+4+4*i);ret=1
  if name=='read':ret=int((0x2000000<=args(0)<0x2100000)or(0x40000000<=args(0)<0x40380000))
  elif name=='offset':
   field=wide(args(1)); names=['RightHandMesh','LeftHandMesh','RightArmMesh','LeftArmMesh','CloakMesh','CloakCoverMesh','RightHandSkins','LeftHandSkins','RightArmSkins','LeftArmSkins','CloakSkins','WeaponScale','RightHandBone','LeftHandBone','RightArmBone','LeftArmBone','SpineBone','LowbodyBone','CapeBone','AttackItemEnchantedValue','HeadBone','RightFootBone','LeftFootBone'];ret=0x140 if field=='Mesh' else (0x120 if field=='CollisionHeight' else (0x200 if field=='PortraitFocus' else 0x300+names.index(field)*16))
  elif name=='name':ret=int(clock['root'])
  elif name=='find':ret=0x200a000 if 'PrepareClassic' in wide(args(2)) else 0x200b000
  elif name=='GetProcAddress':
   text=bytes(uc.mem_read(args(1),140)).split(b'\0')[0];ret=match if b'MatchRefBone' in text else (bone if b'GetBoneCoordsWithBoneIndex' in text else event)
  elif name=='match':
   assert uc.reg_read(UC_X86_REG_ECX)==0x200d500
   ret={101:0,102:1,103:2}[args(0)]
  elif name=='bone':
   assert uc.reg_read(UC_X86_REG_ECX)==actor
   assert args(1) in [0,1,2];origin=[(100,200,354),(100,195,296),(100,205,295)][args(1)]
   uc.mem_write(args(0),struct.pack('<12f',*origin,*([0]*9)));ret=args(0)
  elif name=='GetModuleFileNameW':
   path='X:\\system\\MileniumPortraitNative.dll';uc.mem_write(args(1),(path+'\0').encode('utf-16le'));ret=len(path)
  elif name=='_snprintf':
   assert args(4)==actor;uc.mem_write(args(0),b'testlog\r\n');ret=9
  elif name=='CreateFileW':
   assert wide(args(0))=='X:\\system\\Inventory_Viewr_R15.log';ret=123
  elif name=='WriteFile':
   assert args(0)==123 and args(2)==9;put(args(3),9);ret=1
  elif name=='CloseHandle':assert args(0)==123
  elif name=='GetTickCount':ret=clock['now']
  elif name=='GetForegroundWindow':ret=55
  elif name=='GetWindowThreadProcessId':put(args(1),77)
  elif name=='GetCurrentProcessId':ret=77
  elif name=='GetAsyncKeyState':ret=0x8000 if clock['down'] else 0
  elif name=='GetCursorPos':put(args(0),clock['x']);put(args(0)+4,clock['y'])
  elif name=='info':ret=info
  elif name=='copy':calls.append('copy');ret=clock['copy']
  elif name=='event':
   assert uc.reg_read(UC_X86_REG_ECX)==actor
   calls.append('prepare' if args(0)==0x200a000 else 'pose')
   if args(0)==0x200a000:assert get(args(1))==world
  uc.reg_write(UC_X86_REG_EAX,ret);uc.reg_write(UC_X86_REG_ESP,sp+4+pop);uc.reg_write(UC_X86_REG_EIP,get(sp))
 u.hook_add(UC_HOOK_CODE,hook)
 def run():
  if clock['now']<1300:
   for off,val in [(-0x54,180),(-0x50,200),(-0x4c,320)]:fput(fp+off,val)
   put(fp-0x78,32768)
  sp=0xf00f000;put(sp,0x202f000);put(sp+4,base);put(sp+8,fp);u.reg_write(UC_X86_REG_ESP,sp);u.emu_start(base+meta['symbols']['viewer_tick']-0x10000000,0x202f000,count=10000);assert u.reg_read(UC_X86_REG_EIP)==0x202f000
 for off,val in [(-0x6c,170),(-0x60,230),(-0x68,295),(-0x5c,360),(-0x54,180),(-0x50,200),(-0x4c,320)]:fput(fp+off,val)
 put(fp-0x58,0) # Cached render-data bounds unavailable: explicit pose sampling must still work.
 put(actor+0x140,0x200d500);put(world+0x440,101);put(world+0x450,102);put(world+0x460,103)
 put(world+0x300,0x200d000);put(world+0x340,0x200d100);put(world+0x430,25);world_before=bytes(u.mem_read(world,0x500))
 run();assert get(actor+0x430)==25
 assert get(actor+0x300)==0x200d000 and get(actor+0x340)==0x200d100
 assert calls==['copy','prepare','pose'],calls
 # During initial resource loading preserve the engine camera exactly.
 assert [fg(fp-0x54+i*4) for i in range(3)]==[180.0,200.0,320.0]
 # Clicks are registered even before a geometry profile exists.
 clock.update(now=1010,down=1,x=230,y=450);run();assert get(base+meta['symbols']['viewer_state']-0x10000000+12)==2730
 assert get(fp-0x78)==35498,'Arrow clicks must rotate even before a pose profile is ready'
 clock.update(now=1020,down=0);run()
 clock.update(now=1030,down=1,x=120);run();assert get(base+meta['symbols']['viewer_state']-0x10000000+12)==0
 clock.update(now=1040,down=0);run()
 clock.update(now=1300,copy=2);run()
 initial=[fg(fp-0x54+i*4) for i in range(3)]
 distance=initial[0]-100.0;focal=270.0/(2*0.3152988)
 foot=429.0/2+(initial[2]-294.0)*focal/distance
 head=429.0/2+(initial[2]-361.08)*focal/distance
 assert abs(foot-0.80*429)<0.02 and head>24 and foot<389,(foot,head,initial)
 assert initial[2]>300.0,'View must not aim below the player as the R12 log did'

 # Different equipped bounds supplied by original renderer must not change camera.
 clock.update(now=1400,copy=2);fput(actor+0x120,100);fput(actor+0x208,307.33);fput(fp-0x5c,450);fput(fp-0x54,9999);run();assert [fg(fp-0x54+i*4) for i in range(3)]==initial;assert calls[-2:]==['copy','pose']
 # Body drag must never rotate. Only actual button rectangles accept a press.
 clock.update(now=1410,down=1,x=100,y=100);run();clock.update(now=1420,x=228);run();assert get(fp-0x78)==32768
 clock.update(now=1430,down=0);run();assert get(fp-0x78)==32768
 clock.update(now=1440,down=1,x=230,y=450);run();assert get(fp-0x78)==35498
 clock.update(now=1450);run();assert get(fp-0x78)==35498
 clock.update(now=1460,down=0);run();assert get(fp-0x78)==35498
 clock.update(now=1470,down=1,x=120);run();assert get(fp-0x78)==32768
 clock.update(now=1480,x=400);run();assert get(fp-0x78)==32768
 clock.update(now=1490,down=0);run();assert get(fp-0x78)==32768
 # Changes to hands/cape must propagate even when the armor cache reports unchanged.
 clock.update(now=1550,copy=2);put(world+0x300,0x200d200);put(world+0x340,0x200d300);world_before=bytes(u.mem_read(world,0x500));run();assert get(actor+0x300)==0x200d200 and get(actor+0x340)==0x200d300;assert calls[-3:]==['copy','prepare','pose']
 clock.update(now=1660);put(world+0x300,0);put(world+0x340,0);world_before=bytes(u.mem_read(world,0x500));run();assert get(actor+0x300)==0 and get(actor+0x340)==0
 before=len(calls);clock.update(root=False,now=1800);run();assert len(calls)==before
 assert bytes(u.mem_read(world,0x500))==world_before
 # Execute actual relocated detour and preserve incoming registers/stack.
 clock['root']=True;sp=0xf00f000;u.reg_write(UC_X86_REG_ESP,sp);u.reg_write(UC_X86_REG_EBP,fp);u.reg_write(UC_X86_REG_EBX,12345);u.emu_start(base+0x4a20,base+0x4a26,count=15000);assert u.reg_read(UC_X86_REG_EIP)==base+0x4a26;assert u.reg_read(UC_X86_REG_EAX)==ctx+8;assert u.reg_read(UC_X86_REG_EBX)==12345;assert u.reg_read(UC_X86_REG_ESP)==sp
 # Twenty-four separate clicks complete exactly one full turn; holding does not.
 for i in range(24):
  clock.update(now=1810+i*20,down=1,x=230,y=450);run()
  clock.update(now=1820+i*20,down=0);run()
 assert get(fp-0x78)==32768
 # Hold: no extra motion before 300 ms, 120 deg/s, then 180 deg/s.
 clock.update(now=2300,down=1,x=230,y=450);run();assert get(fp-0x78)==35498
 for tick in range(2310,2600,10):clock.update(now=tick);run()
 assert get(fp-0x78)==35498
 clock.update(now=2600);run()
 before=get(fp-0x78)
 clock.update(now=2610);run();slow=(get(fp-0x78)-before)&65535
 assert 217<=slow<=220,slow
 for tick in range(2620,3210,10):clock.update(now=tick);run()
 before=get(fp-0x78);clock.update(now=3210);run();fast=(get(fp-0x78)-before)&65535
 assert 326<=fast<=329 and fast>slow,fast
 before=get(fp-0x78);clock.update(now=3220,x=400);run();assert get(fp-0x78)==before
 clock.update(now=3230,down=0);run();assert get(fp-0x78)==before
 results.append({'base':hex(base),'fixed_camera':initial,'projected_feet_y':foot,'projected_head_y':head,'warmup_keeps_engine_camera':True,'bone_and_capsule_changes_do_not_affect_framing':True,'feet_anchor_unchanged':True,'equipment_refresh':True,'arrow_click_steps_15_degrees':True,'short_hold_preserves_click_step':True,'holding_accelerates':True,'clicks_processed_without_cached_bounds':True,'explicit_head_and_feet_api':True,'body_drag_does_not_rotate':True,'source_untouched':True,'actual_relocated_detour':True})
(P/'TESTE15/VALIDACAO.json').write_text(json.dumps({'windows_game_run':False,'engine_calls_mocked':True,'results':results},indent=2));print('PASS',len(results),'relocated bases')
