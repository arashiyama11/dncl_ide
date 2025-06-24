package io.github.arashiyama11.dncl_ide.domain.usecase

import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.domain.repository.CodeCompletionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeCompletionUseCaseTest {
    private lateinit var repository: MockRepository
    private lateinit var useCase: CodeCompletionUseCase

    @BeforeTest
    fun setup() {
        repository = MockRepository()
        useCase = CodeCompletionUseCase(repository)
    }

    @Test
    fun `LLM suggestions are returned`() = runTest {
        val result = useCase.fetch("test", 0)
        assertEquals(repository.list, result)
    }

    private class MockRepository : CodeCompletionRepository {
        val list = listOf(Definition("mock", null, false))
        override suspend fun fetchSuggestions(code: String, cursorPosition: Int): List<Definition> = list
    }
}
