#define main bridge_program_main
#include "../odin_ds4_bridge.c"
#undef main
#include <assert.h>

static void profile(const char *text) {
    FILE *file = fopen("profile-test.conf", "w");
    assert(file);
    assert(fputs(text, file) >= 0);
    fclose(file);
}

int main(void) {
    profile("device 8224 274\nkey 304 304\nkey 307 305\naxis 0 0 0\naxis 1 1 1\naxis 9 4 0\n");
    assert(load_profile("profile-test.conf") == 0);
    profile_active = true;
    assert(mapped_key(304, 274) == DS4_SQUARE);
    assert(mapped_key(307, 274) == DS4_CROSS);
    assert(mapped_key(305, 274) == -1);
    source_ranges[0].minimum = 0; source_ranges[0].maximum = 255;
    source_ranges[1] = source_ranges[0];
    source_ranges[9].minimum = 0; source_ranges[9].maximum = 1023;
    assert(normalize_axis(0, ABS_X, 0) == -32767);
    assert(normalize_axis(0, ABS_X, 255) == 32767);
    assert(normalize_axis(1, ABS_Y, 0) == 32767);
    assert(normalize_axis(1, ABS_Y, 255) == -32767);
    assert(normalize_axis(9, ABS_RY, 0) == 0);
    assert(normalize_axis(9, ABS_RY, 1023) == 32767);
    profile_active = false;
    assert(mapped_key(BTN_A, ODIN_PRODUCT_ODIN) == DS4_CIRCLE);
    assert(mapped_key(BTN_A, ODIN_PRODUCT_XBOX) == DS4_CROSS);
    /* Taking D-pad left for L1 must not also send D-pad left via HAT. */
    profile("device 8224 274\nkey 546 308\nkey 547 547\naxis 16 16 0\n");
    assert(load_profile("profile-test.conf") == 0);
    profile_active = true;
    source_ranges[ABS_HAT0X].minimum = -1;
    source_ranges[ABS_HAT0X].maximum = 1;
    int channel[2]; assert(pipe(channel) == 0);
    assert(fcntl(channel[0], F_SETFL, O_NONBLOCK) == 0);
    bool dpad[4] = {0};
    struct input_event event = {.type = EV_ABS, .code = ABS_HAT0X, .value = -1}, received;
    forward_event(channel[1], &event, dpad, ODIN_PRODUCT_XBOX);
    assert(read(channel[0], &received, sizeof(received)) == sizeof(received));
    assert(received.type == EV_KEY && received.code == DS4_L1 && received.value == 1);
    assert(read(channel[0], &received, sizeof(received)) == -1 && errno == EAGAIN);
    event.value = 0; forward_event(channel[1], &event, dpad, ODIN_PRODUCT_XBOX);
    assert(read(channel[0], &received, sizeof(received)) == sizeof(received));
    assert(received.type == EV_KEY && received.code == DS4_L1 && received.value == 0);
    event.value = 1; forward_event(channel[1], &event, dpad, ODIN_PRODUCT_XBOX);
    assert(read(channel[0], &received, sizeof(received)) == sizeof(received));
    assert(received.type == EV_ABS && received.code == ABS_HAT0X && received.value == 1);
    event.value = 0; forward_event(channel[1], &event, dpad, ODIN_PRODUCT_XBOX);
    assert(read(channel[0], &received, sizeof(received)) == sizeof(received));
    assert(received.type == EV_ABS && received.value == 0);
    event.type = EV_KEY; event.code = BTN_TL; event.value = 1;
    forward_event(channel[1], &event, dpad, ODIN_PRODUCT_XBOX);
    assert(read(channel[0], &received, sizeof(received)) == -1 && errno == EAGAIN);
    close(channel[0]); close(channel[1]);
    profile_active = false;
    profile("device 8224 274\nkey 304 305\nkey 304 306\n");
    assert(load_profile("profile-test.conf") < 0);
    profile("device 1356 2508\n");
    assert(load_profile("profile-test.conf") < 0);
    profile("device 8224 274\naxis 0 0 2\n");
    assert(load_profile("profile-test.conf") < 0);
    profile("device 8224 274\nkey 304 999\n");
    assert(load_profile("profile-test.conf") < 0);
    unlink("profile-test.conf");
    puts("Profile validation, remapping, axis normalization and inversion passed");
    return 0;
}
