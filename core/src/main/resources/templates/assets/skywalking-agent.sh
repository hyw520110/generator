#!/bin/bash

# SkyWalking Java Agent discovery, download, and per-application preparation.
# This file is sourced by start.sh and relies on start.sh variables/log helpers.

extract_skywalking_major_version() {
     local filename=$1
     local version=$(basename "$filename" .jar | sed 's/.*-\([0-9]\+\)\..*/\1/')
     echo "$version" 2>/dev/null || echo ""
}

get_project_skywalking_version() {
    local pom_file=""
    local candidate
    for candidate in "${CURRENT_DIR}/pom.xml" "${BASE_DIR}/pom.xml"; do
        if [ -f "$candidate" ]; then
            pom_file="$candidate"
            break
        fi
    done

    if [ -n "$pom_file" ]; then
        sed -n 's#.*<skywalking.version>\([^<]*\)</skywalking.version>.*#\1#p' "$pom_file" | head -n 1 | tr -d '[:space:]'
    fi
}

resolve_latest_skywalking_agent_version() {
    local index_urls=()
    if [ -n "${SKYWALKING_AGENT_VERSION_INDEX_URL:-}" ]; then
        index_urls+=("$SKYWALKING_AGENT_VERSION_INDEX_URL")
    else
        index_urls+=(
            "https://archive.apache.org/dist/skywalking/java-agent/"
            "https://downloads.apache.org/skywalking/java-agent/"
            "https://repo1.maven.org/maven2/org/apache/skywalking/apache-skywalking-java-agent/maven-metadata.xml"
        )
    fi

    local index_url=""
    local index=""
    local latest_version=""

    for index_url in "${index_urls[@]}"; do
        if command -v curl >/dev/null 2>&1; then
            index=$(curl -fsSL --connect-timeout 5 --max-time 15 "$index_url" 2>/dev/null || true)
        elif command -v wget >/dev/null 2>&1; then
            index=$(wget -qO- --timeout=15 "$index_url" 2>/dev/null || true)
        else
            return 1
        fi

        latest_version=$(echo "$index" | sed -n 's#.*<release>\([^<]*\)</release>.*#\1#p' | head -n 1 | tr -d '[:space:]')
        if [ -z "$latest_version" ]; then
            latest_version=$(echo "$index" \
                | sed -n 's#.*href="\([0-9][0-9.]*\)/".*#\1#p' \
                | awk -F. '/^[0-9]+(\.[0-9]+)*$/ { printf "%05d.%05d.%05d %s\n", $1, $2, $3, $0 }' \
                | sort \
                | tail -n 1 \
                | awk '{print $2}')
        fi

        if [ -n "$latest_version" ]; then
            echo "$latest_version"
            return 0
        fi
    done

    return 1
}

resolve_skywalking_agent_version() {
    local configured_version="${SKYWALKING_AGENT_VERSION:-}"
    local project_version=""

    if [ "$configured_version" = "latest" ]; then
        configured_version=$(resolve_latest_skywalking_agent_version)
        if [ -z "$configured_version" ]; then
            log_warn "无法解析 SkyWalking Java Agent 最新版本，回退到项目版本或默认版本"
        fi
    fi

    if [ -z "$configured_version" ]; then
        project_version=$(get_project_skywalking_version)
        configured_version="${project_version:-9.4.0}"
    fi

    echo "$configured_version"
}

get_skywalking_agent_archive_name() {
    local version="$1"
    echo "apache-skywalking-java-agent-${version}.tgz"
}

get_skywalking_agent_download_dir() {
    echo "${SKYWALKING_AGENT_DOWNLOAD_DIR:-$HOME/Downloads}"
}

is_valid_skywalking_agent_archive() {
    local archive_file="$1"
    local archive_list=""

    [ -s "$archive_file" ] || return 1
    archive_list=$(tar -tzf "$archive_file" 2>/dev/null) || return 1
    printf '%s\n' "$archive_list" | grep -q 'skywalking-agent/skywalking-agent.jar$'
}

find_skywalking_agent_archive() {
    local version="$1"
    local archive_name
    archive_name=$(get_skywalking_agent_archive_name "$version")
    local download_dir
    download_dir=$(get_skywalking_agent_download_dir)
    local agent_root="${SKYWALKING_AGENT_HOME:-$HOME/.skywalking/agents}"
    local download_archive="$download_dir/$archive_name"
    local candidate=""

    local candidates=(
        "$download_archive"
        "$agent_root/$archive_name"
    )

    for candidate in "${candidates[@]}"; do
        if is_valid_skywalking_agent_archive "$candidate"; then
            if [ "$candidate" != "$download_archive" ]; then
                mkdir -p "$download_dir" 2>/dev/null || true
                cp "$candidate" "$download_archive" 2>/dev/null || true
            fi
            echo "$candidate"
            return 0
        fi
    done

    return 1
}

