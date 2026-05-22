# Tinyflow-java ChangeLog

## v2.0.5 20260522
- feat(core): add configurable state update timeout and retry delay settings
- docs(ChainExecutor): add comprehensive JavaDoc comments for execution methods
- feat(engine): add environment support for chain execution 
- refactor(chain): wrap state update methods with distributed lock mechanism
- refactor(core): replace computeIfAbsent with MapUtil helper method


## v2.0.4 20260426
- feat(chain): add FORM ref type support for user input validation
- fix(node): update ConfirmNode to use FORM refType instead of INPUT
- refactor(parser): update form parameter handling logic
- feat(parser): set default values for input parameters 
- fix(chain): adjust input validation logic in ChainState
- refactor(chain): optimize chain executor event handling 
- refactor(core): simplify chain execution logic by extracting startOrResume method
- feat(chain): add resume functionality and improve chain execution


## v2.0.3 20260417
- refactor(core): simplify concurrent imports and improve exception handling in ChainExecutor
- refactor(core): optimize event manager listener operations
- refactor(core): add chain state ID to ChainSuspendException
- refactor(chain): optimize chain executor event handling logic
- refactor(chain): update ChainStatus enum with correct method references
- refactor(chain): preserve original exception type in ChainExecutor
- feat(chain): add chain suspension handling in ChainExecutor