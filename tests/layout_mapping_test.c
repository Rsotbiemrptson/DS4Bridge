#define main odin_bridge_main
#include "../odin_ds4_bridge.c"
#undef main

#include <assert.h>
#include <stdio.h>

int main(void) {
    struct layout_case {
        unsigned short product;
        unsigned short key;
        int expected;
    } cases[] = {
        {ODIN_PRODUCT_XBOX, BTN_Y, DS4_TRIANGLE}, /* top */
        {ODIN_PRODUCT_XBOX, BTN_X, DS4_SQUARE},   /* left */
        {ODIN_PRODUCT_XBOX, BTN_A, DS4_CROSS},    /* bottom */
        {ODIN_PRODUCT_XBOX, BTN_B, DS4_CIRCLE},   /* right */
        {ODIN_PRODUCT_ODIN, BTN_X, DS4_TRIANGLE}, /* top */
        {ODIN_PRODUCT_ODIN, BTN_Y, DS4_SQUARE},   /* left */
        {ODIN_PRODUCT_ODIN, BTN_B, DS4_CROSS},    /* bottom */
        {ODIN_PRODUCT_ODIN, BTN_A, DS4_CIRCLE},   /* right */
    };
    for (size_t i = 0; i < sizeof(cases) / sizeof(cases[0]); ++i) {
        assert(mapped_key(cases[i].key, cases[i].product) == cases[i].expected);
    }
    puts("Xbox and Odin face-button positions map to PS4 positions");
    return 0;
}
