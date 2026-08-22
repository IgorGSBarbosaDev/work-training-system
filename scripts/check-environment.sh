#!/usr/bin/env bash
set -euo pipefail

command -v java >/dev/null || { echo 'JDK 21 não encontrado.' >&2; exit 1; }
command -v node >/dev/null || { echo 'Node.js 22 não encontrado.' >&2; exit 1; }
command -v npm >/dev/null || { echo 'npm não encontrado.' >&2; exit 1; }
command -v docker >/dev/null || { echo 'Docker não encontrado.' >&2; exit 1; }

java_version="$(java -version 2>&1 | head -n 1)"
[[ "$java_version" =~ version\ \"21\. ]] || { echo "JDK 21 é obrigatório: $java_version" >&2; exit 1; }

node_major="$(node --version | sed 's/^v//' | cut -d. -f1)"
[[ "$node_major" == "22" ]] || { echo "Node.js 22 é obrigatório: $node_major" >&2; exit 1; }

docker info --format '{{.Server.Version}}'
echo 'Ambiente compatível com o aceite técnico.'
