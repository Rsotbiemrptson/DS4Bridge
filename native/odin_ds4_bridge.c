#define _GNU_SOURCE

#include <errno.h>
#include <fcntl.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <poll.h>
#include <signal.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <time.h>
#include <unistd.h>

#define ODIN_VENDOR 0x2020
#define ODIN_PRODUCT_ODIN 0x0111
#define ODIN_PRODUCT_XBOX 0x0112
#define SONY_VENDOR 0x054c
#define DS4_PRODUCT 0x09cc

/* Raw Linux codes expected by Android's Vendor_054c_Product_09cc.kl. */
enum {
    DS4_SQUARE = 0x130, DS4_CROSS = 0x131, DS4_CIRCLE = 0x132,
    DS4_TRIANGLE = 0x133, DS4_L1 = 0x134, DS4_R1 = 0x135,
    DS4_L2 = 0x136, DS4_R2 = 0x137, DS4_SHARE = 0x138,
    DS4_OPTIONS = 0x139, DS4_L3 = 0x13a, DS4_R3 = 0x13b,
    DS4_PS = 0x13c, DS4_TOUCHPAD = 0x13d
};

static volatile sig_atomic_t stop_requested;
static volatile sig_atomic_t forwarding_request = -1;
static int key_profile[KEY_MAX + 1];
static int axis_profile[ABS_MAX + 1];
static bool axis_invert[ABS_MAX + 1];
static unsigned int profile_vendor, profile_product;
static bool has_profile, profile_active;
static struct input_absinfo source_ranges[ABS_MAX + 1];
static double now_seconds(void);

static bool valid_key_target(int code) {
    return (code >= DS4_SQUARE && code <= DS4_TOUCHPAD) ||
           (code >= BTN_DPAD_UP && code <= BTN_DPAD_RIGHT);
}

static bool valid_axis_target(int code) {
    return code == ABS_X || code == ABS_Y || code == ABS_Z || code == ABS_RZ ||
           code == ABS_RX || code == ABS_RY || code == ABS_HAT0X || code == ABS_HAT0Y;
}

/* Profiles contain numeric input codes only; they are never shell commands. */
static int load_profile(const char *path) {
    FILE *file = fopen(path, "r");
    if (!file) return -1;
    for (int i = 0; i <= KEY_MAX; ++i) key_profile[i] = -1;
    for (int i = 0; i <= ABS_MAX; ++i) axis_profile[i] = -1;
    char line[128], tail;
    if (!fgets(line, sizeof(line), file) ||
        sscanf(line, "device %u %u %c", &profile_vendor, &profile_product, &tail) != 2 ||
        profile_vendor > 65535 || profile_product > 65535 ||
        (profile_vendor == SONY_VENDOR && profile_product == DS4_PRODUCT)) goto invalid;
    int lines = 0;
    while (fgets(line, sizeof(line), file)) {
        int source, target, invert;
        if (++lines > 128) goto invalid;
        if (sscanf(line, "key %d %d %c", &source, &target, &tail) == 2) {
            if (source < 0 || source > KEY_MAX || !valid_key_target(target) ||
                key_profile[source] != -1) goto invalid;
            key_profile[source] = target;
        } else if (sscanf(line, "axis %d %d %d %c", &source, &target, &invert, &tail) == 3) {
            if (source < 0 || source > ABS_MAX || !valid_axis_target(target) ||
                (invert != 0 && invert != 1) || axis_profile[source] != -1) goto invalid;
            axis_profile[source] = target;
            axis_invert[source] = invert;
        } else goto invalid;
    }
    fclose(file);
    has_profile = true;
    return 0;
invalid:
    fclose(file);
    fprintf(stderr, "Invalid controller profile\n");
    return -1;
}

