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

$pythonScript = Join-Path $PSScriptRoot "deploy-remote-backend-full.py"
if (-not (Test-Path $pythonScript)) {
    throw "Missing script: $pythonScript"
}

if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    throw "Missing required command: python"
}

$args = @(
    $pythonScript,
    "--target-host", $ServerHost,
    "--target-port", $ServerPort,
    "--target-user", $ServerUser,
    "--remote-dir", $RemoteDir,
    "--compose-project-name", $ComposeProjectName,
    "--cors-allowed-origins", $CorsAllowedOrigins,
    "--gateway-bind-ip", $GatewayBindIp,
    "--gateway-port", $GatewayPort,
    "--nacos-username", $NacosUsername,
    "--nacos-password", $NacosPassword
)

if ($JumpHost) {
    $args += @("--jump-host", $JumpHost, "--jump-port", $JumpPort, "--jump-user", $JumpUser)
}

if ($JumpPrivateKeyFile) {
    $args += @("--jump-private-key-file", $JumpPrivateKeyFile)
}

if ($MiddlewareEnvFile) {
    $args += @("--middleware-env-file", $MiddlewareEnvFile)
}

if ($BackendEnvFile) {
    $args += @("--backend-env-file", $BackendEnvFile)
}

if ($StopLegacyGateway) {
    $args += "--stop-legacy-gateway"
}

& python @args
