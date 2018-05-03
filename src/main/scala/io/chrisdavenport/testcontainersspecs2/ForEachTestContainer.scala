package io.chrisdavenport.testcontainersspecs2

import com.dimafeng.testcontainers.Container
import org.junit.runner.Description
import org.specs2.execute._
import org.specs2.specification.AroundEach

trait ForEachTestContainer extends AroundEach { self =>

  def container: Container

  implicit private val suiteDescription: Description =
    Description.createSuiteDescription(self.getClass)

  override protected def around[R: AsResult](r: => R): Result = {
    container.starting()
    try {
      afterStart()
      val result = AsResult(r)
      result match {
        case f @ Failure(_, _, _, _) => container.failed(f.exception)
        case Error(_, t) => container.failed(t)
        case _ => container.succeeded()
      }
      result
    } catch {
      case t: Throwable =>
        container.failed(t)
        throw t
    } finally {
      try {
        beforeStop()
      } finally {
        container.finished()
      }
    }
  }

  def afterStart(): Unit = {}
  def beforeStop(): Unit = {}

}
