class MobInformerWnd extends UICommonAPI;
// Explicit full coverage is required by the native viewport alpha compositor.

const RESPONSE_TIMER = 6580;
const ANIMATION_TIMER = 6581;
const PREVIEW_INIT_TIMER = 6582;
const MAX_REWARDS = 2048;
const REWARD_ICON_RENDER_TAG = 1296646991;
const PREVIEW_FRAME_TIMER = 6583;
const CATALOG_TIMER = 6584;
const TARGET_FOLLOW_TIMER = 6585;
const MAX_CATALOG_NPCS = 65535;
const SHIFT_OPEN_TIMER = 6586;
const STAGE_TIMER = 6587;
const BOOTSTRAP_TIMER = 6588;
const OPEN_QUERY_TIMER = 6589;
const BOOTSTRAP_SEND_TIMER = 6590;
const AUTO_CATALOG_TIMER = 6591;
const DETAIL_RETRY_TIMER = 6592;
const CATALOG_RETRY_TIMER = 6593;
const MAX_QUESTS = 2048;

struct MobReward
{
    var int ItemID;
    var string Minimum;
    var string Maximum;
    var string Chance;
    var string FallbackIcon;
    var string FallbackName;
    var bool Spoil;
};

struct MobQuest
{
    var int QuestID;
    var string Name;
    var int Roles;
};

struct BrowserNPC
{
    var int NPCID;
    var int displayID;
    var string Name;
    var int Level;
};
var array<BrowserNPC> BrowserRows;
var ListCtrlHandle NPCList;
var bool BrowserEnabled;
var bool WaitingCatalog;
var bool ReceivingCatalog;
var bool CatalogReady;
var int CatalogRequest;
var int CatalogPage;
var int CatalogMode;
var int CatalogTotal;
var int CatalogExpected;
var int ShiftTargetID;
var int ShiftBridgeTarget;
var bool ShiftBridgeEnabled;
var int ShiftOpenAttempts;
var string CatalogQuery;
var int SelectedTemplateID;
var string CurrentTargetName;
var int CurrentTargetLevel;
var bool HasMobLocation;
var WindowHandle Me;
var bool MileniumControlsReady;
var TextureHandle MobViewport;
var Actor PreviewActor;
var float PreviewFOV,PreviewRadius,PreviewHeight;
var int PreviewYaw;
var TextureHandle MobImage;
var TextBoxHandle ViewMessage;
var ListCtrlHandle RewardList;
var array<MobReward> MobRewardRows;
var int RequestID;
var int TargetObjectID;
var int ObservedTargetObjectID;
var int QueuedTargetObjectID;
var int PreviewNPCID;
var int ExpectedMobRewardRows;
var bool Pending;
var bool Receiving;
var bool Ready;
var bool ShowSpoil;
var bool ShowQuests, QuestsAvailable;
var int ExpectedQuestRows;
var ListCtrlHandle QuestList;
var array<MobQuest> MobQuestRows;
var bool ImageMode;
var string ImageTexture;
var bool ServerEnabled;
var bool WaitingBootstrap;
var bool BootstrapBrowser;
var bool WaitingOpenQuery;
var int BootstrapAttempts;
var int DetailAttempts;
var string PendingDetailCommand;
var string PendingCatalogCommand;
var int CatalogAttempts;
var bool PreviewEnabled;
var bool AnimationEnabled;
var Vector MobLocation;
var Vector MapMarkerLocation;
var bool HasMapMarker;
var int SelectedNPCClassID;
var float MobPreviewHeight;
var array<string> PreviewModelBounds;
var EditBoxHandle ItemSearch;
var WindowHandle TitleDragArea;
var bool DraggingTitle;
var int DragWindowX,DragWindowY;
var bool ViewportClick;
var int DragMouseX;
var int DragMouseY;
var int ViewMouseX;
var int ViewMouseY;
var int PreviewFitCameraDistance;
var int PreviewCameraDistance;
var array<Texture> StageFrames;
var string StageGroup;
var int StageFrame,StageNPCID,CurrentCategory;
var bool StagePlaying;
var TextureHandle StagePanel;

function OnRegisterEvent()
{

    RegisterEvent(EV_GamingStateEnter);
    RegisterEvent(EV_GamingStateExit);
    RegisterEvent(EV_Restart);
    RegisterEvent(EV_TargetUpdate);
    RegisterEvent(EV_TargetHideWindow);
}

function OnLoad()
{
    if (MileniumControlsReady) return;
    OnRegisterEvent();
    Me = GetWindowHandle("MobInformerWnd");
    Me.SetDraggable(false);
    TitleDragArea = GetWindowHandle("MobInformerWnd.TitleDragArea");
    TitleDragArea.SetDraggable(false);
    ItemSearch = GetEditBoxHandle("MobInformerWnd.ItemSearch");
    ItemSearch.SetTooltipCustomType(xxMakeTooltipSimpleText("NPC: nome ou ID. Drop / Spoil: item. Quest: nome ou ID da quest. Busca vazia lista todos."));
    NPCList = GetListCtrlHandle("MobInformerWnd.NPCList");

    GetButtonHandle("MobInformerWnd.LocationButton").SetTooltipCustomType(xxMakeTooltipSimpleText("Ver a localizacao no mapa"));
    MobViewport = GetTextureHandle("MobInformerWnd.MobViewport");
    StagePanel = GetTextureHandle("MobInformerWnd.PreviewBackground");
    MobImage = GetTextureHandle("MobInformerWnd.MobImage");
    ViewMessage = GetTextBoxHandle("MobInformerWnd.ViewMessage");
    RewardList = GetListCtrlHandle("MobInformerWnd.RewardList");


    QuestList = GetListCtrlHandle("MobInformerWnd.QuestList");


    GetButtonHandle("MobInformerWnd.QuestButton").SetTooltipCustomType(xxMakeTooltipSimpleText("Quests related to this NPC"));
    GetButtonHandle("MobInformerWnd.QuestButton").EnableWindow();
    GetCheckBoxHandle("MobInformerWnd.SearchQuest").EnableWindow();
    GetCheckBoxHandle("MobInformerWnd.SearchNPC").SetTitle("NPC");
    GetCheckBoxHandle("MobInformerWnd.SearchDrop").SetTitle("Drop");
    GetCheckBoxHandle("MobInformerWnd.SearchSpoil").SetTitle("Spoil");
    GetCheckBoxHandle("MobInformerWnd.SearchQuest").SetTitle("Quest");
    UpdateDetailTabs();
    MileniumControlsReady = true;
    Me.HideWindow();
}

function OnEvent(int eventID, string param)
{
    if (eventID == EV_TargetUpdate)
    {
        RequestShiftBridge();
        QueueTargetFollow();
        RenderTargetList();
        if (!Ready || TargetObjectID != class'UIDATA_TARGET'.static.GetTargetID()) RenderMobRewardRows();
        return;
    }
    if (eventID == EV_TargetHideWindow)
    {
        ShiftBridgeTarget=0;
        ObservedTargetObjectID = 0;
        ShiftTargetID = 0;
        Me.KillTimer(SHIFT_OPEN_TIMER);
        CancelTargetFollow();
        if (SelectedTemplateID <= 0)
        {
            Ready = false;
            Pending = false;
            Receiving = false;
            TargetObjectID = 0;
            Me.KillTimer(DETAIL_RETRY_TIMER);
            Me.KillTimer(RESPONSE_TIMER);
            Me.KillTimer(AUTO_CATALOG_TIMER);
            StopPreview();
            ClearTargetLabels("Selecione um alvo");
        }
        RenderTargetList();
        RenderMobRewardRows();
        return;
    }
    if (eventID == EV_GamingStateEnter)
    {
        ServerEnabled = false;
        BrowserEnabled = false;
        CatalogReady = false;
        QuestsAvailable = false;
        GetButtonHandle("MobInformerWnd.QuestButton").EnableWindow();
        GetCheckBoxHandle("MobInformerWnd.SearchQuest").EnableWindow();
        BrowserRows.Length = 0;
        NPCList.DeleteAllItem();
    }
    if (eventID == EV_GamingStateExit || eventID == EV_Restart)
    {
        ServerEnabled = false;
        ShiftTargetID = 0;
        Me.KillTimer(SHIFT_OPEN_TIMER);
        if (HasMapMarker)
        {
            class'UIAPI_MINIMAPCTRL'.static.DeleteTarget("MinimapWnd.Minimap", MapMarkerLocation);
            HasMapMarker = false;
        }
        Me.HideWindow();
    }
}