static int normalize_axis(int code, int target, int value) {
    if (!profile_active || code > ABS_MAX || source_ranges[code].maximum <= source_ranges[code].minimum)
        return value;
    double fraction = ((double)value - source_ranges[code].minimum) /
                      ((double)source_ranges[code].maximum - source_ranges[code].minimum);
    if (fraction < 0) fraction = 0;
    if (fraction > 1) fraction = 1;
    if (axis_invert[code]) fraction = 1.0 - fraction;
    int minimum = target == ABS_RX || target == ABS_RY ? 0 :
                  target == ABS_HAT0X || target == ABS_HAT0Y ? -1 : -32767;
    int maximum = target == ABS_HAT0X || target == ABS_HAT0Y ? 1 : 32767;
    double scaled = minimum + fraction * (maximum - minimum);
    return (int)(scaled < 0 ? scaled - 0.5 : scaled + 0.5);
}

static void inspect_controllers(void) {
    printf("Native input inspection (read only)\n");
    for (int n = 0; n < 128; ++n) {
        char path[64], name[128] = {0};
        snprintf(path, sizeof(path), "/dev/input/event%d", n);
        int fd = open(path, O_RDONLY | O_NONBLOCK | O_CLOEXEC);
        if (fd < 0) continue;
        struct input_id id = {0};
        unsigned char keys[(KEY_MAX + 8) / 8] = {0};
        unsigned char axes[(ABS_MAX + 8) / 8] = {0};
        ioctl(fd, EVIOCGID, &id);
        ioctl(fd, EVIOCGNAME(sizeof(name)), name);
        ioctl(fd, EVIOCGBIT(EV_KEY, sizeof(keys)), keys);
        ioctl(fd, EVIOCGBIT(EV_ABS, sizeof(axes)), axes);
        bool gamepad = (axes[ABS_X / 8] & (1u << (ABS_X % 8))) &&
                       (axes[ABS_Y / 8] & (1u << (ABS_Y % 8)));
        if (gamepad) {
            printf("DEVICE %u %u %s %s\n", id.vendor, id.product, path, name);
            printf("KEYS");
            for (int k = 0; k <= KEY_MAX; ++k)
                if (keys[k / 8] & (1u << (k % 8))) printf(" %d", k);
            printf("\n");
            for (int a = 0; a <= ABS_MAX; ++a) {
                struct input_absinfo info;
                if ((axes[a / 8] & (1u << (a % 8))) && ioctl(fd, EVIOCGABS(a), &info) == 0)
                    printf("AXIS %d min=%d max=%d flat=%d value=%d\n",
                           a, info.minimum, info.maximum, info.flat, info.value);
            }
        }
        close(fd);
    }
}

static int capture_axis(unsigned int vendor, unsigned int product) {
    if (vendor > 65535 || product > 65535 ||
        (vendor == SONY_VENDOR && product == DS4_PRODUCT)) return 2;
    for (int n = 0; n < 128; ++n) {
        char path[64];
        snprintf(path, sizeof(path), "/dev/input/event%d", n);
        int fd = open(path, O_RDONLY | O_NONBLOCK | O_CLOEXEC);
        if (fd < 0) continue;
        struct input_id id = {0};
        ioctl(fd, EVIOCGID, &id);
        if (id.vendor != vendor || id.product != product) { close(fd); continue; }
        unsigned char keys[(KEY_MAX + 8) / 8] = {0};
        ioctl(fd, EVIOCGBIT(EV_KEY, sizeof(keys)), keys);
        bool gamepad = false;
        for (int k = BTN_GAMEPAD; k <= BTN_THUMBR; ++k)
            if (keys[k / 8] & (1u << (k % 8))) gamepad = true;
        if (!gamepad) { close(fd); continue; }
        struct input_absinfo ranges[ABS_MAX + 1] = {0};
        int low[ABS_MAX + 1] = {0}, high[ABS_MAX + 1] = {0};
        for (int a = 0; a <= ABS_MAX; ++a) {
            ioctl(fd, EVIOCGABS(a), &ranges[a]);
            low[a] = high[a] = ranges[a].value;
        }
        double until = now_seconds() + 5;
        while (now_seconds() < until) {
            struct pollfd pfd = {.fd = fd, .events = POLLIN};
            if (poll(&pfd, 1, 100) <= 0) continue;
            if (pfd.revents & (POLLERR | POLLHUP | POLLNVAL)) break;
            struct input_event event;
            while (read(fd, &event, sizeof(event)) == sizeof(event)) {
                if (event.type == EV_ABS && event.code <= ABS_MAX) {
                    if (event.value < low[event.code]) low[event.code] = event.value;
                    if (event.value > high[event.code]) high[event.code] = event.value;
                }
            }
        }
        close(fd);
        int best = -1;
        double score = 0, second = 0;
        for (int a = 0; a <= ABS_MAX; ++a) {
            double span = (double)ranges[a].maximum - ranges[a].minimum;
            if (span <= 0) continue;
            double moved = ((double)high[a] - low[a]) / span;
            if (moved > score) { second = score; score = moved; best = a; }
            else if (moved > second) second = moved;
        }
        if (score < 0.15 || second > score * 0.8) {
            printf("CAPTURE_AMBIGUOUS move only the requested axis and try again\n");
            return 1;
        }
        printf("CAPTURE_AXIS %d\n", best);
        return 0;
    }
    printf("CAPTURE_NO_DEVICE\n");
    return 1;
}

