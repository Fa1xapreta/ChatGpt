class CompactToolbarWnd extends UICommonAPI;

var WindowHandle Me;

var string LastLayoutSignature;

var int Expanded[4];
var int ArmedPanel;

var bool bInGame;
var bool bApplying;
var bool bGameplayActive;

var int HeaderTooltipState[4];
var bool bHeaderTexturesReady;
var bool bHeaderVertical;


// INICIALIZACAO

function OnLoad()
{
    Me = GetHandle("CompactToolbarWnd");

    LastLayoutSignature = "";
    ArmedPanel = -1;
    bInGame = false;
    bApplying = false;
    bGameplayActive = false;
    RegisterEvent(40);

    Expanded[0] = 0;
    Expanded[1] = 0;
    Expanded[2] = 0;
    Expanded[3] = 1;

    ResetHeaderAppearance();

    RegisterEvent(150);
    RegisterEvent(693);

    if(Me != None)
    {
        Me.KillTimer(80990);
        Me.SetTimer(80990, 250);
    }
}

function OnEnterState(name PreviousState)
{
    Me = GetHandle("CompactToolbarWnd");

    LastLayoutSignature = "";
    ArmedPanel = -1;
    bApplying = false;

    ResetHeaderAppearance();

    if(Me != None)
    {
        Me.KillTimer(80991);
        Me.KillTimer(80990);
        Me.SetTimer(80990, 250);
    }
}

function OnExitState(name NextState)
{
    bInGame = false;
    ArmedPanel = -1;
    bApplying = true;
     bGameplayActive = false;
    LastLayoutSignature = "";

    if(Me != None)
    {
        Me.KillTimer(80990);
        Me.KillTimer(80991);
    }

    HidePanels();

    if(Me != None)
        Me.HideWindow();

    ResetHeaderAppearance();

    bApplying = false;
}

function OnEvent(int EventID, string param)
{
    if(EventID == 150)
    {
        if(!bGameplayActive)
        {
            Expanded[0] = 0;
            Expanded[1] = 0;
            Expanded[2] = 0;
            Expanded[3] = 1;
            ArmedPanel = -1;
            if(Me != None)
                Me.KillTimer(80991);
        }

        bGameplayActive = true;
        LastLayoutSignature = "";

        ApplyLayout();
        return;
    }

    if(EventID == 40)
    {
        bGameplayActive = false;
        ApplyLayout();
        return;
    }

    if(EventID == 693)
        ApplyLayout();
}

function OnTimer(int TimerID)
{
    if(TimerID == 80991)
    {
        if(Me != None)
            Me.KillTimer(80991);

        ArmedPanel = -1;
        return;
    }

    if(TimerID == 80990)
        ApplyLayout();
}


// CLIQUES E SONS

function OnClickButton(string ID)
{
    switch(ID)
    {
        case "btnSoulshot":
            HeaderClick(0);
            break;

        case "btnPotions":
            HeaderClick(1);
            break;

        case "btnSkills":
            HeaderClick(2);
            break;

        case "btnMounts":
            HeaderClick(3);
            break;
    }
}

function HeaderClick(int Index)
{
    if(Index < 0 || Index > 3)
        return;

    if(Me == None || !bInGame)
        return;

    if(Expanded[Index] == 1)
    {
        Expanded[Index] = 0;

        ArmedPanel = -1;
        Me.KillTimer(80991);

        PlaySound("InterfaceSound.system_close_01");

        ApplyLayout();
        return;
    }

    if(ArmedPanel == Index)
    {
        Expanded[Index] = 1;

        ArmedPanel = -1;
        Me.KillTimer(80991);

        PlaySound("InterfaceSound.system_open_01");

        ApplyLayout();
        return;
    }

    ArmedPanel = Index;

    Me.KillTimer(80991);
    Me.SetTimer(80991, 400);
}

function bool IsPanelMinimized(int Index)
{
    if(Index < 0 || Index > 3)
        return false;

    return bInGame && (Expanded[Index] == 0);
}


// NOMES E ESPACAMENTO

function string PanelName(int Index)
{
    switch(Index)
    {
        case 0:
            return "AutoShotItemWnd";

        case 1:
            return "AutoPotionsWnd";

        case 2:
            return "AutoSkillSpamWnd";

        case 3:
            return "MountBtnWnd";
    }

    return "";
}

