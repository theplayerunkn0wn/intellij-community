/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.diff.editor

import com.intellij.diff.impl.DiffRequestProcessor
import com.intellij.diff.util.DiffUserDataKeysEx
import com.intellij.diff.util.DiffUtil
import com.intellij.diff.util.FileEditorBase
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.DiffBundle
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import java.awt.BorderLayout
import java.awt.event.ContainerAdapter
import java.awt.event.ContainerEvent
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
import javax.swing.JPanel
import javax.swing.KeyStroke

open class DiffRequestProcessorEditor(
  private val file: DiffVirtualFile,
  val processor: DiffRequestProcessor
) : FileEditorBase() {
  companion object {
    private val LOG = logger<DiffRequestProcessorEditor>()
  }

  private val panel = MyPanel(processor.component)

  init {
    putUserData(EditorWindow.HIDE_TABS, true)
    if (!DiffUtil.isUserDataFlagSet(DiffUserDataKeysEx.DIFF_IN_EDITOR_WITH_EXPLICIT_DISPOSABLE, processor.context)) {
      Disposer.register(this, Disposable {
        Disposer.dispose(processor)
      })
    }
    Disposer.register(processor, Disposable {
      propertyChangeSupport.firePropertyChange(FileEditor.PROP_VALID, true, false)
    })

    processor.component.registerKeyboardAction({ Disposer.dispose(this) },
                                               KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), WHEN_IN_FOCUSED_WINDOW)
  }

  override fun getComponent(): JComponent = panel
  override fun getPreferredFocusedComponent(): JComponent? = processor.preferredFocusedComponent

  override fun dispose() {}
  override fun isValid(): Boolean = !processor.isDisposed
  override fun getFile(): VirtualFile = file
  override fun getName(): String = DiffBundle.message("diff.file.editor.name")

  override fun selectNotify() {
    processor.updateRequest()
  }

  private inner class MyPanel(component: JComponent) : JPanel(BorderLayout()) {
    init {
      add(component, BorderLayout.CENTER)

      addContainerListener(object : ContainerAdapter() {
        override fun componentRemoved(e: ContainerEvent?) {
          if (Disposer.isDisposed(this@DiffRequestProcessorEditor)) return
          LOG.error("DiffRequestProcessor cannot be shown twice, see com.intellij.ide.actions.SplitAction.FORBID_TAB_SPLIT, file: $file")
        }
      })
    }
  }
}
