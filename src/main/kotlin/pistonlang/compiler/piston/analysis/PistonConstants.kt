package pistonlang.compiler.piston.analysis

class PistonConstants(
    val baseScope: Scope,
    val emptyImportData: ImportData,
    val errorSupertypeData: SupertypeData,
    val emptySupertypeData: SupertypeData,
    val errorReturnData: ReturnData,
    val unitReturnData: ReturnData,
    val emptyParamData: ParamData,
    val emptyTypeBoundData: TypeBoundData,
)