static const unsigned int ds4_keys[] = {
    DS4_SQUARE, DS4_CROSS, DS4_CIRCLE, DS4_TRIANGLE,
    DS4_L1, DS4_R1, DS4_L2, DS4_R2, DS4_SHARE, DS4_OPTIONS,
    DS4_L3, DS4_R3, DS4_PS, DS4_TOUCHPAD
};

static const unsigned int ds4_axes[] = {
    ABS_X, ABS_Y, ABS_Z, ABS_RZ, ABS_RX, ABS_RY, ABS_HAT0X, ABS_HAT0Y
};

static void on_signal(int signum) {
    (void)signum;
    stop_requested = 1;
}

static void on_enable(int signum) {
    (void)signum;
    forwarding_request = 1;
}

static void on_disable(int signum) {
    (void)signum;
    forwarding_request = 0;
}

static double now_seconds(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (double)ts.tv_sec + (double)ts.tv_nsec / 1000000000.0;
}

static int emit(int fd, unsigned short type, unsigned short code, int value) {
    struct input_event event = {0};
    event.type = type;
    event.code = code;
    event.value = value;
    return write(fd, &event, sizeof(event)) == sizeof(event) ? 0 : -1;
}

static int sync_events(int fd) {
    return emit(fd, EV_SYN, SYN_REPORT, 0);
}

static int set_abs(int fd, unsigned int code, int minimum, int maximum, int flat) {
    struct uinput_abs_setup setup = {0};
    setup.code = code;
    setup.absinfo.minimum = minimum;
    setup.absinfo.maximum = maximum;
    setup.absinfo.flat = flat;
    return ioctl(fd, UI_ABS_SETUP, &setup);
}

