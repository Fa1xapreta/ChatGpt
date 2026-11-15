class InventoryWnd extends UICommonAPI;

const DIALOG_USE_RECIPE = 1111;
const DIALOG_POPUP = 2222;
const DIALOG_DROPITEM = 3333;
const DIALOG_DROPITEM_ASKCOUNT = 4444;
const DIALOG_DROPITEM_ALL = 5555;
const DIALOG_DESTROYITEM = 6666;
const DIALOG_DESTROYITEM_ALL = 7777;
const DIALOG_DESTROYITEM_ASKCOUNT = 8888;
const DIALOG_CRYSTALLIZE = 9999;
const DIALOG_DROPITEM_PETASKCOUNT = 10000;

var AutoPotionsWnd s_autoPotions;

const EQUIPITEM_Underwear = 0;
const EQUIPITEM_Head = 1;
const EQUIPITEM_Hair = 2;
const EQUIPITEM_Hair2 = 3;
const EQUIPITEM_Neck = 4;
const EQUIPITEM_RHand = 5;
const EQUIPITEM_Chest = 6;
const EQUIPITEM_LHand = 7;
const EQUIPITEM_REar = 8;
const EQUIPITEM_LEar = 9;
const EQUIPITEM_Gloves = 10;
const EQUIPITEM_Legs = 11;
const EQUIPITEM_Feet = 12;
const EQUIPITEM_RFinger = 13;
const EQUIPITEM_LFinger = 14;
const EQUIPITEM_Max = 15;

const INVENTORY_ITEM_TAB = 0;
const INVENTORY_ITEM_1_TAB = 1;
const INVENTORY_ITEM_2_TAB = 2;
const INVENTORY_ITEM_3_TAB = 3;
const INVENTORY_ITEM_4_TAB = 4;
const QUEST_ITEM_TAB = 5;

var ItemWindowHandle m_hBordaItem;
var ButtonHandle m_btnBorda;
var TextureHandle m_texBordaBG;

var WindowHandle m_hInventoryWnd;
var string m_WindowName;
var string InventoryItem_1;
var string InventoryItem_2;
var string InventoryItem_3;
var string InventoryItem_4;
var Vector m_clickLocation;
var ItemWindowHandle m_invenItem;
var ItemWindowHandle m_questItem;
var ItemWindowHandle zzm_equipItem[15];
var ItemWindowHandle m_hHennaItemWindow;
var ItemWindowHandle m_invenItem_1;
var ItemWindowHandle m_invenItem_2;
var ItemWindowHandle m_invenItem_3;
var ItemWindowHandle m_invenItem_4;
var TextBoxHandle m_hHennaItemName;
var TextBoxHandle m_hHennaItemDesc;
var TextBoxHandle m_hHennaItemDesc2;
var TextBoxHandle m_itemCount;
var TextBoxHandle m_hAdenaTextBox;
var ButtonHandle btnAddHenna;
var ButtonHandle btnHennaAdd;
var ButtonHandle btnRemoveHenna;
var ButtonHandle btnSort;
var ButtonHandle btnInfo;
var ButtonHandle btnRefine;
var array<int> zzm_itemOrder;
var array<ItemInfo> zzm_EarItemList;
var array<ItemInfo> zzm_FingerItemLIst;
var int lastSubClassID;
var int m_selectedItemTab;
var int m_NormalInvenCount;
var int m_QuestInvenCount;
var bool fastDel;
var bool bOption;
var L2Util zzl2UtilScript;
var ExpBarWnd zzs_expBar;
var bool checkautoset;
var QuitReportWnd zzQuitReportWndScript;

function OnRegisterEvent()
{
    RegisterEvent(40);
    RegisterEvent(650);
    RegisterEvent(2570);
    RegisterEvent(2580);
    RegisterEvent(2590);
    RegisterEvent(2600);
    RegisterEvent(2610);
    RegisterEvent(2620);
    RegisterEvent(2630);
    RegisterEvent(2631);
    RegisterEvent(260);
    RegisterEvent(180);
    RegisterEvent(1710);
}

function OnLoad()
{
    OnRegisterEvent();
    zzl2UtilScript = L2Util(GetScript("L2Util"));
    zzQuitReportWndScript = QuitReportWnd(GetScript("QuitReportWnd"));
    InitHandle();

    // Inventory Viewr final: start closed.
    GetHandle("MileniumInventoryPreviewWnd").HideWindow();
    ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTexture("L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over");
    ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTooltipCustomType(SetTooltip("Inventory Viewr"));
}

function InitHandle()
{
    InventoryItem_1 = "EquipItem_Underwear";
    InventoryItem_2 = "EquipItem_Head";
    InventoryItem_3 = "EquipItem_Hair";
    InventoryItem_4 = "EquipItem_Hair2";

    m_hInventoryWnd = xxGetWindowHandle(m_WindowName);
    m_invenItem = ItemWindowHandle(GetHandle(m_WindowName $ ".InventoryItem"));
    m_questItem = ItemWindowHandle(GetHandle(m_WindowName $ ".QuestItem"));
    m_hAdenaTextBox = TextBoxHandle(GetHandle(m_WindowName $ ".AdenaText"));

    s_autoPotions = AutoPotionsWnd(GetScript("AutoPotionsWnd"));

    checkautoset = GetOptionBool("lotus_custom", "AutoEquipSet");
    Class'UIAPI_CHECKBOX'.static.SetCheck("InventoryWnd.Cb_AutoEquip", checkautoset);
    Class'UIAPI_CHECKBOX'.static.SetTitle("InventoryWnd.Cb_AutoEquip", " Enable Auto Set Equip");

    m_invenItem_1 = xxGetItemWindowHandle((m_WindowName $ ".InventoryItem_1.") $ InventoryItem_1);
    m_invenItem_2 = xxGetItemWindowHandle((m_WindowName $ ".InventoryItem_2.") $ InventoryItem_2);
    m_invenItem_3 = xxGetItemWindowHandle((m_WindowName $ ".InventoryItem_3.") $ InventoryItem_3);
    m_invenItem_4 = xxGetItemWindowHandle((m_WindowName $ ".InventoryItem_4.") $ InventoryItem_4);

    zzm_equipItem[0] = ItemWindowHandle(GetHandle("EquipItem_Underwear"));
    zzm_equipItem[1] = ItemWindowHandle(GetHandle("EquipItem_Head"));
    zzm_equipItem[2] = ItemWindowHandle(GetHandle("EquipItem_Hair"));
    zzm_equipItem[3] = ItemWindowHandle(GetHandle("EquipItem_Hair2"));
    zzm_equipItem[4] = ItemWindowHandle(GetHandle("EquipItem_Neck"));
    zzm_equipItem[5] = ItemWindowHandle(GetHandle("EquipItem_RHand"));
    zzm_equipItem[6] = ItemWindowHandle(GetHandle("EquipItem_Chest"));
    zzm_equipItem[7] = ItemWindowHandle(GetHandle("EquipItem_LHand"));
    zzm_equipItem[8] = ItemWindowHandle(GetHandle("EquipItem_REar"));
    zzm_equipItem[9] = ItemWindowHandle(GetHandle("EquipItem_LEar"));
    zzm_equipItem[10] = ItemWindowHandle(GetHandle("EquipItem_Gloves"));
    zzm_equipItem[11] = ItemWindowHandle(GetHandle("EquipItem_Legs"));
    zzm_equipItem[12] = ItemWindowHandle(GetHandle("EquipItem_Feet"));
    zzm_equipItem[13] = ItemWindowHandle(GetHandle("EquipItem_RFinger"));
    zzm_equipItem[14] = ItemWindowHandle(GetHandle("EquipItem_LFinger"));

    zzm_equipItem[7].SetDisableTex("L2UI.InventoryWnd.Icon_dualcap");
    zzm_equipItem[1].SetDisableTex("L2UI.InventoryWnd.Icon_dualcap");
    zzm_equipItem[10].SetDisableTex("L2UI.InventoryWnd.Icon_dualcap");
    zzm_equipItem[11].SetDisableTex("L2UI.InventoryWnd.Icon_dualcap");
    zzm_equipItem[12].SetDisableTex("L2UI.InventoryWnd.Icon_dualcap");
    zzm_equipItem[3].SetDisableTex("L2UI.InventoryWnd.Icon_dualcap");

    m_hHennaItemWindow = ItemWindowHandle(GetHandle("InventoryWnd.HennaItem"));
    m_hHennaItemName = TextBoxHandle(GetHandle("InventoryWnd.HennaName"));
    m_hHennaItemDesc = TextBoxHandle(GetHandle("InventoryWnd.HennaDesc"));
    m_hHennaItemDesc2 = TextBoxHandle(GetHandle("InventoryWnd.HennaDesc2"));

    btnAddHenna = ButtonHandle(GetHandle("InventoryWnd.btnAddHenna"));
    btnHennaAdd = ButtonHandle(GetHandle("InventoryWnd.btnHennaAdd"));
    btnRemoveHenna = ButtonHandle(GetHandle("InventoryWnd.btnHennaRemove"));
    btnSort = ButtonHandle(GetHandle("InventoryWnd.btnSort"));
    btnInfo = ButtonHandle(GetHandle("InventoryWnd.btnInfo"));
    btnRefine = ButtonHandle(GetHandle("InventoryWnd.BtnRefinery"));

    m_itemCount = xxGetTextBoxHandle(m_WindowName $ ".ItemCount");

    btnSort.SetTooltipCustomType(SetTooltip("Sorting"));
    btnInfo.SetTooltipCustomType(SetTooltipOFF("Fast item delete (Ctrl + Alt + Click)"));
    btnAddHenna.SetTooltipCustomType(SetTooltip("Draw a Symbol"));
    btnHennaAdd.SetTooltipCustomType(SetTooltip("Draw a Symbol"));
    btnRemoveHenna.SetTooltipCustomType(SetTooltip("Remove Dye"));
    btnRefine.SetTooltipCustomType(SetTooltip("Augment"));

    zzs_expBar = ExpBarWnd(GetScript("ExpBarWnd"));

    m_btnBorda = ButtonHandle(GetHandle("InventoryWnd.btnBorda"));
    m_texBordaBG = xxGetTextureHandle("InventoryWnd.texBordaBG");
    m_hBordaItem = ItemWindowHandle(GetHandle("InventoryWnd.BordaItem"));

    m_btnBorda.SetTexture("L2UI_CH3.InventoryWnd.MileniumCloakSlot",
        "L2UI_CH3.InventoryWnd.MileniumCloakSlot",
        "L2UI_CH3.InventoryWnd.MileniumCloakSlot");
    m_texBordaBG.SetTexture("L2UI_CH3.AutoShot.ShotToggleEffect_01");
    m_btnBorda.SetTooltipCustomType(SetTooltipBorda());

    m_hBordaItem.Clear();
    UpdateBordaVisual();

    fastDel = false;
}

