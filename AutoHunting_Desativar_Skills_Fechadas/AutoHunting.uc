class AutoHunting extends UICommonAPI;

const FARM_STATE_TAG = 99982;
const FARM_REQUEST_TIMER = 6510;

var WindowHandle zzMe;

var ButtonHandle zzBtnattack;
var ButtonHandle zzBtnattackoffexpand;
var ButtonHandle zzbotonexpandminimized;
var ButtonHandle zzBtnLongRange;
var ButtonHandle zzBtnShortRange;
var ButtonHandle zzBtnRespectHunt;
var ButtonHandle zzBtnSummon;
var ButtonHandle zzBtnBuff;
var ButtonHandle zzBtnPageDec;
var ButtonHandle zzBtnPageInc;
var ButtonHandle zzBtnFarmType;
var ButtonHandle zzBtnCounterAttack;
var ButtonHandle zzBtnMannerMode;
var ButtonHandle zzBtnKeepLocation;
var ButtonHandle zzBtnPerfil;
var ButtonHandle zzBtnInfo;

var AnimTextureHandle zzm_TexAnim1Expand;
var AnimTextureHandle zzm_TexAnim2Expand;

var WindowHandle zzm_ResultAnimation1Expand;
var WindowHandle zzm_ResultAnimation2Expand;

var AutoSkillWnd scriptsauto;

var bool activateautofarm;
var bool zzBtnRespectHuntIcon;
var bool zzbtnSummontIcon;
var bool KeepLocationEnabled;
var bool CounterAttackEnabled;
var bool MagicMode;
var bool BuffProtectionEnabled;
var bool StateKnown;
var bool WaitingResponse;

var int SearchRadius;
var int SkillPage;
var int TargetMode;

var string SelectedSkillType;

