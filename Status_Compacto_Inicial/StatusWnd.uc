class StatusWnd extends UICommonAPI;

const HUD_POINTER_TIMER=7590;
const HUD_PORTRAIT_TIMER=7592;
const HUD_PORTRAIT_CLICK_TIMER=7593;
var bool PortraitClickGuard;
var Pawn PortraitSource;
var int PortraitSourceID,PortraitSourceLookupTicks;
var bool PortraitExpanded;
var HudPortraitPawn PortraitPawn;
var Texture PortraitBackground;
var string PortraitAppearance;
var int PortraitPlayerID;
var bool HudDragging,HudResizing;
var int HudX,HudY,HudMouseX,HudMouseY,HudWidth;
var string m_WindowName;
var WindowHandle Me;
var TextureHandle statusPawnFace;
var TextureHandle statusPawnHairAh;
var TextureHandle statusPawnHairBh;

var int m_UserID;
var bool m_bReceivedUserInfo;
var string staticAhM;
var string staticBhM;
var string staticBhT;
var string hairBh;
var string hairAh;
var string faceStr;
var Array<string> faceSubSplit;

function OnLoad()
{
	RegisterEvent( EV_RegenStatus );
	RegisterEvent( EV_UpdateUserInfo );
	RegisterEvent( EV_UpdateHP );
	RegisterEvent( EV_UpdateMaxHP );
	RegisterEvent( EV_UpdateMP );
	RegisterEvent( EV_UpdateMaxMP );
	RegisterEvent( EV_UpdateCP );
	RegisterEvent( EV_UpdateMaxCP );
	Me = GetHandle(m_WindowName);
	// Manual physical-button dragging avoids sticky native drag capture.
	Me.SetDraggable(False);
 InitOptionalPortrait();
 GetHandle("StatusWnd.HPBarTex.HPBarValueShadow").HideWindow();
}

function OnEnterState( name a_PreStateName )
{
	local Array<string> cleanFaceSubSplit;

	SetPortraitVisible(false,false);
	PortraitClickGuard=false;
	Me.KillTimer(HUD_PORTRAIT_CLICK_TIMER);
	m_bReceivedUserInfo = False;
	UpdateUserInfo();
	staticAhM = "";
	staticBhM = "";
	staticBhT = "";
	hairAh = "";
	hairBh = "";
	faceStr = "";
	faceSubSplit = cleanFaceSubSplit;
 if(PortraitExpanded){UpdateOptionalPortrait();Me.SetTimer(HUD_PORTRAIT_TIMER,150);}
}

function OnLButtonDown(WindowHandle handle,int x,int y)
{
 local Rect bounds,buttonBounds;
 local vector pointer;
 StopHudPointer();
 if(handle!=None&&Right(handle.GetWindowName(),14)=="PortraitToggle"){TogglePortraitOnce();return;}
 pointer=class'HudPortraitNative'.static.GetPointerState();
 buttonBounds=GetHandle("StatusWnd.PortraitToggle").GetRect();
 if(pointer.X>=buttonBounds.nX&&pointer.X<buttonBounds.nX+buttonBounds.nWidth&&pointer.Y>=buttonBounds.nY&&pointer.Y<buttonBounds.nY+buttonBounds.nHeight){TogglePortraitOnce();return;}
 if(x>=buttonBounds.nX&&x<buttonBounds.nX+buttonBounds.nWidth&&y>=buttonBounds.nY&&y<buttonBounds.nY+buttonBounds.nHeight){TogglePortraitOnce();return;}
 if(pointer.Z<0.5)return;
 bounds=Me.GetRect();Me.ClearAnchor();Me.MoveTo(bounds.nX,bounds.nY);
 HudX=bounds.nX;HudY=bounds.nY;HudWidth=bounds.nWidth;
 HudMouseX=int(pointer.X);HudMouseY=int(pointer.Y);
 HudResizing=x>=bounds.nX+bounds.nWidth-28 && y>=bounds.nY && y<=bounds.nY+16;
 HudDragging=!HudResizing;
 if(!HudResizing && x>bounds.nX+60)RequestSelfTarget();
 Me.SetFocus();Me.SetTimer(HUD_POINTER_TIMER,16);
}
function OnLButtonUp(WindowHandle handle,int x,int y){StopHudPointer();}
function OnHide(){StopHudPointer();ReleaseOptionalPortrait();ClearPortraitSource();PortraitClickGuard=false;Me.KillTimer(HUD_PORTRAIT_CLICK_TIMER);}
function OnExitState(name state){StopHudPointer();ReleaseOptionalPortrait();ClearPortraitSource();PortraitClickGuard=false;Me.KillTimer(HUD_PORTRAIT_CLICK_TIMER);}
function StopHudPointer()
{
 HudDragging=False;HudResizing=False;
 if(Me!=None){Me.KillTimer(HUD_POINTER_TIMER);Me.SetDraggable(False);}
}
static function int ClampHudWidth(int width){return Clamp(width,256,640);}
function OnTimer(int timerID)
{
 local vector pointer;
 if(timerID==HUD_PORTRAIT_CLICK_TIMER){PortraitClickGuard=false;Me.KillTimer(HUD_PORTRAIT_CLICK_TIMER);return;}
 if(timerID==HUD_PORTRAIT_TIMER){UpdateUserInfo();UpdateOptionalPortrait();return;}
 if(timerID!=HUD_POINTER_TIMER)return;
 if((!HudDragging && !HudResizing)||!Me.IsShowWindow()){StopHudPointer();return;}
 pointer=class'HudPortraitNative'.static.GetPointerState();
 if(pointer.Z<0.5){StopHudPointer();return;}
 if(HudResizing)Me.SetWindowSize(ClampHudWidth(HudWidth+int(pointer.X)-HudMouseX),CurrentHudHeight());
 else Me.MoveTo(HudX+int(pointer.X)-HudMouseX,HudY+int(pointer.Y)-HudMouseY);
}

