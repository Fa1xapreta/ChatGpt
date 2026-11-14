from pathlib import Path
R=Path(__file__).resolve().parent;OUT=R/'TESTE3'
prepare='''if(StatusWnd(GetScript("StatusWnd")).PortraitExpanded)
                        StatusWnd(GetScript("StatusWnd")).UpdateOptionalPortrait();
                    else
                    {
                        StatusWnd(GetScript("StatusWnd")).PortraitExpanded=True;
                        StatusWnd(GetScript("StatusWnd")).UpdateOptionalPortrait();
                        StatusWnd(GetScript("StatusWnd")).PortraitExpanded=False;
                        GetHandle("StatusWnd.HudPortraitViewport").HideWindow();
                    }
'''
bind='''TextBoxHandle(GetHandle("MileniumInventoryPreviewWnd.RenderStatus")).SetText("Render 3D: " $ string(class'MobPreviewNative'.static.BindPreview(GetHandle("MileniumInventoryPreviewWnd.MobViewport"),StatusWnd(GetScript("StatusWnd")).PortraitPawn,35.0,30.0,100.0)));'''
cleanup='''
    if(GetHandle("MileniumInventoryPreviewWnd").IsShowWindow())
    {
        class'MobPreviewNative'.static.ReleasePreview();
        GetHandle("MileniumInventoryPreviewWnd").HideWindow();
        if(!StatusWnd(GetScript("StatusWnd")).PortraitExpanded)
            StatusWnd(GetScript("StatusWnd")).ReleaseOptionalPortrait();
    }
'''
def appendbody(s,fn,extra):
 start=s.index('function '+fn+'(');op=s.index('{',start);depth=1;p=op+1
 while depth:
  depth+=(s[p]=='{')-(s[p]=='}');p+=1
 return s[:p-1]+extra+s[p-1:]
def replacebody(s,fn,body):
 start=s.index('function '+fn+'(');op=s.index('{',start);depth=1;p=op+1
 while depth:
  depth+=(s[p]=='{')-(s[p]=='}');p+=1
 return s[:op+1]+body+s[p-1:]
s=(R/'TESTE2/Classes/InventoryWnd.uc').read_text()
old='''TextBoxHandle(GetHandle("MileniumInventoryPreviewWnd.RenderStatus")).SetText("Render 3D: " $ string(class'MobPreviewNative'.static.BindPreview(GetHandle("MileniumInventoryPreviewWnd.MobViewport"),class'MobPreviewNative'.static.GetWorldActor(class'UIDATA_PLAYER'.static.GetPlayerID()),35.0,145.0,45.0)));'''
assert old in s;s=s.replace(old,prepare+'GetHandle("MileniumInventoryPreviewWnd").ShowWindow();\n'+bind)
s=s.replace('                    GetHandle("MileniumInventoryPreviewWnd").ShowWindow();\n','',1)
pos=s.index('function OnClickButton(');end=s.index('function Argumentations()',pos);chunk=s[pos:end];needle='GetHandle("MileniumInventoryPreviewWnd").HideWindow();';chunk=chunk.replace(needle,needle+'\n                    if(!StatusWnd(GetScript("StatusWnd")).PortraitExpanded)\n                        StatusWnd(GetScript("StatusWnd")).ReleaseOptionalPortrait();',1);s=s[:pos]+chunk+s[end:]
# Begin from original OnHide so no TESTE2 cleanup is duplicated.
orig=(R/'classes/Classes/InventoryWnd.uc').read_text();st=orig.index('function OnHide(');en=orig.index('\nfunction ',st+1);body=orig[orig.index('{',st)+1:orig.rindex('}',st,en)]+cleanup;s=replacebody(s,'OnHide',body)
s=appendbody(s,'OnEvent','\n    if(GetHandle("MileniumInventoryPreviewWnd").IsShowWindow())\n    {\n'+prepare+'GetHandle("MileniumInventoryPreviewWnd").ShowWindow();\n'+bind+'\n    }\n');(OUT/'Classes/InventoryWnd.uc').write_text(s)
s=(R/'classes/Classes/MobInformerWnd.uc').read_text();s=appendbody(s,'OnShow',cleanup);(OUT/'Classes/MobInformerWnd.uc').write_text(s)

s=(R/'classes/Classes/StatusWnd.uc').read_text();simple='\n if(GetHandle("MileniumInventoryPreviewWnd").IsShowWindow()){class\'MobPreviewNative\'.static.ReleasePreview();GetHandle("MileniumInventoryPreviewWnd").HideWindow();}\n';s=appendbody(s,'ReleaseOptionalPortrait',simple);(OUT/'Classes/StatusWnd.uc').write_text(s)
