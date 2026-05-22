"""从项目根目录 .env 加载环境变量（不依赖 docker-compose）。"""
from __future__ import annotations

import os
from pathlib import Path


def load_dotenv(env_path: Path | None = None, override: bool = False) -> Path | None:
    """
    解析 KEY=VALUE 行写入 os.environ。
    返回实际加载的文件路径；文件不存在则返回 None。
    """
    if env_path is None:
        root = Path(__file__).resolve().parents[2]
        env_path = root / ".env"
    if not env_path.is_file():
        return None

    for raw in env_path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        key, _, value = line.partition("=")
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if not key:
            continue
        if override or key not in os.environ:
            os.environ[key] = value
    return env_path
