#!/usr/bin/env bash
set -euo pipefail

command -v java >/dev/null || { echo 'JDK 21 ou superior não encontrado.' >&2; exit 1; }
command -v node >/dev/null || { echo 'Node.js 22 não encontrado.' >&2; exit 1; }
command -v npm >/dev/null || { echo 'npm não encontrado.' >&2; exit 1; }
command -v docker >/dev/null || { echo 'Docker não encontrado.' >&2; exit 1; }

java_version="$(java -version 2>&1 | head -n 1)"
java_major="$(printf '%s\n' "$java_version" | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p')"
[[ -n "$java_major" && "$java_major" -ge 21 ]] || { echo "JDK 21 ou superior é obrigatório: $java_version" >&2; exit 1; }

node_major="$(node --version | sed 's/^v//' | cut -d. -f1)"
[[ "$node_major" == "22" ]] || { echo "Node.js 22 é obrigatório: $node_major" >&2; exit 1; }

docker info --format '{{.Server.Version}}'
echo 'Ambiente compatível com o aceite técnico.'
