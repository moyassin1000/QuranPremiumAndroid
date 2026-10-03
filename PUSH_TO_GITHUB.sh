#!/usr/bin/env sh
set -eu
if [ "$#" -ne 1 ]; then
  echo "Usage: ./PUSH_TO_GITHUB.sh https://github.com/USERNAME/REPOSITORY.git"
  exit 1
fi
command -v git >/dev/null 2>&1 || { echo "git is required"; exit 1; }
[ -d .git ] || git init
git checkout -B main
git add .
if ! git diff --cached --quiet; then git commit -m "Initial Quran Premium Android project"; fi
git remote remove origin 2>/dev/null || true
git remote add origin "$1"
git push -u origin main
echo "Upload complete. Open GitHub Actions and run: Build Android APK"
