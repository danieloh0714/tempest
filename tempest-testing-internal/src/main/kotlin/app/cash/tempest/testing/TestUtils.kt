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

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput

object TestUtils {
  val port: Int = pickPort()

  private val url = "http://localhost:$port"

  private val awsCredentialsProvider: AWSCredentialsProvider = AWSStaticCredentialsProvider(
    BasicAWSCredentials("key", "secret")
  )

  private val endpointConfiguration = AwsClientBuilder.EndpointConfiguration(
    url,
    Regions.US_WEST_2.toString()
  )

  fun connect(): AmazonDynamoDB {
    return AmazonDynamoDBClientBuilder.standard()
      // The values that you supply for the AWS access key and the Region are only used to name
      // the database file.
      .withCredentials(awsCredentialsProvider)
      .withEndpointConfiguration(endpointConfiguration)
      .build()
  }

  fun connectToStreams(): AmazonDynamoDBStreams {
    return AmazonDynamoDBStreamsClientBuilder.standard()
      .withCredentials(awsCredentialsProvider)
      .withEndpointConfiguration(endpointConfiguration)
      .build()
  }

  private fun pickPort(): Int {
    // There is a tolerable chance of flaky tests caused by port collision.
    return 58000 + (ProcessHandle.current().pid() % 1000).toInt()
  }
}

fun AmazonDynamoDB.createTable(
  table: TestTable
) {
  var tableRequest = DynamoDBMapper(this)
    .generateCreateTableRequest(table.tableClass.java)
    // Provisioned throughput needs to be specified when creating the table. However,
    // DynamoDB Local ignores your provisioned throughput settings. The values that you specify
    // when you call CreateTable and UpdateTable have no effect. In addition, DynamoDB Local
    // does not throttle read or write activity.
    .withProvisionedThroughput(ProvisionedThroughput(1L, 1L))
  val globalSecondaryIndexes = tableRequest.globalSecondaryIndexes ?: emptyList()
  for (globalSecondaryIndex in globalSecondaryIndexes) {
    // Provisioned throughput needs to be specified when creating the table.
    globalSecondaryIndex.provisionedThroughput = ProvisionedThroughput(1L, 1L)
  }
  tableRequest = table.configureTable(tableRequest)

  DynamoDB(this).createTable(tableRequest).waitForActive()
}