find_adl_downloader() {
    local candidate=""
    local candidates=()

    [ -n "${SKYWALKING_AGENT_ADL:-}" ] && candidates+=("$SKYWALKING_AGENT_ADL")
    [ -n "${ADL_BIN:-}" ] && candidates+=("$ADL_BIN")

    if command -v adl >/dev/null 2>&1; then
        candidates+=("$(command -v adl)")
    fi

    for candidate in "${candidates[@]}"; do
        if [ -n "$candidate" ] && [ -x "$candidate" ]; then
            echo "$candidate"
            return 0
        fi
    done

    return 1
}

download_skywalking_archive() {
    local archive_file="$1"
    shift
    local download_urls=("$@")
    local download_dir
    download_dir=$(dirname "$archive_file")
    local archive_name
    archive_name=$(basename "$archive_file")
    local adl_bin=""
    local primary_url="${download_urls[0]:-}"
    local mirrors=("${download_urls[@]:1}")
    local download_url=""

    if [ -z "$archive_file" ] || [ -z "$archive_name" ] || [ "$archive_name" = "." ] || [ -z "$primary_url" ]; then
        log_warn "SkyWalking Java Agent 下载参数异常，跳过自动下载: archive_file=$archive_file primary_url=$primary_url"
        return 1
    fi

    mkdir -p "$download_dir"

    if adl_bin=$(find_adl_downloader); then
        log_info "使用 adl 下载 SkyWalking Java Agent: $adl_bin"
        if ADL_MAX_RETRIES_SMALL="${SKYWALKING_AGENT_ADL_MAX_RETRIES:-1}" \
            ADL_MAX_RETRIES_LARGE="${SKYWALKING_AGENT_ADL_MAX_RETRIES:-1}" \
            "$adl_bin" "$primary_url" "$download_dir" "$archive_name" "${mirrors[@]}"; then
            return 0
        fi
        log_warn "adl 下载失败，回退到 curl/wget"
    fi

    for download_url in "${download_urls[@]}"; do
        log_info "下载地址: $download_url"
        rm -f "$archive_file"

        if command -v curl >/dev/null 2>&1; then
            if ! curl -fL --connect-timeout 10 --max-time 300 -o "$archive_file" "$download_url"; then
                log_warn "SkyWalking Java Agent 下载失败，尝试下一个地址"
                continue
            fi
        elif command -v wget >/dev/null 2>&1; then
            if ! wget -O "$archive_file" "$download_url"; then
                log_warn "SkyWalking Java Agent 下载失败，尝试下一个地址"
                continue
            fi
        else
            log_warn "未安装 curl、wget 或 adl，无法自动下载 SkyWalking Java Agent"
            return 1
        fi

        return 0
    done

    return 1
}

find_skywalking_agent_jar() {
    local version="$1"
    local explicit_agent_jar="${SKYWALKING_AGENT_JAR:-}"
    local agent_root="${SKYWALKING_AGENT_HOME:-$HOME/.skywalking/agents}"
    local cache_file="${SKYWALKING_AGENT_CACHE_FILE:-$agent_root/.skywalking-agent-${version}.path}"
    local agent_jar=""

    if [ -n "$explicit_agent_jar" ]; then
        if [ -f "$explicit_agent_jar" ]; then
            echo "$explicit_agent_jar"
            return 0
        fi
        log_warn "SKYWALKING_AGENT_JAR 指定的文件不存在: $explicit_agent_jar"
    fi

    if [ -f "$cache_file" ]; then
        agent_jar=$(cat "$cache_file" 2>/dev/null || true)
        if [ -f "$agent_jar" ]; then
            echo "$agent_jar"
            return 0
        fi
    fi

    local search_dirs=("$agent_root")
    if [ -n "${SKYWALKING_AGENT_SEARCH_DIRS:-}" ]; then
        local extra_dir=""
        local extra_dirs=()
        IFS=':' read -r -a extra_dirs <<< "$SKYWALKING_AGENT_SEARCH_DIRS"
        for extra_dir in "${extra_dirs[@]}"; do
            [ -n "$extra_dir" ] && search_dirs+=("$extra_dir")
        done
    fi

    for dir in "${search_dirs[@]}"; do
        if [ -d "$dir" ]; then
            agent_jar=$(find "$dir" -maxdepth 6 -path '*/skywalking-agent/skywalking-agent.jar' -type f -print 2>/dev/null | head -n 1)
            if [ -z "$agent_jar" ]; then
                agent_jar=$(find "$dir" -maxdepth 4 -name 'skywalking-agent.jar' -type f -print 2>/dev/null | head -n 1)
            fi
            if [ -n "$agent_jar" ]; then
                mkdir -p "$(dirname "$cache_file")" 2>/dev/null || true
                printf '%s\n' "$agent_jar" > "$cache_file" 2>/dev/null || true
                echo "$agent_jar"
                return 0
            fi
        fi
    done

    return 1
}

