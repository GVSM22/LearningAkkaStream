package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperatorFusion extends App {

  implicit val system = ActorSystem("OperatorFusion")
  implicit val materializer = ActorMaterializer()

  val simpleSource = Source(1 to 1000)
  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleFlow2 = Flow[Int].map(_ * 10)
  val simpleSink = Sink.foreach[Int](println)

  // this runs on the SAME ACTOR
//  simpleSource.via(simpleFlow)
//    .via(simpleFlow2)
//    .to(simpleSink)
  // operator/component FUSION

  // complex flows:
  val complexFlow = Flow[Int].map { x =>
    // simulating a long computation
    Thread.sleep(1000)
    x + 1
  }

  val complexFlow2 = Flow[Int].map { x =>
    // simulating a long computation
    Thread.sleep(1000)
    x * 10
  }

//  simpleSource.via(complexFlow)
//    .via(complexFlow2)
//    .to(simpleSink)
//    .run()

  // async boundary
//  simpleSource.via(complexFlow).async // runs on one actor
//    .via(complexFlow2).async // runs on another actor
//    .to(simpleSink)  // runs on a third actor
//    .run()

  // ordering guarantees
  Source(1 to 3)
    .map(element => {println(s"Flow A: $element"); element})
    .map(element => {println(s"Flow B: $element"); element})
    .map(element => {println(s"Flow C: $element"); element})
    .runWith(Sink.ignore)

  /**
   * the order of the elements being processed will always be the same
   * when using .async, components will be processed asynchronously, but will keep the order
   * in the example above 'Flow A: 2' will NEVER be printed BEFORE 'Flow A: 1'
   */
}
