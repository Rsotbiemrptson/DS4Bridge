package com.odin2.ds4bridgehelper;

/** A shared descriptor does not imply shared transaction codes. */
final class MappingProfiles {
    static final String OLD="com.odin2.touchmapping.sdk.IServerApi",MODERN="com.ro.mapping.sdk.IServerApi";
    static final class Profile {
        final String id,label,pkg,service,descriptor,version;
        final int listCode,changeCode;
        Profile(String id,String label,String pkg,String service,String descriptor,String version,int listCode,int changeCode){
            this.id=id;this.label=label;this.pkg=pkg;this.service=service;this.descriptor=descriptor;this.version=version;this.listCode=listCode;this.changeCode=changeCode;
        }
        boolean supportsVersion(String actual){return version==null||version.equals(actual);}
        void verify(String actualVersion,String actualDescriptor){
            if(!supportsVersion(actualVersion))throw new IllegalArgumentException("Unverified mapping version: "+actualVersion+" for "+label);
            if(!descriptor.equals(actualDescriptor))throw new IllegalArgumentException("Unexpected mapping interface for "+label+": "+actualDescriptor);
        }
    }
    static final Profile[] SUPPORTED={
        new Profile("odin2-modern","Odin 2 (modern)","com.odin2.touchmapping","com.ro.mapping.service.ApiService",MODERN,null,34,35),
        new Profile("odin2-legacy","Odin 2 (legacy)","com.odin2.touchmapping","com.odin2.touchmapping.service.ApiService",OLD,null,33,34),
        new Profile("odin3-20251128","Odin 3","com.odin.mapping","com.ro.mapping.service.ApiService",MODERN,"3.0(20251128_112846)",34,35)
    };
    // Server-side blacklist methods confirmed offline; no exclusion writes enabled yet.
    static final Profile[] DIAGNOSTIC_ONLY={
        new Profile("rp6-20260615-diagnostic","Retroid RP6 (diagnostic only)","com.rp.mapping","com.ro.mapping.service.ApiService",MODERN,"3.0(20260615_164638)",37,38)
    };
    static Profile byId(String id){for(Profile p:SUPPORTED)if(p.id.equals(id))return p;return null;}
    static Profile byComponent(String pkg,String service){for(Profile p:SUPPORTED)if(p.pkg.equals(pkg)&&p.service.equals(service))return p;return null;}
    private MappingProfiles(){}
}
