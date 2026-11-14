# Inventory Viewr — TESTE 11: setas de rotação

## Instalar

Feche o jogo e launcher. Copie os TRÊS arquivos da pasta system deste ZIP para a system do cliente, substituindo os anteriores. O interface.xdat mudou: é indispensável copiá-lo. Mantenha seu L2UI_CH3 e o pacote L2Devil que contém as texturas indicadas.

## Controles

Duas setas de 64x32 na faixa inferior. Clique para girar um passo de 11,25 graus; mantenha o botão esquerdo pressionado sobre a mesma seta para continuar girando. Soltar ou tirar o mouse da seta interrompe o movimento. Clicar/arrastar sobre o corpo não gira mais a prévia. Não há rotação automática.

Esquerda:
L2Devil.Log.Inventory_DF_Btn_RotateRight
L2Devil.Log.Inventory_DF_Btn_RotateRight_down
L2Devil.Log.Inventory_DF_Btn_RotateRight_over

Direita:
L2Devil.Log.Inventory_DF_Btn_RotateLeft
L2Devil.Log.Inventory_DF_Btn_RotateLeft_Down
L2Devil.Log.Inventory_DF_Btn_RotateLeft_Over

As texturas são referenciadas pelos nomes enviados pelo usuário. Não são incluídos nem substituídos os pacotes de textura.

## Prévia e enquadramento

O sincronismo de recursos volta ao TESTE 9 que mostrou o jogador: removidos a clonagem ExtraMeshDatas e o ID da arma acrescentados no TESTE 10. A causa exata da prévia vazia não foi reproduzida neste ambiente. Mantido o enquadramento menor e mais baixo preparado no TESTE 10 para aproximar os pés da rosa dos ventos; precisa ser conferido visualmente.

Capa e brilho de encantamento ainda não estão confirmados/resolvidos. Este teste corrige o controle de rotação e retira as mudanças de recursos do TESTE 10 que deixou a prévia vazia. O valor de + permanece sincronizado como no TESTE 9, o que isoladamente não produziu o brilho.

## Validação

Seis emulações x86 com relocação PE aplicada verificaram clique nas duas setas, giro enquanto pressionadas, interrupção ao soltar/sair, ausência de giro ao arrastar no corpo e sincronismo das malhas. Mais 36 casos do fundo. Layout com dois botões de 64x32, estados de textura exatos e demais janelas byte a byte preservadas. Chamadas do Engine/Windows simuladas; cliente Windows não executado aqui.

## Restaurar

Com jogo fechado, copie os três arquivos de VOLTAR_TESTE9/system para sua system. Não são alterados arquivos do servidor, das capas do mundo, aura, montarias ou efeitos de imagem.
