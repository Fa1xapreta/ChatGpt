Compilar com Zig 0.13.0: zig cc -target x86-windows-gnu -shared -O2 -Wall -Wextra aura_wait.c exports.def -o MilAuraWait.dll
Teste local: zig cc -O2 test_binding.c -o test_binding; ./test_binding
Os testes de contrato e instalação do repositório de auditoria usam caminhos locais de análise; os relatórios estão na raiz do kit. Não equivalem a teste no jogo.
