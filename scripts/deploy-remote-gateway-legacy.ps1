param(
    [string]$ServerHost = "36.137.84.162",
    [int]$ServerPort = 22,
    [string]$ServerUser = "root",
    [string]$JumpHost = "",
    [int]$JumpPort = 22,
    [string]$JumpUser = "root",
    [string]$JumpPrivateKeyFile = "",
    [string]$RemoteDir = "/opt/school-erp/stacks/remote-gateway",
    [string]$ComposeProjectName = "school-erp-remote-gateway",
    [string]$CorsAllowedOrigin = "http://localhost:5173",
    [string]$GatewayBindIp = "0.0.0.0",
    [int]$GatewayPort = 18080
)

$ErrorActionPreference = "Stop"

Write-Warning "当前脚本是 legacy 远端网关联调方案。真正的远端微服务部署请改用 scripts/deploy-remote-gateway.ps1。"

$projectRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $projectRoot "backend"
$gatewayDir = Join-Path $backendDir "gateway-service"
$composeFile = Join-Path $backendDir "gateway-service\docker-compose.remote.yml"
$dockerfile = Join-Path $gatewayDir "Dockerfile"
$jarFile = Join-Path $gatewayDir "target\gateway-service-0.1.0.jar"

function Get-JumpTarget {
    if (-not $JumpHost) {
        return $null
    }

    return "{0}@{1}:{2}" -f $JumpUser, $JumpHost, $JumpPort
}

function Get-JumpOptions {
    if (-not $JumpHost) {
        return @()
    }

    if ($JumpPrivateKeyFile) {
        if (-not (Test-Path $JumpPrivateKeyFile)) {
            throw "Jump private key file not found: $JumpPrivateKeyFile"
        }
        $resolvedKeyPath = (Resolve-Path $JumpPrivateKeyFile).Path
        $proxyCommand = 'ProxyCommand=ssh -i "{0}" -o IdentitiesOnly=yes -p {1} {2}@{3} -W %h:%p' -f $resolvedKeyPath, $JumpPort, $JumpUser, $JumpHost
        return @("-o", $proxyCommand)
    }

    return @("-J", (Get-JumpTarget))
}

function Get-SshBaseArgs {
    $args = @("-p", $ServerPort)
    $args += Get-JumpOptions
    return $args
}

function Get-ScpBaseArgs {
    $args = @("-P", $ServerPort)
    $args += Get-JumpOptions
    return $args
}

foreach ($tool in @("mvn", "ssh", "scp")) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $tool"
    }
}

$sshTarget = "$ServerUser@$ServerHost"
$sshBaseArgs = Get-SshBaseArgs
$scpBaseArgs = Get-ScpBaseArgs
$envFile = Join-Path ([System.IO.Path]::GetTempPath()) "school-erp-gateway.remote.env"

@(
    "SERVER_PORT=8080"
    "GATEWAY_BIND_IP=$GatewayBindIp"
    "GATEWAY_EXPOSE_PORT=$GatewayPort"
    "CORS_ALLOWED_ORIGIN=$CorsAllowedOrigin"
    "AUTH_SERVICE_URI=http://host.docker.internal:18081"
    "MASTER_SERVICE_URI=http://host.docker.internal:18082"
    "ACADEMIC_SERVICE_URI=http://host.docker.internal:18083"
    "WORKFLOW_SERVICE_URI=http://host.docker.internal:18084"
    "BILLING_SERVICE_URI=http://host.docker.internal:18085"
    "NOTIFY_SERVICE_URI=http://host.docker.internal:18086"
) | Set-Content -Path $envFile -Encoding ascii

try {
    Push-Location $backendDir
    mvn -pl gateway-service -am clean package -DskipTests
    Pop-Location

    if (-not (Test-Path $jarFile)) {
        throw "Gateway jar not found: $jarFile"
    }

    & ssh @sshBaseArgs $sshTarget ("mkdir -p {0}" -f $RemoteDir)
    & scp @scpBaseArgs $composeFile ("{0}:{1}/docker-compose.remote.yml" -f $sshTarget, $RemoteDir)
    & scp @scpBaseArgs $envFile ("{0}:{1}/.env" -f $sshTarget, $RemoteDir)
    & scp @scpBaseArgs $dockerfile ("{0}:{1}/Dockerfile" -f $sshTarget, $RemoteDir)
    & scp @scpBaseArgs $jarFile ("{0}:{1}/app.jar" -f $sshTarget, $RemoteDir)

    $remoteCommand = "cd $RemoteDir && docker build -t school-erp/gateway-service:remote -f Dockerfile . && docker compose -p $ComposeProjectName -f docker-compose.remote.yml --env-file .env up -d --force-recreate"
    & ssh @sshBaseArgs $sshTarget $remoteCommand
}
finally {
    if (Test-Path $envFile) {
        Remove-Item $envFile -Force
    }
}
