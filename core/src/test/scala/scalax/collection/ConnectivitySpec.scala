package scalax.collection

import scala.util.Random
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.refspec.RefSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scalax.collection.OuterImplicits._
import scalax.collection.edges._
import scalax.collection.generic._
import scalax.collection.generator.RandomGraph.IntFactory
import scalax.collection.generator._
import scalax.collection.generic.GraphCoreCompanion
import scalax.collection.visualization.Visualizer
/* TODO L
import edge.Implicits._
 */

class ConnectivitySpec
    extends Suites(
      new Connectivity[immutable.Graph](immutable.Graph),
      new Connectivity[mutable.Graph](mutable.Graph)
    )

final class Connectivity[G[N, E <: Edge[N]] <: Graph[N, E] with GraphLike[N, E, G]](
    val factory: GraphCoreCompanion[G]
) extends RefSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with Visualizer[G] {

  implicit val config = PropertyCheckConfiguration(minSuccessful = 5, maxDiscardedFactor = 1.0)

  object `In a weakly connected diGraph` {
    import Data.elementsOfDi_1
    val g = factory(elementsOfDi_1: _*)

    def `there exists no pair of mutually reachable nodes`: Unit =
      given(g) { case g: Graph[Int, DiEdge[Int]] => // `annotated for IntelliJ
        g.nodes.toList.combinations(2) foreach {
          case List(a, b) => List(a pathTo b, b pathTo a) should contain(None)
          case _          => fail()
        }
      }

    def `evaluating strong components from any node yields single-node components`: Unit =
      given(g) { case g: Graph[Int, DiEdge[Int]] => // `annotated for IntelliJ
        g.nodes foreach { n =>
          val components = n.innerNodeTraverser.strongComponents
          components foreach (_.nodes should have size 1)
        }
      }

    def `evaluating all strong components yields a component for every node`: Unit =
      given(g) { case g: Graph[Int, DiEdge[Int]] => // `annotated for IntelliJ
        g.strongComponentTraverser().size shouldBe g.order
      }
  }

  object `Having two strong components` {
    // see example on https://de.wikipedia.org/wiki/Algorithmus_von_Tarjan_zur_Bestimmung_starker_Zusammenhangskomponenten
    val sccExpected = Vector[G[Char, DiEdge[Char]]](
      factory('a' ~> 'b', 'b' ~> 'c', 'c' ~> 'd', 'd' ~> 'a', 'd' ~> 'e', 'c' ~> 'e', 'e' ~> 'c'),
      factory('f' ~> 'g', 'g' ~> 'f', 'g' ~> 'h', 'h' ~> 'j', 'j' ~> 'i', 'i' ~> 'g', 'i' ~> 'f', 'f' ~> 'i')
    )
    val sccExpectedG: Vector[Graph[Char, DiEdge[Char]]] = sccExpected // for IntelliJ

    assert(sccExpected.size == 2)
    assert(sccExpectedG(0).intersect(sccExpectedG(1)).isEmpty)

    def `each is detected as such`: Unit =
      sccExpected.foreach(g =>
        given(g) { case g: Graph[Char, DiEdge[Char]] => // `annotated for IntelliJ
          g.strongComponentTraverser() should have size 1
        }
      )

    def `connected by a diEdge yields a graph with the very same two strong components`: Unit = {
      val r     = new Random
      val union = sccExpected.foldLeft(factory.empty[Char, DiEdge[Char]])((r, g) => g union r)
      val connectors = {
        def pickNode(index: Int) = sccExpected(index).nodes.draw(r).outer
        for (i <- 1 to 10) yield pickNode(0) ~> pickNode(1)
      }
      connectors foreach { connector =>
        val connected = union concat List(connector)
        def check(scc: Iterable[connected.Component], expectedSize: Int): Unit = {
          scc should have size expectedSize
          scc foreach { sc =>
            given(sc.to(factory)) { g =>
              sccExpected should contain(g)
              sc.frontierEdges should have size 1
            }
          }
        }

        check(connected.strongComponentTraverser().toVector, 2)

        val start = connected.nodes.draw(r)
        check(
          start.innerNodeTraverser.strongComponents.toVector,
          if (sccExpected(0) contains start) 2 else 1
        )
      }
    }
  }

  object `Having two weak components` {
    def `weak components are detected, fix #57`: Unit =
      given(factory(11 ~> 12, 13 ~> 14)) { case g: Graph[Int, DiEdge[Int]] => // `annotated for IntelliJ
        g.componentTraverser() should have size 2
      }
  }

  object `Having a bigger graph` {
    val g: G[Int, DiEdge[Int]] = {
      val gOrder = 1000
      val random = RandomGraph.diGraph(
        factory,
        new IntFactory {
          val order       = gOrder
          val nodeDegrees = NodeDegreeRange(gOrder / 10, gOrder / 4)
        }
      )
      random.draw
    }
    lazy val strongComponents = g.strongComponentTraverser().toVector

    def `no stack overflow occurs`: Unit =
      given(g)(_ => strongComponents)

    def `strong components are complete`: Unit =
      given(g) { _ =>
        strongComponents.foldLeft(Set.empty[g.NodeT])((cum, sc) => cum ++ sc.nodes) shouldBe g.nodes
      }

    def `strong components are proper`: Unit =
      given(g) { _ =>
        val maxProbes = 10
        val arbitraryNodes: Vector[Set[g.NodeT]] = strongComponents map { sc =>
          val nodes = sc.nodes
          if (nodes.size <= maxProbes) nodes
          else {
            val every = nodes.size / maxProbes
            nodes.zipWithIndex withFilter { case (_, i) => i % every == 0 } map (_._1)
          }
        }
        arbitraryNodes foreach { nodes =>
          def checkBiConnected(n1: g.NodeT, n2: g.NodeT) =
            if (n1 ne n2) {
              n1 pathTo n2 shouldBe defined
              n2 pathTo n1 shouldBe defined
            }

          nodes.sliding(2) foreach { pairOrSingle =>
            pairOrSingle.toList match {
              case List(n1, n2) => checkBiConnected(n1, n2)
              case n :: Nil     => checkBiConnected(n, nodes.head)
              case _            => fail()
            }
          }
        }
        arbitraryNodes.sliding(2) foreach { pairOrSingle =>
          def checkNonBiConnected(ns1: Set[g.NodeT], ns2: Set[g.NodeT]): Unit =
            if (ns1 ne ns2) {
              ns1 zip ns2 foreach { case (n1, n2) =>
                (n1 pathTo n2) shouldBe defined
                (n2 pathTo n1) should not be defined
              }
            }

          pairOrSingle.toList match {
            case List(ns1, ns2) => checkNonBiConnected(ns1, ns2)
            case ns :: Nil      => checkNonBiConnected(ns, arbitraryNodes.head)
            case _              => fail()
          }
        }
      }
  }
}
