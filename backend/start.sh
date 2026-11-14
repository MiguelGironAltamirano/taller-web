#!/bin/bash
set -euo pipefail

chown app:app /app/evidences
exec setpriv --reuid=10001 --regid=10001 --clear-groups java -jar /app/app.jar
