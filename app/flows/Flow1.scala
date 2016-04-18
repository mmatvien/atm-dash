package flows

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink}
import models.ATMEvent

/**
  * Created by: MAXIRU 
  * Date: 3/30/16
  * Time: 9:11 AM
  */
trait Flow1 {

  val MaximumDistinctWords = 5000

  val groupByStep = Flow[ATMEvent].map { event => println("grouping by step"); event }
  val groupByTerm = Flow[ATMEvent].map { event => event }


  val flow1 = Flow.fromGraph(
    g = GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[ATMEvent](2))
      //
      val groupByTermF = builder.add(groupByTerm)
      //      val groupByStepF = builder.add(groupByStep)

      //      val reduce = reduceByKey(
      //        MaximumDistinctWords,
      //        groupKey = (event: ATMEvent) => event.step,
      //        map = (event: ATMEvent) => 1
      //      )((left: Int, right: Int) => left + right).map(p => Steps(stepNumber = p._1, stepCount = p._2))

      //
      //      val reduce = Flow[ATMEvent]
      //        .groupBy(MaximumDistinctWords, identity)
      //        .map(_ -> 1)
      //        .reduce((l, r) => (l._1, l._2 + r._2) )
      //        .map { d => println(s" d is sssssssssssssssss $d"); d }
      //        .mergeSubstreams
      val count: Flow[ATMEvent, Int, NotUsed] = Flow[ATMEvent].map(_ => 1)

//      val countFlow = Flow[Int].map { x => println(x); x }.fold[Int](0)(_ + _)
//
//      val sumSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

      val foldFlow = Flow[Int].map{x => println(x); x}.fold(0)(_ + _).map(identity)


      //      val reduceByKeyF = builder.add(reduce)


      broadcast ~> count ~> foldFlow.map(println) ~> Sink.ignore
      broadcast ~> groupByTermF


      FlowShape(broadcast.in, groupByTermF.out)
    }
  )
}