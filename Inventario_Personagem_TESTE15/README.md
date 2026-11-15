# Inventory Viewr — TESTE 15

Com jogo e launcher fechados, substitua SOMENTE system/MileniumPortraitNative.dll. Mantenha a interface e as texturas atuais.

O usuário confirmou que a posição do TESTE 14 ficou perfeita. Este ajuste aumenta a altura projetada desejada de 72% para 77% da área útil (aproximadamente 7% maior), mantendo a linha dos pés em 80% da altura da janela e a vista frontal inicial.

Setas: clique dá um passo de 15 graus. Manter pressionado começa o giro contínuo após 300 ms, a 120 graus por segundo; após 900 ms, a velocidade sobe para 180 graus por segundo. Soltar ou sair da seta interrompe o giro. Arrastar no personagem não gira. Vinte e quatro cliques separados completam uma volta.

As coordenadas da cabeça/pés, o sincronismo das malhas e o layout foram preservados. Capa e brilho de encantamento continuam pendentes; esta entrega não os declara corrigidos.

Validação: seis emulações x86 com relocação PE aplicada, mantendo a projeção dos pés em y=343,2 na janela de 429 pixels; verificados clique curto, 24 cliques, atraso e aceleração ao segurar, parada ao sair/soltar e preservação do ator real. Estrutura PE e exportações conferidas. As APIs de Engine e Windows foram simuladas; o tamanho e o comportamento finais precisam de confirmação visual no jogo.

Se houver algum problema, o diagnóstico é system/Inventory_Viewr_R15.log. Para restaurar o teste de posição confirmada, copie VOLTAR_TESTE14/system/MileniumPortraitNative.dll com o jogo fechado.

Não são alterados arquivos da interface, servidor, sistema de capas do mundo, montarias, aura ou efeitos de imagem.
