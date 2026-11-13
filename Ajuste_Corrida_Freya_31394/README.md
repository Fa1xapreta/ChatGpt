# Freya 31394 — teste reversível de corrida, humano fighter masculino

[Baixar o TESTE 6](Milenium_Corrida_Freya_TESTE6.zip).

Feche o jogo, extraia e execute **APLICAR.bat**. Teste correr, virar, parar e montar. Para voltar, feche o jogo e execute **DESFAZER.bat**. O original é guardado em `Milenium_Corrida_Backup_TESTE6` dentro do cliente.

O vídeo confirmou a bota atravessando a região inferior do tecido durante a passada. O teste altera um componente do perfil de movimento MFighter de −50 para −65, para avaliar se o tecido ganha distância da passada. **O resultado visual ainda não foi confirmado no jogo. Não foi adicionada colisão com as pernas.**

O perfil afeta **todas as capas desse corpo**, não apenas a Freya. Apenas um campo double na DLL é alterado; as seções de código e os perfis dos demais corpos foram conferidos sem mudanças. As entradas de parado/caminhada e montaria também foram preservadas, mas a transição montado deve ser testada.

Os arquivos dos efeitos visuais, os INIs, Java, DATs, XMLs e multisells não mudam. O script valida a DLL recebida e cria backup antes de aplicar. O relatório contém hashes, offsets e referências da análise. Testes automatizados verificam aplicação, repetição, restauração, recusa de versões diferentes e recuperação de falha de gravação; usam PowerShell no Linux com elevação/interface Windows simuladas.

Os scripts Python de auditoria/teste dependem das pastas de análise originais desta sessão; não são necessários para instalar. Não foi executado o cliente Windows neste ambiente.
