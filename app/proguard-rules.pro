# R8 rules for the phone app.
#
# Most of what this project uses ships its own consumer rules (Room, Hilt, Compose, Play Services),
# so the list here is short on purpose -- every extra `-keep` is shrinking given away. What follows
# is the part R8 cannot infer.

# Enum constants are persisted *by name*: as Room column values (see Converters and SessionEntity)
# and inside Wearable Data Layer payloads (TrackerSyncCodec, WearSyncListenerService). Letting R8
# rename them would make an existing database unreadable and stop the two devices from agreeing on
# what a value means -- and it would fail silently, because both sides would simply fall back to a
# default instead of throwing.
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

# Room entities are constructed reflectively by the generated DAOs, and their field names are the
# column names. Room's own rules cover the generated code, not the entities themselves.
-keep class com.foxlab.procrastinationtracker.trackerdata.entity.** { *; }
-keep class com.foxlab.procrastinationtracker.data.SessionEntity { *; }