// The launcher must show feedback even if the Java/tutorial bridge is absent.
function OpenFromLauncher()
{
    local int objectID;
    if (!MileniumControlsReady) OnLoad();
    ShowWindowWithFocus("MobInformerWnd");
    Me.ShowWindow();
    Me.SetFocus();
    objectID = class'UIDATA_TARGET'.static.GetTargetID();
    // Repeated clicks must not reset request IDs or discard an in-flight reply.
    if (WaitingBootstrap) return;
    if (WaitingOpenQuery && ShiftTargetID == objectID) return;
    if ((Pending || Ready) && TargetObjectID == objectID && objectID > 0) return;
    CancelTargetFollow();
    Me.KillTimer(OPEN_QUERY_TIMER);
    if (ServerEnabled)
    {
        Me.KillTimer(DETAIL_RETRY_TIMER);
        Me.KillTimer(RESPONSE_TIMER);
        Me.KillTimer(AUTO_CATALOG_TIMER);
        Pending = false;
        Receiving = false;
        ShiftTargetID = objectID;
        ShiftOpenAttempts = 0;
        BootstrapBrowser = objectID <= 0;
        WaitingOpenQuery = true;
        GetTextBoxHandle("MobInformerWnd.RewardStatus").SetText("Atualizando alvo...");
        Me.SetTimer(OPEN_QUERY_TIMER, 800);
        return;
    }
    ResetDetail("Monster Data");
    Pending = false;
    Me.KillTimer(RESPONSE_TIMER);
    WaitingBootstrap = true;
    BootstrapAttempts = 0;
    ViewMessage.SetText("Aguardando resposta do servidor...");
    ViewMessage.ShowWindow();
    GetTextBoxHandle("MobInformerWnd.MobType").SetText("Conectando...");
    GetTextBoxHandle("MobInformerWnd.RewardStatus").SetText("Solicitando dados ao servidor...");
    Me.KillTimer(BOOTSTRAP_TIMER);
    Me.SetTimer(BOOTSTRAP_TIMER, 8000);
    // TargetStatusWnd sends NpcHeaderInfo on selection through the same flood gate.
    Me.SetTimer(BOOTSTRAP_SEND_TIMER, 800);
}

function bool IsAvailable()
{
    return ServerEnabled;
}

function ResetDetail(string npcName, optional bool preserveFocus)
{
    local int i;
    Me.KillTimer(DETAIL_RETRY_TIMER);
    Me.KillTimer(AUTO_CATALOG_TIMER);
    DetailAttempts = 0;
    PendingDetailCommand = "";
    RequestID++;
    if (RequestID <= 0 || RequestID > 1000000) RequestID = 1;
    Pending = true;
    Receiving = false;
    Ready = false;
    HasMobLocation = false;
    ShowSpoil = CatalogMode==2;
    CurrentTargetName=npcName;
    CurrentTargetLevel=0;
    ItemSearch.SetString("");
    PreviewNPCID = 0;
    MobRewardRows.Length = 0;
    MobQuestRows.Length = 0;
    ExpectedQuestRows = 0;
    QuestList.DeleteAllItem();
    QuestList.SetTooltipCustomType(xxMakeTooltipSimpleText("Quests related to this NPC"));
    ShowQuests = CatalogMode==3;
    UpdateDetailTabs();
    GetTextBoxHandle("MobInformerWnd.NoQuests").HideWindow();
    RewardList.DeleteAllItem();
    StopPreview();
    ClearProperties();
    GetTextBoxHandle("MobInformerWnd.Title").SetText("Monster Data");
    GetTextBoxHandle("MobInformerWnd.MobName").SetText(npcName);
    CurrentCategory=class'MobStageData'.static.Category(SelectedTemplateID);
    GetTextBoxHandle("MobInformerWnd.MobName").SetTextColor(NPCNameColor(CurrentCategory));
    GetTextBoxHandle("MobInformerWnd.MobType").SetText("Loading...");
    GetTextBoxHandle("MobInformerWnd.RewardStatus").SetText("");
    GetTextBoxHandle("MobInformerWnd.LocationText").SetText("");
    GetTextBoxHandle("MobInformerWnd.NoItems").HideWindow();
    GetButtonHandle("MobInformerWnd.LocationButton").DisableWindow();
    for (i = 0; i < 8; i++) GetTextBoxHandle("MobInformerWnd.Stat" $ string(i)).SetText("");
    Me.SetDraggable(false);
    Me.ShowWindow();
    if (!preserveFocus)
    {
        Me.SetFocus();
        Me.SetFocus();
    }
    Me.KillTimer(RESPONSE_TIMER);
    Me.SetTimer(RESPONSE_TIMER, 8000);
    RenderTargetList();
}

// Target updates only queue work while the Informer is visible. Repeated
// updates for the same target do not request data or restart the delay.
function CancelTargetFollow()
{
    QueuedTargetObjectID = 0;
    Me.KillTimer(TARGET_FOLLOW_TIMER);
}

function QueueTargetFollow()
{
    local int objectID;
    if (!ServerEnabled || WaitingBootstrap || !Me.IsShowWindow()) return;
    objectID = class'UIDATA_TARGET'.static.GetTargetID();
    if (WaitingOpenQuery && ShiftTargetID == objectID) return;
    if (objectID == QueuedTargetObjectID && objectID > 0) return;
    if (objectID == ObservedTargetObjectID && objectID > 0) return;
    if (objectID == TargetObjectID && (Pending || Ready)) return;
    ObservedTargetObjectID = objectID;
    CancelTargetFollow();
    if (objectID <= 0) return;
    WaitingOpenQuery = false;
    Me.KillTimer(OPEN_QUERY_TIMER);
    Me.KillTimer(DETAIL_RETRY_TIMER);
    Me.KillTimer(RESPONSE_TIMER);
    Me.KillTimer(AUTO_CATALOG_TIMER);
    Pending = false;
    Receiving = false;
    Ready = false;
    StopPreview();
    MobRewardRows.Length=0;
    MobQuestRows.Length=0;
    ClearTargetLabels("Atualizando alvo...");
    QueuedTargetObjectID = objectID;
    // Resolve UserInfo after the selection event, not before native target data settles.
    Me.SetTimer(TARGET_FOLLOW_TIMER, 800);
}

function OpenTarget()
{
    OpenTargetDetails(false);
}

function OpenTargetDetails(bool fromTargetChange)
{
    local UserInfo info;
    if (!ServerEnabled) return;
    if (!GetTargetInfo(info) || !info.bNpc || info.bPet) return;
    SelectedNPCClassID = info.nClassID;
    SelectedTemplateID = 0;
    if (Pending && TargetObjectID == class'UIDATA_TARGET'.static.GetTargetID()) return;
    TargetObjectID = class'UIDATA_TARGET'.static.GetTargetID();
    if (TargetObjectID <= 0) return;
    CancelTargetFollow();
    ObservedTargetObjectID = TargetObjectID;
    ResetDetail(info.Name, fromTargetChange);
    PendingDetailCommand = "milenium_mobinfo show " $ string(TargetObjectID) $ " " $ string(RequestID);
    SendDetailQuery();
}

function OpenTemplate(int npcID, int displayID, string npcName)
{
    // Legacy entry point: always use the actual target, never the supplied template.
    OpenFromLauncher();
}

function SendDetailQuery()
{
    Me.KillTimer(DETAIL_RETRY_TIMER);
    if (!Pending || Receiving || !Me.IsShowWindow() || PendingDetailCommand == "" || DetailAttempts >= 3) return;
    DetailAttempts++;
    RequestBypassToServer(PendingDetailCommand);
    if (DetailAttempts < 3) Me.SetTimer(DETAIL_RETRY_TIMER, 1200);
}

function ClearProperties()
{
    local int i;
    for (i = 0; i < 10; i++) GetTextureHandle("MobInformerWnd.Property" $ string(i)).HideWindow();
}