static int create_ds4(void) {
    int fd = open("/dev/uinput", O_WRONLY | O_NONBLOCK | O_CLOEXEC);
    if (fd < 0) {
        perror("open /dev/uinput");
        return -1;
    }
    if (ioctl(fd, UI_SET_EVBIT, EV_KEY) < 0 ||
        ioctl(fd, UI_SET_EVBIT, EV_ABS) < 0) {
        perror("UI_SET_EVBIT");
        close(fd);
        return -1;
    }
    for (size_t i = 0; i < sizeof(ds4_keys) / sizeof(ds4_keys[0]); ++i) {
        if (ioctl(fd, UI_SET_KEYBIT, ds4_keys[i]) < 0) {
            perror("UI_SET_KEYBIT");
            close(fd);
            return -1;
        }
    }
    for (size_t i = 0; i < sizeof(ds4_axes) / sizeof(ds4_axes[0]); ++i) {
        if (ioctl(fd, UI_SET_ABSBIT, ds4_axes[i]) < 0) {
            perror("UI_SET_ABSBIT");
            close(fd);
            return -1;
        }
    }

    struct uinput_setup setup = {0};
    setup.id.bustype = BUS_USB;
    setup.id.vendor = SONY_VENDOR;
    setup.id.product = DS4_PRODUCT;
    setup.id.version = 0x0100;
    snprintf(setup.name, sizeof(setup.name), "Raven Gamepad");
    if (ioctl(fd, UI_DEV_SETUP, &setup) < 0 ||
        set_abs(fd, ABS_X, -32767, 32767, 15) < 0 ||
        set_abs(fd, ABS_Y, -32767, 32767, 15) < 0 ||
        set_abs(fd, ABS_Z, -32767, 32767, 15) < 0 ||
        set_abs(fd, ABS_RZ, -32767, 32767, 15) < 0 ||
        set_abs(fd, ABS_RX, 0, 32767, 0) < 0 ||
        set_abs(fd, ABS_RY, 0, 32767, 0) < 0 ||
        set_abs(fd, ABS_HAT0X, -1, 1, 0) < 0 ||
        set_abs(fd, ABS_HAT0Y, -1, 1, 0) < 0) {
        perror("UI_DEV_SETUP / UI_ABS_SETUP");
        close(fd);
        return -1;
    }
    if (ioctl(fd, UI_DEV_CREATE) < 0) {
        perror("UI_DEV_CREATE");
        close(fd);
        return -1;
    }
    return fd;
}

static int open_odin(char *selected_path, size_t path_size,
                     unsigned short *selected_product) {
    for (int n = 0; n < 64; ++n) {
        char path[64];
        struct input_id id;
        unsigned char keybits[(KEY_MAX + 8) / 8] = {0};
        unsigned char absbits[(ABS_MAX + 8) / 8] = {0};
        snprintf(path, sizeof(path), "/dev/input/event%d", n);
        int fd = open(path, O_RDONLY | O_NONBLOCK | O_CLOEXEC);
        if (fd < 0) continue;
        if (ioctl(fd, EVIOCGID, &id) == 0 &&
            (((!has_profile || (profile_vendor == ODIN_VENDOR &&
               (profile_product == ODIN_PRODUCT_ODIN || profile_product == ODIN_PRODUCT_XBOX))) && id.vendor == ODIN_VENDOR &&
             (id.product == ODIN_PRODUCT_ODIN || id.product == ODIN_PRODUCT_XBOX)) ||
             (has_profile && id.vendor == profile_vendor && id.product == profile_product)) &&
            ioctl(fd, EVIOCGBIT(EV_KEY, sizeof(keybits)), keybits) >= 0 &&
            ioctl(fd, EVIOCGBIT(EV_ABS, sizeof(absbits)), absbits) >= 0 &&
            ((has_profile && id.vendor == profile_vendor && id.product == profile_product) ||
             ((keybits[BTN_A / 8] & (1u << (BTN_A % 8))) &&
              (keybits[BTN_START / 8] & (1u << (BTN_START % 8))))) &&
            (absbits[ABS_X / 8] & (1u << (ABS_X % 8))) &&
            (absbits[ABS_Y / 8] & (1u << (ABS_Y % 8)))) {
            snprintf(selected_path, path_size, "%s", path);
            *selected_product = id.product;
            profile_active = has_profile && id.vendor == profile_vendor && id.product == profile_product;
            memset(source_ranges, 0, sizeof(source_ranges));
            if (profile_active) for (int a = 0; a <= ABS_MAX; ++a)
                ioctl(fd, EVIOCGABS(a), &source_ranges[a]);
            return fd;
        }
        close(fd);
    }
    return -1;
}