function OnLoad()
{
    zzMe = GetHandle("AutoHunting");

    scriptsauto = AutoSkillWnd(GetScript("AutoSkillWnd"));

    zzm_TexAnim1Expand = AnimTextureHandle(
        GetHandle("AutoHunting.Anim1CircleExpand.Anim1Expand")
    );

    zzm_TexAnim2Expand = AnimTextureHandle(
        GetHandle("AutoHunting.Anim2CoreExpand.Anim2Expand")
    );

    zzm_ResultAnimation1Expand = GetHandle(
        "AutoHunting.Anim1CircleExpand"
    );

    zzm_ResultAnimation2Expand = GetHandle(
        "AutoHunting.Anim2CoreExpand"
    );

    zzbotonexpandminimized = ButtonHandle(
        GetHandle("AutoHunting.btnDrawer")
    );

    zzBtnattack = ButtonHandle(
        GetHandle("AutoHunting.DrawerButtons.Btnattack")
    );

    zzBtnattackoffexpand = ButtonHandle(
        GetHandle("AutoHunting.BtnattackoffExpand")
    );

    zzBtnRespectHunt = ButtonHandle(
        GetHandle("AutoHunting.DrawerButtons.BtnRespectHunt")
    );

    zzBtnSummon = ButtonHandle(
        GetHandle("AutoHunting.DrawerButtons.BtnSummon")
    );

    zzBtnLongRange = ButtonHandle(
        GetHandle("AutoHunting.DrawerButtons.BtnLongRange")
    );

    zzBtnShortRange = ButtonHandle(
        GetHandle("AutoHunting.DrawerButtons.BtnShortRange")
    );

    zzBtnBuff = ButtonHandle(
        GetHandle("AutoHunting.DrawerButtons.BtnBuff")
    );

    zzBtnPageDec = ButtonHandle(
        GetHandle("AutoHunting.DrawerButtons.BtnDecPage")
    );

    zzBtnPageInc = ButtonHandle(
        GetHandle("AutoHunting.DrawerButtons.BtnIncPage")
    );

    zzBtnFarmType = ButtonHandle(
        GetHandle("AutoHunting.DrawerButtons.BtnFarmType")
    );

    zzBtnCounterAttack = ButtonHandle(
        GetHandle("AutoHunting.DrawerButtons.BtnCounterAttack")
    );

    zzBtnMannerMode = ButtonHandle(
        GetHandle("AutoHunting.DrawerButtons.BtnMannerMode")
    );

    zzBtnKeepLocation = ButtonHandle(
        GetHandle("AutoHunting.DrawerButtons.BtnKeepLocation")
    );

    zzBtnPerfil = ButtonHandle(
        GetHandle("AutoHunting.DrawerButtons.BtnPerfil")
    );

    zzBtnInfo = ButtonHandle(
        GetHandle("AutoHunting.DrawerButtons.btnInfoHunting")
    );

    RegisterEvent(50);
    RegisterEvent(40);
    RegisterEvent(580);
    RegisterEvent(55561);
    RegisterEvent(EV_UpdateUserInfo);
    RegisterEvent(EV_PCCafePointInfo);

    zzbotonexpandminimized.SetTexture(
        "L2UI_CH3.AutoHunting.WinMinButton",
        "L2UI_CH3.AutoHunting.WinMinButton_down",
        "L2UI_CH3.AutoHunting.WinMinButton_over"
    );

    zzbotonexpandminimized.SetTooltipCustomType(
        xxSetTooltip("Minimizar")
    );

    zzBtnattack.SetTooltipCustomType(
        xxSetTooltip("Skills automaticas")
    );

    if (zzBtnInfo != None)
    {
        zzBtnInfo.SetTooltipCustomType(
            xxSetTooltip("Configuracoes do AutoFarm")
        );
    }
		// Esquerda: um pouco mais perto do circulo, mantendo a coluna.
	PositionOptionButton(zzBtnCounterAttack, 2, 46, 32);
	PositionOptionButton(zzBtnMannerMode, 2, 82, 32);
	PositionOptionButton(zzBtnFarmType, 2, 118, 32);

	PositionOptionButton(zzBtnKeepLocation, 36, 118, 37);

	// Configuracao: desce um pouco.
	PositionOptionButton(zzBtnInfo, 69, 6, 32);

	// Offline: aproxima do arco.
	PositionOptionButton(zzBtnPerfil, 105, 14, 32);

	// Skills e alcance: manter.
	PositionOptionButton(zzBtnattack, 129, 40, 32);
	PositionOptionButton(zzBtnLongRange, 137, 72, 32);
	PositionOptionButton(zzBtnShortRange, 130, 104, 32);

	// Discord: mais alinhado e proximo do botao de diminuir alcance.
	PositionOptionButton(zzBtnRespectHunt, 116, 128, 32);

    // Discord e Offline sao apenas botoes visuais.
    zzBtnRespectHunt.SetTexture(
        "InterfaceSBlack.AutoSkillWnd.btnDiscord",
        "InterfaceSBlack.AutoSkillWnd.btnDiscord",
        "InterfaceSBlack.AutoSkillWnd.btnDiscord"
    );

    zzBtnRespectHunt.SetTooltipCustomType(
        xxSetTooltip("Discord")
    );

    if (zzBtnPerfil != None)
    {
        zzBtnPerfil.SetTexture(
            "L2UI_NewTex.AutomaticPlay.TargetBTN_threat",
            "L2UI_NewTex.AutomaticPlay.TargetBTN_threat_Down",
            "L2UI_NewTex.AutomaticPlay.TargetBTN_threat_Over"
        );

        zzBtnPerfil.SetTooltipCustomType(
            xxSetTooltip("Farm offline")
        );
    }

    ResetClientState();
    CheckLevelVisibility();
    TrySynchronize();
}

function PositionOptionButton(
    ButtonHandle Button,
    int X,
    int Y,
    int Size)
{
    if (Button == None)
        return;

    Button.ClearAnchor();

    Button.SetAnchor(
        "AutoHunting",
        "TopLeft",
        "TopLeft",
        X,
        Y
    );

    Button.SetWindowSize(Size, Size);
}

function ResetClientState()
{
    zzMe.KillTimer(FARM_REQUEST_TIMER);

    WaitingResponse = false;
    StateKnown = false;

    zzBtnRespectHuntIcon = false;
    zzbtnSummontIcon = false;

    KeepLocationEnabled = false;
    CounterAttackEnabled = false;
    MagicMode = false;
    BuffProtectionEnabled = false;

    SearchRadius = 0;
    SkillPage = 0;
    TargetMode = 0;

    SelectedSkillType = "attack";

    xxOnDie();
    UpdateOptionButtons();
}