upsert_skywalking_agent_config() {
    local key="$1"
    local value="$2"
    local file="$3"

    if grep -q "^${key}=" "$file"; then
        sed_file "^${key}=.*" "${key}=${value}" "$file"
    else
        printf '%s=%s\n' "$key" "$value" >> "$file"
    fi
}

download_skywalking_agent() {
    local version="$1"
    local agent_root="${SKYWALKING_AGENT_HOME:-$HOME/.skywalking/agents}"
    local install_dir="$agent_root/apache-skywalking-java-agent-$version"
    local agent_jar="$install_dir/skywalking-agent/skywalking-agent.jar"
    local expected_agent_jar="$agent_jar"
    local archive_name
    archive_name=$(get_skywalking_agent_archive_name "$version")
    local download_dir
    download_dir=$(get_skywalking_agent_download_dir)
    local archive_file="$download_dir/$archive_name"

    if [ -f "$agent_jar" ]; then
        echo "$agent_jar"
        return 0
    fi

    mkdir -p "$agent_root"
    mkdir -p "$download_dir"

    local download_urls=()
    if [ -n "${SKYWALKING_AGENT_DOWNLOAD_URL:-}" ]; then
        download_urls+=("$SKYWALKING_AGENT_DOWNLOAD_URL")
    else
        download_urls+=(
            "https://archive.apache.org/dist/skywalking/java-agent/$version/apache-skywalking-java-agent-$version.tgz"
            "https://downloads.apache.org/skywalking/java-agent/$version/apache-skywalking-java-agent-$version.tgz"
            "https://repo1.maven.org/maven2/org/apache/skywalking/apache-skywalking-java-agent/$version/apache-skywalking-java-agent-$version.tgz"
        )
    fi

    local cached_archive=""
    if cached_archive=$(find_skywalking_agent_archive "$version"); then
        archive_file="$cached_archive"
        log_info "复用已下载的 SkyWalking Java Agent 安装包: $archive_file"
    else
        if [ "${SKYWALKING_AGENT_AUTO_DOWNLOAD:-true}" != "true" ]; then
            log_warn "未找到完整的本地 SkyWalking Java Agent 安装包，且自动下载已禁用"
            return 1
        fi

        if [ -f "$archive_file" ]; then
            log_warn "发现无效或未下载完整的 SkyWalking Java Agent 安装包，尝试断点续传: $archive_file"
        fi

        log_info "未发现 SkyWalking Java Agent，开始下载版本 $version"
        if ! download_skywalking_archive "$archive_file" "${download_urls[@]}"; then
            log_warn "SkyWalking Java Agent 下载失败"
            return 1
        fi

        if ! is_valid_skywalking_agent_archive "$archive_file"; then
            log_warn "SkyWalking Java Agent 安装包校验失败: $archive_file"
            rm -f "$archive_file"
            return 1
        fi
    fi

    rm -rf "$install_dir"
    mkdir -p "$install_dir"
    if ! tar -xzf "$archive_file" -C "$install_dir"; then
        log_warn "SkyWalking Java Agent 解压失败: $archive_file"
        rm -rf "$install_dir"
        return 1
    fi

    if [ ! -f "$agent_jar" ]; then
        agent_jar=$(find "$install_dir" -maxdepth 6 -path '*/skywalking-agent/skywalking-agent.jar' -type f -print 2>/dev/null | head -n 1)
        if [ -z "$agent_jar" ]; then
            agent_jar=$(find "$install_dir" -maxdepth 4 -name 'skywalking-agent.jar' -type f -print 2>/dev/null | head -n 1)
        fi
    fi

    if [ -f "$agent_jar" ]; then
        printf '%s\n' "$agent_jar" > "${SKYWALKING_AGENT_CACHE_FILE:-$agent_root/.skywalking-agent-${version}.path}" 2>/dev/null || true
        echo "$agent_jar"
        return 0
    fi

    log_warn "SkyWalking Java Agent 下载或解压后未找到: ${agent_jar:-$expected_agent_jar}"
    return 1
}