function string HeaderName(int Index)
{
    switch(Index)
    {
        case 0:
            return "btnSoulshot";

        case 1:
            return "btnPotions";

        case 2:
            return "btnSkills";

        case 3:
            return "btnMounts";
    }

    return "";
}

function int PanelSpan(int Index)
{
    if(Index < 0 || Index > 3)
        return 39;

    if(Expanded[Index] == 0)
        return 39;

    switch(Index)
    {
        case 0:
            return 107;

        case 1:
            return 212;

        case 2:
            return 176;

        case 3:
            return 80;
    }

    return 39;
}


// TOOLTIPS

function ResetHeaderAppearance()
{
    local int i;

    for(i = 0; i < 4; i++)
        HeaderTooltipState[i] = -1;

    bHeaderTexturesReady = false;
}

function string HeaderTitle(int Index)
{
    switch(Index)
    {
        case 0:
            return "Soulshots";

        case 1:
            return "Pocoes automaticas";

        case 2:
            return "Auto Attack";

        case 3:
            return "Montarias";
    }

    return "";
}

function string HeaderDescription(int Index)
{
    if(Index < 0 || Index > 3)
        return "";

    if(Expanded[Index] == 1)
    {
        switch(Index)
        {
            case 0:
                return "Clique para ocultar os soulshots";

            case 1:
                return "Clique para ocultar as pocoes";

            case 2:
                return "Clique para ocultar o Auto Attack";

            case 3:
                return "Clique para ocultar as montarias";
        }
    }
    else
    {
        switch(Index)
        {
            case 0:
                return "Duplo clique para exibir os soulshots";

            case 1:
                return "Duplo clique para exibir as pocoes";

            case 2:
                return "Duplo clique para exibir o Auto Attack";

            case 3:
                return "Duplo clique para exibir as montarias";
        }
    }

    return "";
}

function CustomTooltip MakeHeaderTooltip(int Index)
{
    local CustomTooltip Tooltip;
    local string Title;
    local string Description;
    local int LineWidth;
    local int TitleWidth;

    Title = HeaderTitle(Index);
    Description = HeaderDescription(Index);

    LineWidth = GetTextWidth(Description);
    TitleWidth = GetTextWidth(Title);

    if(TitleWidth > LineWidth)
        LineWidth = TitleWidth;

    Tooltip.DrawList.Length = 3;

    Tooltip.DrawList[0] = xxDrawText(
        Title,
        getAColor(255, 255, 255, 255),
        true, true, 2, 2
    );

    Tooltip.DrawList[1] = DrawTex(
        "L2UI_CH3.tooltip_line",
        LineWidth, 1,
        -1, -1,
        0, 3,
        true, true
    );

    Tooltip.DrawList[2] = xxDrawText(
        Description,
        getAColor(255, 200, 0, 255),
        true, true, 2, 2
    );

    return Tooltip;
}

function UpdateHeaderTooltip(int Index)
{
    local ButtonHandle HeaderButton;

    if(Index < 0 || Index > 3)
        return;

    if(HeaderTooltipState[Index] == Expanded[Index])
        return;

    HeaderButton = xxGetButtonHandle(
        "CompactToolbarWnd." $ HeaderName(Index)
    );

    if(HeaderButton == None)
        return;

    HeaderButton.SetTooltipCustomType(
        MakeHeaderTooltip(Index)
    );

    HeaderTooltipState[Index] = Expanded[Index];
}


// VISIBILIDADE

function SetVisible(string Path, bool Visible)
{
    local WindowHandle W;

    W = xxGetWindowHandle(Path);

    if(W == None)
        return;

    if(Visible)
    {
        if(!W.IsShowWindow())
            W.ShowWindow();
    }
    else
    {
        if(W.IsShowWindow())
            W.HideWindow();
    }
}

function HidePanels()
{
    local int i;

    for(i = 0; i < 4; i++)
        SetVisible(PanelName(i), false);

    SetVisible("AutoPotionsWnd.configPotions", false);
    SetVisible("MountBtnWnd.SetMountWnd", false);
}