function OnEnterState(name a_PreStateName)
{
    Class'UIAPI_WINDOW'.static.SetAnchor(
        "AutoPlay",
        "",
        "BottomRight",
        "BottomRight",
        -300,
        -180
    );

    ResetClientState();
    CheckLevelVisibility();
    TrySynchronize();
}

function OnDefaultPosition()
{
    Class'UIAPI_WINDOW'.static.SetAnchor(
        "AutoPlay",
        "",
        "BottomRight",
        "BottomRight",
        -300,
        -180
    );
}

function CheckLevelVisibility()
{
    local UserInfo Info;

    if (GetPlayerInfo(Info) && Info.nLevel < 20)
    {
        HideWindow("AutoHunting");
        xxOnDie();
    }
}

function OnShow()
{
    local UserInfo Info;

    if (!GetPlayerInfo(Info) || Info.nLevel < 20)
    {
        HideWindow("AutoHunting");
        return;
    }

    UpdateOptionButtons();
    TrySynchronize(true);
}

function TrySynchronize(optional bool Force)
{
    local UserInfo Info;

    if (WaitingResponse || (StateKnown && !Force))
        return;

    if (!GetPlayerInfo(Info) || Info.nLevel < 20)
        return;

    WaitingResponse = true;

    zzMe.SetTimer(FARM_REQUEST_TIMER, 2000);

    RequestBypassToServer("voiced_autoFarmUiStatus2");
}

function SendFarmCommand(string Command)
{
    if (WaitingResponse)
        return;

    if (!StateKnown)
    {
        TrySynchronize();
        return;
    }

    WaitingResponse = true;

    zzMe.SetTimer(FARM_REQUEST_TIMER, 2000);

    RequestBypassToServer(Command$" ui");
}

function OnEvent(int Event_ID, string param)
{
    local int SystemMsgIndex;

    if (Event_ID == EV_PCCafePointInfo)
    {
        HandleFarmState(param);
        return;
    }

    if (Event_ID == 40)
    {
        ResetClientState();
        return;
    }

    if (Event_ID == 50)
    {
        xxOnDie();
        return;
    }

    if (Event_ID == 55561)
    {
        xxTimeLeft(param);
        return;
    }

    if (Event_ID == EV_UpdateUserInfo)
    {
        CheckLevelVisibility();
        TrySynchronize();
        return;
    }

    if (Event_ID != 580)
        return;

    if (!ParseInt(param, "Index", SystemMsgIndex))
        return;

    switch (SystemMsgIndex)
    {
        case 2804:
        case 2805:
        case 2806:
        case 2807:
        case 2808:
        case 2809:
        case 2154:
            // Confere o estado real antes de atualizar os botoes.
            TrySynchronize(true);
            break;
    }
}

function HandleFarmState(string param)
{
    local int Tag;
    local int Flags;
    local int Radius;
    local int Page;
    local int Mode;

    if (!ParseInt(param, "RemainTime", Tag)
        || Tag != FARM_STATE_TAG)
    {
        return;
    }

    if (!ParseInt(param, "TotalPoint", Flags))
        return;

    if (!ParseInt(param, "AddPoint", Radius))
        return;

    if (!ParseInt(param, "PeriodType", Page))
        return;

    if (!ParseInt(param, "PointType", Mode))
        return;

    if (
        Flags < 0 ||
        Flags > 511 ||
        Radius < 200 ||
        Radius > 3000 ||
        Page < 0 ||
        Page > 20 ||
        Mode < 0 ||
        Mode > 2
    )
    {
        return;
    }

    zzMe.KillTimer(FARM_REQUEST_TIMER);

    WaitingResponse = false;
    StateKnown = true;

    SearchRadius = Radius;
    SkillPage = Page;
    TargetMode = Mode;

    switch ((Flags >> 7) & 3)
    {
        case 1:
            SelectedSkillType = "chance";
            break;

        case 2:
            SelectedSkillType = "self";
            break;

        case 3:
            SelectedSkillType = "heal";
            break;

        default:
            SelectedSkillType = "attack";
            break;
    }

    zzbtnSummontIcon = (Flags & 2) != 0;
    zzBtnRespectHuntIcon = (Flags & 4) != 0;

    KeepLocationEnabled = (Flags & 8) != 0;
    MagicMode = (Flags & 16) != 0;
    CounterAttackEnabled = (Flags & 32) != 0;
    BuffProtectionEnabled = (Flags & 64) != 0;

    if ((Flags & 1) != 0)
        StartVisuals();
    else
        xxOnDie();

    UpdateOptionButtons();
}