static int mapped_key(unsigned short code, unsigned short product) {
    if (profile_active && product == profile_product && code <= KEY_MAX)
        return key_profile[code];
    switch (code) {
        /* Xbox labels: top Y, left X, bottom A, right B.
         * Odin labels: top X, left Y, bottom B, right A.
         * Preserve physical PS4 positions in either mode. */
        case BTN_A: return product == ODIN_PRODUCT_ODIN ? DS4_CIRCLE : DS4_CROSS;
        case BTN_B: return product == ODIN_PRODUCT_ODIN ? DS4_CROSS : DS4_CIRCLE;
        case BTN_X: return product == ODIN_PRODUCT_ODIN ? DS4_TRIANGLE : DS4_SQUARE;
        case BTN_Y: return product == ODIN_PRODUCT_ODIN ? DS4_SQUARE : DS4_TRIANGLE;
        case BTN_TL: return DS4_L1;
        case BTN_TR: return DS4_R1;
        case BTN_TL2: return DS4_L2;
        case BTN_TR2: return DS4_R2;
        case BTN_SELECT: return DS4_SHARE;
        case BTN_START: return DS4_OPTIONS;
        case BTN_THUMBL: return DS4_L3;
        case BTN_THUMBR: return DS4_R3;
        case BTN_MODE: return DS4_PS;
        case BTN_DPAD_UP: case BTN_DPAD_DOWN: case BTN_DPAD_LEFT: case BTN_DPAD_RIGHT:
            return code;
        default: return -1;
    }
}

static int mapped_axis(unsigned short code) {
    if (profile_active && code <= ABS_MAX) return axis_profile[code];
    switch (code) {
        case ABS_X: return ABS_X;
        case ABS_Y: return ABS_Y;
        case ABS_Z: return ABS_Z;
        case ABS_RZ: return ABS_RZ;
        case ABS_GAS: return ABS_RY;   /* Odin right trigger -> DS4 R2 */
        case ABS_BRAKE: return ABS_RX; /* Odin left trigger -> DS4 L2 */
        case ABS_HAT0X: return ABS_HAT0X;
        case ABS_HAT0Y: return ABS_HAT0Y;
        default: return -1;
    }
}

static int source_hat[2];

static void forward_event(int out, const struct input_event *event, bool dpad[4],
                          unsigned short product) {
    if (event->type == EV_KEY) {
        int axis = -1;
        int value = 0;
        int index = -1;
        int target = mapped_key(event->code, product);
        switch (target) {
            case BTN_DPAD_LEFT: index = 0; axis = ABS_HAT0X; break;
            case BTN_DPAD_RIGHT: index = 1; axis = ABS_HAT0X; break;
            case BTN_DPAD_UP: index = 2; axis = ABS_HAT0Y; break;
            case BTN_DPAD_DOWN: index = 3; axis = ABS_HAT0Y; break;
        }
        if (index >= 0) {
            dpad[index] = event->value != 0;
            value = axis == ABS_HAT0X ? (int)dpad[1] - (int)dpad[0]
                                      : (int)dpad[3] - (int)dpad[2];
            emit(out, EV_ABS, axis, value);
            return;
        }
        if (target >= 0) emit(out, EV_KEY, target, event->value);
    } else if (event->type == EV_ABS) {
        int target = mapped_axis(event->code);
        /* A D-pad axis and its digital button aliases must use the same
         * assignments. Otherwise a displaced direction still leaks through. */
        if (profile_active && (target == ABS_HAT0X || target == ABS_HAT0Y)) {
            int slot = target == ABS_HAT0X ? 0 : 1;
            int value = normalize_axis(event->code, target, event->value);
            int previous = source_hat[slot];
            source_hat[slot] = value;
            if (value != previous) {
                int negative = slot == 0 ? BTN_DPAD_LEFT : BTN_DPAD_UP;
                int positive = slot == 0 ? BTN_DPAD_RIGHT : BTN_DPAD_DOWN;
                if (previous != 0) {
                    struct input_event key = {.type = EV_KEY,
                        .code = previous < 0 ? negative : positive, .value = 0};
                    forward_event(out, &key, dpad, product);
                }
                if (value != 0) {
                    struct input_event key = {.type = EV_KEY,
                        .code = value < 0 ? negative : positive, .value = 1};
                    forward_event(out, &key, dpad, product);
                }
            }
            return;
        }
        if (target >= 0) emit(out, EV_ABS, target, normalize_axis(event->code, target, event->value));
    } else if (event->type == EV_SYN && event->code == SYN_REPORT) {
        sync_events(out);
    }
}

