param(
    [string]$ServerHost = "36.137.84.162",
    [int]$ServerPort = 22,
    [string]$ServerUser = "root",
    [string]$JumpHost = "",
    [int]$JumpPort = 22,
    [string]$JumpUser = "root",
    [string]$JumpPrivateKeyFile = "",
    [string]$TunnelBindAddress = "0.0.0.0"
)

$ErrorActionPreference = "Stop"

Write-Warning "当前脚本仅用于 legacy 远端网关 + 本地业务服务联调。真正微服务部署不再需要这些反向隧道。"

if (-not (Get-Command ssh -ErrorAction SilentlyContinue)) {
    throw "Missing required command: ssh"
}

function Get-JumpSshOptions {
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

    return @("-J", ("{0}@{1}:{2}" -f $JumpUser, $JumpHost, $JumpPort))
}

$sshTarget = "$ServerUser@$ServerHost"
$forwardings = @(
    "${TunnelBindAddress}:18081:127.0.0.1:8081",
    "${TunnelBindAddress}:18082:127.0.0.1:8082",
    "${TunnelBindAddress}:18083:127.0.0.1:8083",
    "${TunnelBindAddress}:18084:127.0.0.1:8084",
    "${TunnelBindAddress}:18085:127.0.0.1:8085",
    "${TunnelBindAddress}:18086:127.0.0.1:8086"
)

$sshArgs = @(
    "-NT",
    "-o", "ExitOnForwardFailure=yes",
    "-o", "ServerAliveInterval=30",
    "-p", $ServerPort
)

if ($JumpHost) {
    $sshArgs += Get-JumpSshOptions
}

foreach ($forwarding in $forwardings) {
    $sshArgs += @("-R", $forwarding)
}

$sshArgs += $sshTarget

Write-Host "Opening reverse tunnels to $sshTarget. Keep this window open while debugging."
& ssh @sshArgs
