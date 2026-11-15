# Inventory Viewr — atualização visual 23

Feche jogo e launcher. Copie os TRÊS arquivos da pasta system para a system do cliente, substituindo os anteriores: interface.u, interface.xdat e MileniumPortraitNative.dll. Esta atualização muda a interface e o XDAT, além de retirar o registro de diagnóstico da DLL. Mantenha L2UI_CH3 e os demais pacotes atuais que contêm as texturas indicadas.

A faixa L2UI_CH3.AbnormalWnd.TitleViwer aparece no tamanho original de 256x32, centralizada acima do título. Ela é uma decoração separada, ancorada à prévia, que abre e fecha junto com ela. A janela original da prévia e TODOS os seus controles foram mantidos byte a byte no XDAT: posição, tamanho, área 3D, fundo, título e setas não foram redimensionados ou deslocados.

O botão de abrir/fechar agora tem 32x32. Com a prévia fechada, usa L2UI_CH3.ShortcutWnd.AS_ShortcutWnd_ADD e AS_ShortcutWnd_ADD_Over ao passar o mouse. Com a prévia aberta, usa AS_ShortcutWnd_DEL e AS_ShortcutWnd_DEL_Over. Como não foi fornecida uma textura Down, o estado pressionado usa a textura Over correspondente. A descrição ao passar o mouse é Abrir Inventory Viewr ou Fechar Inventory Viewr. Os estados são atualizados no clique, na inicialização e nos fechamentos do inventário ou da sessão. A janela continua abrindo pelo clique.

Foi desativada a geração dos arquivos Inventory_Viewr_R*.log. O logger antigo da DLL já estava desativado e permanece assim. Não é necessário manter os logs antigos do Inventory Viewr; os arquivos que já existem podem ser apagados manualmente. Esta atualização não desativa os registros de outros módulos do cliente.

Capas, bloqueio dos nomes, rotação, zoom e enquadramento seguem a implementação aprovada do TESTE 22. O código de diagnóstico não escreve mais arquivos; o código de desenho não ganhou novos ajustes de câmera ou de capa. A exceção restrita da cena do CapasClassic permanece.

Verificação realizada: bytecode das seis funções alteradas decodificado, com saltos e referências conferidos; 64 cenários de interface executados em interpretação simulada, verificando ADD/DEL, texturas de mouse, descrição e abertura/fechamento da faixa. Todos os objetos não envolvidos permanecem iguais ao original. O XDAT foi lido novamente e comparado ao instalado: janela da prévia inteira e demais janelas/controles não envolvidos ficaram byte a byte iguais; só o botão do inventário foi alterado e uma janela de decoração foi acrescentada.

Também passaram os testes x86 de nomes, zoom, desenho da capa no trecho real de decisão do CapasClassic e enquadramento, em seis bases relocadas, com chamadas Engine/Windows simuladas. As chamadas de diagnóstico/arquivo foram proibidas nos testes dos callbacks. Conferidos ausência do nome de arquivo de diagnóstico e de referências aos imports de escrita no código novo, retorno imediato do logger antigo, PE, exportações, ZIP e hashes. O jogo Windows não foi executado neste ambiente; confira a aparência das texturas no seu cliente.

Para voltar, feche o jogo e copie os TRÊS arquivos de VOLTAR_TESTE22/system para system. Essa pasta contém a DLL e a interface/XDAT que você aprovou antes do ajuste visual. Nenhum arquivo de servidor, pacote de capa, aura ou textura é substituído por esta atualização.