static void neutralize_virtual(int out, bool dpad[4]) {
    for (size_t i = 0; i < sizeof(ds4_keys) / sizeof(ds4_keys[0]); ++i) {
        emit(out, EV_KEY, ds4_keys[i], 0);
    }
    for (size_t i = 0; i < sizeof(ds4_axes) / sizeof(ds4_axes[0]); ++i) {
        emit(out, EV_ABS, ds4_axes[i], 0);
    }
    memset(dpad, 0, sizeof(bool) * 4);
    memset(source_hat, 0, sizeof(source_hat));
    sync_events(out);
}

static void snapshot_odin_state(int source, int out, unsigned short product, bool dpad[4]) {
    memset(dpad, 0, sizeof(bool) * 4);
    memset(source_hat, 0, sizeof(source_hat));
    unsigned char keybits[(KEY_MAX + 8) / 8] = {0};
    if (ioctl(source, EVIOCGKEY(sizeof(keybits)), keybits) >= 0) {
        for (unsigned int raw = 0; raw <= KEY_MAX; ++raw) {
            if (keybits[raw / 8] & (1u << (raw % 8))) {
                struct input_event event = {.type = EV_KEY, .code = raw, .value = 1};
                forward_event(out, &event, dpad, product);
            }
        }
    }
    for (unsigned int raw = 0; raw <= ABS_MAX; ++raw) {
        struct input_absinfo info;
        int target = mapped_axis(raw);
        if (target >= 0 && ioctl(source, EVIOCGABS(raw), &info) >= 0) {
            struct input_event event = {.type = EV_ABS, .code = raw, .value = info.value};
            forward_event(out, &event, dpad, product);
        }
    }
    sync_events(out);
}

