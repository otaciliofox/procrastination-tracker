# R8 rules for the watch app.
#
# Deliberately the same enum and entity rules as the phone module: both apps read and write the
# same Room schema and the same Data Layer payloads, so a rule that exists on only one side would
# produce exactly the failure it was meant to prevent -- one device writing names the other cannot
# resolve.

# Enum constants are persisted by name, in Room columns and in sync payloads. See the phone
# module's rules for the full reasoning.
-keepclassmembers enum com.foxlab.procrastinationtracker.core.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public java.lang.String name();
}
-keepclassmembers enum com.foxlab.procrastinationtracker.trackerdata.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public java.lang.String name();
}

# Room entities: field names are column names, and the generated DAOs construct them reflectively.
-keep class com.foxlab.procrastinationtracker.trackerdata.entity.** { *; }
