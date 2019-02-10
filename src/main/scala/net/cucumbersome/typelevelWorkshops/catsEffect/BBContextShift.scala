package net.cucumbersome.typelevelWorkshops.catsEffect

import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO}
import net.cucumbersome.typelevelWorkshops.utils.PrintingUtils._
import cats.implicits._
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
object BBContextShift {
  def main(args: Array[String]): Unit = {
    val mainPool: ContextShift[IO] = IO.contextShift(global)
    val otherPool: ContextShift[IO] = IO.contextShift(
      ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
    )

    def tick = IO{
      val currentThread = Thread.currentThread().getName
      println(s"I'm running on $currentThread")
    }

    h1("Context shift")
    val tickAndShift = for{
      _ <- tick
      _ <- mainPool.shift
      _ <- tick
      _ <- otherPool.shift
      _ <- tick
      _ <- tick
    } yield ()

    res("tick and shift")(tickAndShift.unsafeRunSync())

    h1("parMapN again")

    h2("just tick")
    def tickTickTickTick(implicit cs: ContextShift[IO]) =
      (tick, tick, tick, tick).parMapN{ case _ => "a"}

    res("ticking in parallel")(tickTickTickTick(mainPool).unsafeRunSync())

    h2("tick and sleep")
    def tickAndSleep =
      tick *> IO(Thread.sleep(100)) // almost meme arrow

    def tickAndSleepTickAndSleepTickAndSleepTickAndSleepT(implicit cs: ContextShift[IO]) =
      (tickAndSleep, tickAndSleep, tickAndSleep, tickAndSleep).parMapN{case _ => "b"}

    res("ticking and sleeping in parallel")(tickAndSleepTickAndSleepTickAndSleepTickAndSleepT(mainPool).unsafeRunSync())

  }
}