function HideOldHeaders()
{
    SetVisible(
        "AutoPotionsWnd.header_potions_Texture",
        false
    );

    SetVisible(
        "AutoPotionsWnd.header_potions_Texture_v",
        false
    );

    SetVisible(
        "AutoSkillSpamWnd.header_skill",
        false
    );

    SetVisible(
        "AutoSkillSpamWnd.header_skill_v",
        false
    );

    SetVisible(
        "MountBtnWnd.header_pet",
        false
    );

    SetVisible(
        "AutoShotItemWnd.AutoShotItemWnd_HorWnd.header_weapon_Texture",
        false
    );

    SetVisible(
        "AutoShotItemWnd.AutoShotItemWnd_VerWnd.header_weapon_Texture",
        false
    );

    SetVisible("AutoPotionsWnd.btnPotion", false);
    SetVisible("AutoSkillSpamWnd.BtnAutoSkillAtk", false);
}


// TEXTURAS

function SetHeaderTexture(
    string ButtonName,
    string TextureName
)
{
    local ButtonHandle HeaderButton;

    HeaderButton = xxGetButtonHandle(
        "CompactToolbarWnd." $ ButtonName
    );

    if(HeaderButton == None)
        return;

    HeaderButton.SetTexture(
        TextureName,
        TextureName,
        TextureName
    );
}

function UpdateHeaderTextures(bool Vertical)
{
    if(bHeaderTexturesReady &&
        bHeaderVertical == Vertical)
        return;

    SetHeaderTexture(
        "btnSoulshot",
        "L2UI_CH3.AutoShotWnd.header_weapon"
    );

    if(Vertical)
    {
        SetHeaderTexture(
            "btnPotions",
            "L2UI_CH3.ICONS_ASSASSINS.New_AutoPotion_Header_v"
        );

        SetHeaderTexture(
            "btnSkills",
            "L2UI_CH3.AutoSkill.autoskillheader_v"
        );

        SetHeaderTexture(
            "btnMounts",
            "L2UI_CH3.MountWnd.header_pet_v"
        );
    }
    else
    {
        SetHeaderTexture(
            "btnPotions",
            "L2UI_CH3.ICONS_ASSASSINS.New_AutoPotion_Header"
        );

        SetHeaderTexture(
            "btnSkills",
            "L2UI_CH3.AutoSkill.autoskillheader_h"
        );

        SetHeaderTexture(
            "btnMounts",
            "L2UI_CH3.MountWnd.header_pet"
        );
    }

    bHeaderVertical = Vertical;
    bHeaderTexturesReady = true;

    HeaderTooltipState[0] = -1;
    HeaderTooltipState[1] = -1;
    HeaderTooltipState[2] = -1;
    HeaderTooltipState[3] = -1;
}


// POSICIONAMENTO INTERNO

function AnchorControl(
    string Path,
    string ParentPath,
    int X,
    int Y
)
{
    local WindowHandle W;

    W = xxGetWindowHandle(Path);

    if(W == None)
        return;

    W.ClearAnchor();

    W.SetAnchor(
        ParentPath,
        "TopLeft", "TopLeft",
        X, Y
    );
}

function UpdateSoulshotVisibility(bool Vertical)
{
    local bool ShowHorizontal;
    local bool ShowVertical;

    ShowHorizontal = (Expanded[0] == 1) && !Vertical;
    ShowVertical = (Expanded[0] == 1) && Vertical;

    SetVisible(
        "AutoShotItemWnd.AutoShotItemWnd_HorWnd",
        ShowHorizontal
    );

    SetVisible(
        "AutoShotItemWnd.AutoShotItemWnd_VerWnd",
        ShowVertical
    );

    SetVisible(
        "AutoShotItemWnd.AutoShotItemWnd_HorWnd.WeaponSlotGroupWnd_HorWnd",
        ShowHorizontal
    );

    SetVisible(
        "AutoShotItemWnd.AutoShotItemWnd_VerWnd.WeaponSlotGroupWnd_VerWnd",
        ShowVertical
    );
}

