# Inventory Viewr — TESTE 13: câmera baseada nos limites do modelo

## Instalar

Com jogo e launcher fechados, copie SOMENTE system/MileniumPortraitNative.dll deste ZIP para a system do cliente, substituindo a DLL do TESTE 12.

Mantenha interface.u, interface.xdat, L2Devil.u e L2UI_CH3 do teste atual. O layout, fundo e setas não foram alterados.

## Evidência e correção candidata

O registro do cliente confirmou um ator válido chegando ao callback de desenho em cinco quadros. Sua posição era aproximadamente (83307.98,148207.03,-3410.60), enquanto a câmera calculada apontava horizontalmente na altura Z=-3415.87, cerca de 5,27 unidades abaixo da origem do ator. Isso confirma que não era simplesmente falta de vínculo do Pawn. A ausência de imagem ainda não foi reproduzida no Windows deste ambiente; o registro não prova sozinho a causa completa.

O cálculo anterior tentava deduzir o corpo inteiro a partir de CollisionHeight e PortraitFocus, calculado por osso, e congelava essa referência na primeira passagem. Essa referência foi retirada da câmera.

Agora a câmera original calculada pelo Engine é preservada durante os primeiros 300 ms e enquanto não houver uma caixa de limites válida do modelo. Depois, os limites WORLD-space que o próprio renderer usa determinam o enquadramento. A altura projetada desejada é 62% da área útil, com margem lateral para as malhas presentes. O ponto inferior é projetado a 83,5% da altura da janela: cerca de y=358 na janela de 429 pixels, próximo à rosa dos ventos marcada pelo usuário.

O perfil é fixado após a inicialização para evitar mudança de tamanho ao trocar o set. Mantém-se a rotação somente pelas setas. A aparência final ainda precisa de confirmação visual no cliente.

## Conferir

Abra a prévia e aguarde um segundo. Confira se o boneco aparece, a posição dos pés e a rotação pelas setas. Se continuar invisível ou fora do lugar, envie system/Inventory_Viewr_R13.log e uma captura. Se o arquivo não for criado, informe isso. O registro inclui agora os limites Z/Y e o estado de inicialização do enquadramento, além de ator e câmera.

O registro limita-se a 30 linhas por execução, com intervalo quando os limites ainda não estão disponíveis. Pode depender de permissão de escrita na pasta do cliente. Não coleta senha, conta ou conversas.

Capa e brilho do encantamento continuam pendentes. O sincronismo de malhas permanece igual ao TESTE 12; este pacote não declara esses efeitos resolvidos.

## Validação

Seis execuções emuladas x86 em bases diferentes, com relocação PE aplicada. Verificados preservação da câmera do Engine durante o carregamento, uso de limites válidos, projeção dos pés em y=358,215, enquadramento estável apesar de alterações do foco do osso/cápsula e dos limites posteriores, controle pelas setas, troca de malhas e caminho de registro. As caixas de limites dos testes são simuladas, não medidas no cliente do usuário. Chamadas do Windows/Engine são simuladas; o jogo Windows não foi executado neste ambiente.

Verificados também seções PE consecutivas, arquivos dentro dos limites e exportações nativas preservadas.

## Restaurar

Com jogo fechado, copie VOLTAR_TESTE12/system/MileniumPortraitNative.dll de volta para a system. Não são substituídos arquivos do servidor, capas do mundo, aura, montarias, interface ou efeitos de imagem.
