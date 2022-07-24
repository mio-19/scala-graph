package scalax.collection.generic

trait Label[L] { this: Edge[_] =>
  def label: L
}

object Label {
  def unapply[L](edge: Edge[_] with Label[L]): Option[L] = Some(edge.label)
}

/** Intermediate infix extractor for the "unlabeled part" of a weighted edge.
  * It is to be combined with `:~` or `:~>` like `case a :~ b % w`.
  */
case object % {
  def unapply[N](nw: (N, Double)): Option[(N, Double)] = Some(nw._1, nw._2)
}

trait UnapplyLabeledEdge[N, E <: Edge[N], L] {
  def unapply(edge: E): Option[(N, (N, L))] = Some(edge._1 -> (edge._2, label(edge)))
  protected def label(edge: E): L
}

trait UnapplyLabel[N, L] {
  def unapply(rest: (N, L)): Option[(N, L)] = Some(rest)
}

trait UnapplyGenericLabeledEdge[E[X] <: AnyEdge[X], L] {
  def unapply[N](edge: E[N] with Label[L]): Option[(N, (N, L))] = Some(edge._1 -> (edge._2, label(edge)))
  protected def label[N](edge: E[N] with Label[L]): L           = edge.label
}

trait UnapplyGenericLabeledHyperEdge[E[X] <: AbstractHyperEdge[X], L] {
  def unapply[N](hyperedge: E[N] with Label[L]): Option[(Ends[N], L)] =
    Some(hyperedge.ends, label(hyperedge))

  protected def label[N](edge: E[N] with Label[L]): L = edge.label
}

trait UnapplyGenericLabeledDiHyperEdge[E[X] <: AnyDiHyperEdge[X], L] {
  def unapply[N](diHyperedge: E[N] with Label[L]): Option[(Seq[N], (Seq[N], L))] =
    Some(diHyperedge.sources.toSeq -> (diHyperedge.targets.toSeq, label(diHyperedge)))

  protected def label[N](edge: E[N] with Label[L]): L = edge.label
}

trait UnapplyGenericLabel[L] {
  def unapply[N](rest: (N, L)): Option[(N, L)] = Some(rest)
}

trait UnapplyGenericHyperLabel[L] {
  def unapply[N](rest: (Seq[N], L)): Option[(Seq[N], L)] = Some(rest)
}

trait ExtendedKeyByWeight extends ExtendedKey { this: Edge[_] =>
  override def extendKeyBy: Seq[Double] = weight :: Nil
}