function OnTimer(int TimerID)
{
    if (TimerID != FARM_REQUEST_TIMER)
        return;

    zzMe.KillTimer(FARM_REQUEST_TIMER);

    WaitingResponse = false;
    StateKnown = false;
}

function StartVisuals()
{
    if (!activateautofarm)
    {
        zzm_TexAnim1Expand.SetLoopCount(-1);
        zzm_TexAnim2Expand.SetLoopCount(-1);

        zzm_TexAnim1Expand.Play();
        zzm_TexAnim2Expand.Play();
    }

    activateautofarm = true;

    zzm_ResultAnimation1Expand.ShowWindow();
    zzm_ResultAnimation2Expand.ShowWindow();

    BtnOnExpand();

    if (scriptsauto != None && scriptsauto.zzMe != None)
    {
        if (!scriptsauto.zzwaeawea)
        {
            scriptsauto.zzwaeawea = true;
            scriptsauto.xxwemakajjas();
        }
    }
}

function xxOnDie()
{
    activateautofarm = false;

    zzm_TexAnim1Expand.Stop();
    zzm_TexAnim2Expand.Stop();

    zzm_ResultAnimation1Expand.HideWindow();
    zzm_ResultAnimation2Expand.HideWindow();

    BtnOffExpand();

    if (scriptsauto != None && scriptsauto.zzMe != None)
    {
        scriptsauto.zzwaeawea = false;
        scriptsauto.xxdweaew213a();
    }
}

function xxOnTeleport()
{
    TrySynchronize(true);
}

function xxResetReady()
{
    if (activateautofarm)
        StartVisuals();
    else
        xxOnDie();

    UpdateOptionButtons();
}

function BtnOn()
{
    zzBtnattack.SetTexture(
        "L2UI_CH3.AutoHuntingWnd.FightON_Auto",
        "L2UI_CH3.AutoHuntingWnd.FightON_Auto",
        "L2UI_CH3.AutoHuntingWnd.FightON_Auto"
    );
}

function BtnOff()
{
    zzBtnattack.SetTexture(
        "L2UI_CH3.AutoHuntingWnd.fightoff",
        "L2UI_CH3.AutoHuntingWnd.fightoff",
        "L2UI_CH3.AutoHuntingWnd.fightoff_auto"
    );
}

function BtnOnExpand()
{
    zzBtnattackoffexpand.SetTexture(
        "L2UI_CH3.AutoHuntingWnd.FightON_Auto",
        "L2UI_CH3.AutoHuntingWnd.FightON_Auto",
        "L2UI_CH3.AutoHuntingWnd.FightON_Auto"
    );

    zzBtnattackoffexpand.SetTooltipCustomType(
        xxSetTooltip("AutoFarm: ligado")
    );
}

function BtnOffExpand()
{
    zzBtnattackoffexpand.SetTexture(
        "L2UI_CH3.AutoHuntingWnd.fightoff",
        "L2UI_CH3.AutoHuntingWnd.fightoff",
        "L2UI_CH3.AutoHuntingWnd.fightoff_auto"
    );

    zzBtnattackoffexpand.SetTooltipCustomType(
        xxSetTooltip("AutoFarm: desligado")
    );
}

function BtnSummonOn()
{
    zzBtnSummon.SetTexture(
        "l2faris_ch3.autohuntingwnd.raidsbtn-shotd-normalon",
        "l2faris_ch3.autohuntingwnd.raidsbtn-shotd-normalon",
        "l2faris_ch3.autohuntingwnd.raidsbtn-shotd-overon"
    );

    zzBtnSummon.SetTooltipCustomType(
        xxSetTooltip("Ataque do summon: ligado")
    );
}

