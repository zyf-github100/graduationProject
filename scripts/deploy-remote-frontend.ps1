param(
    [string]$JumpHost = "8.148.181.9",
    [int]$JumpPort = 22,
    [string]$JumpUser = "root",
    [string]$JumpPrivateKeyFile = "",
    [string]$TargetHost = "36.137.84.162",
    [int]$TargetPort = 22,
    [string]$TargetUser = "root",
    [string]$RemoteDir = "/opt/school-erp/stacks/erp-frontend",
    [string]$ComposeProjectName = "school-erp-frontend",
    [string]$FrontendBindIp = "127.0.0.1",
    [int]$FrontendExposePort = 18200,
    [int]$BastionFrontendPort = 90
)

$ErrorActionPreference = "Stop"

$pythonScript = Join-Path $PSScriptRoot "deploy-remote-frontend.py"
if (-not (Test-Path $pythonScript)) {
    throw "Missing script: $pythonScript"
}

if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    throw "Missing required command: python"
}

$args = @(
    $pythonScript,
    "--jump-host", $JumpHost,
    "--jump-port", $JumpPort,
    "--jump-user", $JumpUser,
    "--target-host", $TargetHost,
    "--target-port", $TargetPort,
    "--target-user", $TargetUser,
    "--remote-dir", $RemoteDir,
    "--compose-project-name", $ComposeProjectName,
    "--frontend-bind-ip", $FrontendBindIp,
    "--frontend-expose-port", $FrontendExposePort,
    "--bastion-frontend-port", $BastionFrontendPort
)

if ($JumpPrivateKeyFile) {
    $args += @("--jump-private-key-file", $JumpPrivateKeyFile)
}

& python @args
