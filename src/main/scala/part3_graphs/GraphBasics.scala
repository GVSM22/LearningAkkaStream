package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Concat, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

import scala.language.postfixOps

object GraphBasics extends App {

  implicit val system: ActorSystem = ActorSystem("GraphBasics")
  implicit val mat: ActorMaterializer = ActorMaterializer()


  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(_ + 1)
  val multiplier = Flow[Int].map(_ * 10)
  val output = Sink.foreach[(Int, Int)](println)

  // step 1 - setting up the fundamentals for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder: GraphDSL.Builder[NotUsed] => // builder => shape; builder is a MUTABLE data structure
      import GraphDSL.Implicits._ // brings some nice operators into scope

      // step 2 - add the necessary components of this graph
      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator (1 input, 2 output)
      val zip = builder.add(Zip[Int, Int]) // fan-in operator (2 input, 1 output)

      // step 3 - tying up the components
      input ~> broadcast // input feeds into broadcast

      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output

      // step 4 - return a closed shape
      ClosedShape // FREEZE the builder's shape, make the builder IMMUTABLE
    } // inert/static graph
  ) // runnable graph

//  graph.run() // run the graph and materialize it

  /**
   * Exercise 1: feed a source into 2 sinks at the same time
   */
  val simpleOutput = Sink.foreach[String](s => println(s"${Console.RESET} Simple output: $s"))
  val gourmetOutput = Sink.foreach[String](s => println(s"${Console.BLUE} Gourmet output: $s"))

  val numbersGraph = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[String](2))

      input ~> Flow[Int].map(_.toString) ~> broadcast ~> simpleOutput // implicit port numbering
                                            broadcast ~> gourmetOutput

      ClosedShape
    }
  )

//  numbersGraph.run()

  /**
   * Exercise 2
   */

  import scala.concurrent.duration._
  val fastSource = Source((1 to 1000).map(n => s"FastSource-$n")).throttle(5, 1 second)
  val slowSource = Source((1 to 1000).map(n => s"SlowSource-$n")).throttle(2, 1 second)

  val sink = (name: String) => Sink.fold[Int, String](0)((count, elem) => {
    println(s"$name received element: $elem, and has a total of $count elements!")
    count + 1
  })
Concat
  val crazyGraph = RunnableGraph.fromGraph(
    GraphDSL.create(){ implicit builder =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[String](2))
      val balance = builder.add(Balance[String](2))

      fastSource ~> merge
      slowSource ~> merge

      merge ~> balance ~> sink("Sink1")
               balance ~> sink("Sink2")

      ClosedShape
    }
  )
  crazyGraph.run()
}
