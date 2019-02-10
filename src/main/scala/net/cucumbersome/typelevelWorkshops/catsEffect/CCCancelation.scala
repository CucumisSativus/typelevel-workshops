package net.cucumbersome.typelevelWorkshops.catsEffect

import cats.effect.{CancelToken, ContextShift, IO}
import cats.implicits._
import scala.concurrent.ExecutionContext.global
object CCCancelation {
  def main(args: Array[String]): Unit = {
    val cs: ContextShift[IO] = IO.contextShift(global)
    def longRunningProcess(chunkNumber: Long): IO[Unit] = {
      for{
        _ <- IO(println(s"running chunkNumber $chunkNumber"))
        _ <- IO(Thread.sleep(100))
        _ <-  cs.shift *> longRunningProcess(chunkNumber +1) //show what happens if we dont shift there
      } yield ()
    }

    val process = cs.shift *> longRunningProcess(0)
    val cancel = process.unsafeRunCancelable(_ => println("process canceled"))

    Thread.sleep(1000)

    cancel.unsafeRunSync()

    Thread.sleep(10000)
  }
}