function BtnSummonOff()
{
    zzBtnSummon.SetTexture(
        "l2faris_ch3.autohuntingwnd.raidsbtn-shotd-normaloff",
        "l2faris_ch3.autohuntingwnd.raidsbtn-shotd-normaloff",
        "l2faris_ch3.autohuntingwnd.raidsbtn-shotd-overoff"
    );

    zzBtnSummon.SetTooltipCustomType(
        xxSetTooltip("Ataque do summon: desligado")
    );
}

function SetOptionTexture(
    ButtonHandle Button,
    string Normal,
    string Over)
{
    if (Button != None)
        Button.SetTexture(Normal, Normal, Over);
}

function BtnRespectHuntOn()
{
    SetOptionTexture(
        zzBtnMannerMode,
        "L2_GF.AutoHuntingWnd.MannerBTNON_Normal",
        "L2_GF.AutoHuntingWnd.MannerBTNON_over"
    );

    if (zzBtnMannerMode != None)
    {
        zzBtnMannerMode.SetTooltipCustomType(
            xxSetTooltip("Respect Hunt: ligado")
        );
    }
}

function BtnRespectHuntOff()
{
    SetOptionTexture(
        zzBtnMannerMode,
        "L2_GF.AutoHuntingWnd.MannerBTNOff_Normal",
        "L2_GF.AutoHuntingWnd.MannerBTNOff_over"
    );

    if (zzBtnMannerMode != None)
    {
        zzBtnMannerMode.SetTooltipCustomType(
            xxSetTooltip("Respect Hunt: desligado")
        );
    }
}

function UpdateOptionButtons()
{
    if (zzbtnSummontIcon)
        BtnSummonOn();
    else
        BtnSummonOff();

    if (zzBtnRespectHuntIcon)
        BtnRespectHuntOn();
    else
        BtnRespectHuntOff();

    if (CounterAttackEnabled)
    {
        SetOptionTexture(
            zzBtnCounterAttack,
            "L2_GF.AutoHuntingWnd.CounterAttack_on",
            "L2_GF.AutoHuntingWnd.CounterAttack_on_over"
        );
    }
    else
    {
        SetOptionTexture(
            zzBtnCounterAttack,
            "L2_GF.AutoHuntingWnd.CounterAttack_Normal",
            "L2_GF.AutoHuntingWnd.CounterAttack_over"
        );
    }

    if (zzBtnCounterAttack != None)
    {
        if (CounterAttackEnabled)
        {
            zzBtnCounterAttack.SetTooltipCustomType(
                xxSetTooltip("Contra-ataque: ligado")
            );
        }
        else
        {
            zzBtnCounterAttack.SetTooltipCustomType(
                xxSetTooltip("Contra-ataque: desligado")
            );
        }
    }

    if (MagicMode)
    {
        SetOptionTexture(
            zzBtnFarmType,
            "L2_GF.AutoHuntingWnd.ModeBTN_mage",
            "L2_GF.AutoHuntingWnd.ModeBTN_mage"
        );
    }
    else
    {
        SetOptionTexture(
            zzBtnFarmType,
            "L2_GF.AutoHuntingWnd.ModeBTN_fighter",
            "L2_GF.AutoHuntingWnd.ModeBTN_fighter"
        );
    }

    if (zzBtnFarmType != None)
    {
        if (!StateKnown)
        {
            zzBtnFarmType.SetTooltipCustomType(
                xxSetTooltip("Estilo: aguardando servidor")
            );
        }
        else if (MagicMode)
        {
            zzBtnFarmType.SetTooltipCustomType(
                xxSetTooltip("Estilo: Mage")
            );
        }
        else
        {
            zzBtnFarmType.SetTooltipCustomType(
                xxSetTooltip("Estilo: Fighter")
            );
        }
    }

    if (KeepLocationEnabled)
    {
        SetOptionTexture(
            zzBtnKeepLocation,
            "L2_GF.AutoHuntingWnd.ZoneBTN_ShotD_NormalON",
            "L2_GF.AutoHuntingWnd.ZoneBTN_ShotD_NormalON"
        );
    }
    else
    {
        SetOptionTexture(
            zzBtnKeepLocation,
            "L2_GF.AutoHuntingWnd.ZoneBTN_ShotD_NormalOFF",
            "L2_GF.AutoHuntingWnd.ZoneBTN_ShotD_NormalOFF"
        );
    }

    if (zzBtnKeepLocation != None)
    {
        if (KeepLocationEnabled)
        {
            zzBtnKeepLocation.SetTooltipCustomType(
                xxSetTooltip("Manter raio do ponto inicial: ligado")
            );
        }
        else
        {
            zzBtnKeepLocation.SetTooltipCustomType(
                xxSetTooltip("Manter raio do ponto inicial: desligado")
            );
        }
    }

    UpdateRangeTooltips();

    if (zzBtnPageInc != None)
    {
        zzBtnPageInc.SetTooltipCustomType(
            xxSetTooltip("Proxima pagina do seletor de skills")
        );
    }

    if (zzBtnPageDec != None)
    {
        zzBtnPageDec.SetTooltipCustomType(
            xxSetTooltip("Pagina anterior do seletor de skills")
        );
    }

    if (BuffProtectionEnabled)
    {
        zzBtnBuff.SetTooltipCustomType(
            xxSetTooltip("Protecao sem buffs: ligada")
        );
    }
    else
    {
        zzBtnBuff.SetTooltipCustomType(
            xxSetTooltip("Protecao sem buffs: desligada")
        );
    }
}

