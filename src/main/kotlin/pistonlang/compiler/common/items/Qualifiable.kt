package pistonlang.compiler.common.items

import pistonlang.compiler.common.main.MainInterners

interface Qualifiable {
    fun qualify(interners: MainInterners): String
}

fun Pair<Qualifiable, Qualifiable>.qualify(interners: MainInterners) =
    "${first.qualify(interners)}: ${second.qualify(interners)}"

@JvmName("qualifyFirst")
fun Pair<Qualifiable, Any>.qualify(interners: MainInterners) =
    "${first.qualify(interners)}: $second"

@JvmName("qualifyList")
fun Pair<Qualifiable, Iterable<Qualifiable>>.qualify(interners: MainInterners) =
    "${first.qualify(interners)}: ${second.qualify(interners)}"

@JvmName("qualifyDependentPair")
fun Pair<Qualifiable, Dependent<*, Qualifiable>>.qualify(interners: MainInterners) =
    "${first.qualify(interners)}: ${second.qualify(interners)}"

@JvmName("qualifyDependentListPair")
fun Pair<Qualifiable, Dependent<*, Iterable<Qualifiable>>>.qualify(interners: MainInterners) =
    "${first.qualify(interners)}: ${second.qualify(interners)}"

fun Iterable<Qualifiable>.qualify(interners: MainInterners): String =
    joinToString(prefix = "[", postfix = "]") { it.qualify(interners) }

@JvmName("qualifyQualifiable")
fun Dependent<*, Qualifiable>.qualify(interners: MainInterners): String =
    "Dependent(${dependencies.qualify(interners)}, ${data.qualify(interners)})"

@JvmName("qualifyList")
fun Dependent<*, Iterable<Qualifiable>>.qualify(interners: MainInterners): String =
    "Dependent(${dependencies.qualify(interners)}, ${data.qualify(interners)})"

fun Dependent<*, *>.qualify(interners: MainInterners): String =
    "Dependent(${dependencies.qualify(interners)}, ${data})"