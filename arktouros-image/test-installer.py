"""Offline installer test. All mutable data belongs to a temporary copy."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import time
import tarfile
import zipfile
from io import BytesIO
from urllib.request import urlopen


def run(*args, cwd=None, check=True):
    return subprocess.run(args, cwd=cwd, text=True, encoding="utf-8", check=check, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)


def get(path, port=50050):
    with urlopen(f"http://127.0.0.1:{port}{path}", timeout=5) as response:
        return response.read()


def api(path):
    value = json.loads(get("/api/v0" + path))
    assert value["code"] == 2000, value
    return value.get("result")


def wait_for_data():
    for _ in range(60):
        try:
            if int(api("/logs?pageNum=1&pageSize=10")["total"]) == 60:
                return
        except (OSError, AssertionError, KeyError, ValueError):
            pass
        time.sleep(1)
    raise AssertionError("Example import timed out")


def main(bundle):
    if os.name == "nt":
        os.environ["MSYS_NO_PATHCONV"] = "1"
    run("docker", "info")
    if run("docker", "container", "inspect", "arktouros-all-in-one", check=False).returncode == 0:
        raise SystemExit("An existing arktouros-all-in-one container must not be disturbed")
    if run("docker", "image", "inspect", "arktouros-all-in-one:0.1", check=False).returncode == 0:
        raise SystemExit("The legacy fixture tag is already occupied; use an isolated Docker engine")
    with tempfile.TemporaryDirectory(prefix="arktouros-smoke-") as temporary:
        root = Path(temporary)
        if os.name != "nt":
            docker_path = shutil.which("docker")
            binaries = root / "docker-only"
            binaries.mkdir()
            for name in ("bash", "dirname", "uname", "sha256sum", "mkdir", "cat", "date", "sleep"):
                (binaries / name).symlink_to(shutil.which(name))
            wrapper = binaries / "docker"
            wrapper.write_text('#!/bin/bash\nif [[ "${1:-}" == compose ]]; then exit 127; fi\n'
                               + f'exec "{docker_path}" "$@"\n', encoding="utf-8")
            wrapper.chmod(0o755)
            os.environ["PATH"] = str(binaries)
            assert shutil.which("docker-compose") is None
            assert run("docker", "compose", "version", check=False).returncode == 127
        with zipfile.ZipFile(bundle) as archive:
            for member in archive.namelist():
                if not (root / member).resolve().is_relative_to(root):
                    raise ValueError("Unsafe ZIP path")
            archive.extractall(root)
        working = root / "install-arktouros-image"
        fixture = root / "legacy.tar"
        with tarfile.open(fixture, "w") as archive:
            marker = b"legacy data must survive container cleanup"
            info = tarfile.TarInfo("legacy-marker.txt")
            info.size = len(marker)
            archive.addfile(info, BytesIO(marker))
        legacy_id = run("docker", "import", str(fixture), "arktouros-all-in-one:0.1").stdout.strip()
        run("docker", "create", "--name", "arktouros-all-in-one", "arktouros-all-in-one:0.1", "/bin/true")
        try:
            print(run("bash", "start-arktouros-all-in-one.sh", "-i", "127.0.0.1", cwd=working).stdout, flush=True)
            assert run("docker", "image", "inspect", legacy_id, check=False).returncode != 0
            backup = next((working / "backups").glob("*/container-rootfs.tar"))
            with tarfile.open(backup) as archive:
                assert archive.extractfile("legacy-marker.txt").read() == marker
            example = working / "examples/arktouros-demo.txt"
            inbox = working / "arktouros-apm-api/input_logs/arktouros-demo.txt"
            shutil.copyfile(example, inbox)
            wait_for_data()
            assert int(api("/services?pageNum=1&pageSize=10")["total"]) == 4
            topology = api("/service/topology")
            assert len(topology["nodes"]) == 4 and len(topology["calls"]) == 3, topology
            assert len(api("/metrics?serviceName=demo-gateway")) == 2
            assert len(api("/trace/spanNames?serviceName=demo-order")) == 1
            assert api("/trace/topology?serviceName=demo-gateway&traceId=demo-trace-00&innerService=false")
            for port in (80, 50052, 50053):
                assert b"<html" in get("/", port).lower()
            assert b"http://127.0.0.1:50050" in get("/arktourosUiConfig.js", 50052)
            assert json.loads(get("/config.json", 50053))["backendUrl"] == "http://127.0.0.1:50050/api/v0"
            assert run("docker", "exec", "arktouros-all-in-one", "test", "-d", "/usr/share/elasticsearch", check=False).returncode == 1
            counts = run("docker", "exec", "arktouros-all-in-one", "python3", "-c",
                         "import sqlite3,json; c=sqlite3.connect('/usr/local/arktouros-apm-api/db/arktouros.sqlite'); print(json.dumps(dict(c.execute('select kind,count(*) from telemetry group by kind'))))")
            assert json.loads(counts.stdout) == {"service": 4, "span": 120, "log": 60, "metric": 240}, counts.stdout
            assert run("bash", "import-example.sh", cwd=working, check=False).returncode != 0
            print(run("docker", "restart", "arktouros-all-in-one").stdout, flush=True)
            wait_for_data()
            assert int(api("/services?pageNum=1&pageSize=10")["total"]) == 4
            # Re-running the one-click installer must not remove data or duplicate examples.
            print(run("bash", "start-arktouros-all-in-one.sh", "-i", "127.0.0.1", cwd=working).stdout, flush=True)
            assert int(api("/logs?pageNum=1&pageSize=10")["total"]) == 60
            run("docker", "exec", "arktouros-all-in-one", "curl", "-fsS", "-X", "DELETE", "http://127.0.0.1:50050/api/v0/all")
            assert int(api("/services?pageNum=1&pageSize=10")["total"]) == 0
            assert int(api("/logs?pageNum=1&pageSize=10")["total"]) == 0
            shutil.copyfile(example, inbox)
            wait_for_data()
            print("PASS: ZIP extraction, docker-only startup, legacy backup/cleanup, manual TXT import, all web ports, topology, restart, clear and reimport", flush=True)
        except Exception as error:
            if isinstance(error, subprocess.CalledProcessError):
                print(error.stdout)
            print(run("docker", "logs", "--tail", "100", "arktouros-all-in-one", check=False).stdout)
            raise
        finally:
            run("docker", "stop", "--time", "45", "arktouros-all-in-one", check=False)
            run("docker", "rm", "arktouros-all-in-one", check=False)
            run("docker", "image", "rm", legacy_id, check=False)


if __name__ == "__main__":
    main(Path(sys.argv[1]).resolve())
