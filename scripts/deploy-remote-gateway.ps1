param(
    [string]$ServerHost = "36.137.84.162",
    [int]$ServerPort = 22,
    [string]$ServerUser = "root",
    [string]$JumpHost = "",
    [int]$JumpPort = 22,
    [string]$JumpUser = "root",
    [string]$JumpPrivateKeyFile = "",
    [string]$RemoteDir = "/opt/school-erp/stacks/erp-backend-full",
    [string]$ComposeProjectName = "school-erp-backend-full",
    [Alias("CorsAllowedOrigin")]
    [string]$CorsAllowedOrigins = "http://localhost:5173,http://8.148.181.9:90",
    [string]$GatewayBindIp = "0.0.0.0",
    [int]$GatewayPort = 18080,
    [string]$NacosUsername = "nacos",
    [string]$NacosPassword = "nacos",
    [string]$MiddlewareEnvFile = "",
    [string]$BackendEnvFile = "",
    [bool]$StopLegacyGateway = $true
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $projectRoot "backend"
$composeFile = Join-Path $backendDir "docker-compose.microservice.yml"
$dockerfile = Join-Path $backendDir "Dockerfile.service"
$defaultMiddlewareEnv = Join-Path $projectRoot "tmp\remote-middleware.generated.env"
$serviceModules = @(
    "gateway-service",
    "auth-service",
    "master-service",
    "academic-service",
    "workflow-service",
    "billing-service",
    "notify-service"
)

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

function Read-EnvFile([string]$Path) {
    $values = @{}
    foreach ($line in Get-Content -Path $Path -Encoding UTF8) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }

        $parts = $trimmed -split "=", 2
        if ($parts.Count -eq 2) {
            $values[$parts[0]] = $parts[1]
        }
    }
    return $values
}

function New-GeneratedBackendEnv([hashtable]$MiddlewareValues, [string]$OutputPath) {
    $requiredKeys = @(
        "POSTGRES_DB",
        "POSTGRES_USER",
        "POSTGRES_PASSWORD",
        "REDIS_PASSWORD",
        "RABBITMQ_DEFAULT_USER",
        "RABBITMQ_DEFAULT_PASS",
        "RABBITMQ_DEFAULT_VHOST"
    )

    foreach ($key in $requiredKeys) {
        if (-not $MiddlewareValues.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($MiddlewareValues[$key])) {
            throw "Missing required key '$key' in middleware env file."
        }
    }

    @(
        "GATEWAY_BIND_IP=$GatewayBindIp"
        "GATEWAY_EXPOSE_PORT=$GatewayPort"
        "CORS_ALLOWED_ORIGINS=$CorsAllowedOrigins"
        "NACOS_SERVER_ADDR=nacos:8848"
        "NACOS_USERNAME=$NacosUsername"
        "NACOS_PASSWORD=$NacosPassword"
        "NACOS_DISCOVERY_ENABLED=true"
        "NACOS_CONFIG_ENABLED=false"
        "NACOS_NAMESPACE="
        "NACOS_DISCOVERY_GROUP=DEFAULT_GROUP"
        "NACOS_CONFIG_GROUP=DEFAULT_GROUP"
        "POSTGRES_HOST=postgres"
        "POSTGRES_PORT=5432"
        "POSTGRES_DB=$($MiddlewareValues["POSTGRES_DB"])"
        "POSTGRES_USER=$($MiddlewareValues["POSTGRES_USER"])"
        "POSTGRES_PASSWORD=$($MiddlewareValues["POSTGRES_PASSWORD"])"
        "REDIS_HOST=redis"
        "REDIS_PORT=6379"
        "REDIS_PASSWORD=$($MiddlewareValues["REDIS_PASSWORD"])"
        "RABBITMQ_HOST=rabbitmq"
        "RABBITMQ_PORT=5672"
        "RABBITMQ_USERNAME=$($MiddlewareValues["RABBITMQ_DEFAULT_USER"])"
        "RABBITMQ_PASSWORD=$($MiddlewareValues["RABBITMQ_DEFAULT_PASS"])"
        "RABBITMQ_VHOST=$($MiddlewareValues["RABBITMQ_DEFAULT_VHOST"])"
    ) | Set-Content -Path $OutputPath -Encoding ascii
}

