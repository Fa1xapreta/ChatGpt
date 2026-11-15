# Reward diário — somente troca da imagem de fundo

Com o jogo fechado, substitua somente interface.xdat pelo arquivo deste pacote. Nenhuma classe UC, interface.u, DLL, posição, tamanho, UV, botão, slot, efeito ou outro controle foi alterado.

Única mudança: o campo file de BoardWnd.texBackBottom passou de iconfox-v3.DailyReward_BGNew para L2UI_CH3.AbnormalWnd.board_back5.

A textura board_back5 precisa existir no L2UI_CH3.u atual. Ela fazia parte das texturas removidas do pacote de teste anterior. Este ajuste do XDAT não recria nem reimporta a textura. Se estiver usando aquele pacote sem board_back5, a imagem nova não poderá ser carregada até que essa textura seja reimportada corretamente.

O arquivo completo foi relido e comparado; somente os bytes da string dessa imagem mudaram. Todos os demais campos e bytes foram preservados. Não foi executado o jogo Windows neste ambiente.

Para voltar, use o interface.xdat de VOLTAR_FUNDO_ANTERIOR.
