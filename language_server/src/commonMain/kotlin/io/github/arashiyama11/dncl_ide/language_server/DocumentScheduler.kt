package io.github.arashiyama11.dncl_ide.language_server

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class JobPriority { UserAction, Background }
enum class JobKind { ParseAndDiagnose }
enum class JobStatus { Queued, Running, Cancelled, Failed, Succeeded }

data class JobState(
    val uri: String,
    val version: Int,
    val kind: JobKind,
    val status: JobStatus,
    val requestId: Long? = null
)

data class DocumentJobRequest(
    val uri: String,
    val version: Int,
    val kind: JobKind = JobKind.ParseAndDiagnose,
    val priority: JobPriority = JobPriority.UserAction,
    val requestId: Long? = null,
    val task: suspend () -> DocumentAnalysis?
)

interface DocumentJobHandle {
    suspend fun await(): DocumentAnalysis?
    fun cancel(reason: String? = null)
    val requestId: Long?
}

interface DocumentScheduler {
    fun submit(request: DocumentJobRequest): DocumentJobHandle
    fun cancelByUri(uri: String)
    fun cancelByRequestId(requestId: Long)
    fun shutdown()
    suspend fun dumpState(): List<JobState>
}

private class DocumentJobHandleImpl(
    private val deferred: CompletableDeferred<DocumentAnalysis?>,
    override val requestId: Long?
) : DocumentJobHandle {
    private var job: Job? = null

    override suspend fun await(): DocumentAnalysis? = deferred.await()
    override fun cancel(reason: String?) {
        job?.cancel(CancellationException(reason ?: "Cancelled"))
        deferred.cancel(CancellationException(reason ?: "Cancelled"))
    }

    fun attach(job: Job) {
        this.job = job
    }

    fun complete(result: DocumentAnalysis?) {
        if (!deferred.isCompleted) deferred.complete(result)
    }

    fun completeExceptionally(t: Throwable) {
        if (!deferred.isCompleted) deferred.completeExceptionally(t)
    }
}

class DefaultDocumentScheduler(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : DocumentScheduler {

    private val mutex = Mutex()
    private val stateHistoryLimit = 200
    private val latestVersionByUri = mutableMapOf<String, Int>()
    private val activeByUri = mutableMapOf<String, DocumentJobHandleImpl>()
    private val activeByRequestId = mutableMapOf<Long, DocumentJobHandleImpl>()
    private val pendingStates = mutableListOf<JobState>()

    override fun submit(request: DocumentJobRequest): DocumentJobHandle {
        val handle = DocumentJobHandleImpl(CompletableDeferred(), request.requestId)

        val job = scope.launch {
            // coalescing & versionチェック
            val shouldRun = mutex.withLock {
                val latest = latestVersionByUri[request.uri] ?: Int.MIN_VALUE
                if (request.version < latest) {
                    handle.cancel("Outdated version")
                    return@withLock false
                }
                latestVersionByUri[request.uri] = request.version
                activeByUri[request.uri]?.cancel("Superseded by new request")
                activeByUri[request.uri] = handle
                request.requestId?.let { activeByRequestId[it] = handle }
                pendingStates.add(
                    JobState(
                        uri = request.uri,
                        version = request.version,
                        kind = request.kind,
                        status = JobStatus.Queued,
                        requestId = request.requestId
                    )
                )
                true
            }
            if (!shouldRun) return@launch

            try {
                updateState(request, JobStatus.Running)
                val result = request.task()
                updateState(request, JobStatus.Succeeded)
                handle.complete(result)
            } catch (e: CancellationException) {
                updateState(request, JobStatus.Cancelled)
                handle.cancel(e.message)
            } catch (t: Throwable) {
                updateState(request, JobStatus.Failed)
                handle.completeExceptionally(t)
            } finally {
                mutex.withLock {
                    if (activeByUri[request.uri] == handle) {
                        activeByUri.remove(request.uri)
                    }
                    request.requestId?.let { reqId ->
                        if (activeByRequestId[reqId] == handle) {
                            activeByRequestId.remove(reqId)
                        }
                    }
                }
            }
        }

        handle.attach(job)
        return handle
    }

    override fun cancelByUri(uri: String) {
        scope.launch {
            mutex.withLock { activeByUri[uri] }?.cancel("Cancelled by URI")
        }
    }

    override fun cancelByRequestId(requestId: Long) {
        scope.launch {
            mutex.withLock { activeByRequestId[requestId] }?.cancel("Cancelled by client")
        }
    }

    override fun shutdown() {
        scope.cancel("Scheduler shutdown")
    }

    override suspend fun dumpState(): List<JobState> {
        return mutex.withLock {
            val active = activeByUri.map { (uri, handle) ->
                JobState(
                    uri = uri,
                    version = latestVersionByUri[uri] ?: -1,
                    kind = JobKind.ParseAndDiagnose,
                    status = JobStatus.Running,
                    requestId = handle.requestId
                )
            }
            (pendingStates + active).toList()
        }
    }

    private suspend fun updateState(request: DocumentJobRequest, status: JobStatus) {
        mutex.withLock {
            pendingStates.add(
                JobState(
                    uri = request.uri,
                    version = request.version,
                    kind = request.kind,
                    status = status,
                    requestId = request.requestId
                )
            )
            // 履歴が肥大化しないように上限を設ける
            if (pendingStates.size > stateHistoryLimit) {
                repeat(pendingStates.size - stateHistoryLimit) {
                    pendingStates.removeAt(0)
                }
            }
        }
    }
}