function OnEvent( int a_EventID, string a_Param )
{
	switch ( a_EventID )
	{
		case EV_UpdateUserInfo:
			UpdateUserInfo();
			break;
		case EV_UpdateHP:
		case EV_UpdateMaxHP:
		case EV_UpdateMP:
		case EV_UpdateMaxMP:
		case EV_UpdateCP:
		case EV_UpdateMaxCP:
			HandleUpdateInfo( a_Param );
			break;
		case EV_RegenStatus:
			HandleRegenStatus( a_Param );
			break;
	}
}

function OnTick()
{
 local Actor MyActor;
 local SBlackBrowserWnd browser;
 if(!GetPlayerActor(MyActor))return;
 browser=SBlackBrowserWnd(GetScript("SBlackBrowserWnd"));
 if(browser!=None)browser.PollHotkey();
}

function bool isForcedDrawenAh( string charType, string hairStyle )
{
	if ( hairStyle == "m009" && charType != "MShaman" && charType != "FShaman" && charType != "MOrc" && charType != "FOrc" )
		return True;

	switch ( charType )
	{			
		case "MDarkElf":
			if ( hairStyle == "m002" )
				return True;
			break;
		case "FDarkElf":
			if ( hairStyle == "m006" )
				return True;
			break;	
		case "FMagic":
			if ( hairStyle == "m001" )
				return True;
			break;	
		case "FShaman":
			if ( hairStyle == "m000" || hairStyle == "m006" )
				return True;
			break;
		case "MDwarf":
			return True;
			break;
	}
	return False;
}

function HandleUpdateInfo( string param )
{
	local int ServerID;

	ParseInt( param, "ServerID", ServerID );

	if ( m_UserID == ServerID || !m_bReceivedUserInfo )
		UpdateUserInfo();
}

function HandleRegenStatus( string param )
{
	local int type;
	local int duration;
	local int ticks;
	local float amount;

	ParseInt( param, "Type", type );
	
	if ( type == 1 )
	{
		ParseInt( param, "Duration", duration );
		ParseInt( param, "Ticks", ticks );
		ParseFloat( param, "Amount", amount );
		//class'UIAPI_STATUSBARCTRL'.static.SetRegenInfo(m_WindowName$".HPBar", duration, ticks, amount);
	}
}

