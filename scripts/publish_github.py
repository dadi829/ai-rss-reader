#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
一键发布脚本：创建 GitHub 仓库 -> 推送 main 与 tag -> 等 Actions 出包 -> 下载 APK -> 建 Release 上传 APK。

用法（在项目根目录）：
    set GITHUB_TOKEN=ghp_xxx          # Windows CMD
    export GITHUB_TOKEN=ghp_xxx       # bash
    python scripts/publish_github.py

    # 只想推代码，不等 CI 出包：
    python scripts/publish_github.py --skip-wait

    # 自定义仓库名 / 私有仓库：
    python scripts/publish_github.py --repo my-repo --private

Token 需要 classic PAT 的 repo + workflow 权限，或 fine-grained PAT 勾选
Administration(Read/Write)、Contents(Read/Write)、Workflows(Read/Write)、Actions(Read)。

只依赖标准库，不需要 gh CLI 或 jq。
"""
from __future__ import annotations

import argparse
import io
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
import zipfile
from pathlib import Path

API = "https://api.github.com"
ROOT = Path(__file__).resolve().parent.parent
TAG = "v1.0.0"
APP_NAME = "AI 资讯 RSS 阅读器"

DEFAULT_REPO = "ai-rss-reader"
DEFAULT_DESCRIPTION = "AI 资讯 RSS 阅读器 · Kotlin + Jetpack Compose 课程设计"


def call(method: str, path: str, token: str, payload=None, accept: str | None = None):
    url = path if path.startswith("http") else API + path
    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    request = urllib.request.Request(url, data=data, method=method)
    request.add_header("Authorization", f"Bearer {token}")
    request.add_header("Accept", accept or "application/vnd.github+json")
    request.add_header("X-GitHub-Api-Version", "2022-11-28")
    if data is not None:
        request.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            body = response.read()
            return response.status, json.loads(body) if body else {}
    except urllib.error.HTTPError as exc:
        body = exc.read()
        try:
            parsed = json.loads(body) if body else {}
        except json.JSONDecodeError:
            parsed = {"raw": body[:400].decode("utf-8", "replace")}
        return exc.code, parsed


def run_git(*args: str) -> None:
    result = subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True)
    if result.returncode != 0:
        sys.exit(f"git {' '.join(args)} 失败：\n{result.stderr.strip()}")


def ensure_repo(token: str, owner: str, name: str, private: bool, description: str) -> str:
    status, body = call("POST", "/user/repos", token, {
        "name": name,
        "description": description,
        "private": private,
        "has_issues": True,
        "has_wiki": False,
        "auto_init": False,
    })
    if status == 201:
        print(f"[1/5] 仓库已创建：{body['html_url']}")
        return body["html_url"]
    if status == 422:
        print(f"[1/5] 仓库 {owner}/{name} 已存在，直接复用")
        return f"https://github.com/{owner}/{name}"
    sys.exit(f"创建仓库失败 {status}: {body}")


def push(token: str, owner: str, name: str) -> None:
    remote = f"https://x-access-token:{token}@github.com/{owner}/{name}.git"
    print("[2/5] 推送 main 与 tag ...")
    # --force 让脚本可重复执行；origin 最后会重置成不带 token 的地址，避免凭据落盘
    run_git("push", "--quiet", remote, "main", "--tags", "--force")
    subprocess.run(["git", "remote", "remove", "origin"], cwd=ROOT, capture_output=True)
    run_git("remote", "add", "origin", f"https://github.com/{owner}/{name}.git")
    print("      推送完成（origin 已设为无凭据地址）")


def wait_for_run(token: str, owner: str, name: str, timeout_min: int = 20) -> dict:
    print("[3/5] 等待 GitHub Actions 构建 ...")
    deadline = time.time() + timeout_min * 60
    while time.time() < deadline:
        status, body = call("GET", f"/repos/{owner}/{name}/actions/runs?per_page=5", token)
        if status == 200:
            runs = [r for r in body.get("workflow_runs", []) if r.get("head_branch") in (None, "main")]
            if runs:
                latest = max(runs, key=lambda r: r["created_at"])
                if latest["status"] == "completed":
                    print(f"      结论：{latest['conclusion']}  {latest['html_url']}")
                    return latest
                print(f"      运行中：{latest['status']} ({latest['html_url']})")
        time.sleep(20)
    sys.exit(f"等待超时，请到 Actions 页面查看：https://github.com/{owner}/{name}/actions")


def dump_failure_logs(token: str, owner: str, name: str, run_id: int) -> None:
    status, body = call("GET", f"/repos/{owner}/{name}/actions/runs/{run_id}/jobs", token)
    if status != 200:
        return
    for job in body.get("jobs", []):
        for step in job.get("steps", []):
            if step.get("conclusion") == "failure":
                print(f"      失败步骤：{step['name']}")
    job_id = body["jobs"][0]["id"] if body.get("jobs") else None
    if job_id is None:
        return
    url = f"{API}/repos/{owner}/{name}/actions/jobs/{job_id}/logs"
    request = urllib.request.Request(url)
    request.add_header("Authorization", f"Bearer {token}")
    request.add_header("Accept", "application/vnd.github+json")
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            text = response.read().decode("utf-8", "replace")
    except urllib.error.HTTPError:
        print("      日志需要登录才能下载，请打开 Actions 页面查看")
        return
    lines = [line for line in text.splitlines() if "error:" in line or "FAILURE" in line]
    print("      ---- 编译错误摘要 ----")
    for line in lines[:40]:
        print("      " + line[:220])


def download_apk(token: str, owner: str, name: str, run_id: int) -> Path | None:
    status, body = call("GET", f"/repos/{owner}/{name}/actions/runs/{run_id}/artifacts", token)
    if status != 200 or not body.get("artifacts"):
        print("      没有找到构建产物")
        return None
    artifact = body["artifacts"][0]
    print(f"[4/5] 下载产物 {artifact['name']} ({artifact['size_in_bytes'] // 1024} KB)")
    request = urllib.request.Request(artifact["archive_download_url"])
    request.add_header("Authorization", f"Bearer {token}")
    request.add_header("Accept", "application/vnd.github+json")
    with urllib.request.urlopen(request, timeout=180) as response:
        blob = response.read()
    out_dir = ROOT / "dist"
    out_dir.mkdir(exist_ok=True)
    with zipfile.ZipFile(io.BytesIO(blob)) as archive:
        for info in archive.infolist():
            if info.filename.endswith(".apk"):
                target = out_dir / Path(info.filename).name
                target.write_bytes(archive.read(info))
                print(f"      APK 已保存：{target}")
                return target
    return None


def create_release(token: str, owner: str, name: str, apk: Path) -> None:
    print("[5/5] 创建 Release 并上传 APK ...")
    status, body = call("POST", f"/repos/{owner}/{name}/releases", token, {
        "tag_name": TAG,
        "name": f"{APP_NAME} {TAG}",
        "body": (
            "课程设计构建产物（debug 签名，可直接安装）。\n\n"
            "首次启动会自动写入 6 个预置 AI 方向 RSS 源，"
            "所有网络请求均由用户手动触发，无后台刷新与推送。"
        ),
        "draft": False,
        "prerelease": False,
    })
    if status not in (200, 201):
        print(f"      Release 创建失败 {status}: {body}")
        return
    upload_url = body["upload_url"].split("{")[0]
    data = apk.read_bytes()
    request = urllib.request.Request(
        f"{upload_url}?name={apk.name}&label=debug-apk", data=data, method="POST",
    )
    request.add_header("Authorization", f"Bearer {token}")
    request.add_header("Content-Type", "application/vnd.android.package-archive")
    try:
        with urllib.request.urlopen(request, timeout=300) as response:
            asset = json.loads(response.read())
        print(f"      下载地址：{asset['browser_download_url']}")
        print(f"      Release 页面：{body['html_url']}")
    except urllib.error.HTTPError as exc:
        print(f"      上传失败 {exc.code}: {exc.read()[:300]}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", default=DEFAULT_REPO, help="仓库名")
    parser.add_argument("--private", action="store_true", help="创建私有仓库")
    parser.add_argument("--description", default=DEFAULT_DESCRIPTION)
    parser.add_argument("--skip-wait", action="store_true", help="只推送，不等待构建")
    args = parser.parse_args()

    token = os.environ.get("GITHUB_TOKEN", "").strip()
    if not token:
        sys.exit("请先设置环境变量 GITHUB_TOKEN（GitHub PAT）")

    status, me = call("GET", "/user", token)
    if status != 200:
        sys.exit(f"Token 无效：{status} {me}")
    owner = me["login"]
    print(f"账号：{owner}")

    ensure_repo(token, owner, args.repo, args.private, args.description)
    push(token, owner, args.repo)
    if args.skip_wait:
        return

    run = wait_for_run(token, owner, args.repo)
    if run.get("conclusion") != "success":
        dump_failure_logs(token, owner, args.repo, run["id"])
        sys.exit("构建失败，先修代码再重跑")

    apk = download_apk(token, owner, args.repo, run["id"])
    if apk is None:
        sys.exit("构建成功但未取到 APK")
    create_release(token, owner, args.repo, apk)


if __name__ == "__main__":
    main()
