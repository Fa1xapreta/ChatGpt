# Inventory Viewr — TESTE 14

Com o jogo e launcher fechados, copie SOMENTE system/MileniumPortraitNative.dll deste ZIP para a system do cliente. Mantenha interface.u, interface.xdat, L2Devil.u e L2UI_CH3 atuais.

## Ajustes

- Personagem mais alto no painel e um pouco maior. A altura projetada desejada passou de 62% para 72% da área útil, e a linha inferior de 83,5% para 80% da janela. O objetivo é mostrar o corpo inteiro acima das setas; a aparência final depende de conferência no cliente.
- O enquadramento prefere agora as coordenadas reais da cabeça e dos pés da pose. Os nomes HeadBone, RightFootBone e LeftFootBone são copiados do jogador para a prévia. O Engine resolve os índices com USkeletalMesh.MatchRefBone e obtém as coordenadas por AActor.GetBoneCoordsWithBoneIndex. Não depende só de uma caixa de limites em cache que pode ainda estar indisponível.
- A câmera parte da frente do Pawn em rotação zero, sem inclinação, e enquadra o centro do corpo. Ao reabrir após ficar oculto, volta à vista frontal.
- Cada clique na seta gira 15 graus. Vinte e quatro cliques completam uma volta. Segurar não continua girando; arrastar no corpo não gira.
- O processamento dos cliques foi separado da disponibilidade do enquadramento. Quando os dados da pose ainda não estão prontos, as setas também podem girar a câmera original do Engine.

O fundo e os botões do TESTE 12 não foram substituídos. O sincronismo existente de armaduras e malhas de armas é mantido. Capa e brilho de encantamento ainda não estão confirmados/resolvidos por este teste.

## Conferir

Abra a prévia e aguarde um segundo. Confira tamanho, altura e frente do personagem. Clique, solte e clique novamente em cada seta: cada clique deve dar um passo. Se posição ou controle ainda falharem, envie system/Inventory_Viewr_R14.log e uma captura. O registro inclui a referência usada para o corpo, alturas da cabeça/pés e posição da área de desenho.

## Validação

Seis emulações x86 com relocação PE aplicada: coordenadas de pose simuladas, referência frontal, projeção inferior em y=343,2 e superior em y=80,4 para uma janela de 429 pixels; cliques antes do enquadramento estar disponível; passos de 15 graus; ausência de giro contínuo ao segurar; 24 cliques retornando à frente; estabilidade ao trocar equipamento e preservação do ator real. Outros 36 casos verificaram o fundo. Seções PE e exportações conferidas. As APIs do Engine/Windows são simuladas, portanto não foi confirmado o resultado visual no cliente Windows neste ambiente.

As funções nativas chamadas foram encontradas no Engine fornecido e suas convenções de chamada x86 foram conferidas no binário. A caixa do renderer anterior ainda é alternativa se as coordenadas dos ossos não estiverem disponíveis; caso nenhuma referência seja válida, permanece a câmera original, com rotação pelas setas.

## Restaurar

Com jogo fechado, copie VOLTAR_TESTE13/system/MileniumPortraitNative.dll para a system. Nenhum arquivo do servidor, das capas do mundo, aura, montarias ou efeitos de imagem é alterado.