function ShowProperties()
{
    local array<int> properties;
    local int i;
    local int shown;
    local string iconName;
    local TextureHandle icon;
    local TargetStatusWnd formatter;
    ClearProperties();
    if (PreviewNPCID <= 0) return;
    class'UIDATA_NPC'.static.GetNpcProperty(PreviewNPCID, properties);
    formatter = TargetStatusWnd(GetScript("TargetStatusWnd"));
    for (i = 0; i + 1 < properties.Length && shown < 10; i += 2)
    {
        iconName = class'UIDATA_SKILL'.static.GetIconName(properties[i], properties[i + 1]);
        if (iconName == "" || Caps(iconName) == "NONE" || Caps(iconName) == "UNDEFINED") continue;
        icon = GetTextureHandle("MobInformerWnd.Property" $ string(shown));
        icon.SetTexture(iconName);
        if (formatter != None) icon.SetTooltipCustomType(formatter.SetNpcInfoTooltip(properties[i], properties[i + 1]));
        icon.ShowWindow();
        shown++;
    }
}

// The former search controls now filter only the selected target's data.
function ClearTargetLabels(string displayText)
{
    local int i;
    CurrentTargetName="";
    CurrentTargetLevel=0;
    HasMobLocation=false;
    GetTextBoxHandle("MobInformerWnd.MobName").SetText(displayText);
    GetTextBoxHandle("MobInformerWnd.MobType").SetText("");
    GetTextBoxHandle("MobInformerWnd.LocationText").SetText("");
    GetButtonHandle("MobInformerWnd.LocationButton").DisableWindow();
    ClearProperties();
    for(i=0;i<8;i++)GetTextBoxHandle("MobInformerWnd.Stat"$string(i)).SetText("");
}

function RequestCatalog(bool newSearch)
{
    RenderTargetList();
}

function SendCatalogQuery() { Me.KillTimer(CATALOG_RETRY_TIMER); }
function ApplyCatalogPacket(string data) { } // Ignore stale replies from older global searches.
function FailCatalog(string message) { Me.KillTimer(CATALOG_TIMER); }

function bool HasCurrentTargetSession()
{
    return TargetObjectID > 0 && TargetObjectID == class'UIDATA_TARGET'.static.GetTargetID();
}

function bool TargetListMatches(string text, int id)
{
    local string query;
    query=NormalizeSearch(ItemSearch.GetString());
    return query=="" || InStr(NormalizeSearch(text),query)>=0 || query==string(id);
}

function InsertTargetMessage(string message)
{
    local LVDataRecord record;
    record.LVDataList.Length=2;
    record.LVDataList[0].szData=message;
    record.LVDataList[0].bUseTextColor=true;
    record.LVDataList[0].TextColor=MileniumMobColor(220,220,220,255);
    record.LVDataList[1].szData="";
    record.nReserved1=-1;
    NPCList.InsertRecord(record);
}

function RenderTargetList()
{
    local UserInfo info;
    local LVDataRecord record;
    local string entryName;
    local int level,i,count,total;
    NPCList.DeleteAllItem();
    GetTextBoxHandle("MobInformerWnd.NPCHeader").SetText("Nome");
    GetTextBoxHandle("MobInformerWnd.LevelHeader").SetText("");
    if(!GetTargetInfo(info) || !info.bNpc || info.bPet || info.nID!=class'UIDATA_TARGET'.static.GetTargetID())
    {
        InsertTargetMessage("Selecione um alvo");
        return;
    }
    if(CatalogMode==0)
    {
        entryName=info.Name;level=info.nLevel;
        if(HasCurrentTargetSession() && CurrentTargetName!="")entryName=CurrentTargetName;
        if(HasCurrentTargetSession() && CurrentTargetLevel>0)level=CurrentTargetLevel;
        GetTextBoxHandle("MobInformerWnd.LevelHeader").SetText("Nivel");
        record.LVDataList.Length=2;
        record.LVDataList[0].szData=entryName;
        record.LVDataList[0].bUseTextColor=true;
        record.LVDataList[0].TextColor=MileniumMobColor(255,255,255,255);
        record.LVDataList[1].szData=string(level);
        record.nReserved1=0;record.szReserved=entryName$" - Nivel "$string(level);
        NPCList.InsertRecord(record);
        NPCList.SetTooltipCustomType(xxMakeTooltipSimpleText(record.szReserved));
        return;
    }
    if(!Ready || !HasCurrentTargetSession())
    {
        InsertTargetMessage("Carregando...");
        return;
    }
    record.LVDataList.Length=2;
    record.LVDataList[0].bUseTextColor=true;
    record.LVDataList[0].TextColor=MileniumMobColor(255,255,255,255);
    record.LVDataList[1].szData="";
    if(CatalogMode==3)
    {
        for(i=0;i<MobQuestRows.Length;i++)
        {
            total++;
            entryName=MobQuestRows[i].Name;
            if(!TargetListMatches(entryName,MobQuestRows[i].QuestID))continue;
            record.LVDataList[0].szData=entryName;
            record.szReserved=entryName;
            record.nReserved1=i;record.nReserved2=MobQuestRows[i].QuestID;
            NPCList.InsertRecord(record);count++;
        }
    }
    else
    {
        for(i=0;i<MobRewardRows.Length;i++)
        {
            if(MobRewardRows[i].Spoil!=(CatalogMode==2))continue;
            total++;
            entryName=class'UIDATA_ITEM'.static.GetItemName(MobRewardRows[i].ItemID);
            if(entryName=="")entryName=MobRewardRows[i].FallbackName;
            if(entryName=="")entryName="Item #"$string(MobRewardRows[i].ItemID);
            if(!TargetListMatches(entryName,MobRewardRows[i].ItemID))continue;
            record.LVDataList[0].szData=entryName;record.szReserved=entryName;
            record.nReserved1=i;record.nReserved2=MobRewardRows[i].ItemID;
            NPCList.InsertRecord(record);count++;
        }
    }
    if(count==0)
    {
        if(total==0)InsertTargetMessage("Sem drop");
        else InsertTargetMessage("Sem resultado");
    }
}

function SelectTargetMode(int mode)
{
    if(CatalogMode!=mode)ItemSearch.SetString("");
    CatalogMode=mode;
    ShowSpoil=mode==2;
    ShowQuests=mode==3;
    GetCheckBoxHandle("MobInformerWnd.SearchNPC").SetCheck(mode==0);
    GetCheckBoxHandle("MobInformerWnd.SearchDrop").SetCheck(mode==1);
    GetCheckBoxHandle("MobInformerWnd.SearchSpoil").SetCheck(mode==2);
    GetCheckBoxHandle("MobInformerWnd.SearchQuest").SetCheck(mode==3);
    UpdateDetailTabs();
    RenderTargetList();
    RenderMobRewardRows();
}

function OnClickListCtrlRecord(string listID)
{
    local LVDataRecord record;
    if(listID=="NPCList")
    {
        record=NPCList.GetSelectedRecord();
        if(record.nReserved1>=0 && record.szReserved!="")
            NPCList.SetTooltipCustomType(xxMakeTooltipSimpleText(record.szReserved));
    }
    else if(listID=="QuestList")
    {
        record=QuestList.GetSelectedRecord();
        if(record.nReserved1>=0 && record.nReserved1<MobQuestRows.Length)
            QuestList.SetTooltipCustomType(xxMakeTooltipSimpleText(MobQuestRows[record.nReserved1].Name));
    }
}

function OnClickCheckBox(string checkID)
{
    switch(checkID)
    {
        case "SearchNPC": SelectTargetMode(0);break;
        case "SearchDrop": SelectTargetMode(1);break;
        case "SearchSpoil": SelectTargetMode(2);break;
        case "SearchQuest": SelectTargetMode(3);break;
    }
}

function string ReadField(string data, string key)
{
    local int position;
    local int endPosition;
    local string work;
    local string tag;
    tag = "|" $ key $ "=";
    position = InStr(data, tag);
    if (position < 0) return "";
    work = Mid(data, position + Len(tag));
    endPosition = InStr(work, "|");
    if (endPosition >= 0) return Left(work, endPosition);
    return work;
}

function string TakeField(out string data, string delimiter)
{
    local int position;
    local string result;
    position = InStr(data, delimiter);
    if (position < 0)
    {
        result = data;
        data = "";
        return result;
    }
    result = Left(data, position);
    data = Mid(data, position + Len(delimiter));
    return result;
}

