package org.intellij.plugins.html.util

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
internal class JavaStackTracePluginScope(private val coroutineScope: CoroutineScope) {
  companion object {
    fun createChildScope(project: Project): CoroutineScope {
      return scope(project).childScope()
    }

    fun scope(project: Project): CoroutineScope {
      return project.service<JavaStackTracePluginScope>().coroutineScope
    }
  }
}
