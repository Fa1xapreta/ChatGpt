> TESTE 4 RETIRADO: a tabela de relocação podia corromper o salto do fundo quando a DLL era carregada fora da base preferida. Use o [TESTE 5 corrigido](../Inventario_Personagem_TESTE5/README.md). O ZIP defeituoso foi removido; os fontes e relatórios abaixo ficam como histórico.

# Inventário 3D — TESTE 4: fundo no L2UI_CH3 do usuário

Referência informada e configurada:
`L2UI_CH3.AbnormalWnd.Inventory_DF_CharacterEquipBg`

Este teste reúne a cópia do personagem do TESTE 3 e a integração do fundo. Pode ser aplicado sobre o TESTE 2 ou 3. O arquivo L2UI_CH3 em que você importou a imagem deve permanecer instalado.

## Instalação manual

1. Feche o jogo e o launcher.
2. Faça backup de system/interface.u, system/interface.xdat e system/MileniumPortraitNative.dll.
3. Copie os TRÊS arquivos da pasta system deste ZIP para a system do cliente.
4. Mantenha o L2UI_CH3 que você editou; este ZIP não inclui nem substitui esse pacote.
5. Abra o inventário e clique no botão de personagem.

## O que mudou

O painel 2D referencia a textura no grupo AbnormalWnd. O renderizador também carrega essa mesma textura como fundo da cena 3D; alterar somente o painel deixaria o retângulo preto da cena cobrindo a imagem.

O atlas original tem 512 x 512 e a área útil é 284 x 419. O painel e a cena usam essa área, evitando amostrar o espaço transparente restante. A DLL ajusta o recorte apenas para MileniumInventoryPreviewWnd; o recorte das outras janelas permanece como antes. A janela 3D agora ocupa toda a área de 270 x 429 do painel.

Esse recorte pressupõe que foi importada a PNG/TGA original de 512 x 512. Se a ferramenta de importação redimensionou a textura ou você usou o recorte PREVIA, o enquadramento pode precisar de ajuste.

A cópia do personagem, a pose de espera e o tratamento do HUD continuam como no TESTE 3. A imagem do personagem, as capas, os brilhos e a aura na cópia ainda precisam ser conferidos no jogo. Nenhuma alteração foi feita ao personagem real ou às DLLs das capas, aos efeitos de imagem ou ao servidor.

## Diagnóstico no rodapé

`Render 3D: 1 / Fundo: 1`: a DLL aceitou a prévia e encontrou um objeto Texture na referência informada. Isso não confirma sozinho a aparência final.

`Fundo: 0`: a referência não foi carregada como Texture. Confira o nome e qual cópia do L2UI_CH3 o jogo está carregando; existem clientes com arquivos de mesmo nome em system e systextures. Envie uma captura do painel se houver erro, recorte incorreto ou tela preta.

## Restauração

Prefira repor os três arquivos do backup feito antes deste teste. Como alternativa, VOLTAR_TESTE2/system contém exatamente os três arquivos do TESTE 2 confirmado funcionando; essa alternativa volta também ao modo de prévia daquele teste.

As pastas Classes e os relatórios são para referência. Somente os três arquivos da pasta system precisam ser copiados ao cliente.

## Validação executada

- Estrutura Unreal, referências, conversões 39/53, saltos e contextos das funções modificadas.
- Leitura do XDAT e preservação dos controles originais do inventário e das demais janelas.
- Interpretação do bytecode real com APIs simuladas, incluindo textura ausente e restauração do estado do HUD.
- Conferência da rotina FCanvasUtil::DrawTile do Engine: as coordenadas são em pixels e são normalizadas pela largura/altura do material.
- Emulação x86 do recorte em três bases de carregamento, dois tamanhos de janela e três nomes de janela: somente o painel de inventário recebe UV 0,0,284,419. Pilha, parâmetros restantes e registradores preservados.
- Código original de restauração das flags do ator, exportações e relocações preservados.

O cliente Windows não foi executado neste ambiente. O conteúdo do L2UI_CH3 modificado pelo usuário não foi recebido novamente; a referência foi configurada conforme sua mensagem.
