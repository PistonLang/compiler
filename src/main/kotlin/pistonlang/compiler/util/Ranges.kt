package pistonlang.compiler.util

operator fun IntRange.contains(other: IntRange) =
    this.first <= other.first && other.last <= this.last