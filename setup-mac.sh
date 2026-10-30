#!/usr/bin/env bash
# Sets up Study Smart AI on macOS and opens it in NetBeans, ready to run.
# Usage:  bash setup-mac.sh          (setup + open the project in NetBeans)
#         bash setup-mac.sh run      (setup + run the app directly, no NetBeans)
#         bash setup-mac.sh test     (setup + run the tests)
set -euo pipefail

cd "$(dirname "$0")"
MODE="${1:-netbeans}"

step() { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }
ok()   { printf '\033[1;32m    ✓ %s\033[0m\n' "$1"; }
fail() { printf '\033[1;31m    ✗ %s\033[0m\n' "$1"; exit 1; }

case "$MODE" in
  netbeans|run|test) ;;
  *) fail "Unknown option '$MODE' - use: bash setup-mac.sh [run|test]" ;;
esac

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
if [ -z "${OPENAI_API_KEY:-}" ] && [ "$MODE" != "test" ]; then
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
if [ "$MODE" = "test" ]; then
  step "Running tests (first run downloads Maven + libraries)"
  ./mvnw test
  exit 0
fi
if [ "$MODE" = "run" ]; then
  step "Starting the app (first run downloads Maven + libraries, may take a few minutes)"
  ./mvnw javafx:run
  exit 0
fi

# 6. NetBeans
step "Checking Apache NetBeans"
find_netbeans() {
  for app in /Applications/*NetBeans*.app "$HOME"/Applications/*NetBeans*.app; do
    [ -x "$app/Contents/Resources/netbeans/bin/netbeans" ] && { echo "$app"; return 0; }
  done
  return 1
}
if ! NB_APP=$(find_netbeans); then
  echo "    NetBeans not found - installing (this is a large download)..."
  brew install --cask netbeans
  NB_APP=$(find_netbeans) || fail "NetBeans install failed"
fi
ok "Found $(basename "$NB_APP")"

# 7. Make NetBeans' Run button use JavaFX and pass the API key.
#    nbactions.xml holds the key, so it is kept out of git (see .gitignore).
step "Configuring NetBeans Run action"
xml_key=$(printf '%s' "$OPENAI_API_KEY" | sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g')
model_prop=""
if [ -n "${OPENAI_MODEL:-}" ]; then
  model_prop="<Env.OPENAI_MODEL>${OPENAI_MODEL}</Env.OPENAI_MODEL>"
fi
cat > nbactions.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<actions>
    <action>
        <actionName>run</actionName>
        <packagings>
            <packaging>jar</packaging>
        </packagings>
        <goals>
            <goal>clean</goal>
            <goal>javafx:run</goal>
        </goals>
        <properties>
            <Env.OPENAI_API_KEY>${xml_key}</Env.OPENAI_API_KEY>
            ${model_prop}
        </properties>
    </action>
</actions>
EOF
ok "Wrote nbactions.xml (Run ▶ = clean javafx:run)"

# 8. Download dependencies now so the first Run in NetBeans is quick
step "Downloading libraries (first time may take a few minutes)"
./mvnw -q dependency:resolve test-compile && ok "Project builds"

# 9. Open the project in NetBeans
step "Opening the project in NetBeans"
"$NB_APP/Contents/Resources/netbeans/bin/netbeans" --jdkhome "$JAVA_HOME" "$PWD" >/dev/null 2>&1 &
disown
ok "NetBeans is starting - when it opens, press the green Run ▶ button (or F6)"
