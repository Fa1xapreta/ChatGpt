#include <stdio.h>
#include <stdlib.h>
#include "binding.h"
#define check(x) do{if(!(x)){fprintf(stderr,"FAIL %d: %s\n",__LINE__,#x);abort();}}while(0)
int main(void){uintptr_t shim=0x10000,core=0x20000,slot=0;check(!binding_ready(slot,shim,core));check(!binding_replace_allowed(slot,shim,core));slot=shim;check(!binding_ready(slot,shim,0));check(!binding_ready(slot,shim,core));check(binding_replace_allowed(slot,shim,core));if(binding_replace_allowed(slot,shim,core))slot=core;check(binding_ready(slot,shim,core));check(!binding_replace_allowed(slot,shim,core));check(!binding_ready(core,core,core));slot=0x30000;check(!binding_replace_allowed(slot,shim,core));check(!binding_ready(slot,shim,core));puts("PASS: delayed identity, status-before/after binding, idempotence and unknown-owner rejection");return 0;}