function OnEvent(int Event_ID, string param)
{
    switch(Event_ID)
    {
        case 40:
            HandleRestart();
        break;

        case 650:
            ClearHenna();
        break;

        case 2570:
            HandleClear();
        break;

        case 2580:
            HandleOpenWindow();
        break;

        case 2590:
            HandleHideWindow();
        break;

        case 2600:
            HandleAddItem(param);
        break;

        case 2610:
            HandleUpdateItem(param);
        break;

        case 2620:
            HandleItemListEnd();
        break;

        case 2630:
            HandleAddHennaInfo(param);
        break;

        case 260:
            HandleUpdateHennaInfo(param);
        break;

        case 2631:
            HandleToggleWindow();
        break;

        case 1710:
            HandleDialogOK();
        break;

        case 180:
            HandleUpdateUserInfo();
        break;

        default:
        break;
    }

    // Inventory Viewr final: restart/hidden-inventory cleanup; no item-event rebind.
    if(Event_ID == 40)
    {
        GetHandle("MileniumInventoryPreviewWnd").HideWindow();
        class'MobPreviewNative'.static.ReleasePreview();
        ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTexture("L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over");
        ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTooltipCustomType(SetTooltip("Inventory Viewr"));
    }
    else if(!GetHandle("InventoryWnd").IsShowWindow())
    {
        if(GetHandle("MileniumInventoryPreviewWnd").IsShowWindow())
        {
            GetHandle("MileniumInventoryPreviewWnd").HideWindow();
            class'MobPreviewNative'.static.ReleasePreview();
            ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTexture("L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over");
            ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTooltipCustomType(SetTooltip("Inventory Viewr"));
        }
    }
}

function OnShow()
{
    if(Class'UIDATA_PLAYER'.static.HasCrystallizeAbility())
    {
        ShowWindow(m_WindowName $ ".CrystallizeButton");
    }
    else
    {
        HideWindow(m_WindowName $ ".CrystallizeButton");
    }

    SetAdenaText();
    SetItemCount();
    UpdateHennaInfo();
    UpdateBordaVisual();

    // Inventory Viewr final: release the preview with the inventory.
    GetHandle("MileniumInventoryPreviewWnd").HideWindow();
    class'MobPreviewNative'.static.ReleasePreview();
    ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTexture("L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over");
    ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTooltipCustomType(SetTooltip("Inventory Viewr"));
}

function OnHide()
{
    local int idx;
    local ItemInfo Info;
    local ItemWindowHandle hWindow;

    SaveItemOrder();
    
    class'UICommonAPI'.static.ClearAllNewItems();

    for(idx = 0; idx < m_invenItem.GetItemNum(); idx++)
    {
        if(m_invenItem.GetItem(idx, Info))
        {
            if(Info.ForeTexture != "")
            {
                Info.ForeTexture = "";
                m_invenItem.SetItem(idx, Info);

                hWindow = getItemWindowHandleByItemType(Info);

                if(hWindow != None)
                {
                    hWindow.SetItem(
                        hWindow.FindItemWithServerID(Info.ServerID),
                        Info);
                }
            }
        }
    }

    // Inventory Viewr final: release the preview with the inventory.
    GetHandle("MileniumInventoryPreviewWnd").HideWindow();
    class'MobPreviewNative'.static.ReleasePreview();
    ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTexture("L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over");
    ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTooltipCustomType(SetTooltip("Inventory Viewr"));
}

function OnDBClickItemWithHandle(
    ItemWindowHandle a_hItemWindow,
    int Index)
{
    local ItemInfo Info;

    if(a_hItemWindow == None)
        return;

    if(IsKeyDown(IK_Shift))
    {
        if(a_hItemWindow != m_hBordaItem)
        {
            if(a_hItemWindow.GetItem(Index, Info))
            {
                SetItemTextLink(Info);
            }
        }

        // Apenas insere o link, sem usar ou equipar o item.
        return;
    }

    if(a_hItemWindow == m_hBordaItem)
    {
        if(m_hBordaItem.GetItem(Index, Info))
        {
            if(Info.bEquipped)
            {
                RequestUnequipItem(
                    Info.ServerID,
                    Info.SlotBitType
                );
            }
        }

        return;
    }

    UseItem(a_hItemWindow, Index);
}

function OnRClickItemWithHandle(
    ItemWindowHandle a_hItemWindow, int Index)
{
    local ItemInfo Info;

    if(a_hItemWindow == m_hBordaItem)
    {
        return;
    }

    if(a_hItemWindow.GetItem(Index, Info))
    {
        if(IsBordaItemAllowed(Info))
        {
            return;
        }
    }

    UseItem(a_hItemWindow, Index);
}

function OnSelectItemWithHandle(ItemWindowHandle a_hItemWindow, int a_Index)
{
    local int i;
    local ItemInfo Info;

    if(a_hItemWindow == m_hBordaItem)
    {
        return;
    }

    if(a_hItemWindow.GetSelectedItem(Info))
    {
        if(class'UICommonAPI'.static.IsItemNew(Info.ServerID))
        {
            class'UICommonAPI'.static.ClearNewItem(Info.ServerID);
            Info.ForeTexture = "";
            a_hItemWindow.SetItem(a_Index, Info);
        }
    }

    if((IsKeyDown(IK_Ctrl) && IsKeyDown(IK_Alt)) && fastDel == true)
    {
        if(a_hItemWindow.GetSelectedItem(Info))
        {
            if(a_hItemWindow.FindItemWithServerID(Info.ServerID) != -1)
            {
                if(Class'UIDATA_PLAYER'.static.HasCrystallizeAbility() &&
                    Class'UIDATA_ITEM'.static.IsCrystallizable(Info.ServerID))
                {
                    RequestCrystallizeItem(Info.ServerID, 1);
                    PlayConsoleSound(IFST_TRASH_BASKET);
                }
                else
                {
                    RequestDestroyItem(Info.ServerID, Info.ItemNum);
                    PlayConsoleSound(IFST_TRASH_BASKET);
                }
            }
        }
    }

    switch(a_hItemWindow)
    {
        case m_invenItem:
        case m_invenItem_1:
        case m_invenItem_2:
        case m_invenItem_3:
        case m_invenItem_4:
        case m_questItem:
            return;
        break;

        default:
            i = 0;
            J0x16E:
            if(i < 15)
            {
                if(a_hItemWindow != zzm_equipItem[i])
                {
                    zzm_equipItem[i].ClearSelect();
                }

                ++ i;
                goto J0x16E;
            }

            if(a_hItemWindow == m_hHennaItemWindow)
            {
                a_hItemWindow.GetSelectedItem(Info);
                SetHennaDesc(Info.Description);
            }

            return;
        break;
    }
}

function HandleRestart()
{
    m_hInventoryWnd.HideWindow();
}

function SetHennaDesc(string Desc)
{
    local int i, Length;
    local string S;

    Length = Len(Desc);
    i = 1;

    J0x14:
    if(i <= 3)
    {
        S = Right(Left(Desc, i), 3);
        ++ i;
        goto J0x14;
    }

    switch(S)
    {
        case "Str":
            m_hHennaItemName.SetText("Strength (STR)");
            m_hHennaItemDesc.SetText("Affects P.Atk.");
            m_hHennaItemDesc2.SetText("");
        break;

        case "Int":
            m_hHennaItemName.SetText("Intelligence (INT)");
            m_hHennaItemDesc.SetText("Affects M.Atk.");
            m_hHennaItemDesc2.SetText("");
        break;

        case "Dex":
            m_hHennaItemName.SetText("Dexterity (DEX)");
            m_hHennaItemDesc.SetText("Affects Atk. Spd, Accuracy,");
            m_hHennaItemDesc2.SetText("Evasion and Critical Rate.");
        break;

        case "Wit":
            m_hHennaItemName.SetText("Wit (WIT)");
            m_hHennaItemDesc.SetText("Affects Casting Spd, M. Critical");
            m_hHennaItemDesc2.SetText("Rate and M. Damage Resistance.");
        break;

        case "Con":
            m_hHennaItemName.SetText("Constitution (CON)");
            m_hHennaItemDesc.SetText("Affects HP/CP Max and Recovery,");
            m_hHennaItemDesc2.SetText("Weight, Shield Defense Rate.");
        break;

        case "Men":
            m_hHennaItemName.SetText("Mental (MEN)");
            m_hHennaItemDesc.SetText("Affects M. Def, Max MP,");
            m_hHennaItemDesc2.SetText("MP Recovery and M. Cancel Rate.");
        break;

        default:
        break;
    }
}

function SortQuestItem()
{
    local int i, j, invenLimit;
    local ItemInfo item, temp;
    local int numQuest;
    local array<ItemInfo> QuestList;

    invenLimit = m_questItem.GetItemNum();
    i = 0;

    J0x1C:
    if(i < invenLimit)
    {
        m_questItem.GetItem(i, item);

        if(!xxIsValidItemID(item))
        {
            goto J0x76;
        }

        QuestList[numQuest] = item;
        numQuest = numQuest + 1;

        J0x76:
        ++ i;
        goto J0x1C;
    }

    m_questItem.Clear();
    ItemboxUpdate(m_questItem, GetQuestItemInventoryLimit());
    i = 0;

    J0xA7:
    if(i < numQuest)
    {
        j = i;

        J0xC1:
        if(j < numQuest)
        {
            if(QuestList[i].ServerID < QuestList[j].ServerID)
            {
                temp = QuestList[i];
                QuestList[i] = QuestList[j];
                QuestList[j] = temp;
            }

            ++ j;
            goto J0xC1;
        }

        ++ i;
        goto J0xA7;
    }

    i = 0;

    J0x149:
    if(i < numQuest)
    {
        m_questItem.SetItem(i, QuestList[i]);
        ++ i;
        goto J0x149;
    }
}

