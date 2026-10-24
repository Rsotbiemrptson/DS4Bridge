package com.odin2.ds4bridgehelper;

public final class MappingProfilesTest {
    private static int checked;
    private static void check(boolean ok,String reason){checked++;if(!ok)throw new AssertionError(reason);}
    private static void rejects(Runnable action){boolean rejected=false;try{action.run();}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Unverified protocol accepted");}
    public static void main(String[] args){
        MappingProfiles.Profile old=MappingProfiles.byComponent("com.odin2.touchmapping","com.odin2.touchmapping.service.ApiService");
        check(old!=null&&old.listCode==33&&old.changeCode==34,"Odin2 legacy codes");old.verify("318",MappingProfiles.OLD);
        MappingProfiles.Profile modern=MappingProfiles.byComponent("com.odin2.touchmapping","com.ro.mapping.service.ApiService");
        check(modern!=null&&modern.listCode==34&&modern.changeCode==35,"Odin2 modern codes");modern.verify("387",MappingProfiles.MODERN);
        MappingProfiles.Profile odin3=MappingProfiles.byComponent("com.odin.mapping","com.ro.mapping.service.ApiService");
        check(odin3!=null&&odin3.listCode==34&&odin3.changeCode==35,"Odin3 codes");odin3.verify("3.0(20251128_112846)",MappingProfiles.MODERN);
        rejects(()->odin3.verify("3.0(unknown)",MappingProfiles.MODERN));
        rejects(()->odin3.verify("3.0(20251128_112846)",MappingProfiles.OLD));
        rejects(()->old.verify("318",MappingProfiles.MODERN));
        rejects(()->modern.verify("387","untrusted.interface"));
        check(MappingProfiles.byComponent("com.rp.mapping","com.ro.mapping.service.ApiService")==null,"Retroid must stay diagnostic-only despite matching descriptor");
        MappingProfiles.Profile rp=MappingProfiles.DIAGNOSTIC_ONLY[0];
        rp.verify("3.0(20260615_164638)",MappingProfiles.MODERN);
        check(rp.pkg.equals("com.rp.mapping")&&rp.listCode==37&&rp.changeCode==38,"Retroid-specific code layout");
        check(MappingProfiles.byId(rp.id)==null,"Diagnostic profile must not become an exclusion owner");
        rejects(()->rp.verify("unknown",MappingProfiles.MODERN));
        check(MappingProfiles.byComponent("unknown.mapping","com.ro.mapping.service.ApiService")==null,"Unknown package admitted");
        check(MappingProfiles.byComponent("com.odin.mapping","unknown.Service")==null,"Unknown component admitted");
        check(MappingProfiles.byId("arbitrary")==null,"Persisted unknown adapter admitted");
        for(MappingProfiles.Profile p:MappingProfiles.SUPPORTED){check(MappingProfiles.byId(p.id)==p,"Adapter ID round-trip");check(p.changeCode==p.listCode+1,"Unexpected code pair");}
        System.out.println("PASS: "+checked+" adapter checks; Odin2 legacy/modern, pinned Odin3, descriptor/version rejection, Retroid collision and unknown endpoints.");
    }
}