prepare_app_skywalking_agent() {
    local source_agent_jar="$1"
    local agent_root="${SKYWALKING_AGENT_HOME:-$HOME/.skywalking/agents}"
    local app_agent_dir="${SKYWALKING_APP_AGENT_DIR:-$agent_root/apps/$APP_NAME}"
    local source_agent_dir
    source_agent_dir=$(dirname "$source_agent_jar")

    if [ ! -f "$app_agent_dir/skywalking-agent.jar" ]; then
        rm -rf "$app_agent_dir"
        mkdir -p "$(dirname "$app_agent_dir")"
        cp -R "$source_agent_dir" "$app_agent_dir"
    fi

    if [ -d "$BASE_DIR/lib" ]; then
        local gateway_jars=""
        local webflux_jars=""
        local gateway_major_version=""
        local webflux_major_version=""
        local gateway_plugin=""
        local webflux_plugin=""

        gateway_jars=$(find "$BASE_DIR/lib" -name "spring-cloud-gateway-server-*.jar" -type f | head -n 1)
        webflux_jars=$(find "$BASE_DIR/lib" -name "spring-webflux-*.jar" -type f | head -n 1)
        if [ -n "$gateway_jars" ] || [ -n "$webflux_jars" ]; then
            gateway_major_version=$(extract_skywalking_major_version "$gateway_jars")
            webflux_major_version=$(extract_skywalking_major_version "$webflux_jars")
            if [ -n "$gateway_major_version" ]; then
                gateway_plugin=$(find "$app_agent_dir/optional-plugins" -name "apm-spring-cloud-gateway-${gateway_major_version}.x-plugin-*.jar" -type f 2>/dev/null | head -n 1)
            fi
            if [ -n "$webflux_major_version" ]; then
                webflux_plugin=$(find "$app_agent_dir/optional-plugins" -name "apm-spring-webflux-${webflux_major_version}.x-plugin-*.jar" -type f 2>/dev/null | head -n 1)
            fi
            log_info "检测到当前应用为 Spring Cloud Gateway/WebFlux 类型，安装对应插件: $gateway_plugin $webflux_plugin"
            [ -n "$gateway_plugin" ] && cp "$gateway_plugin" "$app_agent_dir/plugins/"
            [ -n "$webflux_plugin" ] && cp "$webflux_plugin" "$app_agent_dir/plugins/"
        fi
    fi

    mkdir -p "$app_agent_dir/config"
    if [ ! -f "$app_agent_dir/config/agent.config" ]; then
        touch "$app_agent_dir/config/agent.config"
    fi
    upsert_skywalking_agent_config "agent.service_name" "\$\{SW_AGENT_NAME:$APP_NAME\}" "$app_agent_dir/config/agent.config"
    upsert_skywalking_agent_config "logging.level" "\$\{SW_LOGGING_LEVEL:${SKYWALKING_AGENT_LOG_LEVEL:-WARN}\}" "$app_agent_dir/config/agent.config"
    upsert_skywalking_agent_config "logging.output" "\$\{SW_LOGGING_OUTPUT:${SKYWALKING_AGENT_LOG_OUTPUT:-FILE}\}" "$app_agent_dir/config/agent.config"

    echo "$app_agent_dir/skywalking-agent.jar"
}

configure_skywalking_jvm_opts() {
    local jvm_opts="$1"
    local skywalking_version=""
    local skywalking_agent_jar=""
    local app_skywalking_agent_jar=""

    skywalking_version=$(resolve_skywalking_agent_version)
    skywalking_agent_jar=$(find_skywalking_agent_jar "$skywalking_version" || true)
    if [ -z "$skywalking_agent_jar" ]; then
        skywalking_agent_jar=$(download_skywalking_agent "$skywalking_version" || true)
    fi

    if [ -n "$skywalking_agent_jar" ]; then
        app_skywalking_agent_jar=$(prepare_app_skywalking_agent "$skywalking_agent_jar")
        local sw_host="${SW_AGENT_COLLECTOR_BACKEND_SERVICES:-127.0.0.1:11800}"
        local skywalking_opts="-javaagent:$app_skywalking_agent_jar -Dskywalking.agent.service_name=$APP_NAME -Dskywalking.collector.backend_service=$sw_host"
        jvm_opts="$jvm_opts $skywalking_opts"
        log_info "SkyWalking Java Agent 已启用: $app_skywalking_agent_jar"
    else
        log_warn "未启用 SkyWalking Java Agent：未找到且未成功下载版本 $skywalking_version"
    fi

    echo "$jvm_opts"
}
