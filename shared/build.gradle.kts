import org.kingdoms.main.KingdomsGradleCommons

plugins {
    commons
}

group = "org.kingdoms.shared"
version = "1.0.0"

// [19:49:42] [Server thread/ERROR]: Error occurred while enabling Kingdoms v1.16.20.1 (Is it up to date?)
//  java.lang.IncompatibleClassChangeError: Method 'void org.kingdoms.main.permissions.registry.PermissionRegistry.registerAllRegistries()' must be InterfaceMethodref constant
//	at org.kingdoms.main.Kingdoms.onEnable(Kingdoms.java:335) ~[?:?]

dependencies {
    compileOnly(KingdomsGradleCommons.XSERIES)
}