# Inventory Viewr — TESTE 17

Feche o jogo e o launcher. Copie os TRÊS arquivos de system para a system do cliente: interface.u, interface.xdat e MileniumPortraitNative.dll. Copie todos juntos. Mantenha L2Devil.u e L2UI_CH3 atuais.

O log R16 enviado apresenta live=1, porém ready=0 nos 30 registros: o ator corresponde ao jogador, mas o perfil de enquadramento não inicializou. A nova versão resolve as classes diretamente, sem depender da inicialização do retrato opcional do HUD, e procura os ossos na malha quando os nomes do ator estão ausentes. Isso corrige uma condição reproduzida em simulação compatível com o log; os globais internos do cliente não foram medidos pelo registro antigo.

A câmera usa cabeça e pés para enquadrar o corpo, visando os pés a 83,5% da altura da janela, sobre a rosa dos ventos, com vista frontal inicial. Mantém o ator real e sua animação. As setas giram 15 graus por clique; segurar inicia giro contínuo após 300 ms e acelera após 900 ms. O resultado de tamanho e posição precisa ser conferido no cliente.

A prévia inicia fechada, fecha ao abrir/fechar o inventário e ao reiniciar a sessão. Somente o botão faz um novo vínculo válido. Os eventos de itens não refazem mais o vínculo. Foram adicionadas verificações de carregamento da malha, vínculo obsoleto após troca de jogador e desenho recursivo. O log recebido não contém a pilha do travamento: estas medidas não constituem confirmação de sua causa nem de sua eliminação.

Para evitar nomes duplicados, a DLL intercepta em memória uma implementação reconhecida de UCanvas.DrawTargetName, apenas durante o desenho da prévia e na mesma thread. Fora dessa passagem, o desenho original é preservado. A interceptação é ignorada se a implementação não for reconhecida ou já tiver sido modificada por outro módulo. Nenhum Engine.dll em disco é substituído.

Armaduras e armas continuam vindo do próprio ator do mundo. Não foi aplicada uma correção específica às capas ou ao enchant: sua presença no segundo passe ainda precisa de confirmação. Com o corpo inteiro visível, gire para conferir as costas e troque capa/arma encantada com a janela aberta.

Teste primeiro entrar com outro personagem: a prévia deve permanecer fechada. Abra o inventário e depois use o botão da prévia. Confira posição, setas, troca de equipamentos e ausência do nome duplicado. Repita após sair e entrar em outra conta. Se falhar, envie system/Inventory_Viewr_R17.log e a mensagem de erro, se houver. O novo registro inclui estado de inicialização, motivo de falha na busca dos ossos e estado da interceptação de nomes.

Verificação: 18 casos x86 com seis bases PE realmente relocadas e APIs Engine/Windows simuladas; 64 casos de ciclo de vida executando bytecode da interface; 36 casos de fundo. Foram verificados os desvios de entrada/saída, proteções de recursão e vínculo obsoleto, nomes ausentes, inicialização sem HUD, rotação e escopo dos nomes. Somente seis corpos de funções da interface diferem da origem; os demais objetos permanecem byte a byte iguais. Estrutura PE, exportações, integridade ZIP e hashes conferidos. Os relatórios acompanham o pacote.

O jogo Windows não foi executado neste ambiente. Capas, enchant, enquadramento visual e estabilidade na troca de conta continuam pendentes de validação no cliente. Os testes de memória cobrem o callback novo, não todos os efeitos internos do renderer original.

Para restaurar, feche o jogo e copie TODOS os três arquivos de VOLTAR_TESTE15/system. Essa pasta reúne DLL e interface compatíveis entre si; não restaure apenas a DLL sobre a interface nova. Arquivos de servidor, capas do mundo, aura e efeitos gráficos não são substituídos por este pacote.
