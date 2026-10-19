# Project-specific ProGuard / R8 rules for maximum release compression
-repackageclasses ''
-allowaccessmodification

# Remove Kotlin intrinsics null checks to shrink bytecode in release
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNullParameter(...);
    public static void checkNotNull(...);
    public static void checkExpressionValueIsNotNull(...);
}