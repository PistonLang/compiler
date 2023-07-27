package pistonlang.compiler.common.items

import kotlinx.collections.immutable.PersistentMap

data class VirtualMembers(
    val functions: Map<String, List<MemberId>>,
    val getters: Map<String, List<MemberId>>,
    val setters: Map<String, List<MemberId>>,
    val overriders: PersistentMap<MemberId, MemberId>,
    val overrides: Map<MemberId, List<MemberId>>,
    val unimplemented: Set<MemberId>
)