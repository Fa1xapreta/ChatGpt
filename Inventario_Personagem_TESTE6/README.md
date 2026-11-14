# Inventário 3D — TESTE 6

Copie os três arquivos de system para a system do cliente com o jogo e launcher fechados. Mantenha o seu L2UI_CH3 com L2UI_CH3.AbnormalWnd.Inventory_DF_CharacterEquipBg.

- Título: Inventory Viewr.
- Enquadramento fixo por corpo/raça/sexo, usando a aproximação maior como referência. A armadura não recalcula a distância da câmera.
- Atualização do equipamento consultada a cada 100 ms enquanto a prévia é desenhada.
- Para girar, clique e segure o botão esquerdo dentro da prévia e arraste horizontalmente. A volta completa corresponde a 512 pixels de arrasto; pode soltar e arrastar novamente.

Teste vestir/tirar a mesma armadura sem fechar a janela, trocar arma e escudo e girar para conferir as costas. Compare a altura dos pés e da cabeça equipado e desequipado. O enquadramento final depende da confirmação visual no cliente.

A DLL nova foi verificada com emulação x86 em seis bases reais de relocação, incluindo o desvio instalado, atualização, rotação e câmera fixa. O fundo passou por 36 casos de regressão. Chamadas do jogo e do Windows foram simuladas; não foi executado o cliente Windows neste ambiente.

Para restaurar a versão confirmada funcionando, copie os três arquivos de VOLTAR_TESTE5/system para sua system com o jogo fechado.

Este pacote altera apenas a prévia do inventário. Não contém alterações no servidor, nas DLLs de capas, aura ou efeitos de imagem. interface.u permanece idêntica ao TESTE 5.
