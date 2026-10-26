"""Assemble a clean offline installer; never overwrite a populated destination."""
import argparse
import hashlib
import json
import shutil
import subprocess
import tarfile
import time
import zipfile
from pathlib import Path


def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def package(image, output):
    repo = Path(__file__).resolve().parent.parent
    template = repo / "arktouros-image"
    with tarfile.open(image) as archive:
        manifest = json.load(archive.extractfile("manifest.json"))
        entry = next(row for row in manifest if "arktouros-all-in-one:sqlite" in (row.get("RepoTags") or []))
        config_bytes = archive.extractfile(entry["Config"]).read()
        config = json.loads(config_bytes)
        if config.get("os") != "linux" or config.get("architecture") != "amd64":
            raise ValueError("The delivery image must be linux/amd64")
        if config.get("config", {}).get("Labels", {}).get("edu.npu.arktouros.storage") != "sqlite":
            raise ValueError("The delivery image is not the SQLite build")
    if output.exists() and any(output.iterdir()):
        raise FileExistsError(f"Destination must be empty: {output}")
    output.mkdir(parents=True, exist_ok=True)
    for folder in ("arktouros-apm-api/config", "arktouros-apm-api/logs", "arktouros-apm-api/input_logs",
                   "arktouros-apm-api/db", "arktouros-ui", "arktouros-bigscreen", "examples", "nginx/logs"):
        (output / folder).mkdir(parents=True, exist_ok=True)
    paths = ["docker-compose.yaml", "start-arktouros-all-in-one.sh", "import-example.sh", "安装与排障指南.md",
             "arktouros-apm-api/config/application.yaml", "arktouros-ui/arktourosUiConfig.js",
             "arktouros-bigscreen/config.json", "examples/arktouros-demo.txt", "examples/树状调用示例说明.txt"]
    for relative in paths:
        shutil.copyfile(template / relative, output / relative)
    example = output / "examples/arktouros-demo.txt"
    records = [json.loads(line) for line in example.read_text(encoding="utf-8").splitlines() if line.strip()]
    offset = int(time.time() * 1000) - 1000 - max(row.get("timestamp", row.get("endTime", 0)) for row in records)
    for row in records:
        for field in ("timestamp", "startTime", "endTime"):
            if field in row and row[field] >= 0:
                row[field] += offset
    example.write_text("".join(json.dumps(row, ensure_ascii=False) + "\n" for row in records), encoding="utf-8", newline="\n")
    shutil.copyfile(template / "nginx.conf", output / "nginx/nginx.conf")
    for name in ("banner.txt", "logback.xml"):
        shutil.copyfile(repo / "arktouros-apm/arktouros-apm-api/src/main/resources" / name,
                        output / "arktouros-apm-api/config" / name)
    for script in output.glob("*.sh"):
        script.write_text(script.read_text(encoding="utf-8"), encoding="utf-8", newline="\n")
        script.chmod(0o755)
    destination = output / "arktouros-all-in-one.tar"
    shutil.copyfile(image, destination)
    digest = sha256(destination)
    (output / "arktouros-all-in-one.tar.sha256").write_text(f"{digest}  arktouros-all-in-one.tar\n", encoding="ascii", newline="\n")
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=repo, text=True).strip()
    dirty = bool(subprocess.check_output(["git", "status", "--porcelain", "--untracked-files=no", "--",
                                         "pom.xml", "arktouros-apm", "arktouros-protocol", "arktouros-ui",
                                         "arktouros-bigscreen", "arktouros-image"], cwd=repo, text=True).strip())
    details = dict(image="arktouros-all-in-one:sqlite", platform="linux/amd64", image_sha256=digest,
                   image_id="sha256:" + hashlib.sha256(config_bytes).hexdigest(), git_revision=revision,
                   includes_uncommitted_changes=dirty,
                   ui_revision=subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=repo / "arktouros-ui", text=True).strip(),
                   backend_package_sha256=sha256(repo / "build/arktouros-apm-api-leaves.tar.gz"),
                   example_records=2706, example_services=16, example_spans=873, example_logs=873, example_metrics=960,
                   example_traces=30, example_featured_trace="checkout-tree-29", example_sha256=sha256(example))
    (output / "release-manifest.json").write_text(json.dumps(details, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    assert len(records) == 2706
    assert not list((output / "arktouros-apm-api/db").iterdir())
    assert not list((output / "arktouros-apm-api/input_logs").iterdir())
    print(json.dumps(details, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--image-tar", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    package(args.image_tar.resolve(), args.output.resolve())
    archive_path = args.output.resolve().with_suffix(".zip")
    if archive_path.exists():
        raise FileExistsError(archive_path)
    with zipfile.ZipFile(archive_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=1) as archive:
        for path in sorted(args.output.resolve().rglob("*")):
            archive.write(path, Path("install-arktouros-image") / path.relative_to(args.output.resolve()))
    print(f"ZIP: {archive_path}")
