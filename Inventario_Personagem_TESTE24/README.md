# Inventory Viewr — correção visual 24

Feche jogo e launcher. Copie somente interface.u e interface.xdat da pasta system para a system do cliente. Mantenha a MileniumPortraitNative.dll e as texturas atuais. Esta correção não inclui nem altera a DLL.

A faixa L2UI_CH3.AbnormalWnd.TitleViwer permanece com 256x32, centralizada sobre os 270 pixels do painel do personagem. Sua posição agora usa diretamente a mesma origem do inventário usada para posicionar a prévia: ponto superior esquerdo do inventário, ponto superior direito da faixa, deslocamentos -7 em X e +1 em Y. Assim, a faixa ocupa o intervalo horizontal de -263 a -7 em relação à esquerda do inventário, totalmente dentro dos -270 a 0 do painel do personagem. Seu limite inferior coincide com o início da prévia, em Y=33. Ela fica acima do título do Inventory Viewr, sem atravessar o inventário principal.

A âncora também é aplicada explicitamente ao abrir a janela. A faixa continua abrindo e fechando junto com a prévia. A janela original da prévia, seu fundo, título, área 3D e setas permanecem byte a byte iguais no XDAT.

O tooltip do botão agora mostra somente Inventory Viewr, tanto aberto quanto fechado. Ele usa SetTooltipCustomType com a função SetTooltip já existente no InventoryWnd, seguindo o mesmo mecanismo dos outros botões do inventário. Os ícones ADD/DEL, texturas de mouse e tamanho 32x32 permanecem.

Zoom, capas, tamanho/posição do personagem, bloqueio dos nomes e ausência de geração dos logs continuam com a DLL aprovada, sem substituição.

Verificação: 64 cenários de abertura, fechamento, inicialização e reinício executaram o bytecode real em interpretação simulada, conferindo a âncora aplicada, as texturas e o tooltip personalizado. Saltos, tamanhos expandidos, referências e manutenção dos demais objetos foram conferidos. O XDAT foi relido e comparado, preservando todos os controles não envolvidos. A largura e os limites da faixa foram verificados. Imports, nomes e identidade dos exports da interface permanecem compatíveis com a versão anterior. ZIP e hashes conferidos. O jogo Windows não foi executado neste ambiente; confira a posição visual no cliente.

Para voltar, com o jogo fechado copie os dois arquivos de VOLTAR_TESTE23/system para system. Não é necessário substituir DLLs ou pacotes de textura.
