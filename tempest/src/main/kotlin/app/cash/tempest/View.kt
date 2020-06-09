package app.cash.tempest

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDeleteExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.ConsistentReads
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression

interface View<K : Any, I : Any> {
  /**
   * Returns an item whose keys match those of the prototype key object given, or null if no
   * such item exists.
   */
  fun load(key: K, consistentReads: ConsistentReads = ConsistentReads.EVENTUAL): I?

  /**
   * Saves an item in DynamoDB. This method uses [DynamoDBMapperConfig.SaveBehavior.PUT] to clear
   * and replace all attributes, including unmodeled ones, on save. Partial update, i.e.
   * [DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES], is not supported yet.
   *
   * Any options specified in the [saveExpression] parameter will be overlaid on any constraints due
   * to versioned attributes.
   *
   * If [ignoreVersionConstraints] is true, version attributes will be discarded.
   */
  fun save(
    item: I,
    saveExpression: DynamoDBSaveExpression? = null,
    ignoreVersionConstraints: Boolean = false
  )

  /**
   * Deletes the item identified by [key] from its DynamoDB table using [deleteExpression]. Any
   * options specified in the [deleteExpression] parameter will be overlaid on any constraints due
   * to versioned attributes.
   *
   * If the item to be deleted has versioned attributes, load the item and use [delete] instead or
   * use [ignoreVersionConstraints] to discard them.
   */
  fun deleteKey(
    key: K,
    deleteExpression: DynamoDBDeleteExpression? = null,
    ignoreVersionConstraints: Boolean = false
  )

  /**
   * Deletes [item] from its DynamoDB table using [deleteExpression]. Any options specified in the
   * [deleteExpression] parameter will be overlaid on any constraints due to versioned attributes.
   *
   * If [ignoreVersionConstraints] is true, version attributes will not be considered when deleting
   * the object.
   */
  fun delete(
    item: I,
    deleteExpression: DynamoDBDeleteExpression? = null,
    ignoreVersionConstraints: Boolean = false
  )
}
