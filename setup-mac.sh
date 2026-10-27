#!/usr/bin/env bash
# Sets up and runs Study Smart AI on macOS.
# Usage:  bash setup-mac.sh          (setup + run the app)
#         bash setup-mac.sh test     (setup + run the tests)
set -euo pipefail

cd "$(dirname "$0")"

step() { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }
ok()   { printf '\033[1;32m    ✓ %s\033[0m\n' "$1"; }
fail() { printf '\033[1;31m    ✗ %s\033[0m\n' "$1"; exit 1; }

# 1. Homebrew
step "Checking Homebrew"
if ! command -v brew >/dev/null 2>&1; then
  for b in /opt/homebrew/bin/brew /usr/local/bin/brew; do
    [ -x "$b" ] && eval "$("$b" shellenv)" && break
  done
fi
if ! command -v brew >/dev/null 2>&1; then
  echo "    Homebrew not found - installing (it may ask for your Mac password)..."
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  for b in /opt/homebrew/bin/brew /usr/local/bin/brew; do
    [ -x "$b" ] && eval "$("$b" shellenv)" && break
  done
  command -v brew >/dev/null 2>&1 || fail "Homebrew install failed"
fi
ok "Homebrew ready"

# 2. Java 17+
step "Checking Java 17+"
if ! JAVA_HOME_FOUND=$(/usr/libexec/java_home -v 17+ 2>/dev/null); then
  echo "    Java 17 not found - installing Temurin 17..."
  brew install --cask temurin@17
  JAVA_HOME_FOUND=$(/usr/libexec/java_home -v 17+ 2>/dev/null) || fail "Java install failed"
fi
export JAVA_HOME="$JAVA_HOME_FOUND"
export PATH="$JAVA_HOME/bin:$PATH"
ok "Using $(java -version 2>&1 | head -n 1)"

# 3. Maven wrapper (fix Windows line endings + make executable)
step "Preparing Maven wrapper"
[ -f mvnw ] || fail "mvnw not found - run this script from the project folder"
if grep -q $'\r' mvnw; then
  sed -i '' $'s/\r$//' mvnw
  ok "Converted mvnw to Unix line endings"
fi
chmod +x mvnw
ok "mvnw is executable"

# 4. OpenAI API key
step "Checking OPENAI_API_KEY"
if [ -z "${OPENAI_API_KEY:-}" ] && [ "${1:-}" != "test" ]; then
  read -r -s -p "    Paste your OpenAI API key (sk-...): " OPENAI_API_KEY; echo
  [ -n "$OPENAI_API_KEY" ] || fail "No API key entered"
  export OPENAI_API_KEY
  read -r -p "    Save it to ~/.zshrc so you don't have to enter it again? [y/N] " save
  if [[ "$save" =~ ^[Yy]$ ]]; then
    printf '\nexport OPENAI_API_KEY=%q\n' "$OPENAI_API_KEY" >> ~/.zshrc
    ok "Saved to ~/.zshrc"
  fi
fi
ok "API key set"

# 5. Run
if [ "${1:-}" = "test" ]; then
  step "Running tests (first run downloads Maven + libraries)"
  ./mvnw test
else
  step "Starting the app (first run downloads Maven + libraries, may take a few minutes)"
  ./mvnw javafx:run
fi
