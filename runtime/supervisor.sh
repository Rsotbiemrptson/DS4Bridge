#!/system/bin/sh
STATE=/data/local/ds4-bridge-app
HELPER=com.odin2.ds4bridgehelper
BRIDGE_PID=
cleanup() {
    [ -z "$BRIDGE_PID" ] || kill -TERM "$BRIDGE_PID" 2>/dev/null
    am broadcast -n "$HELPER/.ExclusionReceiver" --es action remove >/dev/null 2>&1
    rm -f "$STATE/supervisor.pid" "$STATE/bridge.pid"
}
trap 'exit 0' INT TERM
trap cleanup EXIT
echo "$$" >"$STATE/supervisor.pid"
while [ "$(getprop sys.boot_completed)" != 1 ]; do sleep 2; done
am broadcast -n "$HELPER/.ExclusionReceiver" --es action add >/dev/null 2>&1
sleep 1
content read --uri "content://$HELPER.targets/profile" >"$STATE/profile.conf" 2>/dev/null
if [ -s "$STATE/profile.conf" ]; then
    "$STATE/odin_ds4_bridge" --profile "$STATE/profile.conf" --managed --seconds 0 &
else
    "$STATE/odin_ds4_bridge" --managed --seconds 0 &
fi
BRIDGE_PID=$!
echo "$BRIDGE_PID" >"$STATE/bridge.pid"
sleep 2
kill -0 "$BRIDGE_PID" 2>/dev/null || exit 1
dumpsys input 2>/dev/null | grep -q 'vendor=0x054c, product=0x09cc' || exit 1
while kill -0 "$BRIDGE_PID" 2>/dev/null; do
    pm path "$HELPER" >/dev/null 2>&1 || exit 0
    forwarding="$(content read --uri "content://$HELPER.targets/forwarding" 2>/dev/null)"
    if [ "$forwarding" = off ]; then
        kill -USR2 "$BRIDGE_PID" 2>/dev/null
    elif [ "$forwarding" = all ]; then
        kill -USR1 "$BRIDGE_PID" 2>/dev/null
    else
        top_package="$(dumpsys activity activities 2>/dev/null | grep -m 1 'topResumedActivity=' | sed -n 's/.* u[0-9][0-9]* \([^ /]*\)\/.*/\1/p')"
        targets="$(content read --uri "content://$HELPER.targets/targets" 2>/dev/null)"
        if [ -n "$top_package" ] && printf '%s\n' "$targets" | grep -Fqx "$top_package"; then
            kill -USR1 "$BRIDGE_PID" 2>/dev/null
        else
            kill -USR2 "$BRIDGE_PID" 2>/dev/null
        fi
    fi
    sleep 1
done