function ApplyPacket(string data)
{
    local string packetType;
    local string work;
    local string row;
    local string kind;
    local MobReward reward;
    local MobQuest quest;
    local int displayID;
    local int index;
    local MobInfoLauncherWnd launcherScript;
    if (!MileniumControlsReady) OnLoad();
    packetType = ReadField(data, "TYPE");
    if (packetType == "FEATURES")
    {
        WaitingBootstrap = false;
        Me.KillTimer(BOOTSTRAP_SEND_TIMER);
        Me.KillTimer(BOOTSTRAP_TIMER);
        GetTextBoxHandle("MobInformerWnd.MobType").SetText("Servidor conectado.");
        ServerEnabled = ReadField(data, "VISIBLE") == "1";
        PreviewEnabled = ReadField(data, "PREVIEW") == "1";
        AnimationEnabled = ReadField(data, "ANIMATION") == "1";
        BrowserEnabled = false; // This panel is exclusively bound to the current target.
        ShiftBridgeEnabled = ReadField(data,"SHIFT") == "1";
        QuestsAvailable = ReadField(data, "QUESTS") == "1";
        ShowQuests=CatalogMode==3;
        ShowSpoil=CatalogMode==2;
        GetButtonHandle("MobInformerWnd.QuestButton").EnableWindow();
        GetCheckBoxHandle("MobInformerWnd.SearchQuest").EnableWindow();
        UpdateDetailTabs();
        if (!ServerEnabled) Me.HideWindow();
        launcherScript = MobInfoLauncherWnd(GetScript("MobInfoLauncherWnd"));
        if (launcherScript != None) launcherScript.UpdateVisibility();
        return;
    }
    if (packetType == "OPEN_BROWSER")
    {
        BootstrapBrowser = true;
        WaitingOpenQuery = true;
        Me.SetTimer(OPEN_QUERY_TIMER, 800);
        return;
    }
    if (packetType == "OPEN")
    {
        if (!ServerEnabled) return;
        // A server Shift-click request must open the window without the launcher.
        ShowWindowWithFocus("MobInformerWnd");
        if ((Pending || Ready) && TargetObjectID == int(ReadField(data,"OBJECT")) && HasCurrentTargetSession()) return;
        ShiftTargetID = int(ReadField(data, "OBJECT"));
        ShiftOpenAttempts = 0;
        BootstrapBrowser = false;
        WaitingOpenQuery = true;
        // The server's default bypass flood interval is 500 ms.
        // Wait before the follow-up query instead of bypassing that protection.
        Me.SetTimer(OPEN_QUERY_TIMER, 800);
        return;
    }
    if (!Pending || !Me.IsShowWindow() || int(ReadField(data, "REQ")) != RequestID) return;
    if (packetType == "ERROR")
    {
        FailRequest(ReadField(data, "TEXT"));
        return;
    }
    if (packetType == "BEGIN")
    {
        if (int(ReadField(data, "OBJECT")) != TargetObjectID) return;
        if (SelectedTemplateID > 0 && int(ReadField(data, "NPC")) != SelectedTemplateID) return;
        ExpectedMobRewardRows = int(ReadField(data, "ROWS"));
        ExpectedQuestRows = int(ReadField(data, "QUESTS"));
        if (ExpectedMobRewardRows < 0 || ExpectedMobRewardRows > MAX_REWARDS || ExpectedQuestRows < 0 || ExpectedQuestRows > MAX_QUESTS)
        {
            FailRequest("Lista de recompensas invalida.");
            return;
        }
        Receiving = true;
        MobRewardRows.Length = 0;
        MobQuestRows.Length = 0;
        CurrentTargetName=ReadField(data,"NAME");
        CurrentTargetLevel=int(ReadField(data,"LEVEL"));
        GetTextBoxHandle("MobInformerWnd.MobName").SetText(CurrentTargetName);
        RenderTargetList();
        CurrentCategory=ResolveMobCategory(int(ReadField(data,"NPC")),ReadField(data,"KIND"));
        GetTextBoxHandle("MobInformerWnd.MobName").SetTextColor(NPCNameColor(CurrentCategory));
        GetTextBoxHandle("MobInformerWnd.Title").SetText("Monster Data");
        GetTextBoxHandle("MobInformerWnd.MobType").SetText("Lv. " $ ReadField(data, "LEVEL"));
        GetTextBoxHandle("MobInformerWnd.Stat0").SetText(MakeCostString(ReadField(data, "HP")));
        GetTextBoxHandle("MobInformerWnd.Stat1").SetText(MakeCostString(ReadField(data, "MP")));
        GetTextBoxHandle("MobInformerWnd.Stat2").SetText(MakeCostString(ReadField(data, "PATK")));
        GetTextBoxHandle("MobInformerWnd.Stat3").SetText(MakeCostString(ReadField(data, "PDEF")));
        GetTextBoxHandle("MobInformerWnd.Stat6").SetText(MakeCostString(ReadField(data, "MATK")));
        GetTextBoxHandle("MobInformerWnd.Stat7").SetText(MakeCostString(ReadField(data, "MDEF")));
        GetTextBoxHandle("MobInformerWnd.Stat4").SetText(MakeCostString(ReadField(data, "EXP")));
        GetTextBoxHandle("MobInformerWnd.Stat5").SetText(MakeCostString(ReadField(data, "SP")));
        MobLocation.X = float(ReadField(data, "X"));
        MobLocation.Y = float(ReadField(data, "Y"));
        MobLocation.Z = float(ReadField(data, "Z"));
        HasMobLocation = ReadField(data, "HASSITE") == "1";
        if (HasMobLocation)
        {
            GetTextBoxHandle("MobInformerWnd.LocationText").SetText("View location on map ->");
            GetButtonHandle("MobInformerWnd.LocationButton").EnableWindow();
        }
        else GetTextBoxHandle("MobInformerWnd.LocationText").SetText("No hunting zone registered.");
        displayID = int(ReadField(data, "DISPLAY"));
        PreviewNPCID = ResolveNPCID(displayID);
        if (PreviewNPCID <= 0) PreviewNPCID = ResolveNPCID(SelectedNPCClassID);
        if (PreviewNPCID <= 0) PreviewNPCID = ResolveNPCID(int(ReadField(data, "NPC")));
        ImageTexture = "";
        if (PreviewNPCID > 0) ImageTexture = GetModelIcon(PreviewNPCID);
        ImageMode = false;
        PreviewEnabled = ReadField(data, "PREVIEW") == "1";
        AnimationEnabled = ReadField(data, "ANIMATION") == "1";
        Log("SP26 selection: server=" $ ReadField(data, "NPC") $ " display=" $ displayID $ " targetClass=" $ SelectedNPCClassID $ " resolved=" $ PreviewNPCID $ " mesh=" $ GetModelMesh(PreviewNPCID) $ " preview=" $ string(PreviewEnabled), 'MobInformer');
        ShowProperties();
        StartPreview(float(ReadField(data, "HEIGHT")));
        return;
    }
    if (!Receiving) return;
    if (packetType == "ITEMS")
    {
        if (int(ReadField(data, "OFFSET")) != MobRewardRows.Length)
        {
            FailRequest("Recebimento incompleto. Selecione novamente.");
            return;
        }
        work = ReadField(data, "DATA");
        while (work != "")
        {
            row = TakeField(work, ";");
            if (row == "") continue;
            kind = TakeField(row, ",");
            reward.ItemID = int(TakeField(row, ","));
            reward.Minimum = TakeField(row, ",");
            reward.Maximum = TakeField(row, ",");
            reward.Chance = TakeField(row, ",");
            reward.FallbackIcon = TakeField(row, ",");
            reward.FallbackName = TakeField(row, ",");
            reward.Spoil = kind == "S";
            if ((kind != "D" && kind != "S") || reward.ItemID <= 0 || row != "" || MobRewardRows.Length >= ExpectedMobRewardRows)
            {
                FailRequest("Lista de recompensas invalida.");
                return;
            }
            index = MobRewardRows.Length;
            MobRewardRows.Length = index + 1;
            MobRewardRows[index] = reward;
        }
        return;
    }
    if (packetType == "QUESTS")
    {
        if (int(ReadField(data, "OFFSET")) != MobQuestRows.Length)
        {
            FailRequest("Recebimento de quests incompleto. Selecione novamente.");
            return;
        }
        work = ReadField(data, "DATA");
        while (work != "")
        {
            row = TakeField(work, ";");
            if (row == "") continue;
            quest.QuestID = int(TakeField(row, "^"));
            quest.Name = TakeField(row, "^");
            quest.Roles = int(TakeField(row, "^"));
            if (quest.QuestID <= 0 || quest.QuestID > 1000000 || quest.Name == "" || quest.Roles < 1 || quest.Roles > 15 || row != "" || MobQuestRows.Length >= ExpectedQuestRows)
            {
                FailRequest("Lista de quests invalida.");
                return;
            }
            index = MobQuestRows.Length;
            MobQuestRows.Length = index + 1;
            MobQuestRows[index] = quest;
        }
        return;
    }
    if (packetType == "END")
    {
        if (MobRewardRows.Length != ExpectedMobRewardRows || int(ReadField(data, "COUNT")) != ExpectedMobRewardRows || MobQuestRows.Length != ExpectedQuestRows || int(ReadField(data, "QUESTCOUNT")) != ExpectedQuestRows)
        {
            FailRequest("Recebimento incompleto. Selecione novamente.");
            return;
        }
        Me.KillTimer(RESPONSE_TIMER);
        Me.KillTimer(DETAIL_RETRY_TIMER);
        Pending = false;
        Receiving = false;
        Ready = true;
        RenderMobRewardRows();
        RenderTargetList();
    }
}

