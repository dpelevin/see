/*
 * Copyright 2012 Vasily Shiyan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package see.parser.grammar

import org.parboiled.scala._
import see.parser.numbers.NumberFactory
import see.parser.config.FunctionResolver
import see.tree.nodes.Untyped._

class AltExpressions(val numberFactory: NumberFactory,
                     val functions: FunctionResolver) extends Parser {

  val argumentSeparator = if (numberFactory.getDecimalSeparator != ',') "," else ";"
  val literals = AltLiterals(numberFactory.getDecimalSeparator)

  import literals._

  def fNode(name: String, args: Node*):Node = FNode(functions.get(name).asInstanceOf, args.toIndexedSeq)
  def op(body: Rule0) = (body ~> identity).terminal
  def repeatWithOperator(body: Rule1[Node], operator: Rule0) = rule {
    body ~ zeroOrMore(op(operator) ~ body ~~> ((a:Node, op, b) => fNode(op, a, b)))
  }
  def binOp(op: String)(a: Node, b: Node) = fNode(op, a, b)
  def seqNode(terms: Node*) = if (terms.size == 1) terms.head else fNode(";", terms:_*)


  def ReturnExpression = rule { ExpressionList ~ T("return") ~ RightExpression ~ optional(T(";")) ~~> (seqNode(_, _)) }


  def Block = rule { T("{") ~ ExpressionList ~ T("}") | Term }
  def ExpressionList = rule { zeroOrMore(Term) ~~> seqNode _ }
  def Term:Rule1[Node] = rule { Conditional | ForLoop | WhileLoop | TerminatedExpression }

  def Conditional = rule {
    T("if") ~ T("(") ~ RightExpression ~ T(")") ~ Block ~ optional(T("else") ~ Block) ~~>
    ((cond, then, elseOpt) => fNode("if", cond::then::elseOpt.toList:_*))
  }
  def ForLoop = rule {
    T("for") ~ T("(") ~ VarName ~ (T(":") | T("in")) ~ RightExpression ~ T(")") ~
      Block ~~>
      ((varName, target, body) => fNode("for", varName, target, ConstNode(body)))
  }
  def WhileLoop = rule { T("while") ~ T("(") ~ Expression ~ T(")") ~ Block ~~> binOp("while") }
  def TerminatedExpression = rule { Expression ~ T(";") }


  def Expression: Rule1[Node] = rule { Assignment | Binding | RightExpression }

  def Assignment = rule { optional(T("var")) ~ Settable ~ T("=") ~ Expression ~~> binOp("=") }
  def Settable = rule { SettableProperty | SettableVariable }
  def SettableProperty = rule { Atom ~ oneOrMore(optional(FunctionApplication) ~ PropertyChain) }
  def SettableVariable = rule { VarName ~~> (fNode("v=", _)) }

  def Binding = rule { SetterBinding | SignalCreation }
  def SetterBinding = rule { Settable ~ (T("<-") | T("<<")) ~ SignalExpression ~~> binOp("<-") }
  def SignalCreation = rule { Settable ~ T("<<=") ~ SignalExpression ~~> binOp("=") }
  def SignalExpression = rule { Expression ~~> (expr => fNode("signal", ConstNode(expr))) }

  def RightExpression = OrExpression

  def OrExpression = rule  { repeatWithOperator(EqualExpression, "||") }
  def AndExpression = rule  { repeatWithOperator(EqualExpression, "&&") }
  def EqualExpression = rule { repeatWithOperator(RelationalExpression, "!=" | "==") }
  def RelationalExpression = rule { repeatWithOperator(AdditiveExpression, "<=" | "<=" | "<" | ">") }
  def AdditiveExpression = rule { repeatWithOperator(MultiplicativeExpression, "+" | "-") }
  def MultiplicativeExpression = rule { repeatWithOperator(UnaryExpression, "*" | "/") }

  def UnaryExpression:Rule1[Node] = rule {
    op(anyOf("+-!")) ~ UnaryExpression ~~> (fNode(_, _)) | PowerExpression
  }

  def PowerExpression = rule { PropertyExpression ~ optional(T("^")) }


  def PropertyExpression = rule { Atom ~ zeroOrMore(FunctionApplication | PropertyChain) }

  def FunctionApplication = rule {
    T("(") ~ zeroOrMore(Expression, separator = argumentSeparator) ~ T(")") ~~>
      ((target:Node, args) => fNode("apply", target::args:_*))
  }
  def PropertyChain = rule {
    oneOrMore(SimpleProperty | IndexedProperty) ~~> ((target:Node, props) => fNode(".", PropertyNode(target, props)))
  }
  def SimpleProperty = rule { "." ~ (Identifier ~> PropertyDescriptor.simple _) }.terminal
  def IndexedProperty = rule { T("[") ~ RightExpression ~ T("]") ~~> PropertyDescriptor.indexed _ }


  def Atom = rule {
    Constant |
      SpecialForm |
      FunctionDefinition |
      Variable |
      JsonLiteral |
      T("(") ~ Expression ~ T(")")
  }


  def JsonLiteral = rule { ListLiteral | MapLiteral }
  def ListLiteral = rule {
    T("[") ~ zeroOrMore(Expression, separator = T(",")) ~ T("]") ~~> (fNode("[]", _:_*))
  }
  def MapLiteral = rule {
    T("{") ~ zeroOrMore(KeyValue, separator = T(",")) ~ T("}") ~~> (pairs => fNode("{}", pairs.flatten:_*))
  }
  def KeyValue = rule { (JsonKey | String) ~ T(":") ~ Expression ~~> (Seq(_, _)) }
  def JsonKey = rule { zeroOrMore(Letter | Digit) ~> ConstNode }.terminal


  def FunctionDefinition = rule {
    T("function") ~ T("(") ~ ArgumentDeclaration ~ T(")") ~ T("{") ~ ExpressionList ~ T("}") ~~>
      ((args, body) => fNode("functions", ConstNode(args), body))
  }


  def SpecialForm = rule { IsDefined | MakeSignal | Tree }
  def IsDefined = rule { T("isDefined") ~ T("(") ~ VarName ~ T(")") ~~> (fNode("isDefined", _)) }
  def MakeSignal = rule { T("signal") ~ T("(") ~ SignalExpression ~ T(")") }
  def Tree = rule { T("@tree") ~ Expression ~~> ConstNode.apply _ }


  def ArgSeparator = T(argumentSeparator)
  def ArgumentDeclaration = rule { zeroOrMore(op(Identifier), separator = ArgSeparator) }


  def Variable = rule { Identifier ~> VarNode }.terminal
  def VarName = rule { Identifier ~> ConstNode }.terminal
  def Constant = rule { String | Number | Boolean | Null }.suppressSubnodes.terminal

  def String = rule { StringLiteral ~> const(stripQuotes(_)) }
  def Number = rule { (FloatLiteral | IntLiteral) ~> const(numberFactory.getNumber(_)) }
  def Boolean = rule { BooleanLiteral ~> const(java.lang.Boolean.valueOf(_)) }
  def Null  = rule { NullLiteral ~> const(null) }
}
