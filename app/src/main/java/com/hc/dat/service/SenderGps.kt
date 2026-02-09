package com.hc.dat.service

import com.lws.type.Logger
import com.google.gson.Gson
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import hc.manager.datapp.models.GpsModel
import hc.manager.datapp.utils.Constant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeoutException

class Sender(private val model: GpsModel) {

    suspend fun sendData(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val factory = ConnectionFactory().apply {
                    host = Constant.RabbitServer
                    username = Constant.RabbitUser
                    port = Constant.RabbitPort
                    isAutomaticRecoveryEnabled = false
                    password = Constant.RabbitPwd
                }

                val connection: Connection = factory.newConnection()
                val channel: Channel = connection.createChannel()
                val gson = Gson()
                val json = gson.toJson(model)
                Logger.i("RabbitMQ: $json")

                channel.basicPublish(
                    Constant.RabbitExchange,
                    Constant.RabbitQueue,
                    false,
                    null,
                    json.toByteArray()
                )

                Logger.i("RabbitMQ: Success")
                channel.close()
                connection.close()
                true
            } catch (ex: IOException) {
                Logger.e("RabbitMQ IOException  ${ex.message ?: "Unknown error"}")
                false
            } catch (ex: TimeoutException) {
                Logger.e("RabbitMQ TimeoutException ${ex.message ?: "Unknown error"}")
                false
            } catch (e: Exception) {
                e.printStackTrace()
                Logger.e("RabbitMQ ${e.message ?: "Unknown error"}")
                false
            }
        }
    }
}