function bool HasNPCMesh(int npcID) { local string meshPath,skins;local float radius,height;return class'MobModelData'.static.Lookup(npcID,meshPath,skins,radius,height); }

function int ResolveNPCID(int npcID)
{
    // NPCViewer/MonsterBook use the direct NPC data ID. A real mesh, rather
    // than the broad IsValidData check, identifies a usable preview entry.
    if (HasNPCMesh(npcID)) return npcID;
    if (npcID >= 1000000 && HasNPCMesh(npcID - 1000000)) return npcID - 1000000;
    return 0;
}

function StartPreview(float collisionHeight)
{
    StopPreview();
    MobPreviewHeight = collisionHeight;
    LoadMobStage(); ShowMobStage();
    if (PreviewNPCID <= 0 || !PreviewEnabled) { ShowNPCImage(); return; }
    ImageMode = false;
    MobViewport.ShowWindow();
    Me.SetTimer(PREVIEW_INIT_TIMER,100);
}

// Client mesh bounds include the mesh import scale. Collision cylinders do
// not describe the rendered model. Profiles contain mesh|radius|height.




function ApplyPreviewFrame() { if (PreviewActor != None) class'MobPreviewNative'.static.BindPreview(MobViewport, PreviewActor, PreviewFOV,PreviewRadius,PreviewHeight); }

function SpawnPreviewNPC()
{
    local Actor origin;
    local Mesh model;
    local Material skin;
    local string meshPath,skinPaths,skinPath;
    local float radius,height;
    local int i,status;
    local vector previewLocation;
    if (!Me.IsShowWindow() || PreviewNPCID <= 0) return;
    if (!class'MobModelData'.static.Lookup(PreviewNPCID,meshPath,skinPaths,radius,height)) { ShowNPCImage(); return; }
    model = Mesh(DynamicLoadObject(meshPath,class'Mesh',true));
    if (model == None) { ViewMessage.SetText("Modelo indisponivel neste cliente.");ViewMessage.ShowWindow();return; }
    origin = class'MobPreviewNative'.static.GetWorldActor(class'UIDATA_PLAYER'.static.GetPlayerID());
    if (origin == None) { ViewMessage.SetText("Entre no jogo para ver o monstro.");ViewMessage.ShowWindow();return; }
    previewLocation = origin.Location; previewLocation.Z = -150000.0;
    PreviewActor = origin.Spawn(class'MobPreviewActor',origin,,previewLocation);
    if (PreviewActor == None) { ViewMessage.SetText("Nao foi possivel criar a previa.");ViewMessage.ShowWindow();return; }
    PreviewActor.LinkMesh(model);PreviewActor.SetCollision(false,false,false);PreviewActor.SetPhysics(PHYS_None);
    for (i=0;i<8 && skinPaths!="";i++)
    {
        skinPath=TakeField(skinPaths,",");
        skin=Material(DynamicLoadObject(skinPath,class'Material',true));
        if(skin!=None) PreviewActor.Skins[i]=skin;
    }
    PreviewActor.SetDrawScale(1.0);
    PreviewYaw=1232;SetPreviewRotation();
    PreviewFOV=35.0;PreviewRadius=radius;PreviewHeight=height;
    status=class'MobPreviewNative'.static.BindPreview(MobViewport,PreviewActor,PreviewFOV,radius,height);
    if(status<=0) { ViewMessage.SetText("Previa 3D indisponivel ("$string(status)$").");ViewMessage.ShowWindow();return; }
    ViewMessage.HideWindow();MobImage.HideWindow();
    // This is a display Actor, not a world NPC/Pawn with effect state.
    // Suppress channel-0 notifies before and after starting its idle animation.
    // Engine.Actor.EnableChannelNotify(int Channel, int Switch) is present in
    // the supplied Engine.u; Switch=0 disables notifications on this actor only.
    PreviewActor.EnableChannelNotify(0,0);
    if(AnimationEnabled && PreviewActor.HasAnim('Wait'))
    {
        PreviewActor.LoopAnim('Wait',1.0,0.0,0);
        PreviewActor.EnableChannelNotify(0,0);
    }
    Me.SetTimer(ANIMATION_TIMER,1000);
    ShowMobStage();
}

// NPC classification comes from this server's template hierarchy. The live
// packet remains authoritative, with epic template/AI knowledge for catalog mode.
function int ResolveMobCategory(int npcID,string kind)
{
    local int known;
    known=class'MobStageData'.static.Category(npcID);
    if(known==3||Caps(kind)=="EPIC BOSS")return 3;
    if(Caps(kind)=="RAID BOSS")return 2;
    if(Caps(kind)=="MONSTER")return 1;
    if(Caps(kind)=="NPC")return 0;
    return known;
}
function Color NPCNameColor(int category)
{
    local Color tint;
    tint.R=255;tint.G=255;tint.B=255;tint.A=255;
    if(category==2||category==3){tint.G=160;tint.B=64;}
    return tint;
}
static function string MobStageGroup(int category)
{
    if(category==3)return "Epic";
    if(category==2)return "RaidBoss";
    if(category==1)return "Monster";
    if(category==0)return "NPC";
    return "Unknown";
}
function bool MobStageBridgeReady() { return true; }
// Both surfaces use the same animation tick and the same full-panel scene.
// SetTextureSize sets source UV extents; the existing control rectangle stays put.
function SetPanelFrame(Texture frame) { if(frame==None)return;if(StagePanel!=None)StagePanel.SetTextureWithObject(frame);class'MobPreviewNative'.static.SetStageBackground(frame); }

function bool LoadMobStage()
{
    local int i;local string group;
    group=MobStageGroup(CurrentCategory);
    if(StageGroup==group && StageFrames.Length==48)return true;
    StageFrames.Length=48;StageFrame=0;
    for(i=0;i<48;i++) StageFrames[i]=Texture(DynamicLoadObject("MileniumMonsterArt.MobIL"$group$".Frame_"$Right("000"$string(i),3),class'Texture',true));
    StageGroup=group;SetPanelFrame(StageFrames[0]);return StageFrames[0]!=None;
}
function StopMobStage() { Me.KillTimer(STAGE_TIMER);StagePlaying=false;StageFrame=0; }
function ShowMobStage() { if(!Me.IsShowWindow() || !LoadMobStage())return;SetPanelFrame(StageFrames[StageFrame]);if(!StagePlaying){Me.SetTimer(STAGE_TIMER,125);StagePlaying=true;} }
function AdvanceMobStage() { if(!Me.IsShowWindow() || StageFrames.Length!=48){StopMobStage();return;}StageFrame=(StageFrame+1)%48;SetPanelFrame(StageFrames[StageFrame]); }

function StopPreview()
{
    StopMobStage();Me.KillTimer(PREVIEW_INIT_TIMER);Me.KillTimer(PREVIEW_FRAME_TIMER);Me.KillTimer(ANIMATION_TIMER);
    class'MobPreviewNative'.static.ReleasePreview();
    if(PreviewActor!=None) { PreviewActor.Destroy();PreviewActor=None; }
    MobImage.HideWindow();ViewMessage.HideWindow();
}

