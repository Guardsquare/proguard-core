/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package proguard.exception;

/** Class holding all the error ids for exceptions occurring in the program. */
public final class ErrorId {
  // this id is for testing purposes, to be removed at the end of T20409
  public static int TEST_ID = 99;

  // Partial evaluator exceptions: Range 1000 - 2000
  public static final int VARIABLE_EMPTY_SLOT = 1_000;
  public static final int VARIABLE_INDEX_OUT_OF_BOUND = 1_001;
  public static final int VARIABLE_TYPE = 1_002;
  public static final int EXCESSIVE_COMPLEXITY = 1_003;
  public static final int INCOMPLETE_CLASS_HIERARCHY = 1_004;
  public static final int EMPTY_CODE_ATTRIBUTE = 1_005;
  public static final int STACK_TYPE = 1_006;
  public static final int STACK_CATEGORY_ONE = 1_007;
  public static final int ARRAY_INDEX_OUT_OF_BOUND = 1_008;
  public static final int EXPECTED_ARRAY = 1_009;
  public static final int VARIABLE_GENERALIZATION = 1_010;
  public static final int STACK_GENERALIZATION = 1_011;
  public static final int ARRAY_STORE_TYPE_EXCEPTION = 1_012;
  public static final int PARTIAL_EVALUATOR_ERROR1 = 1_098;
  public static final int PARTIAL_EVALUATOR_ERROR2 = 1_099;
  public static final int NEGATIVE_STACK_SIZE = 2_000;

  public static final int MAX_STACK_SIZE_COMPUTER_ERROR = 3_000;

  public static final int CLASS_REFERENCE_FIXER_ERROR = 4_000;

  public static final int CODE_ATTRIBUTE_EDITOR_ERROR = 5_000;

  public static final int CODE_PREVERIFIER_ERROR = 6_000;

  public static final int CODE_SUBROUTINE_INLINER_ERROR = 7_000;

  // Proguard Util Exceptions
  public static final int WILDCARD_WRONG_INDEX = 8_000;

  /**
   * Group of errors.
   *
   * @see proguard.analysis
   */
  public static final int ANALYSIS_CALL_RESOLVER_SELECTIVE_PARAMETER_RECONSTRUCTION_MISSING_PARAMS =
      9_006;

  public static final int ANALYSIS_JVM_SHALLOW_HEAP_STATE_INCOMPATIBLE = 9_007;
  public static final int ANALYSIS_DOMINATOR_CALCULATOR_NO_DOMINATOR_AT_OFFSET = 9_008;
  public static final int ANALYSIS_STACK_STATE_OPERAND_STACK_INDEX_OUT_OF_BOUNDS = 9_010;
  public static final int
      ANALYSIS_JVM_INTRAPROCEDURAL_CFA_FILLER_ALL_INSTRUCTION_UNEXPECTED_SWITCH = 9_012;
  public static final int ANALYSIS_JVM_MEMORY_LOCATION_TRANSFER_RELATION_TYPE_UNSUPPORTED = 9_014;
  public static final int ANALYSIS_JVM_INVOKE_TAINT_SINK_MISSING_TAINT = 9_023;
  public static final int ANALYSIS_JVM_VALUE_TRANSFER_RELATION_INCORRECT_PARAMETER_COUNT = 9_030;
  public static final int ANALYSIS_VALUE_ABSTRACT_STATE_CONDITION_UNCHECKED = 9_031;
  public static final int ANALYSIS_JVM_DEFAULT_EXPAND_OPERATOR_RETURN_INSTRUCTION_EXPECTED = 9_038;
  public static final int ANALYSIS_JVM_DEFAULT_EXPAND_OPERATOR_EXIT_NODE_EXPECTED = 9_039;
  public static final int
      ANALYSIS_JVM_DEFAULT_EXPAND_OPERATOR_MISSING_EXPECTED_CATCH_NODE_EXPECTED = 9_040;
  public static final int ANALYSIS_JVM_TRANSFER_RELATION_INSTRUCTION_PUSH_COUNT_HIGHER_THAN_TWO =
      9_042;
  public static final int ANALYSIS_JVM_TRANSFER_RELATION_UNEXPECTED_UNKNOWN_SIGNATURE = 9_043;
  public static final int
      ANALYSIS_JVM_TRANSFER_RELATION_CONSTANT_INSTRUCTION_VISITOR_OPCODE_UNSUPPORTED = 9_044;

  public static final int OBJECT_GET_CLASS_EXECUTOR_UNSUPPORTED_SIGNATURE = 9_047;

  public static final int SIGNATURE_AST_INVALID_STRUCTURE = 10_000;

  /** Private constructor to prevent instantiation of the class. */
  private ErrorId() {}
}
