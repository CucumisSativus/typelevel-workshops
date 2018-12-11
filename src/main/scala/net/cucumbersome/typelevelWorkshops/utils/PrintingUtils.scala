package net.cucumbersome.typelevelWorkshops.utils

import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag
import scala.concurrent.duration._
object PrintingUtils {
  def h1(text: String): Unit = {
    println()
    println("*****************************************************************")
    println(text)
    println("*****************************************************************")
    println()
  }

  def h2(text: String): Unit = {
    println()
    println(text)
    println()
  }
  def res[T: ClassTag](title: String)(res: T): Unit = {
    println(s"$title ==> ${res.toString}")
  }

  implicit class futureOpts[T](future: Future[T]){
    def await: T = Await.result(future, 10 seconds)
  }
}