function PlacePanel(
    WindowHandle Panel,
    int Index,
    bool Vertical,
    int Offset
)
{
    local WindowHandle Slot;
    local int PanelOffset;
    local int i;

    if(Panel == None)
        return;

    PanelOffset = Offset;

    if(Index == 2)
        PanelOffset = PanelOffset + 39;

    Panel.ClearAnchor();

    if(Vertical)
    {
        Panel.SetAnchor(
            "CompactToolbarWnd",
            "TopLeft", "TopLeft",
            0, PanelOffset
        );
    }
    else
    {
        Panel.SetAnchor(
            "CompactToolbarWnd",
            "TopLeft", "TopLeft",
            PanelOffset, 0
        );
    }

    if(Index == 0)
    {
        AnchorControl(
            "AutoShotItemWnd.AutoShotItemWnd_HorWnd",
            "AutoShotItemWnd",
            0, 0
        );

        AnchorControl(
            "AutoShotItemWnd.AutoShotItemWnd_VerWnd",
            "AutoShotItemWnd",
            0, 0
        );

        AnchorControl(
            "AutoShotItemWnd.AutoShotItemWnd_HorWnd.WeaponSlotGroupWnd_HorWnd",
            "AutoShotItemWnd.AutoShotItemWnd_HorWnd",
            0, 0
        );

        AnchorControl(
            "AutoShotItemWnd.AutoShotItemWnd_VerWnd.WeaponSlotGroupWnd_VerWnd",
            "AutoShotItemWnd.AutoShotItemWnd_VerWnd",
            0, 0
        );

        SetVisible(
            "AutoShotItemWnd.AutoShotItemWnd_HorWnd",
            (Expanded[0] == 1) && !Vertical
        );

        SetVisible(
            "AutoShotItemWnd.AutoShotItemWnd_VerWnd",
            (Expanded[0] == 1) && Vertical
        );
    }

    if(Index == 2)
    {
        if(Vertical)
            Panel.SetWindowSize(32, 149);
        else
            Panel.SetWindowSize(149, 32);

        for(i = 1; i <= 5; i++)
        {
            Slot = xxGetWindowHandle(
                "AutoSkillSpamWnd.SkillBox_Spam_" $
                string(i)
            );

            if(Slot != None)
            {
                Slot.ClearAnchor();

                if(Vertical)
                {
                    Slot.SetAnchor(
                        "AutoSkillSpamWnd",
                        "TopLeft", "TopLeft",
                        0, (i - 1) * 29
                    );
                }
                else
                {
                    Slot.SetAnchor(
                        "AutoSkillSpamWnd",
                        "TopLeft", "TopLeft",
                        (i - 1) * 29, 0
                    );
                }
            }
        }
    }
}


// CONTROLE DAS ALTERACOES DO LAYOUT

function string ControlSignature(string Path)
{
    local WindowHandle W;
    local Rect R;

    W = xxGetWindowHandle(Path);

    if(W == None)
        return Path $ ":None;";

    R = class'NWindow.UIAPI_WINDOW'.static.GetRect(Path);

    return Path $ ":" $
        string(R.nX) $ "," $
        string(R.nY) $ "," $
        string(R.nWidth) $ "," $
        string(R.nHeight) $ "," $
        string(W.IsShowWindow()) $ ";";
}

function string BuildLayoutSignature(
    string ShortcutPath,
    bool Vertical
)
{
    local string Signature;
    local int i;

    Signature = ShortcutPath $ ":" $
        string(Vertical) $ ";" $
        ControlSignature(ShortcutPath) $
        ControlSignature("CompactToolbarWnd");

    for(i = 0; i < 4; i++)
    {
        Signature = Signature $
            string(Expanded[i]) $ ";" $
            ControlSignature(
                "CompactToolbarWnd." $ HeaderName(i)
            ) $
            ControlSignature(PanelName(i));
    }

    Signature = Signature $
        ControlSignature(
            "AutoShotItemWnd.AutoShotItemWnd_HorWnd"
        ) $
        ControlSignature(
            "AutoShotItemWnd.AutoShotItemWnd_VerWnd"
        ) $
        ControlSignature(
            "AutoShotItemWnd.AutoShotItemWnd_HorWnd.WeaponSlotGroupWnd_HorWnd"
        ) $
        ControlSignature(
            "AutoShotItemWnd.AutoShotItemWnd_VerWnd.WeaponSlotGroupWnd_VerWnd"
        );
    return Signature;
}


// ALINHAMENTO DA BARRA

