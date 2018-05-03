package io.chrisdavenport.testcontainersspecs2

import com.dimafeng.testcontainers.Container
import org.junit.runner.Description
import org.specs2.specification.BeforeAfterAll

trait ForAllTestContainer extends BeforeAfterAll { self =>
  def container: Container
  implicit private val suiteDescription: Description =
    Description.createSuiteDescription(self.getClass)
  override def beforeAll(): Unit = {
    container.starting()
    afterStart()
  }
  override def afterAll(): Unit = {
    beforeStop()
    container.finished()
  }
  def afterStart(): Unit = {}
  def beforeStop(): Unit = {}
}
