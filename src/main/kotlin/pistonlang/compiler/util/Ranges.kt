package pistonlang.compiler.util

operator fun IntRange.contains(other: IntRange) =
    this.first <= other.first && other.last <= this.last

infix fun IntRange.isBefore(other: IntRange) =
    this.first < other.first || (this.first == other.first && this.last < other.last)