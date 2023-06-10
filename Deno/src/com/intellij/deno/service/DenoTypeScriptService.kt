package com.intellij.deno.service

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.deno.DenoSettings
import com.intellij.lang.ecmascript6.resolve.JSFileReferencesUtil
import com.intellij.lang.javascript.DialectDetector
import com.intellij.lang.javascript.JSStringUtil
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.lang.javascript.frameworks.modules.JSUrlImportsUtil
import com.intellij.lang.javascript.integration.JSAnnotationError
import com.intellij.lang.javascript.library.JSCorePredefinedLibrariesProvider
import com.intellij.lang.javascript.service.JSLanguageServiceProvider
import com.intellij.lang.typescript.compiler.TypeScriptCompilerSettings
import com.intellij.lang.typescript.compiler.TypeScriptService.CompletionMergeStrategy
import com.intellij.lang.typescript.compiler.languageService.TypeScriptLanguageServiceUtil
import com.intellij.lang.typescript.compiler.languageService.TypeScriptMessageBus
import com.intellij.lang.typescript.compiler.languageService.protocol.commands.response.TypeScriptQuickInfoResponse
import com.intellij.lang.typescript.library.TypeScriptLibraryProvider
import com.intellij.lang.typescript.lsp.BaseLspTypeScriptService
import com.intellij.lang.typescript.lsp.LspAnnotationError
import com.intellij.lsp.LspServer
import com.intellij.lsp.api.LspServerManager
import com.intellij.lsp.requests.LspHoverRequest
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.text.SemVer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

class DenoTypeScriptServiceProvider(val project: Project) : JSLanguageServiceProvider {

  override fun isHighlightingCandidate(file: VirtualFile) = TypeScriptCompilerSettings.acceptFileType(file.fileType)

  override fun getService(file: VirtualFile) = allServices.firstOrNull()

  override fun getAllServices() =
    if (DenoSettings.getService(project).isUseDeno()) listOf(DenoTypeScriptService.getInstance(project)) else emptyList()
}

@Service(Service.Level.PROJECT)
class DenoTypeScriptService(project: Project) : BaseLspTypeScriptService(project) {
  companion object {
    private val LOG = Logger.getInstance(DenoTypeScriptService::class.java)
    fun getInstance(project: Project): DenoTypeScriptService = project.getService(DenoTypeScriptService::class.java)
  }

  override fun getLspServers(): Collection<LspServer> =
    LspServerManager.getInstance(project).getServersForProvider(DenoLspSupportProvider::class.java)

  override val name: String
    get() = "Deno LSP"
  override val prefix: String
    get() = "Deno"
  override val serverVersion: SemVer? = null

  override fun getStatusText() = withServer {
    // TODO use super method (& display serverVersion)
    when {
      isRunning -> "Deno LSP"
      isMalfunctioned -> "Deno LSP ⚠"
      else -> "..."
    }
  }

  override fun getCompletionMergeStrategy(parameters: CompletionParameters, file: PsiFile, context: PsiElement): CompletionMergeStrategy {
    if (JSTokenTypes.STRING_LITERALS.contains(context.node.elementType)) {
      JSFileReferencesUtil.getReferenceModuleText(context.parent)?.let {
        if (JSUrlImportsUtil.startsWithRemoteUrlPrefix(JSStringUtil.unquoteStringLiteralValue(it))) {
          return CompletionMergeStrategy.MERGE
        }
      }
    }

    return TypeScriptLanguageServiceUtil.getCompletionMergeStrategy(parameters, file, context)
  }

  override fun getServiceFixes(file: PsiFile, element: PsiElement?, result: JSAnnotationError): Collection<IntentionAction> {
    if (element != null && (result is LspAnnotationError) && file.virtualFile != null) {
      return withServer { getQuickFixes(file.virtualFile, result.diagnostic) } ?: return emptyList()
    }
    return emptyList()
  }

  private fun quickInfo(element: PsiElement): TypeScriptQuickInfoResponse? {
    val server = getServer() ?: return null
    val hoverRequest = LspHoverRequest.create(server, element) ?: return null
    val raw = server.sendRequestSync(hoverRequest) ?: return null
    LOG.info("Quick info for $element : $raw")
    val response = TypeScriptQuickInfoResponse().apply {
      displayString = raw.substring("<html><body><pre>".length, raw.length - "</pre></body></html>".length) //
    }
    return response
  }

  override fun getQuickInfoAt(element: PsiElement, originalElement: PsiElement, originalFile: VirtualFile): CompletableFuture<TypeScriptQuickInfoResponse?> =
    completedFuture(quickInfo(element))

  override fun canHighlight(file: PsiFile) = DialectDetector.isTypeScript(file)

  override fun isAcceptable(file: VirtualFile) =
    TypeScriptLanguageServiceUtil.ACCEPTABLE_TS_FILE.value(file) &&
    !JSCorePredefinedLibrariesProvider.isCoreLibraryFile(file) &&
    !DenoTypings.getInstance(project).isDenoTypings(file) &&
    !TypeScriptLibraryProvider.isLibraryOrBundledLibraryFile(project, file)

  override fun restart(recreateToolWindow: Boolean) {
    LspServerManager.getInstance(project).stopAndRestartIfNeeded(DenoLspSupportProvider::class.java)
    TypeScriptMessageBus.get(project).changed()
  }
}