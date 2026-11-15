# Inventory Viewr — TESTE 18: enquadramento independente das funções dos ossos

Com o jogo e launcher fechados, copie os três arquivos de system para a system do cliente. Inclui interface.u e interface.xdat idênticos aos do TESTE 17; a mudança desta versão está em MileniumPortraitNative.dll. Mantenha L2Devil.u e L2UI_CH3 atuais.

O log R17 enviado confirma live=1, init=0, ready=0, body=-1 e caption=-1. Os offsets dos nomes dos ossos foram encontrados (1104, 1108, 1112), porém o conjunto de requisitos da inicialização não se completou. O registro antigo não informa qual função específica faltou. A interceptação do nome também não encontrou sua exportação. A câmera permaneceu exatamente 45 unidades acima do ator.

A falha concreta no caminho alternativo: mesmo com dimensões numéricas plausíveis disponíveis, o perfil só era aceito quando os ossos funcionavam ou o sinalizador FBox.IsValid estava ativo. Sem isso, a câmera nova nunca era aplicada. O TESTE 18 aceita os limites numéricos do corpo após conferir altura, largura e proximidade à posição do ator, independentemente desse sinalizador e das funções dos ossos. Mantém a preferência pelos ossos quando estão disponíveis. Rejeita limites incompletos, dimensões inválidas e limites distantes do ator.

Isso permite calcular altura e centro da câmera a partir dos dados que já aparecem no seu log. Os pés são projetados a 83,5% da altura do painel, na região da rosa dos ventos. O tamanho continua limitado pela largura para evitar cortar modelos largos. O perfil é mantido durante a sessão da prévia para evitar variar com cada troca de equipamento.

Validação específica: foram extraídas nove combinações de posição/dimensões do log, correspondentes a dois atores. Elas foram executadas em seis bases PE relocadas (54 casos), com as funções dos ossos indisponíveis e FBox.IsValid desativado. As oito combinações com corpo carregado passaram a inicializar; a combinação parcial de carregamento foi rejeitada. Nos casos aceitos, a projeção dos pés ficou em aproximadamente 358 pixels num painel de 429 pixels. O mesmo cenário executado na DLL TESTE 17 manteve ready=0 nos 54 casos, reproduzindo a falha anterior. Outros 18 casos verificaram o caminho com ossos, as setas e os desvios. Os relatórios UI e fundo são da versão 17, pois esses dois arquivos e esse caminho não mudaram.

As chamadas Engine/Windows são simuladas. A comparação usa os valores medidos do seu log, mas não substitui a confirmação visual no jogo. A altura calculada deve ser conferida nas raças que apresentavam o corte.

Esta versão não corrige separadamente capa, enchant ou nomes. Mantém as proteções de abertura, troca de personagem e recursão do TESTE 17. O caption=-1 informado significa que a supressão de nomes não foi instalada nessa execução. Não há confirmação de que o travamento foi eliminado.

Teste entrando com um personagem, abrindo o inventário e depois o botão da prévia. Confira corpo inteiro, pés na rosa dos ventos e rotação pelas setas. Confira também as costas, capa e arma encantada. Se persistir, envie system/Inventory_Viewr_R18.log. O novo registro inclui boxFlag, meshOff, rotOff e os endereços das funções dos ossos, para distinguir os requisitos que faltaram.

Para restaurar, feche o jogo e copie todos os três arquivos de VOLTAR_TESTE15/system. Essa restauração reúne DLL e interface compatíveis; não copie apenas a DLL antiga sobre a interface atual. Nenhum arquivo de servidor, capa do mundo, aura ou efeito gráfico é substituído.
