# Inventory Viewr — versão final sem faixa

A faixa TitleViwer foi removida. Personagem, título, tamanho, posição, capas, efeitos, zoom, rotação, botão ADD/DEL de 32x32 e tooltip Inventory Viewr foram preservados. A geração de logs do Inventory Viewr continua desativada pela DLL já instalada.

## Atualizar o patch/cliente pronto

Feche jogo e launcher. Copie interface.u e interface.xdat da pasta system para a system do patch/cliente. A interface.u é idêntica à da versão 25; a mudança visual está no XDAT, que remove somente a faixa. Mantenha as DLLs atuais, especialmente Interface.dll e MileniumPortraitNative.dll, e os pacotes L2UI_CH3/L2Devil que já estão funcionando. Nenhuma DLL ou textura é substituída por este pacote.

## Classes UC completas para o projeto

A pasta Classes contém:

- InventoryWnd.uc: classe completa baseada na enviada por você, com abertura/fechamento da prévia, vínculo ao jogador real, texturas do botão ADD/DEL, tooltip, fundo, setas e limpeza ao reiniciar/fechar o inventário.
- MobInformerWnd.uc: classe completa, com a limpeza da prévia do inventário ao abrir a janela de monstros que compartilha o renderer.
- MobPreviewNative.uc: declarações nativas originais, incluídas como dependência. Não foram acrescentadas novas funções nativas.

Atualize esses arquivos na pasta Classes do seu projeto Interface. Os demais fontes do seu projeto permanecem. Os arquivos UC são fontes de interface; não devem substituir os arquivos compilados da pasta system. O funcionamento nativo de câmera, zoom, capas e bloqueio de nomes continua nas DLLs existentes; ele não foi convertido para UnrealScript.

Os fontes foram montados a partir das classes completas que você enviou, preservando propriedades, defaults e todas as funções não envolvidas. Foram alteradas apenas cinco funções de InventoryWnd e OnShow de MobInformerWnd. O helper de tooltip do InventoryWnd já existente é reutilizado. Em MobInformerWnd, a chamada ao helper usa explicitamente o script InventoryWnd. O resultado comportamental corresponde ao da interface compilada incluída, mas uma recompilação gera outro binário e depende do seu projeto, pacotes base e compilador.

As classes UC foram revisadas e comparadas, mas não foram compiladas com UCC neste ambiente. Para usar a versão pronta, não é necessário recompilar: os arquivos da pasta system já estão preparados.

## Verificação

64 cenários de ciclo de vida interpretaram o bytecode da interface. O XDAT foi relido e comparado à versão anterior: somente a textura da faixa foi retirada; os demais dados de janelas e controles permanecem byte a byte iguais. Confirmados ausência de referências à faixa/janela flutuante e interface.u idêntica à versão 25. Nos fontes UC, a restauração das seis funções modificadas reproduz exatamente as classes originais enviadas por você, incluindo defaults e todas as demais funções. MobPreviewNative.uc está byte a byte igual à declaração original. ZIPs e hashes conferidos. O jogo Windows e a compilação UCC dos fontes não foram executados neste ambiente.

## Restaurar

Com o jogo fechado, copie os dois arquivos de VOLTAR_TESTE25/system para system. A pasta contém a interface/XDAT anteriores. Não é necessário mexer nas DLLs.
