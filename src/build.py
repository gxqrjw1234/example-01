"""Simple POC build script: copy the application into dist/."""

from pathlib import Path
import shutil


SOURCE = Path("src/app.py")
DIST = Path("dist")


def main() -> None:
    if not SOURCE.is_file():
        raise FileNotFoundError(f"Missing source file: {SOURCE}")

    DIST.mkdir(exist_ok=True)
    shutil.copy2(SOURCE, DIST / "app.py")
    print(f"Built {DIST / 'app.py'}")


if __name__ == "__main__":
    main()
