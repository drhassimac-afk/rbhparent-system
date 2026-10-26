# Keep app classes
-keep class com.rbh.parental.** { *; }
-keep class com.rbh.child.** { *; }

# Keep accessibility services
-keep class com.rbh.parental.services.** { *; }
-keep class com.rbh.child.ChildBlockerService { *; }
