# Inventory Viewr — cabeçalho integrado 25

Feche jogo e launcher. Copie somente interface.u e interface.xdat da pasta system para a system do cliente. Mantenha a DLL e as texturas atuais.

A faixa TitleViwer agora pertence ao próprio painel Inventory Viewr, no canto superior esquerdo interno, com 270x32: exatamente a largura do painel do personagem. O título foi centralizado sobre a faixa, em vez de ficar abaixo dela. A janela flutuante usada na versão anterior foi removida. A imagem de origem continua usando seus 256x32 pixels completos, esticados apenas na largura para preencher os 270 pixels do painel.

O cabeçalho fica dentro da janela, sem espaço externo e sem avançar sobre o inventário principal. A altura total, a posição e a área 3D do painel permanecem. Personagem, fundo, setas e câmera não foram redimensionados ou deslocados. O botão ADD/DEL de 32x32 e o tooltip Inventory Viewr permanecem.

Verificação: 64 cenários interpretando bytecode da interface, com saltos e referências conferidos. XDAT relido e comparado: a janela flutuante foi eliminada; as demais janelas ficaram byte a byte iguais; no painel, só foram acrescentados a textura do cabeçalho e o ajuste vertical do título. Geometria nativa, área 3D, fundo e setas permanecem byte a byte iguais. Conferidas largura do cabeçalho, título dentro da faixa, ordem dos controles, UV de origem, imports/exports, ZIP e hashes. O jogo Windows não foi executado neste ambiente; confira a aparência no cliente.

Capas, nomes, zoom e ausência de logs continuam com a DLL atual, que não é substituída.

Para voltar, feche o jogo e copie os dois arquivos de VOLTAR_TESTE24/system para system.
