# Inventory Viewr — final sem faixa, IDs das 34 capas do XML

Com jogo e launcher fechados, copie system/interface.u e system/interface.xdat para a system do patch/cliente. Mantenha todas as DLLs e texturas atuais. O XDAT não mudou em relação à versão final sem faixa; a mudança está na regra compilada de reconhecimento das capas e no fonte InventoryWnd.uc.

IsBordaItemAllowed aceita somente os IDs contidos no XML enviado: 31300 a 31312, 31346 a 31347 e 31380 a 31398. São 34 itens. Os intervalos antigos 31007 a 31073 e 31100 a 31140 foram substituídos. Os intervalos vazios entre os novos grupos não foram liberados.

A regra é usada para reconhecer as capas e encaminhar as equipadas para InventoryWnd.BordaItem, além dos comportamentos de clique e uso já existentes. O XML do servidor não foi alterado. A permissão real de equipar continua dependendo da definição do item no servidor.

A pasta Classes contém os fontes completos InventoryWnd.uc, MobInformerWnd.uc e MobPreviewNative.uc. Copie esses arquivos para Classes do seu projeto Interface se estiver atualizando os fontes. A única alteração em relação às classes finais anteriores está no retorno de IsBordaItemAllowed de InventoryWnd.uc. As outras duas classes e as declarações nativas são idênticas. Os arquivos UC não substituem os compilados da pasta system; para usar o cliente pronto, utilize os arquivos system incluídos.

Os fontes não foram recompilados com UCC neste ambiente. A interface.u pronta foi atualizada diretamente, mantendo imports, exports e demais objetos compatíveis. O predicado foi lido do bytecode real e executado para todos os 65.536 IDs entre 0 e 65535, além dos extremos de int32. O conjunto aceito coincide exatamente com os 34 IDs do XML e com os intervalos do fonte UC. Os tamanhos de salto de curto-circuito foram conferidos. Também passaram os 64 cenários de ciclo de vida da interface. O XDAT e as outras classes permanecem iguais. ZIPs e hashes conferidos. O jogo Windows não foi executado neste ambiente.

Zoom, capas no visor, posição/tamanho do personagem, rotação, botão ADD/DEL de 32x32, tooltip Inventory Viewr, bloqueio de nomes, ausência de faixa e ausência de geração dos logs continuam como estavam. Nenhuma DLL ou textura é substituída.

Para voltar aos IDs antigos, feche o jogo e copie os dois arquivos de VOLTAR_FINAL26/system para system. Essa pasta contém a versão final anterior à troca da lista de capas.
