#!/system/bin/sh
STATE=/data/local/ds4-bridge-app
[ "$(id -u)" = 0 ] || exit 1
for name in supervisor bridge; do
    [ -f "$STATE/$name.pid" ] || continue
    pid="$(cat "$STATE/$name.pid")"
    case "$pid" in ''|*[!0-9]*) continue ;; esac
    if [ "$name" = supervisor ]; then
        cmd="$(tr '\000' ' ' <"/proc/$pid/cmdline" 2>/dev/null)"
        case "$cmd" in *"$STATE/supervisor.sh"*) kill -TERM "$pid" 2>/dev/null ;; esac
    elif [ "$(readlink "/proc/$pid/exe" 2>/dev/null)" = "$STATE/odin_ds4_bridge" ]; then
        kill -TERM "$pid" 2>/dev/null
    fi
done
echo STOP_REQUESTED
