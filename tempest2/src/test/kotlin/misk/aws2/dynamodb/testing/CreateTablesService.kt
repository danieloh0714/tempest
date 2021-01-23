/*
 * Copyright 2020 Square Inc.
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

package misk.aws2.dynamodb.testing

import com.google.common.util.concurrent.AbstractIdleService
import javax.inject.Inject
import javax.inject.Singleton
import misk.logging.getLogger
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput

@Singleton
class CreateTablesService @Inject constructor(
  private val dynamoDbClient: DynamoDbClient,
  private val tables: Set<DynamoDbTable>
) : AbstractIdleService() {

  override fun startUp() {
    // Cleans up the tables before each run.
    for (tableName in dynamoDbClient.listTables().tableNames()) {
      dynamoDbClient.deleteTable(DeleteTableRequest.builder().tableName(tableName).build())
    }

    for (table in tables) {
      dynamoDbClient.createTable(table)
    }
  }

  override fun shutDown() {
    dynamoDbClient.close()
  }

  private fun DynamoDbClient.createTable(
    table: DynamoDbTable
  ) {
    val enhancedClient = DynamoDbEnhancedClient.builder()
      .dynamoDbClient(dynamoDbClient)
      .build()
    var tableRequest = CreateTableEnhancedRequest.builder()
      // Provisioned throughput needs to be specified when creating the table. However,
      // DynamoDB Local ignores your provisioned throughput settings. The values that you specify
      // when you call CreateTable and UpdateTable have no effect. In addition, DynamoDB Local
      // does not throttle read or write activity.
      .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build())
    tableRequest = table.configureTable(tableRequest)
    enhancedClient.table(table.tableName, TableSchema.fromClass(table.tableClass.java))
      .createTable(tableRequest.build())
  }

  companion object {
    val CONFIGURE_TABLE_NOOP: (CreateTableEnhancedRequest.Builder) -> CreateTableEnhancedRequest.Builder = {
      it
    }
  }
}

private val logger = getLogger<CreateTablesService>()
