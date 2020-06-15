package io.chrisdavenport.testcontainersspecs2

import com.dimafeng.testcontainers.Container
import org.specs2.execute._
import org.specs2.specification.AroundEach

trait ForEachTestContainer extends AroundEach { self =>

  def container: Container

  override protected def around[R: AsResult](r: => R): Result = {
    container.start
    try {
      afterStart()
      AsResult(r)
    } finally {
      try {
        beforeStop()
      } finally {
        container.stop()
      }
    }
  }

  def afterStart(): Unit = {}
  def beforeStop(): Unit = {}

}
