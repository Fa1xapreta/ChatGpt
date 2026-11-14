# Inventário 3D — TESTE 3: cópia de exibição e enquadramento

Requer o TESTE 2 instalado e funcionando. Este teste muda SOMENTE interface.u. A DLL, o XDAT, as texturas, as capas, os efeitos de imagem e o servidor continuam como estavam.

## Instalar

1. Feche o jogo e o launcher.
2. Faça backup do seu system/interface.u atual.
3. Copie apenas system/interface.u deste ZIP para a system do seu cliente.
4. Abra o inventário e o botão do personagem como no TESTE 2.

## O que este teste procura corrigir

O TESTE 2 desenhava o ator real, herdando a corrida e as identificações. O TESTE 3 usa o HudPortraitPawn, a cópia de exibição que a própria interface já prepara para o retrato. Ela usa uma animação de espera e rotação própria. O personagem real não recebe comandos para parar, girar ou ocultar seu nome.

O enquadramento foi alterado de radius=145/height=45 para radius=30/height=100, perfil de corpo inteiro. A interface renova os recursos pelos eventos do inventário. O estado expandido/fechado do HUD é restaurado depois de preparar a cópia.

A prévia compartilha a cópia de exibição com o retrato. Se o HUD liberar essa cópia, o painel lateral é fechado para não continuar ligado a um objeto removido. Abrir a prévia de monstros também fecha esse painel, como no TESTE 2.

## Validação no jogo ainda necessária

Confira o tamanho e o enquadramento, se a cópia fica em pose de espera enquanto o personagem real corre e se os nomes/títulos deixam de aparecer na prévia. Troque equipamentos e confira especialmente as capas, brilhos e aura. A cópia pode não reproduzir todos os anexos de plugins; esse ponto NÃO foi confirmado no Windows.

As estruturas, referências, conversões e saltos do bytecode foram verificados. Oito cenários do botão e dezesseis de ciclo de vida foram interpretados com as APIs do jogo simuladas. Não foi executado o cliente Windows neste ambiente.

## Fundo pendente

Este ZIP NÃO troca o fundo. Foram lidas 1.110 texturas do L2UI_CH3 Classic anterior, sem identificar o fundo da referência. O novo L2UI_CT1.rar não pôde ser transferido: excede o limite de 32 MiB da ferramenta. É necessário enviá-lo dividido em volumes de até 20 MB para continuar essa parte da análise. Não substitua o seu L2UI_CH3.u inteiro pelo pacote Classic anterior.

## Voltar ao TESTE 2

Feche o jogo e copie VOLTAR_TESTE2/system/interface.u para a sua system, ou restaure o backup feito antes deste teste. Esse arquivo é exatamente o interface.u do TESTE 2 que você confirmou funcionando.

Classes contém somente os três fontes alterados, para referência; não é necessário copiá-los para o cliente.
