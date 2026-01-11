package utils

import java.lang.ref.WeakReference

object ConfigEvents {
    private val listeners = mutableListOf<WeakReference<() -> Unit>>()

    fun subscribe(listener: () -> Unit) {
        listeners.add(WeakReference(listener))
    }

    fun fire() {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next().get()
            if (listener == null) {
                iterator.remove()
            } else {
                listener.invoke()
            }
        }
    }
}
