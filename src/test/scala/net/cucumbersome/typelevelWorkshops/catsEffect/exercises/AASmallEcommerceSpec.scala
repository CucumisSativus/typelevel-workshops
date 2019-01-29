package net.cucumbersome.typelevelWorkshops.catsEffect.exercises


import java.util.concurrent.atomic.AtomicInteger

import cats.effect.{ContextShift, IO}
import net.cucumbersome.typelevelWorkshops.test.BaseTest
import AASmallEcommerceSpec.{ProductWizard, _}
import net.cucumbersome.typelevelWorkshops.catsEffect.exercises.AASmallEcommerceSpec.ElasticSearchIndexer.IndexingError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import cats.implicits._
class AASmallEcommerceSpec extends BaseTest {
  val product = Product("product1", Price(100, Currency.CHF), Price(376, Currency.PLN), Price(89, Currency.EUR))
  "As a shop owner" >> {
    "I want to create new product and have my prices calculated with exchange rate" >> {
      val chfToEur = (quantity: Double) => IO.pure(Price(quantity * 0.89, Currency.EUR))
      val chfToPln = (quantity: Double) => IO.pure(Price(quantity * 3.76, Currency.PLN))

      val buildProduct: IO[Product] = ProductWizard.create(chfToPln, chfToEur)("product1", Price(100, Currency.CHF))

      val expectedProduct = product

      buildProduct.unsafeRunSync() should_=== expectedProduct
    }

    "I want to make sure that my exchange rates are delivered quickly" >> {
      val chfToEur = (quantity: Double) => { Thread.sleep(1000); IO.pure(Price(quantity * 0.89, Currency.EUR)) }
      val chfToPln = (quantity: Double) => { Thread.sleep(1000); IO.pure(Price(quantity * 3.76, Currency.PLN)) }

      val buildProduct: IO[Product] = ProductWizard.create(chfToPln, chfToEur)("product1", Price(100, Currency.CHF))

      val expectedProduct = product

      val timeBefore = System.currentTimeMillis()
      buildProduct.unsafeRunSync() should_=== expectedProduct
      System.currentTimeMillis() - timeBefore should be_<(2000L)
    }

    "I want to be able to save my freshly created product in ElasticSearch" >> {
      val client = (p: Product) => if(p != product) Future.failed(new Exception("no cheating allowed!")) else Future.successful(())


      ElasticSearchIndexer.index(client)(product).unsafeRunSync() should_=== Right(())
    }

    "I want to retry writing to elastic search since it sometimes fail ¯\\_(ツ)_/¯" >> {
      // this client fails 2 times, on the third returns proper value, check out RetryHandler for help
      val client = new TwoTimesFailingClient

      ElasticSearchIndexer.index(client.save)(product).unsafeRunSync() should_=== Right(())
    }

    "But if it fails I want to make sure that it fails with nice error" >> {
      val client = new AlwaysFailingClient

      ElasticSearchIndexer.index(client.save)(product).unsafeRunSync() should_=== Left(IndexingError("it failed"))
    }

    "I want to have my nice product returned in controller" >> {
      val chfToEur = (quantity: Double) => { Thread.sleep(1000); IO.pure(Price(quantity * 0.89, Currency.EUR)) }
      val chfToPln = (quantity: Double) => { Thread.sleep(1000); IO.pure(Price(quantity * 3.76, Currency.PLN)) }

      val createProduct: (String, Price) => IO[Product] = ProductWizard.create(chfToPln, chfToEur)

      Await.result(ProductController.create(createProduct)("product2", Price(100, Currency.CHF)), 100 millis) must be_===("<html><h1>product2</h1></html>")
    }
  }
}

object AASmallEcommerceSpec {

  private[exercises] implicit val contextShift: ContextShift[IO] = IO.contextShift(global)

  private[exercises] sealed trait Currency

  private[exercises] object Currency {

    case object CHF extends Currency

    case object PLN extends Currency

    case object EUR extends Currency
  }


  private[exercises] final case class Price(quantity: Double, currency: Currency)

  private[exercises] final case class Product(name: String, priceInChf: Price, priceInPln: Price, priceInEur: Price)

  private[exercises] object ProductWizard{
    def create(getPlnExchange: Double => IO[Price], getEurExchange: Double => IO[Price])
              (name: String, priceInChf: Price): IO[Product] = {
      // call getPlnExchange and getEurExchange in parallel and build the result
      // remember that you already have price in chf and name!
      ???

    }
  }

  private[exercises] object ElasticSearchIndexer{
    case class IndexingError(msg: String)
    def index(esClient: Product => Future[Unit])(product: Product): IO[Either[IndexingError, Unit]] = {
      // Implement and use retry handler from the bottom of this class,
      // the idea of RetryHandler is such, that in case of error after the retry is reached, it just fails
      // so make sure that you put your transformation from exception to either in a right place!
      ???
    }
  }

  private[exercises] object ProductController{
    def create(wizard: (String, Price) => IO[Product])(name: String, priceInChf: Price): Future[String] = {
      // pass arguments to wizard and turn it into future
      // then use `serve` method defined below

      ???
    }

    def serve(res: Future[Product]): Future[String] = res.map(p => s"<html><h1>${p.name}</h1></html>")
  }

  private[exercises] class TwoTimesFailingClient{
    private val retryCount = new AtomicInteger(0)
    def save(p: Product): Future[Unit] = {
      if(retryCount.getAndIncrement() == 2) Future.successful(())
      else Future.failed(new Exception("network error"))
    }
  }

  private[exercises] class AlwaysFailingClient{
    def save(p: Product): Future[Unit] = {
      Future.failed(new Exception("this one always fails"))
    }
  }

  private[exercises] object RetryHander{
    def retry[T](t: IO[T], maxRetriesNum: Int): IO[T] = {
      // handle error of t and if you can still retry, retry! IO is stack safe
      // the idea here is to handle error as long as you can retry
      // if after last retry error is still there, just return failed IO
      ???
    }
  }
}
