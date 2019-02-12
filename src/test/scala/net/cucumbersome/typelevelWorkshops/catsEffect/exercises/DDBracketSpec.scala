package net.cucumbersome.typelevelWorkshops.catsEffect.exercises

import java.util.UUID

import cats.effect.IO
import net.cucumbersome.typelevelWorkshops.catsEffect.exercises.DDBracketSpec.ConnectionPool
import net.cucumbersome.typelevelWorkshops.test.BaseTest


class DDBracketSpec extends BaseTest{
  "Connection pool" >> {
    "opening reading and closing" >> {
      "when everything goes fine" >> {
        val pool = new ConnectionPool(None)

        val result = connectToServiceAndRead(pool).unsafeRunSync()

        result should_=== "Σ(O_O)"
        pool.isOpen should_=== false
      }

      "when something fails during the reading" >> {
        val error = new Exception("it failed")
        val pool = new ConnectionPool(Some(error))

        val result = connectToServiceAndRead(pool).attempt.unsafeRunSync()

        result should_=== Left(error)
        pool.isOpen should_=== false
      }
    }
  }

  def connectToServiceAndRead(pool: ConnectionPool): IO[String] = {
    ???
  }
}

object DDBracketSpec{
  class Connection(id: String, parent: ConnectionPool, ex: Option[Exception]){
    def close: Unit ={
      parent.returnConnection(id)
    }

    def read: String = {
      ex.foreach(e => throw e)
      "Σ(O_O)"
    }
  }

  class ConnectionPool(ex: Option[Exception]){
    private var connectionId: Option[String] = None
    def getConnection: Connection ={
      val connId = UUID.randomUUID().toString
      connectionId = Some(connId)
      new Connection(connId, this, ex)
    }

    def returnConnection(id: String): Unit ={
      if(id != connectionId.getOrElse(sys.error("connection not open"))) sys.error("wrong connection id!")
      connectionId = None
    }

    def isOpen: Boolean = connectionId.isDefined
  }
}