function UpdateUserInfo()
{
	local UserInfo userinfo;
	local string level;

	if ( GetPlayerInfo( userinfo ) )
	{
		m_bReceivedUserInfo = True;
		m_UserID = userinfo.nID;
		UpdateGaugeBarTex("CP", userinfo.nCurCP, userinfo.nMaxCP);
		UpdateGaugeBarTex("HP", userinfo.nCurHP, userinfo.nMaxHP);
		UpdateGaugeBarTex("MP", userinfo.nCurMP, userinfo.nMaxMP);
		level = string(userinfo.nLevel);
		if ( Len(userinfo.nLevel) == 1 )
			level = "0"$level;
		class'UIAPI_TEXTBOX'.static.SetText(m_WindowName$".UserInfo.txtStatusLevel", level);
        class'UIAPI_TEXTBOX'.static.SetText(m_WindowName$".PortraitLevel", level);
        // Preserve the existing Milenium EXP integration.
        ExpBarWnd(GetScript("ExpBarWnd")).UpdateExpBar(userinfo.nCurExp, userinfo.nLevel);
	}
}

static function float GaugeRatio(float current,float maximum)
{
 if(maximum<=0.0 || current<=0.0)return 0.0;
 return FMin(current/maximum,1.0);
}

static function int GaugeEndWidth(string type)
{
 if(type=="CP")return 6;
 if(type=="MP")return 12;
 return 10;
}

static function int GaugeRightInset(string type)
{
 if(type=="HP")return 4;
 if(type=="MP")return 3;
 return 2;
}

function UpdateGaugeBarTex( string type, float curVal, float maxVal )
{
 local float ratio;
 ratio=GaugeRatio(curVal,maxVal);
 // Relative sizing keeps the live gauge aligned when the HUD is resized.
 class'UIAPI_WINDOW'.static.SetWindowSizeRel(m_WindowName$"."$type$"BarTex."$type$"Tex_center",ratio,1.0,-int(float(GaugeEndWidth(type)+GaugeRightInset(type))*ratio),0);
 if(type=="HP" || type=="MP")
 {
  class'UIAPI_TEXTBOX'.static.SetText(m_WindowName$"."$type$"BarTex."$type$"BarValueShadow",int(curVal)$"/"$int(maxVal));
  class'UIAPI_TEXTBOX'.static.SetText(m_WindowName$"."$type$"BarTex."$type$"BarValue",int(curVal)$"/"$int(maxVal));
 }
	HideWindow(m_WindowName$"."$type$"BarTex."$type$"Tex_left");
	HideWindow(m_WindowName$"."$type$"BarTex."$type$"Tex_right");
	HideWindow(m_WindowName$"."$type$"BarTex."$type$"Tex_trail");
	if ( curVal > 0.0 ) 
	{
		ShowWindow(m_WindowName$"."$type$"BarTex."$type$"Tex_left");
		if ( curVal == maxVal ) 
			ShowWindow(m_WindowName$"."$type$"BarTex."$type$"Tex_right");
		else if(type!="MP")
			ShowWindow(m_WindowName$"."$type$"BarTex."$type$"Tex_trail");
	}
}

