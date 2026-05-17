#!/bin/sh

# shared hooks directory 세팅
git config core.hooksPath .githooks

# 실행권한 설정
chmod +x ../.githooks/pre-commit

echo "✅ Git hooks configured. pre-commit hook enabled."
