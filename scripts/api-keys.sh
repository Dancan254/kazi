#!/bin/bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -f "$root/.env" ]]; then
  set -a
  source "$root/.env"
  set +a
fi

region="${AWS_REGION:-eu-west-1}"
environment="${KAZI_ENVIRONMENT:-dev}"
base_url="${KAZI_ADMIN_URL:-http://localhost:8080}"
admin_key="$(aws ssm get-parameter --region "$region" --name "/kazi/$environment/mcp-api-key" --with-decryption --query Parameter.Value --output text)"

request() {
  local method="$1"
  local path="$2"
  local body="${3:-}"
  local args=(--fail-with-body --silent --show-error --request "$method" --header "X-API-Key: $admin_key")
  if [[ -n "$body" ]]; then
    args+=(--header "Content-Type: application/json" --data "$body")
  fi
  curl "${args[@]}" "$base_url$path"
}

case "${1:-}" in
  create)
    label="${2:?Owner label is required}"
    email="${3:-}"
    days="${4:-90}"
    body="$(python3 -c 'import json,sys; print(json.dumps({"ownerLabel":sys.argv[1],"ownerEmail":sys.argv[2] or None,"expiresInDays":int(sys.argv[3])}))' "$label" "$email" "$days")"
    request POST /api/admin/api-keys "$body"
    ;;
  list)
    request GET /api/admin/api-keys
    ;;
  revoke)
    id="${2:?API key ID is required}"
    request DELETE "/api/admin/api-keys/$id"
    ;;
  rotate)
    id="${2:?API key ID is required}"
    days="${3:-90}"
    request POST "/api/admin/api-keys/$id/rotation" "{\"expiresInDays\":$days}"
    ;;
  *)
    echo "Usage: $0 create <owner-label> [email] [days] | list | revoke <id> | rotate <id> [days]" >&2
    exit 2
    ;;
esac
