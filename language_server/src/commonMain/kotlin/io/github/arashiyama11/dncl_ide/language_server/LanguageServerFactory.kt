package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.language_server.service.AstInfoService
import io.github.arashiyama11.dncl_ide.language_server.service.CodeActionService
import io.github.arashiyama11.dncl_ide.language_server.service.CompletionService
import io.github.arashiyama11.dncl_ide.language_server.service.DefinitionService
import io.github.arashiyama11.dncl_ide.language_server.service.DiagnosticService
import io.github.arashiyama11.dncl_ide.language_server.service.FormattingService
import io.github.arashiyama11.dncl_ide.language_server.service.HoverService
import io.github.arashiyama11.dncl_ide.language_server.service.ReferenceService
import io.github.arashiyama11.dncl_ide.language_server.service.RenameService
import io.github.arashiyama11.dncl_ide.language_server.service.SemanticTokensService

fun createLanguageServer(
    documentManager: DocumentManager = DocumentManager()
): DNCLLanguageServer {
    val astInfoService = AstInfoService()
    val diagnosticService = DiagnosticService()
    val completionService = CompletionService()
    val hoverService = HoverService(astInfoService)
    val definitionService = DefinitionService(astInfoService)
    val referenceService = ReferenceService(astInfoService)
    val renameService = RenameService(astInfoService)
    val formattingService = FormattingService()
    val codeActionService = CodeActionService()
    val semanticTokensService = SemanticTokensService(astInfoService)

    return DNCLLanguageServer(
        documentManager = documentManager,
        diagnosticService = diagnosticService,
        completionService = completionService,
        hoverService = hoverService,
        definitionService = definitionService,
        referenceService = referenceService,
        renameService = renameService,
        formattingService = formattingService,
        codeActionService = codeActionService,
        semanticTokensService = semanticTokensService,
        astInfoService = astInfoService
    )
}
