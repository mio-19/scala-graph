package scalax.collection.constrained

import scala.annotation.unchecked.{uncheckedVariance => uV}
import scala.collection.Set
import scala.reflect.ClassTag

import scalax.collection.GraphPredef.{EdgeLikeIn, InParam, Param}
import scalax.collection.{Graph => SimpleGraph}
import scalax.collection.constrained.generic.GraphConstrainedCompanion

// ----------------------------------------------------------------------------
/** A trait for dynamically constrained graphs.
  *
  * @tparam N    the type of the nodes (vertices) in this graph.
  * @tparam E    the kind of the edges in this graph.
  * @author Peter Empen
  */
trait Graph[N, E[+X] <: EdgeLikeIn[X]] extends Set[Param[N, E]] with SimpleGraph[N, E] with GraphLike[N, E, Graph] {
  override def empty: Graph[N, E] = Graph.empty[N, E]
}

/** Default factory for constrained graphs.
  * Graph instances returned from this factory will be immutable.
  *
  * @author Peter Empen
  */
object Graph extends GraphConstrainedCompanion[Graph] {
  override def newBuilder[N, E[+X] <: EdgeLikeIn[X]](implicit edgeT: ClassTag[E[N]], config: Config) =
    immutable.Graph.newBuilder[N, E](edgeT, config)

  def empty[N, E[+X] <: EdgeLikeIn[X]](implicit edgeT: ClassTag[E[N]], config: Config = defaultConfig): Graph[N, E] =
    immutable.Graph.empty[N, E](edgeT, config)
  def from[N, E[+X] <: EdgeLikeIn[X]](nodes: Iterable[N], edges: Iterable[E[N]])(implicit
      edgeT: ClassTag[E[N]],
      config: Config = defaultConfig
  ): Graph[N, E] =
    immutable.Graph.from[N, E](nodes, edges)(edgeT, config)
  override protected[collection] def fromWithoutCheck[N, E[+X] <: EdgeLikeIn[X]](
      nodes: Iterable[N],
      edges: Iterable[E[N]]
  )(implicit edgeT: ClassTag[E[N]], config: Config = defaultConfig): Graph[N, E] =
    immutable.Graph.fromWithoutCheck[N, E](nodes, edges)(edgeT, config)
}

trait UserConstrainedGraph[N, E[+X] <: EdgeLikeIn[X], +G <: Graph[N, E]] { _: Graph[N, E] with Constrained[N, E, G] =>
  val constraint: Constraint[N, E, G] @uV

  private type C_NodeT = constraint.self.NodeT
  private type C_EdgeT = constraint.self.EdgeT

  override def preCreate(nodes: Iterable[N], edges: Iterable[E[N]]) =
    constraint preCreate (nodes, edges)
  override def preAdd(node: N)               = constraint preAdd node
  override def preAdd(edge: E[N])            = constraint preAdd edge
  override def preAdd(elems: InParam[N, E]*) = constraint preAdd (elems: _*)
  override def postAdd(
      newGraph: G @uV,
      passedNodes: Iterable[N],
      passedEdges: Iterable[E[N]],
      preCheck: PreCheckResult
  ) =
    constraint postAdd (newGraph, passedNodes, passedEdges, preCheck)

  override def preSubtract(node: self.NodeT, forced: Boolean) =
    constraint preSubtract (node.asInstanceOf[C_NodeT], forced)
  override def preSubtract(edge: self.EdgeT, simple: Boolean) =
    constraint preSubtract (edge.asInstanceOf[C_EdgeT], simple)

  override def preSubtract(nodes: => Set[self.NodeT], edges: => Set[self.EdgeT], simple: Boolean) =
    constraint preSubtract (nodes.asInstanceOf[Set[C_NodeT]],
    edges.asInstanceOf[Set[C_EdgeT]],
    simple)

  override def postSubtract(
      newGraph: G @uV,
      passedNodes: Iterable[N],
      passedEdges: Iterable[E[N]],
      preCheck: PreCheckResult
  ) =
    constraint postSubtract (newGraph, passedNodes, passedEdges, preCheck)
}