function OnClickButton(string strID)
{
    local int Dot;

    Dot = InStr(strID, ".");

    while (Dot >= 0)
    {
        strID = Mid(strID, Dot + 1);
        Dot = InStr(strID, ".");
    }

    switch (Caps(strID))
    {
        case "BTNATTACK":
            openautoskill();
            break;

        case "BTNATTACKOFFEXPAND":
            SendFarmCommand("voiced_enableAutoFarm2");
            break;

        case "BTNLONGRANGE":
            SendFarmCommand("voiced_radiusAutoFarm2 inc");
            break;

        case "BTNSHORTRANGE":
            SendFarmCommand("voiced_radiusAutoFarm2 dec");
            break;

        case "BTNSUMMON":
            SendFarmCommand("voiced_enableSummonAttack");
            break;

        case "BTNMANNERMODE":
            SendFarmCommand("voiced_enableAntiKs");
            break;

        case "BTNKEEPLOCATION":
            SendFarmCommand("voiced_keepLocAutoFarm2");
            break;

        case "BTNFARMTYPE":
            SendFarmCommand("voiced_switchFarmType2");
            break;

        case "BTNCOUNTERATTACK":
            if (CounterAttackEnabled)
            {
                SendFarmCommand(
                    "voiced_targetModeAutoFarm2 mobs"
                );
            }
            else
            {
                SendFarmCommand(
                    "voiced_targetModeAutoFarm2 both"
                );
            }
            break;

        case "BTNBUFF":
            SendFarmCommand("voiced_enableBuffProtect2");
            break;

        case "BTNINCPAGE":
            SendFarmCommand(
                "voiced_openSkillPickerPageAutoFarm2 "
                $SelectedSkillType
                $" "
                $string(SkillPage + 1)
            );
            break;

        case "BTNDECPAGE":
            if (SkillPage > 0)
            {
                SendFarmCommand(
                    "voiced_openSkillPickerPageAutoFarm2 "
                    $SelectedSkillType
                    $" "
                    $string(SkillPage - 1)
                );
            }
            break;

        case "BLOCK1":
            RequestBypassToServer("voiced_ignoreMonster");
            break;

        case "BLOCK2":
            RequestBypassToServer("voiced_activeMonster");
            break;

        case "BTNDRAWER":
            xxminiexpand();
            break;

        case "BTNCONFIG":
        case "BTNINFOHUNTING":
            ExecuteCommand(".autofarm2");
            break;

        // Somente visuais: nenhuma acao.
        case "BTNRESPECTHUNT":
        case "BTNPERFIL":
            break;
    }
}

