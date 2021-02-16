/*
 * Copyright 2021 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.cash.tempest.testing

import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.Ports
import com.google.common.util.concurrent.AbstractIdleService

object DockerDynamoDbServer : AbstractIdleService(), TestDynamoDbServer {

  private val pid = ProcessHandle.current().pid()
  override val id = "docker-dynamodb-local-$pid"

  override val port = TestUtils.port

  override fun startUp() {
    composer.start()

    // Temporary client to block until the container is running
    val client = TestUtils.connect()
    while (true) {
      try {
        client.deleteTable("not a table")
      } catch (e: Exception) {
        if (e is AmazonDynamoDBException) {
          break
        }
        Thread.sleep(100)
      }
    }
    client.shutdown()
  }

  override fun shutDown() {
    composer.stop()
  }

  private val composer = Composer(
    "e-$id",
    Container {
      // DynamoDB Local listens on port 8000 by default.
      val exposedClientPort = ExposedPort.tcp(8000)
      val portBindings = Ports()
      portBindings.bind(exposedClientPort, Ports.Binding.bindPort(TestUtils.port))
      withImage("amazon/dynamodb-local")
        .withName(id)
        .withExposedPorts(exposedClientPort)
        .withCmd("-jar", "DynamoDBLocal.jar", "-sharedDb")
        .withPortBindings(portBindings)
    }
  )
}
