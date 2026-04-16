import argparse
import json
import subprocess


SERVICES = [
    "gateway-service",
    "auth-service",
    "master-service",
    "academic-service",
    "workflow-service",
    "billing-service",
    "notify-service",
]
GLOBAL_PATHS = (
    "backend/erp-common/",
    "backend/pom.xml",
    "backend/.dockerignore",
    "backend/Dockerfile.service",
    "backend/docker-compose.microservice.runtime.yml",
    "scripts/deploy-remote-service.py",
    "scripts/select-backend-services.py",
    ".github/workflows/deploy-backend-services.yml",
)


def git_changed_files(base_sha: str, head_sha: str) -> list[str]:
    if not base_sha or not head_sha or base_sha == head_sha:
        return []
    completed = subprocess.run(
        ["git", "diff", "--name-only", base_sha, head_sha],
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        raise RuntimeError(completed.stderr.strip() or "git diff failed")
    return [line.strip() for line in completed.stdout.splitlines() if line.strip()]


def select_services(changed_files: list[str]) -> list[str]:
    if any(
        changed_file == path or changed_file.startswith(path)
        for changed_file in changed_files
        for path in GLOBAL_PATHS
    ):
        return SERVICES

    selected: list[str] = []
    for service in SERVICES:
        prefix = f"backend/{service}/"
        if any(changed_file.startswith(prefix) for changed_file in changed_files):
            selected.append(service)
    return selected


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--event-name", required=True)
    parser.add_argument("--service-input", default="auto")
    parser.add_argument("--base-sha", default="")
    parser.add_argument("--head-sha", default="")
    args = parser.parse_args()

    if args.event_name == "workflow_dispatch":
        if args.service_input == "all":
            print(json.dumps(SERVICES))
            return
        if args.service_input != "auto":
            print(json.dumps([args.service_input]))
            return
        print(json.dumps(SERVICES))
        return

    changed_files = git_changed_files(args.base_sha, args.head_sha)
    print(json.dumps(select_services(changed_files)))


if __name__ == "__main__":
    main()