function Get-BackendEnvPath {
    if ($BackendEnvFile) {
        if (-not (Test-Path $BackendEnvFile)) {
            throw "Backend env file not found: $BackendEnvFile"
        }
        return (Resolve-Path $BackendEnvFile).Path
    }

    $middlewarePath = if ($MiddlewareEnvFile) { $MiddlewareEnvFile } else { $defaultMiddlewareEnv }
    if (-not (Test-Path $middlewarePath)) {
        throw "Middleware env file not found: $middlewarePath"
    }

    $outputPath = Join-Path ([System.IO.Path]::GetTempPath()) "school-erp-backend.microservice.env"
    $middlewareValues = Read-EnvFile (Resolve-Path $middlewarePath).Path
    New-GeneratedBackendEnv $middlewareValues $outputPath
    return $outputPath
}

function New-StagingTree([string]$BackendEnvPath) {
    $stagingRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("school-erp-backend-full-" + [System.Guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Path $stagingRoot | Out-Null

    Copy-Item -Path $composeFile -Destination (Join-Path $stagingRoot "docker-compose.microservice.yml")
    Copy-Item -Path $dockerfile -Destination (Join-Path $stagingRoot "Dockerfile.service")
    Copy-Item -Path $BackendEnvPath -Destination (Join-Path $stagingRoot ".env.microservice")

    foreach ($module in $serviceModules) {
        $targetDir = Join-Path $stagingRoot "$module\target"
        New-Item -ItemType Directory -Path $targetDir -Force | Out-Null

        $jarPath = Join-Path $backendDir "$module\target\app.jar"
        if (-not (Test-Path $jarPath)) {
            throw "Build artifact not found: $jarPath"
        }

        Copy-Item -Path $jarPath -Destination (Join-Path $targetDir "app.jar")
    }

    return $stagingRoot
}

function Run-Remote([string[]]$SshArgs, [string]$Target, [string]$Command) {
    & ssh @SshArgs $Target $Command
    if ($LASTEXITCODE -ne 0) {
        throw "Remote command failed: $Command"
    }
}

function Upload-StagingTree([string[]]$ScpArgs, [string]$Target, [string]$SourceDir, [string]$DestinationDir) {
    & scp @ScpArgs -r "$SourceDir/." "${Target}:${DestinationDir}/"
    if ($LASTEXITCODE -ne 0) {
        throw "Upload failed: $SourceDir -> $DestinationDir"
    }
}

foreach ($tool in @("mvn", "ssh", "scp")) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $tool"
    }
}

$sshTarget = "$ServerUser@$ServerHost"
$sshBaseArgs = Get-SshBaseArgs
$scpBaseArgs = Get-ScpBaseArgs
$generatedEnvPath = $null
$stagingRoot = $null

try {
    $backendEnvPath = Get-BackendEnvPath
    if (-not $BackendEnvFile) {
        $generatedEnvPath = $backendEnvPath
    }

    Push-Location $backendDir
    mvn package -DskipTests "-Dapp.finalName=app"
    Pop-Location

    $stagingRoot = New-StagingTree $backendEnvPath

    Run-Remote $sshBaseArgs $sshTarget "mkdir -p $RemoteDir"
    Upload-StagingTree $scpBaseArgs $sshTarget $stagingRoot $RemoteDir

    if ($StopLegacyGateway) {
        $legacyDir = "/opt/school-erp/stacks/remote-gateway"
        $legacyCommand = "if [ -f $legacyDir/docker-compose.remote.yml ]; then cd $legacyDir && docker compose -p school-erp-remote-gateway -f docker-compose.remote.yml down --remove-orphans || true; fi"
        Run-Remote $sshBaseArgs $sshTarget $legacyCommand
    }

    Run-Remote $sshBaseArgs $sshTarget "cd $RemoteDir && docker compose -p $ComposeProjectName --env-file .env.microservice -f docker-compose.microservice.yml up -d --build --force-recreate"
    Run-Remote $sshBaseArgs $sshTarget "cd $RemoteDir && docker compose -p $ComposeProjectName --env-file .env.microservice -f docker-compose.microservice.yml ps"
}
finally {
    if ($stagingRoot -and (Test-Path $stagingRoot)) {
        Remove-Item -Path $stagingRoot -Recurse -Force
    }

    if ($generatedEnvPath -and (Test-Path $generatedEnvPath)) {
        Remove-Item -Path $generatedEnvPath -Force
    }
}