// Animation windows can receive the mouse instead of the central button.
// Forward only animation clicks inside the central button rectangle.
function OnLButtonDown(WindowHandle Control, int X, int Y)
{
    local string ControlName;
    local Rect Bounds;

    if (Control == None || zzBtnattackoffexpand == None)
        return;

    ControlName = Caps(Control.GetWindowName());
    if (InStr(ControlName, "ANIM1") < 0
        && InStr(ControlName, "ANIM2") < 0)
        return;

    Bounds = zzBtnattackoffexpand.GetRect();
    if (X < Bounds.nX || X >= Bounds.nX + Bounds.nWidth
        || Y < Bounds.nY || Y >= Bounds.nY + Bounds.nHeight)
        return;

    SendFarmCommand("voiced_enableAutoFarm2");
}

function openautoskill()
{
    if (IsShowWindow("AutoSkillWnd"))
        HideWindow("AutoSkillWnd");
    else
        ShowWindowWithFocus("AutoSkillWnd");
}

function xxminiexpand()
{
    if (IsShowWindow("AutoHunting.DrawerButtons"))
    {
        zzbotonexpandminimized.SetTexture(
            "L2UI_CH3.AutoHunting.WinExpandButton",
            "L2UI_CH3.AutoHunting.WinExpandButton_down",
            "L2UI_CH3.AutoHunting.WinExpandButton_over"
        );

        zzbotonexpandminimized.SetTooltipCustomType(
            xxSetTooltip("Expandir")
        );

        HideWindow("AutoHunting.DrawerButtons");
        HideWindow("AutoSkillWnd");
    }
    else
    {
        zzbotonexpandminimized.SetTexture(
            "L2UI_CH3.AutoHunting.WinMinButton",
            "L2UI_CH3.AutoHunting.WinMinButton_down",
            "L2UI_CH3.AutoHunting.WinMinButton_over"
        );

        zzbotonexpandminimized.SetTooltipCustomType(
            xxSetTooltip("Minimizar")
        );

        ShowWindow("AutoHunting.DrawerButtons");
    }
}

function setactivatedodeactivate(bool send)
{
    if (send)
        StartVisuals();
    else
        xxOnDie();
}

function lucerastart()
{
    SendFarmCommand("voiced_enableAutoFarm2");
}

function string TwoDigits(int Value)
{
    if (Value < 10)
        return "0"$string(Value);

    return string(Value);
}

function xxTimeLeft(string zzparam)
{
    local int TimeMs;
    local int Seconds;
    local string Text;

    if (!ParseInt(zzparam, "TimeHunting", TimeMs))
        return;

    Seconds = TimeMs / 1000;

    if (Seconds < 0)
        Seconds = 0;

    Text =
        TwoDigits(Seconds / 3600)
        $":"
        $TwoDigits((Seconds % 3600) / 60)
        $":"
        $TwoDigits(Seconds % 60);

    Class'UIAPI_TEXTBOX'.static.SetText(
        "AutoHunting.DrawerButtons.Timer.txtTimer",
        Text
    );
}

function CustomTooltip xxSetTooltip(string Text)
{
    local CustomTooltip TooltipInfo;

    TooltipInfo.MinimumWidth = 30;
    TooltipInfo.DrawList.Length = 1;

    TooltipInfo.DrawList[0].eType = DIT_TEXT;
    TooltipInfo.DrawList[0].nOffSetX = 1;
    TooltipInfo.DrawList[0].t_bDrawOneLine = true;

    TooltipInfo.DrawList[0].t_color.R = 255;
    TooltipInfo.DrawList[0].t_color.G = 255;
    TooltipInfo.DrawList[0].t_color.B = 255;
    TooltipInfo.DrawList[0].t_color.A = 255;

    TooltipInfo.DrawList[0].t_strText = Text;

    return TooltipInfo;
}

function UpdateRangeTooltips()
{
    local string RangeText;

    RangeText = "";

    if (SearchRadius > 0)
    {
        RangeText = ": "$string(SearchRadius);
    }

    if (zzBtnLongRange != None)
    {
        zzBtnLongRange.SetTooltipCustomType(
            xxSetTooltip("Aumentar alcance"$RangeText)
        );
    }

    if (zzBtnShortRange != None)
    {
        zzBtnShortRange.SetTooltipCustomType(
            xxSetTooltip("Diminuir alcance"$RangeText)
        );
    }
}

defaultproperties
{
}