function bool HasNPCImage()
{
    return ImageTexture != "" && Caps(ImageTexture) != "NONE" && Caps(ImageTexture) != "UNDEFINED";
}

function ShowNPCImage() { ViewMessage.SetText("Modelo 3D nao disponivel.");ViewMessage.ShowWindow();MobImage.HideWindow(); }

function StepZoom(bool zoomOut) { if(PreviewActor==None)return;if(zoomOut)PreviewFOV=FMin(90.0,PreviewFOV*1.12);else PreviewFOV=FMax(8.0,PreviewFOV/1.12);ApplyPreviewFrame(); }

function string ClipRewardLabel(string text)
{
    local int width;
    local int height;
    local string original;
    original = text;
    GetTextSize(text, width, height);
    if (width <= 140) return text;
    while (Len(text) > 0)
    {
        text = Left(text, Len(text) - 1);
        GetTextSize(text $ "...", width, height);
        if (width <= 140) return text $ "...";
    }
    return original;
}

function string NormalizeSearch(string text)
{
    while (Len(text) > 0 && (Left(text, 1) == " " || Left(text, 1) == Chr(9))) text = Mid(text, 1);
    while (Len(text) > 0 && (Right(text, 1) == " " || Right(text, 1) == Chr(9))) text = Left(text, Len(text) - 1);
    return Caps(text);
}

function string FormatRewardChance(string value)
{
    local float chance;
    local int hundredths;
    local int remainder;
    local string fraction;
    chance = float(value);
    if (chance > 0.0 && chance < 0.01) return "<0.01";
    hundredths = int(chance * 100.0 + 0.5);
    remainder = hundredths % 100;
    if (remainder == 0) return string(hundredths / 100);
    fraction = string(remainder);
    if (remainder < 10) fraction = "0" $ fraction;
    if (Right(fraction, 1) == "0") fraction = Left(fraction, 1);
    return string(hundredths / 100) $ "." $ fraction;
}

function OnKeyUp(EInputKey key) { if(key==IK_Enter)RequestCatalog(true);else if(key==IK_Escape)Me.HideWindow(); }

function bool HasIconName(string iconName)
{
    return iconName != "" && Caps(iconName) != "NONE" && Caps(iconName) != "UNDEFINED";
}

function RenderMobRewardRows()
{
    local int i,count;
    local string fullName,icon;
    local Texture itemTexture;
    local LVDataRecord record;
    if(ShowQuests) { RenderQuestRows();return; }
    GetTextBoxHandle("MobInformerWnd.NoQuests").HideWindow();RewardList.DeleteAllItem();
    GetTextBoxHandle("MobInformerWnd.NoItems").SetText("Sem drop");
    if(!Ready || !HasCurrentTargetSession())
    {
        if(class'UIDATA_TARGET'.static.GetTargetID()>0)GetTextBoxHandle("MobInformerWnd.NoItems").SetText("Carregando...");
        else GetTextBoxHandle("MobInformerWnd.NoItems").SetText("Selecione um alvo");
        GetTextBoxHandle("MobInformerWnd.NoItems").ShowWindow();
        GetTextBoxHandle("MobInformerWnd.RewardStatus").SetText("");
        return;
    }
    for(i=0;i<MobRewardRows.Length;i++)
    {
        if(MobRewardRows[i].Spoil!=ShowSpoil)continue;
        fullName=class'UIDATA_ITEM'.static.GetItemName(MobRewardRows[i].ItemID);
        if(fullName=="")fullName=MobRewardRows[i].FallbackName;
        if(fullName=="")fullName="Item #"$string(MobRewardRows[i].ItemID);
        icon=class'UIDATA_ITEM'.static.GetItemTextureName(MobRewardRows[i].ItemID);
        if(!HasIconName(icon))icon=MobRewardRows[i].FallbackIcon;
        record.LVDataList.Length=5;
        record.LVDataList[0].szData="";
        record.LVDataList[0].szTexture="";
        record.LVDataList[0].arrTexture.Length=0;
        itemTexture=None;
        if(HasIconName(icon))itemTexture=Texture(DynamicLoadObject(icon,class'Texture',true));
        if(itemTexture==None && HasIconName(MobRewardRows[i].FallbackIcon))
            itemTexture=Texture(DynamicLoadObject(MobRewardRows[i].FallbackIcon,class'Texture',true));
        if(itemTexture!=None)
        {
            record.LVDataList[0].arrTexture.Length=1;
            record.LVDataList[0].arrTexture[0].objTex=itemTexture;
            record.LVDataList[0].arrTexture[0].X=2;
            record.LVDataList[0].arrTexture[0].Y=2;
            record.LVDataList[0].arrTexture[0].Width=32;
            record.LVDataList[0].arrTexture[0].Height=32;
            record.LVDataList[0].arrTexture[0].U=0;
            record.LVDataList[0].arrTexture[0].V=0;
            record.LVDataList[0].arrTexture[0].UL=32;
            record.LVDataList[0].arrTexture[0].VL=32;
        }
        record.LVDataList[1].szData=ClipRewardLabel(fullName);
        record.LVDataList[2].szData=MakeCostString(MobRewardRows[i].Minimum);
        record.LVDataList[3].szData=MakeCostString(MobRewardRows[i].Maximum);
        record.LVDataList[4].szData=FormatRewardChance(MobRewardRows[i].Chance)$"%";
        record.LVDataList[4].bUseTextColor=true;record.LVDataList[4].TextColor=MileniumMobColor(220,220,220,255);
        if(float(MobRewardRows[i].Chance)>=50.0)record.LVDataList[4].TextColor=MileniumMobColor(100,240,130,255);
        else if(float(MobRewardRows[i].Chance)<10.0)record.LVDataList[4].TextColor=MileniumMobColor(240,140,180,255);
        record.szReserved=fullName;record.nReserved1=MobRewardRows[i].ItemID;
        record.nReserved3=REWARD_ICON_RENDER_TAG;
        RewardList.InsertRecord(record);count++;
    }
    GetTextBoxHandle("MobInformerWnd.RewardStatus").SetText(string(count)$" items (base)");
    if(count==0)GetTextBoxHandle("MobInformerWnd.NoItems").ShowWindow();else GetTextBoxHandle("MobInformerWnd.NoItems").HideWindow();
}

// Native Classic tab textures; persistent selection follows the existing data mode.
function StyleDataTab(string control,bool selected)
{
 local string face;
 face="_Normal";if(selected)face="_Down";

 GetTextureHandle("MobInformerWnd."$control$"Face").SetTexture("MileniumMonsterArt.MobILUI."$control$face);
 GetTextBoxHandle("MobInformerWnd."$control$"Label").HideWindow();
}

function UpdateDetailTabs()
{
    StyleDataTab("DropsButton", !ShowQuests && !ShowSpoil);
    StyleDataTab("SpoilButton", !ShowQuests && ShowSpoil);
    StyleDataTab("QuestButton", ShowQuests);
    if (ShowQuests)
    {
        RewardList.HideWindow();
        GetTextBoxHandle("MobInformerWnd.ItemHeader").HideWindow();
        GetTextBoxHandle("MobInformerWnd.MinHeader").HideWindow();
        GetTextBoxHandle("MobInformerWnd.MaxHeader").HideWindow();
        GetTextBoxHandle("MobInformerWnd.ChanceHeader").HideWindow();
        GetTextBoxHandle("MobInformerWnd.NoItems").HideWindow();
        QuestList.ShowWindow();
        GetTextBoxHandle("MobInformerWnd.QuestNameHeader").ShowWindow();
        GetTextBoxHandle("MobInformerWnd.QuestIDHeader").ShowWindow();
        GetTextBoxHandle("MobInformerWnd.QuestRoleHeader").ShowWindow();
    }
    else
    {
        QuestList.HideWindow();
        GetTextBoxHandle("MobInformerWnd.QuestNameHeader").HideWindow();
        GetTextBoxHandle("MobInformerWnd.QuestIDHeader").HideWindow();
        GetTextBoxHandle("MobInformerWnd.QuestRoleHeader").HideWindow();
        GetTextBoxHandle("MobInformerWnd.NoQuests").HideWindow();
        RewardList.ShowWindow();
        GetTextBoxHandle("MobInformerWnd.ItemHeader").ShowWindow();
        GetTextBoxHandle("MobInformerWnd.MinHeader").ShowWindow();
        GetTextBoxHandle("MobInformerWnd.MaxHeader").ShowWindow();
        GetTextBoxHandle("MobInformerWnd.ChanceHeader").ShowWindow();
    }
}

