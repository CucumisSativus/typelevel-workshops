package net.cucumbersome.typelevelWorkshops.catsEffect
import cats.effect.IO
import net.cucumbersome.typelevelWorkshops.utils.PrintingUtils._
object DDBracket {

  def main(args: Array[String]): Unit = {

    h1("Bracket")
    case class Resource(name: String, getValue: Unit => String)

    val workingResource = Resource("working", _ => "important data")
    def readFromResource(resource: Resource): IO[Unit] = IO(resource).bracket{ res =>
      IO(println(res.getValue(())))
    }(_ => IO(println(s"release ${resource.name}")))

    res("successful resource")(readFromResource(workingResource).unsafeRunSync())

    val failingResource = Resource("failing", _ => sys.error("it fails"))
    res("failing resource")(readFromResource(failingResource).attempt.unsafeRunSync())
  }


}
