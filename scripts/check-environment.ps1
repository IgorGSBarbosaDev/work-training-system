$ErrorActionPreference = 'Stop'

function Require-Command([string] $Name, [string] $InstallHint) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Comando '$Name' não encontrado. $InstallHint"
    }
}

Require-Command 'java' 'Instale o JDK 21 ou superior e configure JAVA_HOME.'
Require-Command 'node' 'Instale o Node.js 22.'
Require-Command 'npm' 'Instale o npm compatível com Node.js 22.'
Require-Command 'docker' 'Inicie o Docker Desktop com o engine Linux.'
Require-Command 'ffmpeg' 'Instale o ffmpeg para gerar o vídeo real do aceite.'

$javaVersion = (& java -version 2>&1 | Select-Object -First 1).ToString()
$javaMajor = $javaVersion -replace '.*version "(\d+).*', '$1'
if ($javaMajor -notmatch '^\d+$' -or [int]$javaMajor -lt 21) {
	throw "JDK 21 ou superior é obrigatório. Detectado: $javaVersion"
}

$nodeMajor = (& node --version).Trim().TrimStart('v').Split('.')[0]
if ([int]$nodeMajor -ne 22) {
    throw "Node.js 22 é obrigatório. Detectado: $nodeMajor"
}

& docker info --format '{{.ServerVersion}}' | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw 'Docker CLI encontrado, mas o daemon não está disponível. Inicie Docker Desktop antes do aceite.'
}
Write-Host 'Ambiente compatível com o aceite técnico.'
