#!/usr/bin/env bash
# =============================================================================
# Teste do multiusuário (V5, 06/10/2026) contra o backend rodando.
#
# Precisa de 3 contas JÁ CONFIRMADAS (e-mail verificado):
#   - DONO: a conta principal (com máquinas cadastradas, de preferência)
#   - GERENTE e COLABORADOR: contas de teste
# As senhas são lidas sem aparecer na tela e não ficam gravadas.
#
# Uso (no EC2):  bash scripts/teste-multiusuario.sh            (localhost:8080)
#                bash scripts/teste-multiusuario.sh https://api.safra-pay.com
#
# No fim, desfaz tudo: o gerente sai da equipe, o colaborador é removido e a
# tarefa de teste é apagada. Só precisa de curl e python3.
# =============================================================================
set -u
BASE="${1:-http://localhost:8080}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
OK=0; FALHAS=0

check() { # check "nome" condicao(0/1)
    if [ "$2" = "1" ]; then OK=$((OK+1)); echo "  ok   $1"; else FALHAS=$((FALHAS+1)); echo "  FALHOU  $1"; fi
}
# req METODO CAMINHO TOKEN [JSON] -> $ST (status), corpo em $TMP/b, cabeçalhos em $TMP/h
req() {
    local args=(-s -o "$TMP/b" -D "$TMP/h" -w '%{http_code}' -X "$1" "$BASE$2" -H 'Content-Type: application/json')
    [ -n "$3" ] && args+=(-H "Authorization: Bearer $3")
    [ $# -ge 4 ] && args+=(--data "$4")
    ST="$(curl "${args[@]}")"
}
# j 'expressão python sobre d' -> imprime o resultado (d = corpo JSON da última resposta)
j() { python3 -c "import json,sys; d=json.load(open('$TMP/b')); r=($1); print(r if not isinstance(r,bool) else int(r))" 2>/dev/null; }
eq() { [ "$1" = "$2" ] && echo 1 || echo 0; }
login() { # login email senha -> token
    req POST /api/auth/login "" "$(python3 -c 'import json,sys; print(json.dumps({"email":sys.argv[1],"senha":sys.argv[2]}))' "$1" "$2")"
    if [ "$ST" != "200" ]; then echo "Login falhou para $1 (HTTP $ST)"; exit 1; fi
    j 'd["token"]'
}

read -rp  "E-mail do DONO: " E_D;        read -rsp "Senha do DONO: " S_D; echo
read -rp  "E-mail do GERENTE: " E_G;     read -rsp "Senha do GERENTE: " S_G; echo
read -rp  "E-mail do COLABORADOR: " E_C; read -rsp "Senha do COLABORADOR: " S_C; echo

T_D="$(login "$E_D" "$S_D")"; T_G="$(login "$E_G" "$S_G")"; T_C="$(login "$E_C" "$S_C")"
unset S_D S_G S_C
echo "Logins ok em $BASE"

echo "1) Estado inicial (limpa sobras de um teste anterior)"
req POST /api/equipe/sair "$T_G"; req POST /api/equipe/sair "$T_C"
req GET /api/equipe "$T_D"
for u in $(j '" ".join(m["uuid"] for m in d if m["email"].lower() in ("'"${E_G,,}"'","'"${E_C,,}"'") and m["status"] in ("PENDENTE","ACEITO"))'); do
    req DELETE "/api/equipe/membros/$u" "$T_D"
done
req GET /api/equipe/minha "$T_D"; check "dono é DONO" "$(eq "$(j 'd["papel"]')" DONO)"
NOME_D="$(j 'd["pessoas"][0]["nome"]')"
req GET /api/equipe/minha "$T_G"; check "gerente começa como DONO" "$(eq "$(j 'd["papel"]')" DONO)"

echo "2) Convites"
req POST /api/equipe/convites "$T_D" "{\"email\":\"$E_G\",\"papel\":\"GERENTE\"}"
check "convida gerente (201)" "$(eq "$ST" 201)"; CONV_G="$(j 'd["uuid"]')"
req POST /api/equipe/convites "$T_D" "{\"email\":\"$E_C\",\"papel\":\"COLABORADOR\"}"
check "convida colaborador (201)" "$(eq "$ST" 201)"; CONV_C="$(j 'd["uuid"]')"
req POST /api/equipe/convites "$T_D" "{\"email\":\"$E_C\",\"papel\":\"GERENTE\"}"
check "convite repetido (409 com mensagem)" "$(eq "$ST/$(j 'bool(d["mensagem"])')" 409/1)"
req POST /api/equipe/convites "$T_D" "{\"email\":\"$E_D\",\"papel\":\"GERENTE\"}"
check "convidar o próprio e-mail (400)" "$(eq "$ST" 400)"
req GET /api/equipe "$T_D"; check "lista do dono tem os 2 pendentes" "$(j 'sum(1 for m in d if m["status"]=="PENDENTE" and m["uuid"] in ("'"$CONV_G"'","'"$CONV_C"'"))==2')"

echo "3) Aceitar"
req GET /api/equipe/convites/recebidos "$T_G"; check "gerente vê o convite" "$(j 'any(c["uuid"]=="'"$CONV_G"'" for c in d)')"
req POST "/api/equipe/convites/$CONV_C/aceitar" "$T_G"; check "gerente não aceita convite alheio (404)" "$(eq "$ST" 404)"
req POST "/api/equipe/convites/$CONV_G/aceitar" "$T_G"
check "gerente aceitou -> GERENTE" "$(eq "$ST/$(j 'd["papel"]')" 200/GERENTE)"
req POST "/api/equipe/convites/$CONV_C/aceitar" "$T_C"
check "colaborador aceitou -> COLABORADOR" "$(eq "$ST/$(j 'd["papel"]')" 200/COLABORADOR)"
req GET /api/equipe/minha "$T_G"
check "gerente vê o dono e 3 pessoas" "$(j 'd["dono"]["nome"]=="'"$NOME_D"'" and len(d["pessoas"])==3')"

echo "4) Permissões"
req GET /api/maquinas "$T_C"
check "colaborador em /api/maquinas -> 403 PAPEL" "$(eq "$ST/$(grep -i '^x-safrapay-erro' "$TMP/h" | tr -d '\r' | awk '{print $2}')" 403/PAPEL)"
req GET /api/equipe "$T_G"; check "gerente não gerencia equipe (403)" "$(eq "$ST" 403)"
req GET /api/maquinas/sync "$T_D"; N_D="$(j 'len(d)')"
req GET /api/maquinas/sync "$T_G"; N_G="$(j 'len(d)')"
check "gerente enxerga as máquinas do dono ($N_D)" "$(eq "$N_G" "$N_D")"
req GET /api/anotacoes-maquina/sync "$T_G"; check "gerente faz pull de anotações (200)" "$(eq "$ST" 200)"

echo "5) Colaborador"
req GET /api/colaborador/maquinas "$T_C"
check "colaborador lista máquinas ($N_D)" "$(eq "$ST/$(j 'len(d)')" "200/$N_D")"
if [ "$N_D" != "0" ]; then
    MAQ="$(j 'd[0]["uuid"]')"
    req POST "/api/colaborador/maquinas/$MAQ/tarefas" "$T_C" '{"texto":"TESTE multiusuario - pode apagar"}'
    check "reportar problema (201)" "$(eq "$ST" 201)"; TAR="$(j 'd["uuid"]')"
    req GET "/api/colaborador/tarefas?filtro=PENDENTES&maquinaUuid=$MAQ" "$T_C"
    check "tarefa aparece nas pendentes" "$(j 'any(t["uuid"]=="'"$TAR"'" for t in d)')"
    req POST "/api/colaborador/tarefas/$TAR/concluir" "$T_C"
    check "concluir" "$(j 'd["concluida"] and d["concluidoPorNome"] is not None')"
    req POST "/api/colaborador/tarefas/$TAR/reabrir" "$T_C"
    check "reabrir" "$(j 'not d["concluida"] and d["concluidoEm"] is None')"
    req GET /api/anotacoes-maquina/sync "$T_D"
    check "dono recebe no pull com criadoPorNome" "$(j 'any(a["uuid"]=="'"$TAR"'" and a["criadoPorNome"] for a in d)')"
    req GET "/api/colaborador/tarefas?filtro=XYZ" "$T_C"; check "filtro inválido (400)" "$(eq "$ST" 400)"
    # apaga a tarefa de teste
    req GET /api/maquinas "$T_D"; MAQ_ID="$(j '[m["id"] for m in d if m["uuid"]=="'"$MAQ"'"][0]')"
    req GET "/api/anotacoes-maquina?maquinaId=$MAQ_ID" "$T_D"; TAR_ID="$(j '[a["id"] for a in d if a["uuid"]=="'"$TAR"'"][0]')"
    req DELETE "/api/anotacoes-maquina/$TAR_ID" "$T_D"; check "tarefa de teste apagada" "$(eq "$ST" 204)"
else
    echo "  (dono sem máquinas: pulei reportar/concluir)"
fi

echo "6) Sair e remover"
req POST /api/equipe/sair "$T_G"; check "gerente saiu -> DONO" "$(eq "$ST/$(j 'd["papel"]')" 200/DONO)"
req GET /api/equipe "$T_D"; MEM_C="$(j '[m["uuid"] for m in d if m["status"]=="ACEITO" and m["email"].lower()=="'"${E_C,,}"'"][0]')"
req DELETE "/api/equipe/membros/$MEM_C" "$T_D"; check "dono removeu colaborador (204)" "$(eq "$ST" 204)"
req GET /api/equipe/minha "$T_C"; check "colaborador voltou a DONO" "$(eq "$(j 'd["papel"]')" DONO)"
req GET /api/maquinas "$T_C"; check "ex-colaborador acessa os próprios dados (200)" "$(eq "$ST" 200)"

echo
echo "Resultado: $OK ok, $FALHAS falhas"
[ "$FALHAS" = "0" ]
