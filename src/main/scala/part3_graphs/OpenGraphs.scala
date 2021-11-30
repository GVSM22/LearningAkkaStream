package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Sink, Source}

object OpenGraphs extends App {

  implicit val system: ActorSystem = ActorSystem("OpenGraphs")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  val firstSource = Source(1 to 10)
  val secondSource = Source(42 to 1000)

  val sourceGraph = Source.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._
      val concat = builder.add(Concat[Int](2))

      firstSource ~> concat
      secondSource ~> concat

      SourceShape(concat.out)
    }
  )

  val sink1 = Sink.foreach[Int](x => println(s"Meaningful thing 1: $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Meaningful thing 2: $x"))

  val sinkGraph = Sink.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      broadcast ~> sink1
      broadcast ~> sink2

      SinkShape(broadcast.in)
    }
  )

  val plusOne = Flow[Int].map(_+1)
  val timesTen = Flow[Int].map(_*10)
  // step 1
  val flowGraph = Flow.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._

      // everything operates on SHAPES
      // step 2 - defining auxiliary shapes
      val plusOneShape = builder.add(plusOne)
      val timesTenShape = builder.add(timesTen)
      // step 3 - connect the shapes
      plusOneShape ~> timesTenShape

      FlowShape(plusOneShape.in, timesTenShape.out)
    } // static graph
  ) // component

  def createFlow[T](in: Source[T, NotUsed], out: Sink[T, NotUsed]): Flow[T, T, NotUsed] = {
    Flow.fromGraph(
      GraphDSL.create(){ implicit builder =>
        import GraphDSL.Implicits._
        val inShape = builder.add(in)
        val outShape = builder.add(out)

        inShape ~> outShape

        FlowShape(outShape.in, inShape.out)
      }
    )
  }

}
