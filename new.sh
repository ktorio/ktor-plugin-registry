#!/bin/sh
set -eu

die() {
  printf '%s\n' "Error: $*" >&2
  exit 1
}

trim() {
  # shellcheck disable=SC2001
  printf '%s' "$1" | sed 's/^[[:space:]]*//; s/[[:space:]]*$//'
}

to_kebab_case() {
  printf '%s' "$1" \
    | tr '[:upper:]' '[:lower:]' \
    | sed -E 's/[^a-z0-9]+/-/g; s/^-+//; s/-+$//; s/-+/-/g'
}

prompt_with_default() {
  label="$1"
  default_value="$2"
  example="$3"

  if [ -n "$example" ]; then
    printf '%s [%s] (e.g. %s): ' "$label" "$default_value" "$example" >&2
  else
    printf '%s [%s]: ' "$label" "$default_value" >&2
  fi

  IFS= read -r answer || true
  answer="$(trim "$answer")"
  if [ -z "$answer" ]; then
    printf '%s' "$default_value"
  else
    printf '%s' "$answer"
  fi
}

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$script_dir

templates_dir="$repo_root/templates"
repository_dir="$repo_root/repository"

[ -d "$templates_dir" ] || die "Missing templates directory: $templates_dir"
[ -d "$repository_dir" ] || die "Missing repository directory: $repository_dir"

printf '%s\n' "Create a new plugin manifest structure"
printf '%s\n' "Press Enter to accept a default."

group_default="com.example"
group=$(prompt_with_default "Group" "$group_default" "io.github.mycompany")

plugin_name_default="My Plugin"
plugin_name=$(prompt_with_default "Plugin name" "$plugin_name_default" "Koin, Rate Limiting, Swagger UI")

plugin_id_default=$(to_kebab_case "$plugin_name")
[ -n "$plugin_id_default" ] || plugin_id_default="my-plugin"
plugin_id=$(prompt_with_default "Plugin ID" "$plugin_id_default" "lowercase-kebab-case, e.g. my-plugin")

plugin_type_default="server"
printf 'Plugin type [%s] (server/client): ' "$plugin_type_default" >&2
IFS= read -r plugin_type_input || true
plugin_type_input="$(trim "$plugin_type_input")"
if [ -z "$plugin_type_input" ]; then
  plugin_type="$plugin_type_default"
else
  plugin_type=$(printf '%s' "$plugin_type_input" | tr '[:upper:]' '[:lower:]')
fi

case "$plugin_type" in
  server|client) ;;
  *) die "Plugin type must be 'server' or 'client'." ;;
esac

group_path=$(printf '%s' "$group")
plugin_dir="$repository_dir/$group_path/$plugin_id"
module_dir="$plugin_dir/$plugin_type"

[ ! -e "$plugin_dir" ] || die "Plugin already exists: $plugin_dir"

mkdir -p "$module_dir/src"

# group.ksl.yaml
group_file="$repository_dir/$group_path/group.ksl.yaml"
if [ ! -e "$group_file" ]; then
  mkdir -p "$(dirname "$group_file")"
  cat > "$group_file" <<EOF
name: $group
url: https://example.com/
email: <email@example.com>
icon: icon.svg
EOF
fi

# pack.ksl.yaml
pack_template="$templates_dir/pack.ksl.yaml"
pack_file="$plugin_dir/pack.ksl.yaml"
if [ -s "$pack_template" ]; then
  sed \
    -e "s/{{PLUGIN_NAME}}/$plugin_name/g" \
    -e "s/{{PLUGIN_ID}}/$plugin_id/g" \
    -e "s/{{PLUGIN_TYPE}}/$plugin_type/g" \
    -e "s/{{GROUP}}/$group/g" \
    "$pack_template" > "$pack_file"
else
  cat > "$pack_file" <<EOF
name: $plugin_name
description: TODO: describe the plugin
tags:
  - $plugin_type
  - TODO
EOF
fi

# module.ksl.yaml
module_template="$templates_dir/module.ksl.yaml"
module_file="$module_dir/module.ksl.yaml"
if [ -s "$module_template" ]; then
  sed \
    -e "s/{{PLUGIN_NAME}}/$plugin_name/g" \
    -e "s/{{PLUGIN_ID}}/$plugin_id/g" \
    -e "s/{{PLUGIN_TYPE}}/$plugin_type/g" \
    -e "s/{{GROUP}}/$group/g" \
    "$module_template" > "$module_file"
else
  cat > "$module_file" <<EOF
platform: jvm
dependencies: []
EOF
fi

# project.ksl.yaml
project_file="project.ksl.yaml"
cat > "$project_file" <<EOF
name: test-$plugin_id
group: $group
packs:
  - org.gradle/gradle
  - io.ktor/server-core
  - io.ktor/server-netty
  - $group/$plugin_id
EOF

# Optional source file example
plugin_class_name=$(printf '%s' "$plugin_name" | sed -E 's/[^[:alnum:]]+//g')
if [ -z "$plugin_class_name" ]; then
  plugin_class_name="Plugin"
fi

cat > "$module_dir/src/${plugin_class_name}.kt" <<EOF
package kastle

import io.ktor.server.application.*

fun Application.install$plugin_class_name() {
    // install your plugin here
}
EOF

printf '%s\n' "Created plugin at: $plugin_dir"
printf '%s\n' "Next steps:"
printf '%s\n' "  - Fill in $plugin_dir/pack.ksl.yaml"
printf '%s\n' "  - Fill in $module_dir/module.ksl.yaml"
printf '%s\n' "  - Fill in $project_file"
printf '%s\n' "  - Replace the example source under $module_dir/src/"
