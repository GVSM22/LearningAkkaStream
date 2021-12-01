package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}

import scala.concurrent.Future

object GraphMaterializedValues extends App {

  implicit val system: ActorSystem = ActorSystem("GraphMaterializedValues")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  val wordSource = Source(List("Akka", "is", "awesome", "rock", "the", "jvm"))
  val printer = Sink.foreach[String](println)
  val counter = Sink.fold[Int, String](0)((count, _) => count + 1)

  val complexWordSink = Sink.fromGraph(
    GraphDSL.create(counter){ implicit builder => counterShape =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[String](2))
      val lowerCaseFilter = builder.add(Flow[String].filter(word => word == word.toLowerCase))
      val shortStringFilter = builder.add(Flow[String].filter(_.length < 5))

      broadcast ~> lowerCaseFilter ~> printer
      broadcast ~> shortStringFilter ~> counterShape


      SinkShape(broadcast.in)
    }
  )

//  wordSource.toMat(complexWordSink)(Keep.right).run()
//  .onComplete {
//    case Success(count) => println(s"the number of small words is: $count")
//    case Failure(exception) => println(s"Something bad happened: $exception")
//  }


  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val counterSink = Sink.fold[Int, B](0)((count, _) => count + 1)
    Flow.fromGraph(
      GraphDSL.create(counterSink){ implicit builder => countShape =>
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[B](2))
        val flowShape = builder.add(flow)

        flowShape ~> broadcast ~> countShape

        FlowShape(flowShape.in, broadcast.out(1))
      }
    )
  }

  Source(1 to 10).viaMat(
  enhanceFlow(Flow[Int].map(x => x))
  )(Keep.right).to(
  Sink.foreach(println)
  ).run().foreach(t => println(s"O total de elementos é: $t"))

}
