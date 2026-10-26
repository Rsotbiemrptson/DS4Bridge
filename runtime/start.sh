#!/system/bin/sh
STATE=/data/local/ds4-bridge-app
exec >>"$STATE/start.log" 2>&1
[ "$(id -u)" = 0 ] || exit 1
[ "$(cat "$STATE/ready" 2>/dev/null)" = 15 ] || { echo 'STOP: complete app setup required'; exit 1; }
model="$(getprop ro.product.vendor.model)"
case "$model" in Odin2|"Odin 2") ;; *) echo 'STOP: diagnostics only for this model'; exit 1 ;; esac
[ "$(getprop ro.build.version.sdk)" = 33 ] || { echo 'STOP: Odin 2 Android 13 required'; exit 1; }
[ ! -x /system_ext/bin/odin_ds4_bridge ] || { echo 'STOP: firmware bridge already installed'; exit 1; }
[ -c /dev/uinput ] || { echo 'STOP: uinput unavailable'; exit 1; }
if [ -f "$STATE/supervisor.pid" ]; then
    pid="$(cat "$STATE/supervisor.pid")"
    case "$pid" in ''|*[!0-9]*) exit 1 ;; esac
    cmd="$(tr '\000' ' ' <"/proc/$pid/cmdline" 2>/dev/null)"
    case "$cmd" in *"$STATE/supervisor.sh"*) echo 'ALREADY_RUNNING'; exit 0 ;; esac
fi
if pidof odin_ds4_bridge >/dev/null 2>&1; then echo 'STOP: another bridge is running'; exit 1; fi
/system/bin/sh "$STATE/supervisor.sh" </dev/null >>"$STATE/runtime.log" 2>&1 &
echo "START_REQUESTED $!"