function ApplyLayout()
{
    local UserInfo Info;
    local AutoPotionsWnd Potions;

    local WindowHandle Panel;
    local WindowHandle Header;

    local bool Vertical;
    local string ShortcutPath;
    local string LayoutSignature;

    local int Offset;
    local int i;

    if(bApplying || Me == None)
        return;

    bApplying = true;
     
    if(!bGameplayActive)
    {
        bInGame = false;
        ArmedPanel = -1;
        LastLayoutSignature = "";

        Me.KillTimer(80991);

        HidePanels();
        Me.HideWindow();

        bApplying = false;
        return;
    }

    if(!GetPlayerInfo(Info))
    {
        bInGame = false;
        LastLayoutSignature = "";

        HidePanels();
        Me.HideWindow();

        bApplying = false;
        return;
    }

    Potions = AutoPotionsWnd(
        GetScript("AutoPotionsWnd")
    );

    if(Potions == None)
    {
        LastLayoutSignature = "";
        bApplying = false;
        return;
    }

    bInGame = true;
    Vertical = Potions.IsVertical();

    if(Vertical)
    {
        ShortcutPath =
            "ShortcutWnd.ShortcutWndVertical" $
            Potions.GetExpandNum();
    }
    else
    {
        ShortcutPath =
            "ShortcutWnd.ShortcutWndHorizontal" $
            Potions.GetExpandNum();
    }

    LayoutSignature = BuildLayoutSignature(
        ShortcutPath,
        Vertical
    );

    if(LayoutSignature == LastLayoutSignature &&
        bHeaderTexturesReady &&
        HeaderTooltipState[0] == Expanded[0] &&
        HeaderTooltipState[1] == Expanded[1] &&
        HeaderTooltipState[2] == Expanded[2] &&
        HeaderTooltipState[3] == Expanded[3])
    {
        bApplying = false;
        return;
    }

    Me.ClearAnchor();

    if(Vertical)
    {
        Me.SetAnchor(
            ShortcutPath,
            "TopLeft", "TopLeft",
            -32, 0
        );
    }
    else
    {
        Me.SetAnchor(
            ShortcutPath,
            "TopLeft", "TopLeft",
            0, -32
        );
    }

    Offset = 0;

    for(i = 0; i < 4; i++)
    {
        Panel = xxGetWindowHandle(PanelName(i));

        Header = xxGetWindowHandle(
            "CompactToolbarWnd." $ HeaderName(i)
        );

        if(Header != None)
        {
            Header.ClearAnchor();

            if(Vertical)
            {
                Header.SetAnchor(
                    "CompactToolbarWnd",
                    "TopLeft", "TopLeft",
                    0, Offset
                );

                Header.SetWindowSize(30, 39);
            }
            else
            {
                Header.SetAnchor(
                    "CompactToolbarWnd",
                    "TopLeft", "TopLeft",
                    Offset, 0
                );

                Header.SetWindowSize(39, 30);
            }

            if(!Header.IsShowWindow())
                Header.ShowWindow();
        }

        if(Panel != None)
        {
            PlacePanel(
                Panel,
                i,
                Vertical,
                Offset
            );

            SetVisible(
                PanelName(i),
                (Expanded[i] == 1)
            );
        }

        Offset = Offset + PanelSpan(i);
    }

    if(Vertical)
        Me.SetWindowSize(32, Offset);
    else
        Me.SetWindowSize(Offset, 32);

    if(Expanded[1] == 0)
        SetVisible("AutoPotionsWnd.configPotions", false);

    if(Expanded[3] == 0)
        SetVisible("MountBtnWnd.SetMountWnd", false);
       
        UpdateSoulshotVisibility(Vertical);
         HideOldHeaders();

    UpdateHeaderTextures(Vertical);

    for(i = 0; i < 4; i++)
        UpdateHeaderTooltip(i);

    if(!Me.IsShowWindow())
        Me.ShowWindow();

    LastLayoutSignature = BuildLayoutSignature(
        ShortcutPath,
        Vertical
    );

    bApplying = false;
}


// COMPATIBILIDADE COM CHAMADAS ANTIGAS.
// NAO EMITEM MENSAGENS.

static function DebugLog(string Msg)
{
}

function DebugWindow(string Path)
{
}