function OnClickButton(string strID)
{
    switch(strID)
    {
        case "btnSort":
            switch(m_selectedItemTab)
            {
                case 0:
                    zzl2UtilScript.SortItem(m_invenItem);
                    SaveItemOrder();
                break;

                case 1:
                    zzl2UtilScript.SortItem(m_invenItem_1);
                break;

                case 2:
                    zzl2UtilScript.SortItem(m_invenItem_2);
                break;

                case 3:
                    zzl2UtilScript.SortItem(m_invenItem_3);
                break;

                case 4:
                    zzl2UtilScript.SortItem(m_invenItem_4);
                break;

                case 5:
                break;

                default:
                    zzl2UtilScript.SortItem(m_invenItem);
                    SaveItemOrder();
                break;
            }
        break;

        case "btnInfo":
            OnBtnInfo();
        break;

        case "btnAddHenna":
            RequestHennaItemList();
        break;

        case "btnHennaAdd":
            RequestHennaItemList();
        break;

        case "btnHennaRemove":
            RequestHennaUnEquipList();
        break;

        case "BtnRefinery":
            ExecuteEvent(154752);
        break;

        case "InventoryTab0":
            m_selectedItemTab = 0;
            setTexTab();
        break;

        case "InventoryTab1":
            m_selectedItemTab = 1;
            setTexTab();
        break;

        case "InventoryTab2":
            m_selectedItemTab = 2;
            setTexTab();
        break;

        case "InventoryTab3":
            m_selectedItemTab = 3;
            setTexTab();
        break;

        case "InventoryTab4":
            m_selectedItemTab = 4;
            setTexTab();
        break;

        case "InventoryTab5":
            m_selectedItemTab = 5;
            setTexTab();
        break;

        default:
            // Inventory Viewr final: only this button opens/binds the real player.
            if(strID == "btnMileniumPreview")
            {
                if(GetHandle("MileniumInventoryPreviewWnd").IsShowWindow())
                {
                    GetHandle("MileniumInventoryPreviewWnd").HideWindow();
                    class'MobPreviewNative'.static.ReleasePreview();
                    ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTexture("L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD_Over");
                    ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTooltipCustomType(SetTooltip("Inventory Viewr"));
                }
                else if(class'MobPreviewNative'.static.GetWorldActor(class'UIDATA_PLAYER'.static.GetPlayerID()) != None)
                {
                    GetHandle("MobInformerWnd").HideWindow();
                    GetHandle("MileniumInventoryPreviewWnd.MobViewport").ShowWindow();
                    GetHandle("MileniumInventoryPreviewWnd").SetAnchor("InventoryWnd", "TopLeft", "TopRight", 0, 33);
                    GetHandle("MileniumInventoryPreviewWnd").ShowWindow();
                    class'MobPreviewNative'.static.BindPreview(
                        GetHandle("MileniumInventoryPreviewWnd.MobViewport"),
                        class'MobPreviewNative'.static.GetWorldActor(class'UIDATA_PLAYER'.static.GetPlayerID()),
                        25.0, 30.0, 100.0);
                    class'MobPreviewNative'.static.SetStageBackground(
                        Texture(DynamicLoadObject("L2UI_CH3.AbnormalWnd.Inventory_DF_CharacterEquipBg", class'Texture', true)));
                    ButtonHandle(GetHandle("MileniumInventoryPreviewWnd.RotateLeft")).SetTexture(
                        "L2Devil.Log.Inventory_DF_Btn_RotateRight", "L2Devil.Log.Inventory_DF_Btn_RotateRight_Down", "L2Devil.Log.Inventory_DF_Btn_RotateRight_Over");
                    ButtonHandle(GetHandle("MileniumInventoryPreviewWnd.RotateRight")).SetTexture(
                        "L2Devil.Log.Inventory_DF_Btn_RotateLeft", "L2Devil.Log.Inventory_DF_Btn_RotateLeft_Down", "L2Devil.Log.Inventory_DF_Btn_RotateLeft_Over");
                    GetHandle("MileniumInventoryPreviewWnd.RotateLeft").ShowWindow();
                    GetHandle("MileniumInventoryPreviewWnd.RotateRight").ShowWindow();
                    ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTexture("L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_DEL", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_DEL_Over", "L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_DEL_Over");
                    ButtonHandle(GetHandle("InventoryWnd.btnMileniumPreview")).SetTooltipCustomType(SetTooltip("Inventory Viewr"));
                }
            }
        break;
    }
}

function Argumentations()
{
    if(IsShowWindow("RefineryWnd"))
    {
        HideWindow("RefineryWnd");
        PlaySound("InterfaceSound.inventory_close_01");
    }
    else
    {
        ShowWindowWithFocus("RefineryWnd");
        PlaySound("InterfaceSound.inventory_open_01");
    }
}

function setTexTab()
{
    local int i;

    i = 1;

    J0x07:
    if(i < 6)
    {
        if(i != m_selectedItemTab)
        {
            xxGetTextureHandle("InventoryWnd.iconTab" $ string(i - 1)).SetTexture(
                "L2UI_CH3.InventoryWnd.Inventory_Tab_" $ string(i - 1));
            goto J0xF8;
        }

        xxGetTextureHandle("InventoryWnd.iconTab" $ string(i - 1)).SetTexture(
            ("L2UI_CH3.InventoryWnd.Inventory_Tab_" $ string(i - 1)) $ "_Select");

        J0xF8:
        ++ i;
        goto J0x07;
    }

    SetItemCount();
}

function OnBtnInfo()
{
    if(fastDel == true)
    {
        btnInfo.SetTooltipCustomType(
            SetTooltipOFF("Fast item delete (Ctrl + Alt + Click)"));
    }
    else
    {
        btnInfo.SetTooltipCustomType(
            SetTooltipON("Fast item delete (Ctrl + Alt + Click)"));
    }
}

function bool isDragSrcInventory(string DragSrcName)
{
    switch(DragSrcName)
    {
        case "InventoryItem":
        case "QuestItem":
        case "PetInvenWnd":
        case InventoryItem_1:
        case InventoryItem_2:
        case InventoryItem_3:
        case InventoryItem_4:
            return true;
        break;

        default:
            if(-1 != InStr(DragSrcName, "EquipItem"))
            {
                return true;
            }
            return false;
        break;
    }
}

function OnDropItem(string strTarget, ItemInfo Info, int X, int Y)
{
    local int toIndex, fromIndex;
    local ItemWindowHandle normalinven;

    // Protecao do controle que representa a capa equipada.
    if(Info.DragSrcName == "BordaItem")
    {
        return;
    }

    if((Info.DragSrcName == strTarget) &&
        strTarget != "InventoryItem")
    {
        return;
    }

    if(!isDragSrcInventory(Info.DragSrcName))
    {
        return;
    }

    if(Info.DragSrcName == "BordaItem")
    {
        return;
    }

    if((Info.DragSrcName == strTarget) && strTarget != "InventoryItem")
    {
        return;
    }

    if(!isDragSrcInventory(Info.DragSrcName))
    {
        return;
    }

    if(((((strTarget == "InventoryItem") ||
        strTarget == InventoryItem_1) ||
        strTarget == InventoryItem_2) ||
        strTarget == InventoryItem_3) ||
        strTarget == InventoryItem_4)
    {
        if(Info.DragSrcName == strTarget)
        {
            normalinven = getItemWindowHandleBystrTarget(strTarget);
            toIndex = normalinven.GetIndexAt(X, Y, 1, 1);

            if(toIndex >= 0)
            {
                fromIndex = normalinven.FindItemWithServerID(Info.ServerID);

                if(toIndex != fromIndex)
                {
                    normalinven.SwapItems(fromIndex, toIndex);
                }
            }
        }
        else
        {
            if(-1 != InStr(Info.DragSrcName, "EquipItem"))
            {
                RequestUnequipItem(Info.ServerID, Info.SlotBitType);
            }
            else
            {
                if(Info.DragSrcName == "PetInvenWnd")
                {
                    if(IsStackableItem(Info.ConsumeType) && Info.ItemNum > 1)
                    {
                        if(Info.AllItemCount > 0)
                        {
                            if(CheckItemLimit(Info.ClassID, Info.AllItemCount))
                            {
                                Class'PetAPI'.static.RequestGetItemFromPet(
                                    Info.ServerID, Info.AllItemCount, false);
                            }
                        }
                        else
                        {
                            DialogSetID(10000);
                            DialogSetReservedInt(Info.ServerID);
                            DialogSetParamInt(Info.ItemNum);
                            DialogShow(
                                DIALOG_NumberPad,
                                MakeFullSystemMsg(
                                    GetSystemMessage(72), Info.Name));
                        }
                    }
                    else
                    {
                        Class'PetAPI'.static.RequestGetItemFromPet(
                            Info.ServerID, 1, false);
                    }
                }
            }
        }
    }
    else
    {
        if(strTarget == "QuestItem")
        {
            if(Info.DragSrcName == "QuestItem")
            {
                toIndex = m_questItem.GetIndexAt(X, Y, 1, 1);

                if(toIndex >= 0)
                {
                    fromIndex = m_questItem.FindItemWithServerID(Info.ServerID);

                    if(toIndex != fromIndex)
                    {
                        m_questItem.SwapItems(fromIndex, toIndex);
                    }
                }
            }
        }
        else
        {
            if(-1 != InStr(strTarget, "EquipItem"))
            {
                if(Info.DragSrcName == "PetInvenWnd")
                {
                    Class'PetAPI'.static.RequestGetItemFromPet(
                        Info.ServerID, 1, true);
                }
                else
                {
                    if(-1 != InStr(Info.DragSrcName, "EquipItem"))
                    {
                    }
                    else
                    {
                        if(byte(Info.ItemType) != 5)
                        {
                            RequestUseItem(Info.ServerID);
                        }
                    }
                }
            }
            else
            {
                if(strTarget == "TrashButton")
                {
                    if(IsStackableItem(Info.ConsumeType) && Info.ItemNum > 1)
                    {
                        if(Info.AllItemCount > 0)
                        {
                            DialogSetID(7777);
                            DialogSetReservedInt(Info.ServerID);
                            DialogSetReservedInt2(Info.AllItemCount);
                            DialogShow(
                                DIALOG_Warning,
                                MakeFullSystemMsg(
                                    GetSystemMessage(74), Info.Name, ""));
                        }
                        else
                        {
                            DialogSetID(8888);
                            DialogSetReservedInt(Info.ServerID);
                            DialogSetParamInt(Info.ItemNum);
                            DialogShow(
                                DIALOG_NumberPad,
                                MakeFullSystemMsg(
                                    GetSystemMessage(73), Info.Name));
                        }
                    }
                    else
                    {
                        if(Class'UIDATA_PLAYER'.static.HasCrystallizeAbility() &&
                            Class'UIDATA_ITEM'.static.IsCrystallizable(Info.ClassID))
                        {
                            DialogSetID(9999);
                            DialogSetReservedInt(Info.ServerID);
                            DialogShow(
                                DIALOG_Warning,
                                MakeFullSystemMsg(
                                    GetSystemMessage(336), Info.Name));
                        }
                        else
                        {
                            DialogSetID(6666);
                            DialogSetReservedInt(Info.ServerID);
                            DialogShow(
                                DIALOG_Warning,
                                MakeFullSystemMsg(
                                    GetSystemMessage(74), Info.Name));
                        }
                    }
                }
                else
                {
                    if(strTarget == "CrystallizeButton")
                    {
                        if((((((Info.DragSrcName == "InventoryItem") ||
                            Info.DragSrcName == InventoryItem_1) ||
                            Info.DragSrcName == InventoryItem_2) ||
                            Info.DragSrcName == InventoryItem_3) ||
                            Info.DragSrcName == InventoryItem_4) ||
                            -1 != InStr(Info.DragSrcName, "EquipItem"))
                        {
                            if(Class'UIDATA_PLAYER'.static.HasCrystallizeAbility() &&
                                Class'UIDATA_ITEM'.static.IsCrystallizable(Info.ClassID))
                            {
                                DialogSetID(9999);
                                DialogSetReservedInt(Info.ServerID);
                                DialogShow(
                                    DIALOG_Warning,
                                    MakeFullSystemMsg(
                                        GetSystemMessage(336), Info.Name));
                            }
                        }
                    }
                }
            }
        }
    }
}

function OnDropItemSource(string strTarget, ItemInfo Info)
{
    if(Info.DragSrcName == "BordaItem")
    {
        return;
    }

    if(strTarget == "Console")
    {
        if(!isDragSrcInventory(Info.DragSrcName))
        {
            return;
        }

        if((((((Info.DragSrcName == "InventoryItem") ||
            Info.DragSrcName == InventoryItem_1) ||
            Info.DragSrcName == InventoryItem_2) ||
            Info.DragSrcName == InventoryItem_3) ||
            Info.DragSrcName == InventoryItem_4) ||
            -1 != InStr(Info.DragSrcName, "EquipItem"))
        {
            m_clickLocation = GetClickLocation();

            if(IsStackableItem(Info.ConsumeType) && Info.ItemNum > 1)
            {
                if(Info.AllItemCount > 0)
                {
                    DialogHide();
                    DialogSetID(5555);
                    DialogSetReservedInt(Info.ServerID);
                    DialogSetReservedInt2(Info.AllItemCount);
                    DialogShow(
                        DIALOG_Warning,
                        MakeFullSystemMsg(GetSystemMessage(1833), Info.Name, ""));
                }
                else
                {
                    DialogHide();
                    DialogSetID(4444);
                    DialogSetReservedInt(Info.ServerID);
                    DialogSetParamInt(Info.ItemNum);
                    DialogShow(
                        DIALOG_NumberPad,
                        MakeFullSystemMsg(GetSystemMessage(71), Info.Name, ""));
                }
            }
            else
            {
                DialogHide();
                DialogSetID(3333);
                DialogSetReservedInt(Info.ServerID);
                DialogShow(
                    DIALOG_Warning,
                    MakeFullSystemMsg(GetSystemMessage(400), Info.Name, ""));
            }
        }
    }
}

function bool IsEquipItem(out ItemInfo Info)
{
    return Info.bEquipped;
}

function bool IsQuestItem(out ItemInfo Info)
{
    return byte(Info.ItemType) == 3;
}

function HandleClear()
{
    InvenClear();
    invenItem_1Clear();
    invenItem_2Clear();
    invenItem_3Clear();
    invenItem_4Clear();
    QuestInvenClear();
    EquipItemClear();

    zzm_EarItemList.Length = 0;
    zzm_FingerItemLIst.Length = 0;

    InvenLimitUpdate();
}

function InvenClear()
{
    m_invenItem.Clear();
    m_NormalInvenCount = 0;
}

function QuestInvenClear()
{
    m_questItem.Clear();
    m_QuestInvenCount = 0;
}

function invenItem_1Clear()
{
    m_invenItem_1.Clear();
}

function invenItem_2Clear()
{
    m_invenItem_2.Clear();
}

function invenItem_3Clear()
{
    m_invenItem_3.Clear();
}

function invenItem_4Clear()
{
    m_invenItem_4.Clear();
}

function int EquipItemGetItemNum()
{
    local int i, ItemNum;
    local ItemInfo BordaInfo;
    local bool HasBordaItem;
    local bool BordaAlreadyCounted;

    HasBordaItem = m_hBordaItem.GetItem(0, BordaInfo);

    for(i = 0; i < 15; i++)
    {
        if(zzm_equipItem[i].IsEnableWindow())
        {
            ItemNum = ItemNum + zzm_equipItem[i].GetItemNum();

            if(HasBordaItem)
            {
                if(zzm_equipItem[i].FindItemWithServerID(
                    BordaInfo.ServerID) != -1)
                {
                    BordaAlreadyCounted = true;
                }
            }
        }
    }

    if(HasBordaItem && !BordaAlreadyCounted)
    {
        ItemNum = ItemNum + 1;
    }

    return ItemNum;
}

function EquipItemClear()
{
    local int i;

    i = 0;

    J0x07:
    if(i < 15)
    {
        zzm_equipItem[i].Clear();
        ++ i;
        goto J0x07;
    }

    m_hBordaItem.Clear();
    UpdateBordaVisual();
}

function bool EquipItemFind(int a_ServerID)
{
    local int i;

    if(m_hBordaItem.FindItemWithServerID(a_ServerID) != -1)
    {
        return true;
    }

    for(i = 0; i < 15; i++)
    {
        if(zzm_equipItem[i].FindItemWithServerID(a_ServerID) != -1)
        {
            return true;
        }
    }

    return false;
}

function EquipItemDelete(int a_ServerID)
{
    local int i, Index;
    local ItemInfo TheItemInfo;

    i = 0;

    J0x07:
    if(i < 15)
    {
        Index = zzm_equipItem[i].FindItemWithServerID(a_ServerID);

        if(-1 != Index)
        {
            zzm_equipItem[i].Clear();

            if(i == 7)
            {
                if(zzm_equipItem[5].GetItem(0, TheItemInfo))
                {
                    if(TheItemInfo.SlotBitType == 16384)
                    {
                        zzm_equipItem[7].Clear();
                        zzm_equipItem[7].AddItem(TheItemInfo);
                        zzm_equipItem[7].DisableWindow();
                    }
                }
            }
        }

        ++ i;
        goto J0x07;
    }

    if(m_hBordaItem.FindItemWithServerID(a_ServerID) != -1)
    {
        m_hBordaItem.Clear();
        UpdateBordaVisual();
    }
}

function EarItemUpdate()
{
    local int i, LEarIndex, REarIndex;

    LEarIndex = -1;
    REarIndex = -1;
    i = 0;

    J0x1D:
    if(i < zzm_EarItemList.Length)
    {
        switch(IsLOrREar(zzm_EarItemList[i].ServerID))
        {
            case -1:
                LEarIndex = i;
            break;

            case 0:
                zzm_EarItemList.Remove(i, 1);
            break;

            case 1:
                REarIndex = i;
            break;

            default:
            break;
        }

        ++ i;
        goto J0x1D;
    }

    if(-1 != LEarIndex)
    {
        zzm_equipItem[9].Clear();
        zzm_equipItem[9].AddItem(zzm_EarItemList[LEarIndex]);
    }

    if(-1 != REarIndex)
    {
        zzm_equipItem[8].Clear();
        zzm_equipItem[8].AddItem(zzm_EarItemList[REarIndex]);
    }
}

function FingerItemUpdate()
{
    local int i, LFingerIndex, RFingerIndex;

    LFingerIndex = -1;
    RFingerIndex = -1;
    i = 0;

    J0x1D:
    if(i < zzm_FingerItemLIst.Length)
    {
        switch(IsLOrRFinger(zzm_FingerItemLIst[i].ServerID))
        {
            case -1:
                LFingerIndex = i;
            break;

            case 0:
                zzm_FingerItemLIst.Remove(i, 1);
            break;

            case 1:
                RFingerIndex = i;
            break;

            default:
            break;
        }

        ++ i;
        goto J0x1D;
    }

    if(-1 != LFingerIndex)
    {
        zzm_equipItem[14].Clear();
        zzm_equipItem[14].AddItem(zzm_FingerItemLIst[LFingerIndex]);
    }

    if(-1 != RFingerIndex)
    {
        zzm_equipItem[13].Clear();
        zzm_equipItem[13].AddItem(zzm_FingerItemLIst[RFingerIndex]);
    }
}

function OnClickCheckBox(string a_param)
{
    switch(a_param)
    {
        case "Cb_AutoEquip":
            if(Class'UIAPI_CHECKBOX'.static.IsChecked("InventoryWnd.Cb_AutoEquip"))
            {
                SetOptionBool("Lotus_Custom", "AutoEquipSet", true);
            }
            else
            {
                SetOptionBool("Lotus_Custom", "AutoEquipSet", false);
            }
        break;

        default:
        break;
    }
}

function EquipItemUpdate(ItemInfo a_Info)
{
    local ItemWindowHandle hItemWnd;
    local ItemInfo TheItemInfo;
    local bool ClearLHand;
    local ItemInfo RHand, LHand, Legs, Gloves, Feet, Hair2;
    local int i;

        // Capas permitidas sao exibidas somente na Borda.
    if(IsBordaItemAllowed(a_Info))
    {
        if(a_Info.bEquipped)
        {
            a_Info.ForeTexture = "";

            m_hBordaItem.Clear();
            m_hBordaItem.AddItem(a_Info);

            UpdateBordaVisual();
        }

        return;
    }

    if(GetOptionBool("Lotus_Custom", "AutoEquipSet"))
    {
        if((a_Info.SlotBitType == 1024) || a_Info.SlotBitType == 32768)
        {
            EquipSetArmor(a_Info);
        }
    }

    switch(a_Info.SlotBitType)
    {
        case 1:
            hItemWnd = zzm_equipItem[0];
        break;

        case 2:
        case 4:
        case 6:
            i = 0;

            J0x36:
            if(i < zzm_EarItemList.Length)
            {
                if(zzm_EarItemList[i].ServerID == a_Info.ServerID)
                {
                    goto J0x72;
                }

                ++ i;
                goto J0x36;
            }

            J0x72:
            if(i == zzm_EarItemList.Length)
            {
                zzm_EarItemList.Length = zzm_EarItemList.Length + 1;
                zzm_EarItemList[zzm_EarItemList.Length - 1] = a_Info;
            }

            hItemWnd = none;
            EarItemUpdate();
        break;

        case 8:
            hItemWnd = zzm_equipItem[4];
        break;

        case 16:
        case 32:
        case 48:
            i = 0;

            J0xE3:
            if(i < zzm_FingerItemLIst.Length)
            {
                if(zzm_FingerItemLIst[i].ServerID == a_Info.ServerID)
                {
                    goto J0x11F;
                }

                ++ i;
                goto J0xE3;
            }

            J0x11F:
            if(i == zzm_FingerItemLIst.Length)
            {
                zzm_FingerItemLIst.Length = zzm_FingerItemLIst.Length + 1;
                zzm_FingerItemLIst[zzm_FingerItemLIst.Length - 1] = a_Info;
            }

            hItemWnd = none;
            FingerItemUpdate();
        break;

        case 64:
            hItemWnd = zzm_equipItem[1];
            hItemWnd.EnableWindow();
        break;

        case 128:
            hItemWnd = zzm_equipItem[5];
        break;

        case 256:
            hItemWnd = zzm_equipItem[7];
            hItemWnd.EnableWindow();
        break;

        case 512:
            hItemWnd = zzm_equipItem[10];
            hItemWnd.EnableWindow();
        break;

        case 1024:
            hItemWnd = zzm_equipItem[6];
        break;

        case 2048:
            hItemWnd = zzm_equipItem[11];
            hItemWnd.EnableWindow();
        break;

        case 4096:
            hItemWnd = zzm_equipItem[12];
            hItemWnd.EnableWindow();
        break;

        case 8192:
            hItemWnd = zzm_equipItem[0];
        break;

        case 16384:
            hItemWnd = zzm_equipItem[5];
            ClearLHand = true;

            if(IsBowOrFishingRod(a_Info))
            {
                if(zzm_equipItem[7].GetItem(0, TheItemInfo))
                {
                    if(IsArrow(TheItemInfo))
                    {
                        ClearLHand = false;
                    }
                }
            }

            if(ClearLHand)
            {
                if(Len(a_Info.IconNameEx1) != 0)
                {
                    RHand = a_Info;
                    LHand = a_Info;
                    RHand.IconIndex = 1;
                    LHand.IconIndex = 2;

                    zzm_equipItem[5].Clear();
                    zzm_equipItem[5].AddItem(RHand);
                    zzm_equipItem[7].Clear();
                    zzm_equipItem[7].AddItem(LHand);
                    zzm_equipItem[7].DisableWindow();

                    hItemWnd = none;
                }
                else
                {
                    zzm_equipItem[7].Clear();
                    zzm_equipItem[7].AddItem(a_Info);
                    zzm_equipItem[7].DisableWindow();
                }
            }
        break;

        case 32768:
            hItemWnd = zzm_equipItem[6];
            Legs = a_Info;
            Legs.IconName = a_Info.IconNameEx2;

            zzm_equipItem[11].Clear();
            zzm_equipItem[11].AddItem(Legs);
            zzm_equipItem[11].DisableWindow();
        break;

        case 65536:
            hItemWnd = zzm_equipItem[2];
        break;

        case 131072:
            hItemWnd = zzm_equipItem[6];

            Hair2 = a_Info;
            Gloves = a_Info;
            Legs = a_Info;
            Feet = a_Info;

            Hair2.IconName = a_Info.IconNameEx1;
            Gloves.IconName = a_Info.IconNameEx2;
            Legs.IconName = a_Info.IconNameEx3;
            Feet.IconName = a_Info.IconNameEx4;

            zzm_equipItem[1].Clear();
            zzm_equipItem[1].AddItem(Hair2);
            zzm_equipItem[1].DisableWindow();

            zzm_equipItem[10].Clear();
            zzm_equipItem[10].AddItem(Gloves);
            zzm_equipItem[10].DisableWindow();

            zzm_equipItem[11].Clear();
            zzm_equipItem[11].AddItem(Legs);
            zzm_equipItem[11].DisableWindow();

            zzm_equipItem[12].Clear();
            zzm_equipItem[12].AddItem(Feet);
            zzm_equipItem[12].DisableWindow();
        break;

        case 262144:
            hItemWnd = zzm_equipItem[3];
            hItemWnd.EnableWindow();
        break;

        case 524288:
            hItemWnd = zzm_equipItem[2];

            zzm_equipItem[3].Clear();
            zzm_equipItem[3].AddItem(a_Info);
            zzm_equipItem[3].DisableWindow();
        break;

        default:
        break;
    }

    if(none != hItemWnd)
    {
        hItemWnd.Clear();
        hItemWnd.AddItem(a_Info);
    }
}

function EquipSetArmor(ItemInfo paramItemInfo)
{
    local array<int> setParts;
    local int auxItemPos;
    local string auxItemName;
    local ItemInfo auxItemInfo;
    local int idx;

    Class'UIDATA_ITEM'.static.GetSetItemIDList(
        paramItemInfo.ClassID, 0, setParts);

    idx = 0;

    J0x33:
    if(idx < setParts.Length)
    {
        if(setParts[idx] == paramItemInfo.ClassID)
        {
            goto J0xD7;
        }

        auxItemName = Class'UIDATA_ITEM'.static.GetItemName(setParts[idx]);
        auxItemPos = m_invenItem.FindItemWithClassID(setParts[idx]);

        if(auxItemPos < 0)
        {
            goto J0xD7;
        }

        m_invenItem.GetItem(auxItemPos, auxItemInfo);
        RequestUseItem(auxItemInfo.ServerID);

        J0xD7:
        idx++;
        goto J0x33;
    }

    return;
}

function HandleOpenWindow()
{
    LoadItemOrder();
    ShowWindow(m_WindowName);
    Class'UIAPI_WINDOW'.static.SetFocus(m_WindowName);
}

function HandleHideWindow()
{
    HideWindow(m_WindowName);
}

function HandleAddItem(string param)
{
    local ItemInfo Info;

    ParamToItemInfo(param, Info);

    if(IsEquipItem(Info))
    {
        EquipItemUpdate(Info);
    }
    else
    {
        if(IsQuestItem(Info))
        {
            QuestInvenAddItem(Info);
        }
        else
        {
            NormalInvenAddItem(Info);
        }
    }
}

function NormalInvenAddItem(ItemInfo NewItem)
{
    local int idx, CurLimit, FindIdx;
    local ItemInfo CurItem;
    local ItemWindowHandle detailItemWindow;

    if(class'UICommonAPI'.static.IsItemNew(NewItem.ServerID))
    {
        NewItem.ForeTexture = "inventoryNotifyNew.inventoryAnimation0";
    }
    else
    {
        NewItem.ForeTexture = "";
    }

    FindIdx = -1;
    CurLimit = m_invenItem.GetItemNum();
    isAdena(NewItem);

    if(FindIdx < 0)
    {
        idx = 0;

        J0x3D:
        if(idx < CurLimit)
        {
            if(m_invenItem.GetItem(idx, CurItem))
            {
                if(!xxIsValidItemID(CurItem))
                {
                    FindIdx = idx;
                    goto J0x90;
                }
            }

            ++ idx;
            goto J0x3D;
        }
    }

    J0x90:
    if(FindIdx > -1)
    {
        m_invenItem.SetItem(FindIdx, NewItem);
    }
    else
    {
        m_invenItem.AddItem(NewItem);
    }

    ++ m_NormalInvenCount;

    detailItemWindow = getItemWindowHandleByItemType(NewItem);
    FindIdx = -1;
    idx = 0;

    J0xF9:
    if(idx < CurLimit)
    {
        if(detailItemWindow.GetItem(idx, CurItem))
        {
            if(!xxIsValidItemID(CurItem))
            {
                FindIdx = idx;
                goto J0x14C;
            }
        }

        ++ idx;
        goto J0xF9;
    }

    J0x14C:
    if(FindIdx > -1)
    {
        detailItemWindow.SetItem(FindIdx, NewItem);
    }
    else
    {
        detailItemWindow.AddItem(NewItem);
    }
}

function QuestInvenAddItem(ItemInfo NewItem)
{
    local int idx, CurLimit, FindIdx;
    local ItemInfo CurItem;

    FindIdx = -1;

    if(FindIdx < 0)
    {
        CurLimit = m_questItem.GetItemNum();
        idx = 0;

        J0x32:
        if(idx < CurLimit)
        {
            if(m_questItem.GetItem(idx, CurItem))
            {
                if(!xxIsValidItemID(CurItem))
                {
                    FindIdx = idx;
                    goto J0x85;
                }
            }

            ++ idx;
            goto J0x32;
        }
    }

    J0x85:
    if(FindIdx > -1)
    {
        m_questItem.SetItem(FindIdx, NewItem);
    }
    else
    {
        m_questItem.AddItem(NewItem);
    }

    ++ m_QuestInvenCount;
}

function ItemWindowHandle getItemWindowHandleBystrTarget(string strTarget)
{
    switch(strTarget)
    {
        case "InventoryItem":
            return m_invenItem;
        break;

        case InventoryItem_1:
            return m_invenItem_1;
        break;

        case InventoryItem_2:
            return m_invenItem_2;
        break;

        case InventoryItem_3:
            return m_invenItem_3;
        break;

        case InventoryItem_4:
            return m_invenItem_4;
        break;

        default:
        break;
    }
}

function ItemWindowHandle getItemWindowHandleByItemType(ItemInfo item)
{
    local EItemType EItemType;

    if(!xxIsValidItemID(item))
    {
        return m_invenItem_4;
    }

    EItemType = EItemType(item.ItemType);

    switch(EItemType)
    {
        case ITEM_ASSET:
            return m_invenItem_4;
        break;

        case ITEM_WEAPON:
        case ITEM_ARMOR:
        case ITEM_ACCESSARY:
            return m_invenItem_1;
        break;

        case ITEM_ETCITEM:
            switch(byte(item.ItemSubType))
            {
                case 1:
                case 2:
                case 3:
                case 19:
                case 20:
                case 21:
                case 22:
                case 24:
                case 23:
                case 16:
                    return m_invenItem_2;
                break;

                case 5:
                case 6:
                    return m_invenItem_3;
                break;

                case 0:
                case 4:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 17:
                case 18:
                    if(xxisConsumable(item.ClassID))
                    {
                        return m_invenItem_2;
                    }

                    return m_invenItem_4;
                break;

                default:
                break;
            }

        default:
            return m_invenItem_4;
        break;
    }
}

function isAdena(ItemInfo Info)
{
    local INT64 adenas;

    if(Info.ClassID != 57)
    {
        return;
    }

    adenas = Int2Int64(GetAdena());
    zzQuitReportWndScript.UpdateAdena(adenas);
}

function HandleUpdateItem(string param)
{
    local string Type;
    local ItemInfo Info;
    local ItemInfo OldInfo;
    local int Index;
    local ItemWindowHandle detailItemWindowHandle;

    ParseString(param, "type", Type);
    ParamToItemInfo(param, Info);

    if((gettypees(Info.SlotBitType)) || validid(Info.ClassID))
    {
        ExecuteEvent(99977);
    }

    if((GetPotionTypeProx(Info.ClassID)) != -1)
    {
        s_autoPotions.superiorinfota(Info);
    }

    if(Info.ClassID == zzs_expBar.zzitemCountID)
    {
        zzs_expBar.HandleItemIDcount(Info.ItemNum);
    }

    if(Type == "add")
    {
        class'UICommonAPI'.static.AddNewItem(Info.ServerID);

        if(IsEquipItem(Info))
        {
            EquipItemUpdate(Info);
        }
        else
        {
            if(IsQuestItem(Info))
            {
                QuestInvenAddItem(Info);
            }
            else
            {
                zzQuitReportWndScript.externalAddItem(Info);
                NormalInvenAddItem(Info);
            }
        }
    }
    else
    {
        if(Type == "update")
        {
            if(IsEquipItem(Info))
            {
                if(EquipItemFind(Info.ServerID))
                {
                    EquipItemUpdate(Info);
                }
                else
                {
                    InvenDelete(Info);
                    EquipItemUpdate(Info);
                }
            }
            else
            {
                if(IsQuestItem(Info))
                {
                    Index = m_questItem.FindItemWithServerID(Info.ServerID);

                    if(Index != -1)
                    {
                        m_questItem.SetItem(Index, Info);
                    }
                    else
                    {
                        EquipItemDelete(Info.ServerID);
                        QuestInvenAddItem(Info);
                    }
                }
                else
                {
                    Index = m_invenItem.FindItemWithServerID(Info.ServerID);

                    if(Index != -1)
                    {
                        m_invenItem.GetItem(Index, OldInfo);

                        if(Info.ItemNum > OldInfo.ItemNum)
                        {
                            class'UICommonAPI'.static.AddNewItem(Info.ServerID);
                        }

                        if(class'UICommonAPI'.static.IsItemNew(Info.ServerID))
                        {
                            Info.ForeTexture = "inventoryNotifyNew.inventoryAnimation0";
                        }
                        else
                        {
                            Info.ForeTexture = "";
                        }

                        isAdena(Info);
                        zzQuitReportWndScript.externalAddItem(Info);
                        m_invenItem.SetItem(Index, Info);

                        detailItemWindowHandle = getItemWindowHandleByItemType(Info);
                        detailItemWindowHandle.SetItem(
                            detailItemWindowHandle.FindItemWithServerID(Info.ServerID),
                            Info);
                    }
                    else
                    {
                        EquipItemDelete(Info.ServerID);
                        NormalInvenAddItem(Info);
                    }
                }
            }
        }
        else
        {
            if(Type == "delete")
            {
                if(IsEquipItem(Info))
                {
                    EquipItemDelete(Info.ServerID);
                }
                else
                {
                    if(IsQuestItem(Info))
                    {
                        Index = m_questItem.FindItemWithServerID(Info.ServerID);
                        m_questItem.DeleteItem(Index);
                    }
                    else
                    {
                        InvenDelete(Info);
                    }
                }
            }
        }
    }

    SetAdenaText();
    SetItemCount();
}

function InvenDelete(ItemInfo item)
{
    local int FindIdx, DetailFindIdx;
    local ItemWindowHandle detailItemWindow;

    detailItemWindow = getItemWindowHandleByItemType(item);

    FindIdx = m_invenItem.FindItemWithServerID(item.ServerID);
    DetailFindIdx = detailItemWindow.FindItemWithServerID(item.ServerID);

    if(FindIdx != -1)
    {
        m_invenItem.DeleteItem(FindIdx);
        detailItemWindow.DeleteItem(DetailFindIdx);
        -- m_NormalInvenCount;
    }
}

function QuestInvenDelete(ItemInfo item)
{
    local int FindIdx;

    FindIdx = m_questItem.FindItemWithServerID(item.ServerID);

    if(FindIdx != -1)
    {
        m_questItem.DeleteItem(FindIdx);
        -- m_QuestInvenCount;
    }
}

function HandleItemListEnd()
{
    SetAdenaText();
    SetItemCount();
    OrderItem();
    SortAll();
}

function HandleAddHennaInfo(string param)
{
    UpdateHennaInfo();
}

function HandleUpdateHennaInfo(string param)
{
    UpdateHennaInfo();
}

function UpdateHennaInfo()
{
    local int i, HennaInfoCount, HennaID, IsActive;
    local ItemInfo HennaItemInfo;
    local UserInfo PlayerInfo;
    local int ClassStep;

    if(GetPlayerInfo(PlayerInfo))
    {
        ClassStep = GetClassStep(PlayerInfo.nSubClass);

        switch(ClassStep)
        {
            case 1:
            case 2:
            case 3:
                m_hHennaItemWindow.SetRow(ClassStep);
            break;

            default:
                m_hHennaItemWindow.SetRow(0);
            break;
        }
    }

    m_hHennaItemWindow.Clear();

    HennaInfoCount = Class'HennaAPI'.static.GetHennaInfoCount();

    if(HennaInfoCount > ClassStep)
    {
        HennaInfoCount = ClassStep;
    }

    i = 0;

    J0xAB:
    if(i < HennaInfoCount)
    {
        if(Class'HennaAPI'.static.GetHennaInfo(i, HennaID, IsActive))
        {
            if(!Class'UIDATA_HENNA'.static.GetItemName(
                HennaID, HennaItemInfo.Name))
            {
                goto J0x193;
            }

            if(!Class'UIDATA_HENNA'.static.GetDescription(
                HennaID, HennaItemInfo.Description))
            {
                goto J0x193;
            }

            if(!Class'UIDATA_HENNA'.static.GetIconTex(
                HennaID, HennaItemInfo.IconName))
            {
                goto J0x193;
            }

            if(0 == IsActive)
            {
                HennaItemInfo.bDisabled = true;
            }
            else
            {
                HennaItemInfo.bDisabled = false;
            }

            m_hHennaItemWindow.AddItem(HennaItemInfo);
        }

        ++ i;
        goto J0xAB;
    }

    J0x193:
    return;
}

function SetAdenaText()
{
    local string adenaString, lcoinstring;
    local Color AdenaColor;

    adenaString = MakeCostString(string(GetAdena()));
    lcoinstring = MakeCostString(string(FindNoStackableItem(14446)));

    AdenaColor = GetNumColor(adenaString);

    m_hAdenaTextBox.SetTextColor(AdenaColor);
    m_hAdenaTextBox.SetText(adenaString);
    m_hAdenaTextBox.SetTooltipString(ConvertNumToText(string(GetAdena())));

    Class'UIAPI_TEXTBOX'.static.SetText(
        "ExpBarWnd.textAdenaCount", adenaString);

    Class'UIAPI_TEXTBOX'.static.SetTextColor(
        "ExpBarWnd.textAdenaCount", AdenaColor);

    Class'UIAPI_TEXTBOX'.static.SetText(
        "ExpBarWnd.btnAdena", adenaString);

    Class'UIAPI_TEXTBOX'.static.SetTextColor(
        "TranscendentInstanceWnd.CountinvneedPVE", AdenaColor);

    Class'UIAPI_TEXTBOX'.static.SetTextColor(
        "TranscendentInstanceWnd.CountinvneedPVP", AdenaColor);

    Class'UIAPI_TEXTBOX'.static.SetTextColor(
        "SuperiorInstancesWnd.Instance1PVE.CountinvneedPVE", AdenaColor);

    Class'UIAPI_TEXTBOX'.static.SetTextColor(
        "SuperiorInstancesWnd.Instance2PVP.CountinvneedPVP", AdenaColor);

    Class'UIAPI_TEXTBOX'.static.SetTextColor(
        "SuperiorInstancesWnd.Instance3Solo.CountinvneedSolo", AdenaColor);

    Class'UIAPI_TEXTBOX'.static.SetTextColor(
        "SuperiorInstancesWnd.Instance4PVP.CountinvneedLairPVP", AdenaColor);
}

function UseItem(ItemWindowHandle a_hItemWindow, int Index)
{
    local ItemInfo Info;

    if(a_hItemWindow.GetItem(Index, Info))
    {
       // Montarias sao utilizadas pelo MountBtnWnd.
        if(IsMountButtonItem(Info.ClassID))
        return;

        // Capas do nosso sistema.
        if(IsBordaItemAllowed(Info))
        {
            if(Info.bEquipped)
            {
                RequestUnequipItem(
                    Info.ServerID, Info.SlotBitType);
            }
            else
            {
                RequestUseItem(Info.ServerID);
            }

            return;
        }

        // Comportamento original dos outros itens.
        if(Info.bRecipe)
        {
            DialogSetReservedInt(Info.ServerID);
            DialogSetID(1111);

            DialogShow(
                DIALOG_Warning,
                GetSystemMessage(798));
        }
        else
        {
            if(Info.PopMsgNum > 0)
            {
                DialogSetID(2222);
                DialogSetReservedInt(Info.ServerID);

                DialogShow(
                    DIALOG_Warning,
                    GetSystemMessage(Info.PopMsgNum));
            }
            else
            {
                RequestUseItem(Info.ServerID);
            }
        }
    }
}
function SaveItemOrder()
{
    local ItemInfo Info;
    local int i;

    zzm_itemOrder.Length = m_invenItem.GetItemNum();
    i = 0;

    J0x1D:
    if(i < zzm_itemOrder.Length)
    {
        m_invenItem.GetItem(i, Info);
        zzm_itemOrder[i] = Info.ClassID;
        ++ i;
        goto J0x1D;
    }

    SaveInventoryOrder(zzm_itemOrder);
}

function LoadItemOrder()
{
    LoadInventoryOrder(zzm_itemOrder);
}

function OrderItem()
{
    local int newItemIndex, ItemNum, itemIndex, orderIndex;
    local ItemInfo Info;
    local bool matched;

    newItemIndex = 0;
    ItemNum = m_invenItem.GetItemNum();
    itemIndex = 0;

    J0x23:
    if(itemIndex < ItemNum)
    {
        m_invenItem.GetItem(itemIndex, Info);
        matched = false;
        orderIndex = 0;

        J0x5A:
        if(orderIndex < zzm_itemOrder.Length)
        {
            if(Info.ClassID == zzm_itemOrder[orderIndex])
            {
                matched = true;
                goto J0x99;
            }

            ++ orderIndex;
            goto J0x5A;
        }

        J0x99:
        if(!matched)
        {
            m_invenItem.SwapItems(itemIndex, newItemIndex);
            ++ newItemIndex;
        }

        ++ itemIndex;
        goto J0x23;
    }

    orderIndex = 0;

    J0xD5:
    if(orderIndex < zzm_itemOrder.Length)
    {
        itemIndex = 0;

        J0xEC:
        if(itemIndex < ItemNum)
        {
            m_invenItem.GetItem(itemIndex, Info);

            if((Info.ClassID == zzm_itemOrder[orderIndex]) && Info.ServerID > 0)
            {
                m_invenItem.SwapItems(itemIndex, newItemIndex);
                ++ newItemIndex;
                goto J0x16D;
            }

            ++ itemIndex;
            goto J0xEC;
        }

        J0x16D:
        ++ orderIndex;
        goto J0xD5;
    }
}

function SortAll()
{
    zzl2UtilScript.SortItem(m_invenItem_1);
    zzl2UtilScript.SortItem(m_invenItem_2);
    zzl2UtilScript.SortItem(m_invenItem_3);
    zzl2UtilScript.SortItem(m_invenItem_4);
}

function int GetMyInventoryLimit()
{
    return Class'UIDATA_PLAYER'.static.GetInventoryLimit();
}

function int GetQuestItemInventoryLimit()
{
    return 30;
}

function SetItemCount()
{
    local int limit, Count, Percent;
    local Color Red, DefaultColor;

    Red.R = 210;
    Red.G = 10;
    Red.B = 10;

    DefaultColor.R = 176;
    DefaultColor.G = 155;
    DefaultColor.B = 121;

    if(m_selectedItemTab == 5)
    {
        Count = m_QuestInvenCount;
        limit = GetQuestItemInventoryLimit();
    }
    else
    {
        Count = m_NormalInvenCount + (EquipItemGetItemNum());
        limit = GetMyInventoryLimit();
    }

    Percent = int((float(Count) / float(limit)) * float(100));

    m_itemCount.SetText(
        ((((string(Percent) $ "% (") $ string(Count)) $ "/") $
        string(limit)) $ ")");

    Class'UIAPI_TEXTBOX'.static.SetText(
        "ExpBarWnd.textInventoryCount",
        ((("(" $ string(m_NormalInvenCount + (EquipItemGetItemNum()))) $
        "/") $ string(limit)) $ ")");
}

function HandleDialogOK()
{
    local int Id, Reserved, reserved2, Number;

    if(!DialogIsMine())
    {
        return;
    }

    Id = DialogGetID();
    Reserved = DialogGetReservedInt();
    reserved2 = DialogGetReservedInt2();
    Number = int(DialogGetString());

    switch(Id)
    {
        case 1111:
        case 2222:
            RequestUseItem(Reserved);
        break;

        case 3333:
            RequestDropItem(
                Reserved, 1, m_clickLocation);
        break;

        case 4444:
            if(Number == 0)
            {
                Number = 1;
            }

            RequestDropItem(
                Reserved, Number, m_clickLocation);
        break;

        case 5555:
            RequestDropItem(
                Reserved, reserved2, m_clickLocation);
        break;

        case 6666:
            RequestDestroyItem(Reserved, 1);
            PlayConsoleSound(IFST_TRASH_BASKET);
        break;

        case 8888:
            RequestDestroyItem(Reserved, Number);
            PlayConsoleSound(IFST_TRASH_BASKET);
        break;

        case 7777:
            RequestDestroyItem(Reserved, reserved2);
            PlayConsoleSound(IFST_TRASH_BASKET);
        break;

        case 9999:
            RequestCrystallizeItem(Reserved, 1);
            PlayConsoleSound(IFST_TRASH_BASKET);
        break;

        case 10000:
            Class'PetAPI'.static.RequestGetItemFromPet(
                Reserved, Number, false);
        break;

        default:
        break;
    }
}

function HandleUpdateUserInfo()
{
    if(m_hOwnerWnd.IsShowWindow())
    {
        EarItemUpdate();
        FingerItemUpdate();
        InvenLimitUpdate();
    }
}

function HandleToggleWindow()
{
    if(m_hOwnerWnd.IsShowWindow())
    {
        m_hOwnerWnd.HideWindow();
    }
    else
    {
        RequestItemList();
        m_hOwnerWnd.ShowWindow();
        m_hOwnerWnd.SetFocus();
    }
}

function bool IsShowInventoryWndUponEvent()
{
    local WindowHandle m_warehouseWnd, m_privateShopWnd, m_tradeWnd;
    local WindowHandle m_shopWnd, m_multiSellWnd, m_deliverWnd;
    local PrivateShopWnd m_scriptPrivateShopWnd;

    m_warehouseWnd = GetHandle("WarehouseWnd");
    m_privateShopWnd = GetHandle("PrivateShopWnd");
    m_tradeWnd = GetHandle("TradeWnd");
    m_shopWnd = GetHandle("ShopWnd");
    m_multiSellWnd = GetHandle("MultiSellWnd");
    m_deliverWnd = GetHandle("DeliverWnd");

    m_scriptPrivateShopWnd = PrivateShopWnd(GetScript("PrivateShopWnd"));

    if(m_warehouseWnd.IsShowWindow())
    {
        return false;
    }

    if(m_warehouseWnd.IsShowWindow())
    {
        return false;
    }

    if(m_tradeWnd.IsShowWindow())
    {
        return false;
    }

    if(m_shopWnd.IsShowWindow())
    {
        return false;
    }

    if(m_multiSellWnd.IsShowWindow())
    {
        return false;
    }

    if(m_deliverWnd.IsShowWindow())
    {
        return false;
    }

    if(m_privateShopWnd.IsShowWindow() && m_scriptPrivateShopWnd.m_type == 2)
    {
        return false;
    }

    return true;
}

function int IsLOrREar(int a_ServerID)
{
    local int LEar, REar, LFinger, RFinger;

    GetAccessoryServerID(LEar, REar, LFinger, RFinger);

    if(a_ServerID == LEar)
    {
        return -1;
    }
    else
    {
        if(a_ServerID == REar)
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }
}

function int IsLOrRFinger(int a_ServerID)
{
    local int LEar, REar, LFinger, RFinger;

    GetAccessoryServerID(LEar, REar, LFinger, RFinger);

    if(a_ServerID == LFinger)
    {
        return -1;
    }
    else
    {
        if(a_ServerID == RFinger)
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }
}

function bool IsBowOrFishingRod(ItemInfo a_Info)
{
    if((6 == a_Info.WeaponType) || 10 == a_Info.WeaponType)
    {
        return true;
    }

    return false;
}

function bool IsArrow(ItemInfo a_Info)
{
    return a_Info.bArrow;
}

function CustomTooltip SetTooltip(string Text)
{
    local CustomTooltip TooltipInfo;

    TooltipInfo.DrawList.Length = 4;
    TooltipInfo.DrawList[0].eType = DIT_TEXT;
    TooltipInfo.DrawList[0].nOffSetX = 1;
    TooltipInfo.DrawList[0].t_bDrawOneLine = true;
    TooltipInfo.DrawList[0].t_color.R = byte(255);
    TooltipInfo.DrawList[0].t_color.G = byte(255);
    TooltipInfo.DrawList[0].t_color.B = byte(255);
    TooltipInfo.DrawList[0].t_color.A = byte(255);
    TooltipInfo.DrawList[0].t_strText = Text;

    return TooltipInfo;
}

function CustomTooltip SetTooltipON(string Text)
{
    local CustomTooltip TooltipInfo;

    fastDel = true;

    TooltipInfo.DrawList.Length = 4;

    TooltipInfo.DrawList[0].eType = DIT_TEXT;
    TooltipInfo.DrawList[0].nOffSetX = 1;
    TooltipInfo.DrawList[0].t_bDrawOneLine = true;
    TooltipInfo.DrawList[0].t_color.R = byte(255);
    TooltipInfo.DrawList[0].t_color.G = byte(255);
    TooltipInfo.DrawList[0].t_color.B = byte(255);
    TooltipInfo.DrawList[0].t_color.A = byte(255);
    TooltipInfo.DrawList[0].t_strText = Text;

    TooltipInfo.DrawList[1].eType = DIT_TEXT;
    TooltipInfo.DrawList[1].t_bDrawOneLine = true;
    TooltipInfo.DrawList[1].t_color.R = byte(255);
    TooltipInfo.DrawList[1].t_color.G = 200;
    TooltipInfo.DrawList[1].t_color.B = 0;
    TooltipInfo.DrawList[1].t_color.A = byte(255);
    TooltipInfo.DrawList[1].t_strText = "On";
    TooltipInfo.DrawList[1].bLineBreak = true;

    TooltipInfo.DrawList[2].eType = DIT_TEXT;
    TooltipInfo.DrawList[2].t_bDrawOneLine = true;
    TooltipInfo.DrawList[2].t_color.R = 128;
    TooltipInfo.DrawList[2].t_color.G = 128;
    TooltipInfo.DrawList[2].t_color.B = 128;
    TooltipInfo.DrawList[2].t_color.A = byte(255);
    TooltipInfo.DrawList[2].t_strText = "Off";
    TooltipInfo.DrawList[2].bLineBreak = true;

    return TooltipInfo;
}

function CustomTooltip SetTooltipOFF(string Text)
{
    local CustomTooltip TooltipInfo;

    fastDel = false;

    TooltipInfo.DrawList.Length = 4;

    TooltipInfo.DrawList[0].eType = DIT_TEXT;
    TooltipInfo.DrawList[0].nOffSetX = 1;
    TooltipInfo.DrawList[0].t_bDrawOneLine = true;
    TooltipInfo.DrawList[0].t_color.R = byte(255);
    TooltipInfo.DrawList[0].t_color.G = byte(255);
    TooltipInfo.DrawList[0].t_color.B = byte(255);
    TooltipInfo.DrawList[0].t_color.A = byte(255);
    TooltipInfo.DrawList[0].t_strText = Text;

    TooltipInfo.DrawList[1].eType = DIT_TEXT;
    TooltipInfo.DrawList[1].t_bDrawOneLine = true;
    TooltipInfo.DrawList[1].t_color.R = byte(255);
    TooltipInfo.DrawList[1].t_color.G = 200;
    TooltipInfo.DrawList[1].t_color.B = 0;
    TooltipInfo.DrawList[1].t_color.A = byte(255);
    TooltipInfo.DrawList[1].t_strText = "On";
    TooltipInfo.DrawList[1].bLineBreak = true;

    TooltipInfo.DrawList[2].eType = DIT_TEXT;
    TooltipInfo.DrawList[2].t_bDrawOneLine = true;
    TooltipInfo.DrawList[2].t_color.R = 128;
    TooltipInfo.DrawList[2].t_color.G = 128;
    TooltipInfo.DrawList[2].t_color.B = 128;
    TooltipInfo.DrawList[2].t_color.A = byte(255);
    TooltipInfo.DrawList[2].t_strText = "Off";
    TooltipInfo.DrawList[2].bLineBreak = true;

    return TooltipInfo;
}

function ClearHenna()
{
    local int SubClassID;
    local UserInfo Info;

    GetPlayerInfo(Info);
    SubClassID = Info.nSubClass;

    if((lastSubClassID > 0) && lastSubClassID != SubClassID)
    {
        m_hHennaItemName.SetText("");
        m_hHennaItemDesc.SetText("");
        m_hHennaItemDesc2.SetText("");
    }

    lastSubClassID = SubClassID;
}

function InvenLimitUpdate()
{
    ItemboxUpdate(m_invenItem, GetMyInventoryLimit());
    ItemboxUpdate(m_invenItem_1, GetMyInventoryLimit());
    ItemboxUpdate(m_invenItem_2, GetMyInventoryLimit());
    ItemboxUpdate(m_invenItem_3, GetMyInventoryLimit());
    ItemboxUpdate(m_invenItem_4, GetMyInventoryLimit());
    ItemboxUpdate(m_questItem, GetQuestItemInventoryLimit());
}

function ItemboxUpdate(ItemWindowHandle hItemWnd, int iInvenLimit)
{
    local int iCount, iItemCount, iAddedCount, iDeletedCount;
    local ItemInfo kCurItem;

    iItemCount = hItemWnd.GetItemNum();

    if(iItemCount < iInvenLimit)
    {
        iAddedCount = iInvenLimit - iItemCount;
    }
    else
    {
        if(iItemCount > iInvenLimit)
        {
            iDeletedCount = iItemCount - iInvenLimit;
            iCount = hItemWnd.GetItemNum() - 1;

            J0x72:
            if(iCount >= 0)
            {
                if(iDeletedCount > 0)
                {
                    hItemWnd.GetItem(iCount, kCurItem);

                    if(!xxIsValidItemID(kCurItem))
                    {
                        hItemWnd.DeleteItem(iCount);
                        -- iDeletedCount;
                    }

                    if(iDeletedCount <= 0)
                    {
                        goto J0xE4;
                    }
                }

                -- iCount;
                goto J0x72;
            }
        }
    }

    J0xE4:
    return;
}

function bool getInventoryItemInfo(
    int Id,
    out ItemInfo InvenItemInfo,
    optional bool onlyUseClassID)
{
    local bool bHasItem;
    local int i, Index;
    local ItemInfo tempOutItemInfo, tmpItemInfo;

    // Consulta tambem o novo controle de equipamento.
    if(onlyUseClassID)
    {
        Index = m_hBordaItem.FindItemWithClassID(Id);
    }
    else
    {
        Index = m_hBordaItem.FindItemWithServerID(Id);
    }

    if(Index > -1)
    {
        if(m_hBordaItem.GetItem(Index, InvenItemInfo))
        {
            return true;
        }
    }

    bHasItem = false;

    if(onlyUseClassID)
    {
        Index = m_invenItem.FindItemWithClassID(Id);
    }
    else
    {
        Index = m_invenItem.FindItemWithServerID(Id);
    }

    if(Index > -1)
    {
        m_invenItem.GetItem(Index, InvenItemInfo);
        bHasItem = true;
    }

    Index = m_questItem.FindItemWithServerID(Id);

    if(Index > -1)
    {
        m_questItem.GetItem(Index, InvenItemInfo);
        bHasItem = true;
    }

    i = 0;

    J0xC9:
    if(i < 15)
    {
        if(onlyUseClassID)
        {
            Index = zzm_equipItem[i].FindItemWithClassID(Id);
        }
        else
        {
            Index = zzm_equipItem[i].FindItemWithServerID(Id);
        }

        if(Index > -1)
        {
            zzm_equipItem[i].GetItem(Index, tempOutItemInfo);

            switch(i)
            {
                case 7:
                    zzm_equipItem[5].GetItem(0, tmpItemInfo);
                break;

                case 3:
                    zzm_equipItem[2].GetItem(0, tmpItemInfo);
                break;

                default:
                break;
            }

            if(tempOutItemInfo.ServerID == tmpItemInfo.ServerID)
            {
                goto J0x1CB;
            }

            InvenItemInfo = tempOutItemInfo;
            bHasItem = true;
            goto J0x1D5;
        }

        J0x1CB:
        ++ i;
        goto J0xC9;
    }

    J0x1D5:
    return bHasItem;
}

function int GetPotionTypeProx(int Id)
{
    switch(Id)
    {
        case 5592:
            return 0;
        break;

        case 1539:
            return 1;
        break;

        case 728:
            return 2;
        break;

        case 8627:
            return 3;
        break;

        case 8639:
            return 4;
        break;

        default:
            return -1;
        break;
    }
}

function bool IsMountButtonItem(int ClassID)
{
    return ClassID == 4422|| (ClassID >= 10133 && ClassID <= 10142)|| (ClassID >= 29000 && ClassID <= 29009)|| (ClassID >= 31830 && ClassID <= 31844);
}

function UpdateBordaVisual()
{
    local ItemInfo Info;
    local bool bTemCapa;

    m_texBordaBG.HideWindow();

    xxGetTextureHandle(
        "InventoryWnd.texBordaArrow").HideWindow();

    if(m_hBordaItem.GetItem(0, Info))
    {
        bTemCapa = Info.bEquipped &&
                   IsBordaItemAllowed(Info);
    }

    if(bTemCapa)
    {
        Info.ForeTexture = "";
        m_hBordaItem.SetItem(0, Info);
        m_btnBorda.HideWindow();
        m_hBordaItem.ShowWindow();
    }
    else
    {
        m_hBordaItem.HideWindow();
        m_btnBorda.ShowWindow();
    }
}

function CustomTooltip SetTooltipBorda()
{
    local CustomTooltip TooltipInfo;

    TooltipInfo.DrawList.Length = 3;

    // Icone.
    TooltipInfo.DrawList[0].eType = DIT_TEXTURE;
    TooltipInfo.DrawList[0].u_strTexture = "L2UI_CH3.InventoryWnd.MileniumCloakSlot";
    TooltipInfo.DrawList[0].u_nTextureWidth = 32;
    TooltipInfo.DrawList[0].u_nTextureHeight = 32;

    // Nome ao lado do icone.
    TooltipInfo.DrawList[1].eType = DIT_TEXT;
    TooltipInfo.DrawList[1].nOffSetX = 5;
    TooltipInfo.DrawList[1].t_bDrawOneLine = true;
    TooltipInfo.DrawList[1].t_color.R = 255;
    TooltipInfo.DrawList[1].t_color.G = 215;
    TooltipInfo.DrawList[1].t_color.B = 100;
    TooltipInfo.DrawList[1].t_color.A = 255;
    TooltipInfo.DrawList[1].t_strText = "Cloak Armor";

    // Descricao em uma nova linha.
    TooltipInfo.DrawList[2].eType = DIT_TEXT;
    TooltipInfo.DrawList[2].bLineBreak = true;
    TooltipInfo.DrawList[2].nOffSetY = 4;
    TooltipInfo.DrawList[2].t_bDrawOneLine = true;
    TooltipInfo.DrawList[2].t_color.R = 220;
    TooltipInfo.DrawList[2].t_color.G = 220;
    TooltipInfo.DrawList[2].t_color.B = 220;
    TooltipInfo.DrawList[2].t_color.A = 255;
    TooltipInfo.DrawList[2].t_strText ="Duplo clique na capa para equipar.";

    return TooltipInfo;
}

function bool IsBordaItemAllowed(ItemInfo Info)
{
    return (Info.ClassID >= 31007 && Info.ClassID <= 31073)
        || (Info.ClassID >= 31100 && Info.ClassID <= 31140);
}

defaultproperties
{
    m_WindowName="InventoryWnd"
}