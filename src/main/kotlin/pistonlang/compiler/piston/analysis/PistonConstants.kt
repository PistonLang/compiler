package pistonlang.compiler.piston.analysis

import pistonlang.compiler.common.language.ReturnData
import pistonlang.compiler.common.language.SupertypeData
import pistonlang.compiler.piston.parser.PistonType

class PistonConstants(
    val baseScope: Scope,
    val errorSupertypeData: SupertypeData<PistonType>,
    val emptySupertypeData: SupertypeData<PistonType>,
    val unitReturnData: ReturnData<PistonType>,
)