package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

import java.util.Date

object MoreOpenGraphs extends App {

  implicit val system: ActorSystem = ActorSystem("MoreOpenGraphs")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  /*
    Example: Max3 operator
    - 3 inputs of type int
    - the maximum of the 3
   */

  val max3staticGraph = GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._

    val max1 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))
    val max2 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))

    max1.out ~> max2.in0
    UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
  }

  val source1 = Source(1 to 10)
  val source2 = Source(Vector.iterate(1, 10)(_ => 5))
  val source3 = Source((1 to 10).reverse)

  val maxSink = Sink.foreach[Int](x => println(s"Max is $x"))
  val max3RunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._

      val max3Shape = builder.add(max3staticGraph)
      source1 ~> max3Shape.in(0)
      source2 ~> max3Shape.in(1)
      source3 ~> max3Shape.in(2)
      max3Shape.out ~> maxSink
      ClosedShape
    }
  )

  /*
    Non-uniform fan out shape

    Processing bank transactions
    Transaction suspicious if amount > 10.000

    Streams component for transactions
    - output1: let the transaction go through
    - output2: suspicious transactions IDs
   */

  case class Transaction(id: String, source: String, recipient: String, amount: Int, date: Date)

  val transactionSource = Source(List(
    Transaction("42094029", "Paul", "Jim", 100, new Date()),
    Transaction("42020421", "Daniel", "Jim", 10001, new Date()),
    Transaction("10201230", "Jim", "Alice", 7000, new Date())
  ))

  val bankProcessor = Sink.foreach[Transaction](println)
  val suspiciousAnalysisService = Sink.foreach[String](id => println(s"Suspicious transaction id: $id"))

  val suspiciousTransactionStaticGraph = GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._

    val broadcast = builder.add(Broadcast[Transaction](2))
    val suspiciousTransactionFilter = builder.add(Flow[Transaction].filter(t => t.amount > 10000))
    val transactionIdExtractor = builder.add(Flow[Transaction].map[String](_.id))

    broadcast.out(0) ~> suspiciousTransactionFilter ~> transactionIdExtractor
    new FanOutShape2(broadcast.in, transactionIdExtractor.out, broadcast.out(1))
  }

  val suspiciousTransactionRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._

      val suspiciousTransactionShape = builder.add(suspiciousTransactionStaticGraph)

      transactionSource ~> suspiciousTransactionShape.in
      suspiciousTransactionShape.out0 ~> suspiciousAnalysisService
      suspiciousTransactionShape.out1 ~> bankProcessor
      ClosedShape
    }
  )

  suspiciousTransactionRunnableGraph.run()
}