function string QuestRoles(int roles, optional bool full)
{
    local string result;
    if ((roles & 1) != 0) result = "Start";
    if ((roles & 2) != 0 && (full || result == ""))
    {
        if (result != "") result = result $ " / ";
        result = result $ "Talk";
    }
    if ((roles & 4) != 0 && (full || result == ""))
    {
        if (result != "") result = result $ " / ";
        result = result $ "Hunt";
    }
    if ((roles & 8) != 0 && (full || result == ""))
    {
        if (result != "") result = result $ " / ";
        result = result $ "Related";
    }
    return result;
}

function string ClipQuestLabel(string text)
{
    local int width, height;
    GetTextSize(text, width, height);
    if (width <= 207) return text;
    while (Len(text) > 0)
    {
        text = Left(text, Len(text) - 1);
        GetTextSize(text $ "...", width, height);
        if (width <= 207) return text $ "...";
    }
    return "...";
}

function RenderQuestRows()
{
    local int i;
    local LVDataRecord record;
    QuestList.DeleteAllItem();
    GetTextBoxHandle("MobInformerWnd.NoItems").HideWindow();
    GetTextBoxHandle("MobInformerWnd.NoQuests").SetText("Sem drop");
    if(!Ready || !HasCurrentTargetSession())
    {
        if(class'UIDATA_TARGET'.static.GetTargetID()>0)GetTextBoxHandle("MobInformerWnd.NoQuests").SetText("Carregando...");
        else GetTextBoxHandle("MobInformerWnd.NoQuests").SetText("Selecione um alvo");
        GetTextBoxHandle("MobInformerWnd.NoQuests").ShowWindow();
        GetTextBoxHandle("MobInformerWnd.RewardStatus").SetText("");
        return;
    }
    for (i = 0; i < MobQuestRows.Length; i++)
    {
        record.LVDataList.Length = 3;
        record.LVDataList[0].szData = ClipQuestLabel(MobQuestRows[i].Name);
        record.LVDataList[1].szData = string(MobQuestRows[i].QuestID);

        record.LVDataList[2].szData = QuestRoles(MobQuestRows[i].Roles);

        record.nReserved1 = i;
        record.nReserved2 = MobQuestRows[i].QuestID;
        QuestList.InsertRecord(record);
    }
    GetTextBoxHandle("MobInformerWnd.RewardStatus").SetText(string(MobQuestRows.Length) $ " quests");
    if (MobQuestRows.Length == 0) GetTextBoxHandle("MobInformerWnd.NoQuests").ShowWindow();
    else GetTextBoxHandle("MobInformerWnd.NoQuests").HideWindow();
}

function FailRequest(string message)
{
    Me.KillTimer(DETAIL_RETRY_TIMER);
    Pending = false;
    Receiving = false;
    Ready = false;
    MobQuestRows.Length = 0;
    QuestList.DeleteAllItem();
    GetTextBoxHandle("MobInformerWnd.NoQuests").HideWindow();
    MobRewardRows.Length = 0;
    RewardList.DeleteAllItem();
    Me.KillTimer(RESPONSE_TIMER);
    GetTextBoxHandle("MobInformerWnd.RewardStatus").SetText(message);
}

function OnTimer(int timerID)
{
    local int objectID;
    if (timerID == BOOTSTRAP_SEND_TIMER)
    {
        Me.KillTimer(BOOTSTRAP_SEND_TIMER);
        if (WaitingBootstrap && Me.IsShowWindow() && BootstrapAttempts < 3)
        {
            BootstrapAttempts++;
            RequestBypassToServer("milenium_mobinfo open");
            if (BootstrapAttempts < 3) Me.SetTimer(BOOTSTRAP_SEND_TIMER, 1200);
        }
        return;
    }
    if (timerID == DETAIL_RETRY_TIMER) { SendDetailQuery(); return; }
    if (timerID == CATALOG_RETRY_TIMER) { SendCatalogQuery(); return; }
    if (timerID == AUTO_CATALOG_TIMER)
    {
        Me.KillTimer(AUTO_CATALOG_TIMER);
        if (Ready && Me.IsShowWindow() && !CatalogReady && !WaitingCatalog) RequestCatalog(false);
        return;
    }
    if (timerID == OPEN_QUERY_TIMER)
    {
        Me.KillTimer(OPEN_QUERY_TIMER);
        WaitingOpenQuery = false;
        if (!Me.IsShowWindow()) return;
        if (BootstrapBrowser) OpenBrowser();
        else TryShiftOpen();
        return;
    }
    if (timerID == BOOTSTRAP_TIMER)
    {
        Me.KillTimer(BOOTSTRAP_TIMER);
        if (WaitingBootstrap)
        {
            WaitingBootstrap = false;
            ViewMessage.SetText("Sem resposta da integracao Monster Data.");
            ViewMessage.ShowWindow();
            GetTextBoxHandle("MobInformerWnd.MobType").SetText("Conexao nao confirmada.");
            GetTextBoxHandle("MobInformerWnd.RewardStatus").SetText("Verifique o servidor e a TutorialViewerWnd.");
        }
        return;
    }
    if(timerID==99002) { UpdateTitleDrag();return; }
    if(timerID==99001) { Me.KillTimer(99001);ApplyInterludeVisuals();return; }
    if (timerID == STAGE_TIMER) AdvanceMobStage();
    else if (timerID == SHIFT_OPEN_TIMER) TryShiftOpen();
    else if (timerID == TARGET_FOLLOW_TIMER)
    {
        objectID = QueuedTargetObjectID;
        CancelTargetFollow();
        if (ServerEnabled && Me.IsShowWindow() && objectID > 0 &&
            class'UIDATA_TARGET'.static.GetTargetID() == objectID)
        {
            ShiftTargetID = objectID;
            ShiftOpenAttempts = 0;
            TryShiftOpen();
        }
    }
    else if (timerID == PREVIEW_INIT_TIMER)
    {
        Me.KillTimer(PREVIEW_INIT_TIMER);
        SpawnPreviewNPC();
    }
    else if (timerID == PREVIEW_FRAME_TIMER)
    {
        Me.KillTimer(PREVIEW_FRAME_TIMER);
        ApplyPreviewFrame();
    }
    else if (timerID == CATALOG_TIMER) FailCatalog("Servidor sem resposta. Busque novamente.");
    else if (timerID == RESPONSE_TIMER) FailRequest("Servidor sem resposta. Selecione novamente.");
    else if (timerID == ANIMATION_TIMER)
        KeepPreviewAlive();
}

function OnLButtonDown(WindowHandle window,int x,int y)
{
 local Rect bounds;
 local vector pointer;
 Me.SetDraggable(false);
 bounds=Me.GetRect();
 pointer=class'MobPreviewNative'.static.GetPointerState();
 DraggingTitle=pointer.Z>0.5 && x>=bounds.nX+6 && x<bounds.nX+bounds.nWidth-30 && y>=bounds.nY+3 && y<bounds.nY+31;
 if(DraggingTitle) {
  Me.ClearAnchor();DragMouseX=int(pointer.X);DragMouseY=int(pointer.Y);
  DragWindowX=bounds.nX;DragWindowY=bounds.nY;
  Me.SetTimer(99002,16);
 } else Me.KillTimer(99002);
 ViewportClick=window==MobViewport;
 ViewMouseX=x;ViewMouseY=y;
 if(DraggingTitle||ViewportClick)Me.SetFocus();
}

function OnMouseMove(WindowHandle window,int x,int y)
{
 if(ViewportClick && (Abs(x-ViewMouseX)>4 || Abs(y-ViewMouseY)>4))ViewportClick=false;
}

function OnLButtonUp(WindowHandle window,int x,int y)
{
 if(ViewportClick && window==MobViewport && PreviewActor!=None){PreviewYaw+=4096;SetPreviewRotation();}
 StopTitleDrag();ViewportClick=false;
}

