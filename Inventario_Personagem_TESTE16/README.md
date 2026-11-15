# Inventory Viewr — TESTE 16: personagem real em uma segunda vista

## Instalar

Feche jogo e launcher. Copie os TRÊS arquivos da pasta system para a system do cliente, substituindo os anteriores. Nesta versão interface.u mudou e precisa ser copiada junto com a DLL. Mantenha seus pacotes L2Devil.u e L2UI_CH3 atuais.

## Referência analisada

Foi analisado o vídeo comprimido enviado pelo usuário: 854x480, 60 quadros por segundo, duração de 6,75 segundos. A prévia mostra o personagem com duas espadas, acompanha a corrida e a animação de uma habilidade e mantém um enquadramento frontal. O cenário permanece independente do painel. O vídeo não demonstra todos os casos de capas e encantamentos customizados.

## Mudança principal

A interface agora obtém o próprio Pawn do jogador por GetWorldActor(GetPlayerID()) e o vincula à vista 3D. O código da prévia deixa de copiar roupas/armas para HudPortraitPawn e de impor Wait_Hand. A malha, a escala da arma, o equipamento e a animação usados pelo renderer passam a ser os do ator real, em vez dos de uma cópia com pose genérica.

O código novo de câmera consulta os dados do jogador e os ossos, sem aplicar escala, posição, rotação ou animação ao jogador do mundo. As setas giram a câmera da prévia. A câmera inicial acompanha o ângulo real do ator para manter a vista frontal; os deslocamentos do enquadramento são guardados no espaço local do ator para acompanhar deslocamento e mudança de direção.

O campo de visão desta vista passa a 25 graus, com distância calculada para manter a altura do corpo. A perspectiva mais fechada busca reduzir o aumento aparente das armas próximas da lente. O ponto inferior é projetado a 83,5% da janela, sobre a região da rosa dos ventos. As proporções e a posição ainda precisam de comparação visual no cliente.

Clique nas setas: passo de 15 graus. Segurar: giro contínuo após 300 ms e aceleração depois de 900 ms. Fundo, título e texturas das setas permanecem iguais.

A prévia acompanha as ações do personagem real, incluindo correr e usar habilidade, como observado no vídeo. Este modo já não obriga o personagem da prévia a ficar sempre parado.

## O que ainda precisa ser confirmado no jogo

As capas e os efeitos de encantamento agora chegam ao renderer pelo ator que os possui no mundo. Isso remove a limitação da cópia de aparência, mas não prova que todas as DLLs de capas e todos os emissores customizados renderizem nesse segundo passe. É necessário conferir. O pacote não declara capas, partículas ou cores de enchant resolvidas sem esse teste.

Teste uma arma comum, uma arma encantada e uma capa, olhando também as costas pelas setas. Troque as peças com a janela aberta. Corra e use uma habilidade para comparar a animação com o mundo. Confira as raças que apresentavam arma/pose desproporcionais. Verifique também se aparecem nomes/títulos duplicados ou se há diferença no efeito das capas.

Se houver problema, envie system/Inventory_Viewr_R16.log e uma captura ou vídeo curto. O registro contém live=1 quando o ator vinculado corresponde ao jogador atual. Não registra senha, conta ou conversas.

## Verificação realizada

- Bytecode real da interface decodificado e executado em oito casos simulados de abertura/fechamento, jogador ausente e estados do HUD. Verificado o vínculo ao ator real e a ausência de empréstimo/liberação do Pawn do retrato do HUD. Somente quatro corpos de funções de UI diferem do arquivo de origem; os demais objetos permanecem byte a byte iguais.
- Dezoito casos x86: seis bases de carregamento com relocação PE aplicada e três perfis de altura simulados. Verificados ator real vinculado, ausência de cópia de aparência/eventos de pose, memória do ator preservada pelo callback novo, acompanhamento de movimento/ângulo, cliques e giro acelerado, enquadramento e ABI do desvio.
- Mais 36 casos verificaram o fundo. Estrutura PE e exportações conferidas.

As APIs do Engine/Windows e os perfis físicos nos testes são simulados. Não foi executado o jogo Windows neste ambiente. O resultado visual e o comportamento das DLLs de capas/efeitos no passe secundário ainda precisam da confirmação do usuário.

O renderer original já altera temporariamente sinalizadores de visibilidade para desenhar e os restaura no fim da passagem. Esse mecanismo existente foi preservado; o teste de memória descrito acima cobre o callback novo de câmera, não constitui prova de ausência de todos os efeitos internos do Engine durante um desenho real.

## Restaurar

Com jogo fechado, copie os três arquivos de VOLTAR_TESTE15/system para a system. O diretório contém a DLL do TESTE 15 e a interface/XDAT que o acompanhavam. Nenhum arquivo do servidor, das capas do mundo, montarias, aura ou efeitos de imagem é substituído.