int main(int argc, char **argv) {
    if (argc == 2 && strcmp(argv[1], "--check-hooks") == 0) {
        const char *paths[] = {"/dev/uinput", "/dev/uhid"};
        for (size_t i = 0; i < sizeof(paths) / sizeof(paths[0]); ++i) {
            int fd = open(paths[i], O_RDWR | O_NONBLOCK | O_CLOEXEC);
            if (fd >= 0) { printf("HOOK %s open succeeded (no device created)\n", paths[i]); close(fd); }
            else printf("HOOK %s unavailable: %s\n", paths[i], strerror(errno));
        }
        return 0;
    }
    if (argc == 4 && strcmp(argv[1], "--capture-axis") == 0) {
        char *vendor_end, *product_end;
        unsigned long vendor = strtoul(argv[2], &vendor_end, 10);
        unsigned long product = strtoul(argv[3], &product_end, 10);
        if (*vendor_end || *product_end || vendor > 65535 || product > 65535) return 2;
        return capture_axis((unsigned int)vendor, (unsigned int)product);
    }
    bool forward = false;
    bool managed = false;
    int seconds = 45;
    for (int i = 1; i < argc; ++i) {
        if (strcmp(argv[i], "--forward") == 0) {
            forward = true;
        } else if (strcmp(argv[i], "--managed") == 0) {
            managed = true;
        } else if (strcmp(argv[i], "--seconds") == 0 && i + 1 < argc) {
            seconds = atoi(argv[++i]);
        } else if (strcmp(argv[i], "--inspect") == 0) {
            inspect_controllers();
            return 0;
        } else if (strcmp(argv[i], "--profile") == 0 && i + 1 < argc) {
            if (load_profile(argv[++i]) < 0) return 2;
        } else {
            fprintf(stderr, "Usage: %s [--forward | --managed] [--seconds 0..300] [--profile file]; or --inspect / --check-hooks / --capture-axis VID PID\n", argv[0]);
            return 2;
        }
    }
    if (forward && managed) {
        fprintf(stderr, "Choose either --forward or --managed, not both\n");
        return 2;
    }
    if (seconds < 0 || seconds > 300) {
        fprintf(stderr, "Duration must be 0..300 seconds (0 means until stopped)\n");
        return 2;
    }

    signal(SIGINT, on_signal);
    signal(SIGTERM, on_signal);
    if (managed) {
        signal(SIGUSR1, on_enable);
        signal(SIGUSR2, on_disable);
    }
    int source = -1;
    char source_path[64] = {0};
    unsigned short source_product = 0;
    if (forward || managed) {
        source = open_odin(source_path, sizeof(source_path), &source_product);
    }
    int virtual_pad = create_ds4();
    if (virtual_pad < 0) {
        if (source >= 0) close(source);
        return 1;
    }
    fprintf(stderr, "Virtual DualShock 4 054c:09cc created; %s%s; duration %d seconds (0 = until stopped)\n",
            managed ? "managed idle, source " : forward ? "forwarding " : "identity only",
            (forward || managed) ? source_path : "", seconds);
    fflush(stderr);

    bool dpad[4] = {0};
    bool forwarding_active = forward;
    double forwarding_lease_until = 0;
    double until = now_seconds() + seconds;
    while (!stop_requested && (seconds == 0 || now_seconds() < until)) {
        if (managed) {
            int request = forwarding_request;
            forwarding_request = -1;
            if (request == 1) {
                forwarding_lease_until = now_seconds() + 4.0;
                if (!forwarding_active) {
                    if (source >= 0) {
                        snapshot_odin_state(source, virtual_pad, source_product, dpad);
                    }
                    forwarding_active = true;
                    fprintf(stderr, "Forwarding enabled\n");
                }
            } else if (request == 0) {
                forwarding_lease_until = 0;
            }
            if (forwarding_active && now_seconds() >= forwarding_lease_until) {
                neutralize_virtual(virtual_pad, dpad);
                forwarding_active = false;
                fprintf(stderr, "Forwarding disabled\n");
            }
        }
        if (source < 0) {
            if (forward || managed) {
                source = open_odin(source_path, sizeof(source_path), &source_product);
                if (source >= 0) {
                    fprintf(stderr, "Odin controller connected: %s\n", source_path);
                    if (forwarding_active) {
                        snapshot_odin_state(source, virtual_pad, source_product, dpad);
                    }
                    continue;
                }
            }
            struct timespec pause = {.tv_sec = 0, .tv_nsec = 250000000};
            nanosleep(&pause, NULL);
            continue;
        }
        struct pollfd pfd = {.fd = source, .events = POLLIN};
        int ready = poll(&pfd, 1, 100);
        if (ready < 0 && errno != EINTR) {
            perror("poll Odin controller");
            break;
        }
        if (ready <= 0) continue;
        if (pfd.revents & (POLLERR | POLLHUP | POLLNVAL)) {
            fprintf(stderr, "Odin controller disconnected: %s\n", source_path);
            if (forwarding_active) neutralize_virtual(virtual_pad, dpad);
            close(source);
            source = -1;
            source_path[0] = '\0';
            source_product = 0;
            continue;
        }
        if (pfd.revents & POLLIN) {
            struct input_event event;
            while (read(source, &event, sizeof(event)) == sizeof(event)) {
                if (forwarding_active) {
                    forward_event(virtual_pad, &event, dpad, source_product);
                }
            }
        }
    }

    ioctl(virtual_pad, UI_DEV_DESTROY);
    close(virtual_pad);
    if (source >= 0) close(source);
    fprintf(stderr, "Virtual controller removed\n");
    return 0;
}
