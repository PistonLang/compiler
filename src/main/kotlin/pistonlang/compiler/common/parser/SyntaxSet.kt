package pistonlang.compiler.common.parser

import pistonlang.compiler.common.language.SyntaxType
import java.util.*

@JvmInline
value class SyntaxSet<T> internal constructor(private val set: BitSet) where T : SyntaxType, T : Enum<T> {
    operator fun contains(item: T): Boolean = set[item.ordinal]

    infix fun union(other: SyntaxSet<T>): SyntaxSet<T> {
        val new = BitSet(set.size())
        0.until(set.size()).forEach {
            new[it] = set[it] || other.set[it]
        }
        return SyntaxSet(new)
    }

    infix fun intersect(other: SyntaxSet<T>): SyntaxSet<T> {
        val new = BitSet(set.size())
        0.until(set.size()).forEach {
            new[it] = set[it] && other.set[it]
        }
        return SyntaxSet(new)
    }
}

fun <T> syntaxSet(clazz: Class<T>, vararg members: T): SyntaxSet<T> where T : SyntaxType, T : Enum<T> {
    val set = BitSet(clazz.enumConstants.size)
    members.forEach {
        set[it.ordinal] = true
    }
    return SyntaxSet(set)
}

inline fun <reified T> syntaxSet(vararg members: T) where T : SyntaxType, T : Enum<T> =
    syntaxSet(T::class.java, *members)