function InitOptionalPortrait()
{
 PortraitExpanded=false;
 HidePortraitVisuals();SetPortraitVisible(false,false);
}
function OnShow()
{
 if(PortraitExpanded){UpdateOptionalPortrait();Me.SetTimer(HUD_PORTRAIT_TIMER,150);}
}
function OnClickButton(string control)
{
 if(Right(control,14)=="PortraitToggle")TogglePortraitOnce();
}
function OnClickButtonWithHandle(ButtonHandle control)
{
 if(control!=None&&Right(control.GetWindowName(),14)=="PortraitToggle")TogglePortraitOnce();
}
function TogglePortraitOnce()
{
 if(PortraitClickGuard)return;
 PortraitClickGuard=true;Me.SetTimer(HUD_PORTRAIT_CLICK_TIMER,250);
 StopHudPointer();SetPortraitVisible(!PortraitExpanded,false);
}
function ClearPortraitSource()
{
 PortraitSource=None;PortraitSourceID=0;PortraitSourceLookupTicks=0;
}
function int CurrentHudHeight(){if(PortraitExpanded)return 116;return 68;}
function HidePortraitVisuals()
{
 GetHandle("StatusWnd.PortraitFrame").HideWindow();
 GetHandle("StatusWnd.PortraitLevel").HideWindow();
 GetHandle("StatusWnd.HudPortraitViewport").HideWindow();
 GetHandle("StatusWnd.PortraitLevelBackdrop").HideWindow();
}
function ReleaseOptionalPortrait()
{
 Me.KillTimer(HUD_PORTRAIT_TIMER);
 class'HudPortraitNative'.static.ReleasePortrait();
 GetHandle("StatusWnd.HudPortraitViewport").HideWindow();
 if(PortraitPawn!=None){PortraitPawn.Destroy();PortraitPawn=None;}
 PortraitAppearance="";PortraitPlayerID=0;

 if(GetHandle("MileniumInventoryPreviewWnd").IsShowWindow()){class'MobPreviewNative'.static.ReleasePreview();GetHandle("MileniumInventoryPreviewWnd").HideWindow();}
}
function SetPortraitVisible(bool showPortrait,bool saveChoice)
{
 local Rect bounds;local int extra,inset,gaugeY,backgroundX;local string arrow;
 local Texture portraitMaterial,levelMaterial;
 local TextureHandle frame,levelBack;local ButtonHandle button;
 portraitMaterial=Texture(DynamicLoadObject("L2UI_CH3.AbnormalWnd.HudPortraitFrame",class'Texture',true));
 levelMaterial=Texture(DynamicLoadObject("L2UI_CH3.AbnormalWnd.HudPortraitLevel",class'Texture',true));
 if(portraitMaterial!=None)portraitMaterial.bAlphaTexture=True;
 if(levelMaterial!=None)levelMaterial.bAlphaTexture=True;
 PortraitExpanded=showPortrait;extra=0;gaugeY=0;
 if(PortraitExpanded){extra=44;gaugeY=26;}
 bounds=Me.GetRect();Me.SetWindowSize(ClampHudWidth(bounds.nWidth),CurrentHudHeight());
 // UserInfo keeps its original compact size; portrait controls anchor directly to StatusWnd.
 GetHandle("StatusWnd.UserInfo.txtStatusLevel").SetAnchor("StatusWnd","TopLeft","CenterCenter",32,38);
 if(PortraitExpanded)
 {
  GetHandle("StatusWnd.UserInfo.txtStatusLevel").HideWindow();
  GetHandle("StatusWnd.PortraitLevel").ShowWindow();
 }
 else
 {
  GetHandle("StatusWnd.PortraitLevel").HideWindow();
  GetHandle("StatusWnd.UserInfo.txtStatusLevel").ShowWindow();
 }
 // Keep the portrait inside its window; bars begin 26px below its top.
 GetHandle("StatusWnd.StatusRightTex").SetAnchor("StatusWnd","TopRight","TopRight",0,gaugeY);
 // Continue the bar background to the diamond center, behind its front rim.
 backgroundX=64+extra;if(PortraitExpanded)backgroundX=59;
 GetHandle("StatusWnd.StatusCenterTex").SetAnchor("StatusWnd","TopLeft","TopLeft",backgroundX,gaugeY);
 GetHandle("StatusWnd.StatusCenterTex").SetWindowSizeRel(1.0,0.0,-109-backgroundX,68);
 GetHandle("StatusWnd.CPBarTex").SetAnchor("StatusWnd","TopLeft","TopLeft",60+extra,5+gaugeY);
 GetHandle("StatusWnd.CPBarTex").SetWindowSizeRel(1.0,0.0,-73-extra,10);
 GetHandle("StatusWnd.HPBarTex").SetAnchor("StatusWnd","TopLeft","TopLeft",60+extra,18+gaugeY);
 GetHandle("StatusWnd.HPBarTex").SetWindowSizeRel(1.0,0.0,-79-extra,20);
 GetHandle("StatusWnd.MPBarTex").SetAnchor("StatusWnd","TopLeft","TopLeft",60+extra,40+gaugeY);
 GetHandle("StatusWnd.MPBarTex").SetWindowSizeRel(1.0,0.0,-90-extra,20);
 button=ButtonHandle(GetHandle("StatusWnd.PortraitToggle"));
 if(PortraitExpanded)button.SetAnchor("StatusWnd","TopLeft","TopLeft",84,88);
 else button.SetAnchor("StatusWnd","TopLeft","TopLeft",46,50);
 arrow="L2UI_CH3.TargetWnd.TargetContractBtn";
 if(PortraitExpanded)arrow="L2UI_CH3.TargetWnd.TargetExpandBtn";
 button.SetTexture(arrow,arrow$"_down",arrow$"_over");button.ShowWindow();
 if(PortraitExpanded)
 {
  GetHandle("StatusWnd.StatusLeftTex").HideWindow();
  frame=TextureHandle(GetHandle("StatusWnd.PortraitFrame"));
  frame.SetTexture("L2UI_CH3.AbnormalWnd.HudPortraitFrame");
  frame.SetTextureCtrlType(ETextureCtrlType.TCT_Stretch);frame.SetUV(0,0);
  class'MobPreviewNative'.static.SetHuntTextureSize(frame,86,92);frame.SetWindowSize(108,116);frame.ShowWindow();
  levelBack=TextureHandle(GetHandle("StatusWnd.PortraitLevelBackdrop"));
  levelBack.SetTexture("L2UI_CH3.AbnormalWnd.HudPortraitLevel");
  levelBack.SetTextureCtrlType(ETextureCtrlType.TCT_Stretch);levelBack.SetUV(0,0);
  class'MobPreviewNative'.static.SetHuntTextureSize(levelBack,79,42);levelBack.SetWindowSize(99,53);levelBack.ShowWindow();
  button.SetTooltipText("Ocultar retrato do personagem");
  UpdateOptionalPortrait();Me.SetTimer(HUD_PORTRAIT_TIMER,150);
 }
 else
 {
  ReleaseOptionalPortrait();ClearPortraitSource();HidePortraitVisuals();GetHandle("StatusWnd.StatusLeftTex").ShowWindow();
  button.SetTooltipText("Exibir retrato do personagem");
 }
 if(saveChoice){inset=0;if(PortraitExpanded)inset=1;SetINIInt("SBlackHUD","Portrait3D",inset,"WindowsInfo.ini");}
 UpdateUserInfo();
}
function UpdateOptionalPortrait()
{
 local Actor sourceActor;local Pawn source;local UserInfo info;
 local string appearance;local vector place;local int result;
 if(!PortraitExpanded||!Me.IsShowWindow())return;
 if(!GetPlayerInfo(info)){Me.SetTimer(HUD_PORTRAIT_TIMER,150);return;}
 if(PortraitSourceID!=info.nID)
 {
  ReleaseOptionalPortrait();ClearPortraitSource();PortraitSourceID=info.nID;Me.SetTimer(HUD_PORTRAIT_TIMER,150);
 }
 if(PortraitSource==None||PortraitSource.bDeleteMe)
 {
  if(PortraitSourceLookupTicks>0){PortraitSourceLookupTicks--;return;}
  sourceActor=None;GetPlayerActor(sourceActor);
  if(sourceActor!=None){source=Pawn(sourceActor);if(source==None)source=sourceActor.Instigator;}
  if(source==None)source=Pawn(class'MobPreviewNative'.static.GetWorldActor(info.nID));
  PortraitSource=source;PortraitSourceLookupTicks=1;
 }
 source=PortraitSource;
 if(source==None||source.bDeleteMe||source.Mesh==None)return;
 appearance=class'HudPortraitPawn'.static.AppearanceKey(source);
 if(PortraitPawn==None||PortraitPawn.bDeleteMe||PortraitPlayerID!=info.nID||PortraitAppearance!=appearance)
 {
  if(PortraitPawn==None||PortraitPawn.bDeleteMe||PortraitPlayerID!=info.nID)
  {
   ReleaseOptionalPortrait();
   place=source.Location;
   PortraitPawn=source.Spawn(class'HudPortraitPawn',,,place,rot(0,0,0));
   if(PortraitPawn==None){Me.SetTimer(HUD_PORTRAIT_TIMER,150);return;}
  }
  PortraitPlayerID=info.nID;PortraitAppearance=appearance;
  PortraitBackground=Texture(DynamicLoadObject("L2UI_CH3.AbnormalWnd.HudPortraitFrame",class'Texture',true));
  result=class'HudPortraitNative'.static.BindPortrait(GetHandle("StatusWnd.HudPortraitViewport"),PortraitPawn,PortraitBackground,info.nID);
  // R10 native: 1 = resources changed; 2 = same native appearance (cached).
  if(result==1)PortraitPawn.PrepareClassicAppearance(source);
  if(result>0)GetHandle("StatusWnd.HudPortraitViewport").ShowWindow();
  else{PortraitAppearance="";Log("[HudPortrait] bind failed "$string(result));}
  Me.SetTimer(HUD_PORTRAIT_TIMER,150);
 }
 if(PortraitPawn!=None)
 {
  PortraitPawn.SetLocation(source.Location);
  PortraitPawn.UpdateClassicPose();
 }
}

defaultproperties
{
	m_WindowName="StatusWnd"
}