function OnClickButton(string buttonID)
{
    switch (buttonID)
    {
        case "ZoomIn": StepZoom(false);break;
        case "ZoomOut": StepZoom(true);break;
        case "RotateLeft": PreviewYaw-=4096;SetPreviewRotation();break;
        case "RotateRight": PreviewYaw+=4096;SetPreviewRotation();break;
        case "CloseButton": Me.HideWindow(); break;
        case "SearchButton": RequestCatalog(true); break;
        case "LocationButton":
            if (!Ready || !HasMobLocation) break;
            ShowWindowWithFocus("MinimapWnd");
            if (HasMapMarker) class'UIAPI_MINIMAPCTRL'.static.DeleteTarget("MinimapWnd.Minimap", MapMarkerLocation);
            MapMarkerLocation = MobLocation;
            HasMapMarker = true;
            class'UIAPI_MINIMAPCTRL'.static.AddTarget("MinimapWnd.Minimap", MobLocation);
            class'UIAPI_MINIMAPCTRL'.static.AdjustMapView("MinimapWnd.Minimap", MobLocation, false, false);
            break;
        case "DropsButton": SelectTargetMode(1);break;
        case "SpoilButton": SelectTargetMode(2);break;
        case "QuestButton": SelectTargetMode(3);break;
    }
}

function OnHide()
{
    Ready = false;
    TargetObjectID = 0;
    SelectedTemplateID = 0;
    Me.KillTimer(CATALOG_RETRY_TIMER);
    WaitingOpenQuery = false;
    Me.KillTimer(BOOTSTRAP_SEND_TIMER);
    Me.KillTimer(AUTO_CATALOG_TIMER);
    Me.KillTimer(DETAIL_RETRY_TIMER);
    Me.KillTimer(OPEN_QUERY_TIMER);
    WaitingBootstrap = false;
    Me.KillTimer(BOOTSTRAP_TIMER);
    StopTitleDrag();
    ShowQuests = false;
    ShiftTargetID = 0;
    Me.KillTimer(SHIFT_OPEN_TIMER);
    CancelTargetFollow();
    ObservedTargetObjectID = 0;
    WaitingCatalog = false;
    ReceivingCatalog = false;
    Me.KillTimer(CATALOG_TIMER);
    DraggingTitle = false;
    ViewportClick = false;
    Pending = false;
    Receiving = false;
    Me.KillTimer(RESPONSE_TIMER);
    StopPreview();
}

function OnReceivedCloseUI()
{
    Me.HideWindow();
}




// SP73: keep catalog progress and pagination feedback without the removed footer.
function SetCatalogStatus(string caption)
{
    local CustomTooltip searchTooltip;
    local ButtonHandle searchButton;

    searchTooltip = xxMakeTooltipSimpleText("Search" $ Chr(10) $ caption);
    searchButton = GetButtonHandle("MobInformerWnd.SearchButton");
    searchButton.SetTooltipCustomType(searchTooltip);
}


// The server Action packet carries Shift even if the user already released it.
// Wait briefly for native target data when this was the very first NPC click.
function TryShiftOpen()
{
    local UserInfo info;
    Me.KillTimer(SHIFT_OPEN_TIMER);
    if (!ServerEnabled || ShiftTargetID <= 0) return;
    if (class'UIDATA_TARGET'.static.GetTargetID() != ShiftTargetID || !GetTargetInfo(info) || info.nID != ShiftTargetID || !info.bNpc || info.bPet)
    {
        ShiftOpenAttempts++;
        if (ShiftOpenAttempts < 10) Me.SetTimer(SHIFT_OPEN_TIMER, 50);
        else ShiftTargetID = 0;
        return;
    }
    ShiftTargetID = 0;
    OpenTarget();
}

function WindowHandle GetWindowHandle(string path) { return GetHandle(path); }

function TextBoxHandle GetTextBoxHandle(string path) { return TextBoxHandle(GetHandle(path)); }

function TextureHandle GetTextureHandle(string path) { return TextureHandle(GetHandle(path)); }

function ButtonHandle GetButtonHandle(string path) { return ButtonHandle(GetHandle(path)); }

function EditBoxHandle GetEditBoxHandle(string path) { return EditBoxHandle(GetHandle(path)); }

function ListCtrlHandle GetListCtrlHandle(string path) { return ListCtrlHandle(GetHandle(path)); }

function CheckBoxHandle GetCheckBoxHandle(string path) { return CheckBoxHandle(GetHandle(path)); }

function RequestShiftBridge()
{
 local int objectID;local UserInfo info;
 objectID=class'UIDATA_TARGET'.static.GetTargetID();
 if(objectID==ShiftBridgeTarget)return;
 ShiftBridgeTarget=objectID;
 if(!ServerEnabled || !ShiftBridgeEnabled || Me.IsShowWindow() || objectID<=0 || !IsKeyDown(IK_Shift))return;
 if(!GetTargetInfo(info) || !info.bNpc || info.bPet)return;
 RequestBypassToServer("milenium_mobinfo shift "$string(objectID));
}
function OnShow() { if(!MileniumControlsReady)OnLoad();ApplyInterludeVisuals();Me.SetTimer(99001,100); 
    // Inventory Viewr final: shared preview renderer must be released.
    GetHandle("MileniumInventoryPreviewWnd").HideWindow();
    class'MobPreviewNative'.static.ReleasePreview();
    ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTexture("L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over");
    ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTooltipCustomType(InventoryWnd(GetScript("InventoryWnd")).SetTooltip("Inventory Viewr"));
}
function StopTitleDrag() { DraggingTitle=false;Me.KillTimer(99002);Me.SetDraggable(false);if(TitleDragArea!=None)TitleDragArea.SetDraggable(false); }
function UpdateTitleDrag()
{
 local vector pointer;
 if(!DraggingTitle || !Me.IsShowWindow()){StopTitleDrag();return;}
 pointer=class'MobPreviewNative'.static.GetPointerState();
 if(pointer.Z<0.5){StopTitleDrag();return;}
 Me.SetDraggable(false);
 Me.MoveTo(DragWindowX+int(pointer.X)-DragMouseX,DragWindowY+int(pointer.Y)-DragMouseY);
}
function ApplyInterludeVisuals()
{
 class'UIAPI_CHECKBOX'.static.SetTitle("MobInformerWnd.SearchNPC","NPC");
 class'UIAPI_CHECKBOX'.static.SetTitle("MobInformerWnd.SearchDrop","Drop");
 class'UIAPI_CHECKBOX'.static.SetTitle("MobInformerWnd.SearchSpoil","Spoil");
 class'UIAPI_CHECKBOX'.static.SetTitle("MobInformerWnd.SearchQuest","Quest");
 SelectTargetMode(CatalogMode);
}
function Color MileniumMobColor(int r,int g,int b,int a) { local Color c;c.R=r;c.G=g;c.B=b;c.A=a;return c; }
function string GetModelMesh(int npcID) { local string m,s;local float r,h;class'MobModelData'.static.Lookup(npcID,m,s,r,h);return m; }
function string GetModelIcon(int npcID) { return ""; }
function SetPreviewRotation() { local rotator angle;if(PreviewActor==None)return;angle.Yaw=PreviewYaw;PreviewActor.SetRotation(angle); }
function KeepPreviewAlive() { if(PreviewActor!=None)PreviewActor.LifeSpan=120.0; }
function OpenBrowser()
{
    Me.ShowWindow();Me.SetFocus();
    RenderTargetList();
    if(class'UIDATA_TARGET'.static.GetTargetID()>0)OpenTarget();
    else
    {
        GetTextBoxHandle("MobInformerWnd.MobName").SetText("Selecione um alvo");
        GetTextBoxHandle("MobInformerWnd.MobType").SetText("");
        GetTextBoxHandle("MobInformerWnd.RewardStatus").SetText("Selecione um monstro para consultar.");
    }
}
function string TrimQuery(string text) { while(Len(text)>0 && (Left(text,1)==" " || Left(text,1)==Chr(9)))text=Mid(text,1);while(Len(text)>0 && (Right(text,1)==" " || Right(text,1)==Chr(9)))text=Left(text,Len(text)-1);return text; }
function string EncodeQuery(string text) { local int i,value;local string result,digits;digits="0123456789ABCDEF";for(i=0;i<Len(text);i++){value=Asc(Mid(text,i,1));result=result$Mid(digits,(value/4096)%16,1)$Mid(digits,(value/256)%16,1)$Mid(digits,(value/16)%16,1)$Mid(digits,value%16,1);}return result; }

defaultproperties {}
