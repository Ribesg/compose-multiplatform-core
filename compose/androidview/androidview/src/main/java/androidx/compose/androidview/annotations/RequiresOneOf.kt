package androidx.compose.androidview.annotations

@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class RequiresOneOf(vararg val properties: String)