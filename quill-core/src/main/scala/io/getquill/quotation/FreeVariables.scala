package io.getquill.quotation

import io.getquill.ast._

case class State(seen: Set[Ident], free: Set[Ident])

case class FreeVariables(state: State)
    extends StatefulTransformer[State] {

  override def apply(ast: Ast): (Ast, StatefulTransformer[State]) =
    ast match {
      case ident: Ident if (!state.seen.contains(ident)) =>
        (ident, FreeVariables(State(state.seen, state.free + ident)))
      case f @ Function(params, body) =>
        val (_, t) = FreeVariables(State(state.seen ++ params, state.free))(body)
        (f, FreeVariables(State(state.seen, state.free ++ t.state.free)))
      case OptionOperation(t, a, b, c) =>
        (ast, free(a, b, c))
      case other =>
        super.apply(other)
    }

  override def apply(query: Query): (Query, StatefulTransformer[State]) =
    query match {
      case q @ Filter(a, b, c)  => (q, free(a, b, c))
      case q @ Map(a, b, c)     => (q, free(a, b, c))
      case q @ FlatMap(a, b, c) => (q, free(a, b, c))
      case q @ SortBy(a, b, c)  => (q, free(a, b, c))
      case q @ GroupBy(a, b, c) => (q, free(a, b, c))
      case q @ OuterJoin(t, a, b, iA, iB, on) =>
        val (_, freeA) = apply(a)
        val (_, freeB) = apply(a)
        val (_, freeOn) = FreeVariables(State(state.seen + iA + iB, Set.empty))(on)
        (q, FreeVariables(State(state.seen, state.free ++ freeA.state.free ++ freeB.state.free ++ freeOn.state.free)))
      case _: Entity | _: Reverse | _: Take | _: Drop | _: Union | _: UnionAll | _: Aggregation =>
        super.apply(query)
    }

  private def free(a: Ast, ident: Ident, c: Ast) = {
    val (_, ta) = apply(a)
    val (_, tc) = FreeVariables(State(state.seen + ident, state.free))(c)
    FreeVariables(State(state.seen, state.free ++ ta.state.free ++ tc.state.free))
  }
}

object FreeVariables {
  def apply(ast: Ast): Set[Ident] =
    new FreeVariables(State(Set(), Set()))(ast) match {
      case (_, transformer) =>
        transformer.state.free
    }
}
