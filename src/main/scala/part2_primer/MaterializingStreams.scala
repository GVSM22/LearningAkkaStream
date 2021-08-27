package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

object MaterializingStreams extends App {

  implicit val system: ActorSystem = ActorSystem("MaterializingStreams")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import system.dispatcher

  val simpleGraph = Source(1 to 10).to(Sink.foreach(println))
//  val simpleMaterializedValue = simpleGraph.run()

  val source = Source(1 to 10)
  val sink = Sink.reduce[Int]((a, b) => a + b)

  val sumFuture = source.runWith(sink)

  sumFuture.onComplete {
    case Failure(exception) => println(s"the sum of the elements could not be computed. Exception: $exception")
    case Success(value) => println(s"the sum of all values is $value")
  }

  // choosing materialized value
  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(x => x + 1)
  val simpleSink = Sink.foreach[Int](println)

  simpleSource.viaMat(simpleFlow)((sourceMat, flowMat) => flowMat) // Keep.right do the same
  simpleSource.viaMat(simpleFlow)(Keep.right)
    .toMat(simpleSink)(Keep.right)
    .run()
    .onComplete {
      case Success(_) => println("Stream processing finished")
      case Failure(exception) => println(s"Stream processing failed with: $exception")
    }

  // sugars
  Source(1 to 10).runWith(Sink.reduce[Int](_ + _)) // source.to(Sink.reduce)(Keep.right)
  Source(1 to 10).runReduce[Int](_ + _)

  // backwards
  Sink.foreach[Int](println).runWith(Source.single(42))
  // both ways
  Flow[Int].map(x => x * 2).runWith(simpleSource, simpleSink)


  // remember! viaMat and toMat always give you the control of which materialized value you want at the end


  /**
   * - return the last element out of a source (use Sink.last)
   * - compute the total word count out of a stream of sentences
   *    - map, fold, reduce
   */

//  Source(65 to 70).toMat(Sink.last)(Keep.right)
//    .run()
//    .onComplete {
//      case Failure(exception) => println(exception.getMessage)
//      case Success(value) => println(s"Last value: $value")
//    }

//  Source(65 to 70).runReduce[Int](Keep.right)
//    .onComplete {
//      case Failure(exception) => println(exception.getMessage)
//      case Success(value) => println(s"Last value: $value")
//    }

  Source(List("scala is awesome", "akka is awesome", "reactive is awesome"))
    .viaMat(Flow[String].map(_.split(" ").length))(Keep.right)
    .viaMat(Flow[Int].reduce[Int](_ + _))(Keep.right)
    .runForeach(wordCount => println(s"the total word count is $wordCount"))



  Source(List("scala is awesome", "akka is awesome", "reactive is awesome"))
    .viaMat(Flow[String].map(_.split(" ").length))(Keep.right)
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()
    .onComplete {
      case Success(wordCount) => println(s"the total word count is $wordCount")
      case Failure(exception) => println(exception.getMessage)
    }
}
