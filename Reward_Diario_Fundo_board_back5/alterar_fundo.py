from pathlib import Path
import sys,struct,json,hashlib,zipfile,shutil
sys.path.insert(0,'/workspace/work/inventory-preview');from xdat import Reader
SRC=Path('/workspace/attachments/92d662b2-eeae-42cc-9a49-4a1017802504/interface.xdat');D=Path('/workspace/work/github-delivery/Reward_Diario_Fundo_board_back5');D.mkdir(exist_ok=True)
b=SRC.read_bytes();r=Reader(b);old=[r.node('Window') for _ in range(r.i())];tail=b[r.p:]
w=next(w for w in old if w['fields']['name']=='BoardWnd');target=next(c for c in w['fields']['children'] if c['fields']['name']=='texBackBottom')
assert target['type']=='Texture';before=target['fields']['file'];after='L2UI_CH3.AbnormalWnd.board_back5';a,end=target['spans']['file']
def ci(v):
 out=[v&63];v>>=6
 if v:out[0]|=64
 while v:out.append((v&127)|(128 if v>>7 else 0));v>>=7
 return bytes(out)
encoded=after.encode('latin1')+b'\0';replacement=ci(len(encoded))+encoded;changed=b[:a]+replacement+b[end:]
assert changed[:a]==b[:a] and changed[a+len(replacement):]==b[end:]
r2=Reader(changed);new=[r2.node('Window') for _ in range(r2.i())];assert changed[r2.p:]==tail

def shape(n):
 return (n['type'],{k:[shape(x) for x in v] if isinstance(v,list) else v for k,v in n['fields'].items()})
# Revert only this one field in memory: the entire parsed structures must match.
newtarget=next(c for w in new if w['fields']['name']=='BoardWnd' for c in w['fields']['children'] if c['fields']['name']=='texBackBottom');assert newtarget['fields']['file']==after
newtarget['fields']['file']=before;assert [shape(x) for x in old]==[shape(x) for x in new]
(D/'interface.xdat').write_bytes(changed)
report={'changed_control':'BoardWnd.texBackBottom','changed_field':'file','before':before,'after':after,'all_other_bytes_preserved':True,'all_other_parsed_values_preserved':True,'uc_or_native_changes':False,'texture_package_not_modified':True,'texture_must_exist_in_client_package':True,'source_sha256':hashlib.sha256(b).hexdigest(),'result_sha256':hashlib.sha256(changed).hexdigest(),'windows_game_run':False}
(D/'VALIDACAO.json').write_text(json.dumps(report,indent=2))
text='''# Reward diário — somente troca da imagem de fundo

Com o jogo fechado, substitua somente interface.xdat pelo arquivo deste pacote. Nenhuma classe UC, interface.u, DLL, posição, tamanho, UV, botão, slot, efeito ou outro controle foi alterado.

Única mudança: o campo file de BoardWnd.texBackBottom passou de iconfox-v3.DailyReward_BGNew para L2UI_CH3.AbnormalWnd.board_back5.

A textura board_back5 precisa existir no L2UI_CH3.u atual. Ela fazia parte das texturas removidas do pacote de teste anterior. Este ajuste do XDAT não recria nem reimporta a textura. Se estiver usando aquele pacote sem board_back5, a imagem nova não poderá ser carregada até que essa textura seja reimportada corretamente.

O arquivo completo foi relido e comparado; somente os bytes da string dessa imagem mudaram. Todos os demais campos e bytes foram preservados. Não foi executado o jogo Windows neste ambiente.

Para voltar, use o interface.xdat de VOLTAR_FUNDO_ANTERIOR.
'''
(D/'README.md').write_text(text);backup=D/'VOLTAR_FUNDO_ANTERIOR';backup.mkdir(exist_ok=True);shutil.copy2(SRC,backup/'interface.xdat')
with zipfile.ZipFile(D/'Reward_Diario_Fundo_board_back5.zip','w',zipfile.ZIP_DEFLATED) as z:
 for name in ['interface.xdat','README.md','VALIDACAO.json','VOLTAR_FUNDO_ANTERIOR/interface.xdat']:z.write(D/name,name)
with zipfile.ZipFile(D/'Reward_Diario_Fundo_board_back5.zip') as z:assert z.testzip() is None and z.read('interface.xdat')==changed
shutil.copy2(__file__,D/'alterar_fundo.py')
main=D.parent/'README.md';main.write_text('## Reward diário — troca somente do fundo\n\n[Baixar ZIP](Reward_Diario_Fundo_board_back5/Reward_Diario_Fundo_board_back5.zip) · [XDAT](Reward_Diario_Fundo_board_back5/interface.xdat). Só a referência de imagem em BoardWnd.texBackBottom foi alterada para L2UI_CH3.AbnormalWnd.board_back5. A textura precisa existir no pacote do cliente.\n\n'+main.read_text())
print(json.dumps(report,indent=2))
