package io.chrisdavenport.testcontainersspecs2

import com.dimafeng.testcontainers.Container
import org.specs2.specification.BeforeAfterAll

trait ForAllTestContainer extends BeforeAfterAll { self =>
  def container: Container

  override def beforeAll(): Unit = {
    container.start()
    afterStart()
  }
  override def afterAll(): Unit = {
    beforeStop()
    container.stop()
  }
  def afterStart(): Unit = {}
  def beforeStop(): Unit = {}
}
