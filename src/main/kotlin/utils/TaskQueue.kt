package utils

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * A utility to manage serialized execution of tasks globally.
 * Ensures only one task runs at a time across the entire queue,
 * while preventing duplicate tasks of the same type from being queued.
 */
class TaskQueue(private val scope: CoroutineScope) {
    private val globalMutex = Mutex()
    private val activeTypes = ConcurrentHashMap.newKeySet<String>()
    private val pendingTasks = ConcurrentHashMap<String, Job>()

    /**
     * Executes a task. 
     * - If a task of the same [type] is already running/pending, this call is ignored.
     * - Otherwise, it waits its turn in the global queue (serialized).
     */
    fun submit(type: String, block: suspend () -> Unit): Job? {
        // Immediate check to prevent duplicate queuing
        if (!activeTypes.add(type)) {
            Log.debug("TaskQueue", "Task of type '$type' is already active or queued. Skipping.")
            return null
        }

        return scope.launch {
            try {
                globalMutex.withLock {
                    Log.debug("TaskQueue", "Starting serialized task: $type")
                    block()
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.debug("TaskQueue", "Task '$type' failed", e)
                }
            } finally {
                activeTypes.remove(type)
                Log.debug("TaskQueue", "Finished task: $type")
            }
        }
    }

    /**
     * Executes a task, cancelling any existing task of the same [type].
     * This bypasses the global serialization lock as it's intended for 
     * fast UI updates or searches where only the latest request matters.
     */
    fun submitUnique(type: String, block: suspend () -> Unit): Job {
        pendingTasks[type]?.cancel()
        val job = scope.launch {
            try {
                block()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.debug("TaskQueue", "Unique task of type '$type' failed", e)
                }
            }
        }
        pendingTasks[type] = job
        return job
    }
}
