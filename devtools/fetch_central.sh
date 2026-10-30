#!/bin/bash
# Fetch Central artifacts that shared-IP 429 blocks, into the local filler maven.
# Usage: fetch_central.sh  (reads artifact list below)
set -u
OUT=/home/z/my-project/tools/localmaven
ARTIFACTS=(
  "org/scala-lang/scala-xml_2.11/1.0.2|pom|jar"
  "org/scala-lang/scala-parser-combinators_2.11/1.0.1|pom|jar"
  "org/scala-lang/scala-swing_2.11/1.0.1|pom|jar"
  "lzma/lzma/0.0.1|pom|jar"
)
BASES=(
  "https://repo.maven.apache.org/maven2"
  "https://repo.spongepowered.org/maven"
)

fetch() {
  local url="$1" dest="$2"
  [ -s "$dest" ] && return 0
  for attempt in 1 2 3 4 5 6; do
    code=$(curl -sL -o "$dest" -w "%{http_code}" --max-time 60 "$url")
    if [ "$code" = "200" ]; then
      echo "OK  $url"
      return 0
    fi
    echo "  try$attempt $code $url"
    rm -f "$dest"
    sleep $((attempt * 15))
  done
  echo "FAIL $url"
  return 1
}

rc=0
for entry in "${ARTIFACTS[@]}"; do
  path="${entry%%|*}"; IFS='|' read -ra exts <<< "${entry#*|}"
  name=$(basename "$path"); dir=$(dirname "$path")
  for ext in "${exts[@]}"; do
    mkdir -p "$OUT/$dir"
    ok=0
    for base in "${BASES[@]}"; do
      if fetch "$base/$dir/$name.$ext" "$OUT/$dir/$name.$ext"; then ok=1; break; fi
    done
    [ $ok -eq 0 ] && rc=1
  done
done
exit $rc
