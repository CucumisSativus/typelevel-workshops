package net.cucumbersome.typelevelWorkshops.catsEffect

import cats.effect.{ContextShift, IO}
import net.cucumbersome.typelevelWorkshops.utils.PrintingUtils._

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future
import cats.implicits._

object AAIntro {
  def main(args: Array[String]): Unit = {
    h1("Building")

    val pureValue = IO.pure(42)

    def aFunctionWithEffect(): Int = {
      println("I'm an effect!")
      12
    }

    val valueToComputeWhichCanHaveSomeEffect = IO(aFunctionWithEffect()) //show type signature here and compare with pure

    val failedValue: IO[Int] = IO.raiseError[Int](new Exception("failed exception"))


    h1("Executing")

    h2("sync")
    val pureResult: Int = pureValue.unsafeRunSync()
    res("pureResult")(pureResult)

    res("value with side effect")(valueToComputeWhichCanHaveSomeEffect.unsafeRunSync())

    h2("async (with callback)")

    val callback: Either[Throwable, Int] => Unit = _.fold(ex => println(s"Exception $ex"), value => println(s"Value $value"))

    pureValue.unsafeRunAsync(callback)
    failedValue.unsafeRunAsync(callback)

    h2("to future")

    val startedIO = pureValue.unsafeToFuture() // show how is it implemented

    h1("IO and futures")

    def aFutureValue(name: String): Future[Int] = Future {
      println(s"Future is being computed, called from $name")
      54
    }(global)

    val ioFromFuture = IO.fromFuture(IO(aFutureValue("IO"))) // show signature and explain

    h2("memoization")

    val future1: Future[Int] = aFutureValue("just future")
    val future2: Future[Int] = future1

    h2("futures")
    res("future1 result")(future1.await)
    res("future2 result")(future2.await)

    val effect1 = IO.fromFuture(IO(aFutureValue("IO")))
    val effect2 = effect1

    h2("IO")
    res("effect1")(effect1.unsafeRunSync())
    res("effect2")(effect2.unsafeRunSync())


    h1("composition")

    def pureMethod(value: Int): Int = value + 2

    def methodWithEffect(value: String): IO[Int] = IO {
      value.toInt
    }

    def timeConsumingMethod[T](value: T): IO[T] = IO {
      Thread.sleep(1000); value
    }

    // notice no execution context when doing map/flatMap
    val computation1 = IO.pure("12").flatMap(methodWithEffect).map(pureMethod).flatMap(timeConsumingMethod)
    val computation2 = IO.pure("failure").flatMap(methodWithEffect).map(pureMethod).flatMap(timeConsumingMethod)

    val resucedComputation = computation2.handleErrorWith {
      case e: NumberFormatException => IO.pure(0)
      case ex => IO.raiseError(ex)
    }

    res("computation1")(computation1.unsafeRunSync())
    res("rescued computation")(resucedComputation.unsafeRunSync())
    h1("parallelism")

    val longTask = IO {
      println("starting long task")
      Thread.sleep(500)
      "answer"
    }
    val longerTask = IO {
      println("starting longer task")
      Thread.sleep(600)
      12
    }

    def combineResults(str: String, int: Int): String = s"$str is $int"

    implicit val contextShift = IO.contextShift(global)

    val tasksInParallel: IO[String] = (longTask, longerTask).parMapN(combineResults)

    res("parallel task")(tasksInParallel.unsafeRunSync())


    h1("racing")

    val fastTask = IO {
      println("starting fast task")
      Thread.sleep(100)
      1
    }

    val slowerTask = IO {
      println("starting slower task")
      Thread.sleep(200)
      "slower and a string"
    }

    val runTasks = IO.race(fastTask, slowerTask)

    res("racing result")(runTasks.unsafeRunSync())
  }
}
