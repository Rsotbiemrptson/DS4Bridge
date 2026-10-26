package com.odin2.ds4bridgehelper;

/** Shared Raven Backup contrast rules for arbitrary accent colors. */
final class AppearanceColors {
    static double channel(int c){double n=c/255.0;return n<=.04045?n/12.92:Math.pow((n+.055)/1.055,2.4);}
    static double luminance(int c){return .2126*channel((c>>16)&255)+.7152*channel((c>>8)&255)+.0722*channel(c&255);}
    static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    static int ink(int background){return contrast(0xff000000,background)>=contrast(0xffffffff,background)?0xff000000:0xffffffff;}
    static int readable(int color,int surface){if(contrast(color,surface)>=4.5)return color;int target=luminance(surface)>.5?0:255;for(int i=1;i<=100;i++){float t=i/100f;int r=Math.round(((color>>16)&255)*(1-t)+target*t),g=Math.round(((color>>8)&255)*(1-t)+target*t),b=Math.round((color&255)*(1-t)+target*t);int value=0xff000000|(r<<16)|(g<<8)|b;if(contrast(value,surface)>=4.5)return value;}return target==0?0xff000000:0xffffffff;}
}
