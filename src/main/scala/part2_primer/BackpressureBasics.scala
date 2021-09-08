package part2_primer

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object BackpressureBasics extends App {

  implicit val system: ActorSystem = ActorSystem("BackpressureBasics")
  implicit val materializer: ActorMaterializer = ActorMaterializer()


  val fastSource = Source(1 to 1000)

  val slowSink = Sink.foreach[Int] { x =>
    Thread.sleep(1000)
    println(s"Sink: $x")
  }

//  fastSource.to(slowSink).run() // fusing?!
  // not backpressure

//  fastSource.async.to(slowSink).run()
  // backpressure

  val aSimpleFlow = Flow[Int].map { i =>
    println(s"incoming $i")
    i + 1
  }

//  fastSource.async
//    .via(aSimpleFlow).async
//    .to(slowSink)
//    .run()

  /*
    reactions to backpressure (in order):
    - try to slow down if possible
    - buffer elements until there's more demand
    - drop down elements from the buffer if it overflow // we can only control it here
    - tear down/kill the whole stream (failure)
   */

//  val bufferedFlow = aSimpleFlow.buffer(10, overflowStrategy = OverflowStrategy.dropHead)
//  fastSource.async
//    .via(bufferedFlow).async
//    .to(slowSink)
//    .run()
  /*
    1 - 16: nobody is backpressured
    17 - 26: flow will buffer, flow will start dropping at the next element
    26 - 1000: flow will always drop the oldest element
   */

  /*
    overflow strategies:
    - drop head = oldest
    - drop tail = newest
    - drop new = exact element to be added = keeps the buffer
    - drop the entire buffer
    - backpressure signal
    - fail
   */

  // throttling
  fastSource.throttle(15, 2 seconds)
    .runWith(Sink.foreach[Int](println))
}
