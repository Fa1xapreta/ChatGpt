# Coleta dos arquivos instalados do inventário

Extraia os dois arquivos COLETAR_INVENTARIO na pasta system do cliente. Feche jogo e launcher para que os arquivos permaneçam estáveis durante a coleta. Execute COLETAR_INVENTARIO.cmd. Envie o ZIP Inventario_Instalado com data/hora criado na Área de Trabalho; se ela estiver indisponível, o caminho temporário será exibido.

O coletor copia interface.u, interface.xdat, MileniumPortraitNative.dll e, se existirem, L2Devil.utx/L2Devil.u das pastas system e SysTextures. Inclui somente esses arquivos e uma relação de tamanhos/hashes SHA256. Não lê credenciais, configurações de conexão ou outros arquivos do cliente. Não carrega DLLs nem substitui arquivos do jogo. Requer Windows PowerShell e .NET com System.IO.Compression.FileSystem.

Este é um coletor, não uma nova correção. A prévia vazia e as setas sem aparecer ainda precisam de diagnóstico nos arquivos efetivamente instalados. A verificação automatizada de bytecode e emulação anterior não reproduziu o desenho real no Windows.
