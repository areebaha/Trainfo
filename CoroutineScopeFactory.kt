package edu.vassar.cmpu203.myfirstapplication.Controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * A class for creating a coroutine scope.
 */
class CoroutineScopeFactory {
    companion object {
        fun getMainScope(): CoroutineScope {
            return CoroutineScope(Dispatchers.Main + SupervisorJob())
        }
    }
}