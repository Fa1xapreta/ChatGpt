# Aura automática de PK e PvP flag — B2

O diagnóstico A1 confirmou o driver original e o núcleo pronto, mas a aura automática continuou ausente. Encontramos outro bloqueio: o CliExt procurava a função ContornoDourado_Status no carregador de espera, que não a exportava. Além disso, o controlador precisava do identificador do núcleo real para concluir sua inicialização.

A ponte B2 exporta a função e corrige esse identificador somente no perfil conhecido do CliExt. Mantém os módulos originais e suas verificações. O driver original restaurado no A1 também é preservado.

## Instalar

1. Feche o jogo e extraia o ZIP inteiro em uma pasta.
2. Execute INSTALAR.bat e aceite a elevação do Windows. O instalador procura C:\Program Files (x86)\LINEAGE ROYAL NOVO; permite selecionar outro cliente.
3. Entre no jogo e teste ficando com PvP flag ou PK. Não é necessário pressionar F10 para esse teste automático.

Não reinstale o kit completo. Este reparo preserva as capas, Fighter.ukx, montarias, cores e arquivos do servidor. Não exige nova alteração Java. O envio dos comandos de aura pelo servidor continua necessário.

O instalador valida os arquivos e guarda o estado anterior em Milenium_Aura_Backup_B2_*. DESFAZER.bat restaura esse estado, inclusive a ponte anterior. Recusa sobrescrever modificações posteriores.

Se continuar ausente, mantenha o jogo aberto com o personagem flag ou PK, execute VERIFICAR.bat e envie Resultado_Aura_B2.txt. O novo log Milenium_Aura_PKFlag_B2.log registra a inicialização do controlador e mudanças de estado, sem conteúdo de chat.

## Validação e limites

Compilação Windows x86, testes do contrato binário com relocação, máquina de estados da vinculação e nove cenários de instalação/restauração passaram. A execução do jogo no Windows e o retorno visual da aura ainda precisam de confirmação. CONTRATO.json e TESTES.json registram esses limites.

O código novo está em fonte/ sob MIT. Os componentes de terceiros preservam suas licenças e verificações. Nenhum bypass de licença foi acrescentado.
