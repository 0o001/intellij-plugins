// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.grazie.ide.notification

import com.intellij.grazie.GrazieConfig
import com.intellij.grazie.GraziePlugin
import com.intellij.grazie.ide.ui.components.dsl.msg
import com.intellij.grazie.jlanguage.Lang
import com.intellij.grazie.remote.GrazieRemote
import com.intellij.grazie.utils.joinToStringWithOxfordComma
import com.intellij.notification.*
import com.intellij.notification.Notification
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.util.containers.ConcurrentMultiMap

object Notification {
  private enum class Group { UPDATE, LANGUAGES }

  private val shownNotifications = ConcurrentMultiMap<Group, Notification>()

  private val NOTIFICATION_GROUP_INSTALL = NotificationGroup(msg("grazie.install.group"),
                                                             NotificationDisplayType.STICKY_BALLOON, true)
  private val NOTIFICATION_GROUP_UPDATE = NotificationGroup(msg("grazie.update.group"),
                                                            NotificationDisplayType.STICKY_BALLOON, true)
  private val NOTIFICATION_GROUP_LANGUAGES = NotificationGroup(msg("grazie.languages.group"),
                                                               NotificationDisplayType.STICKY_BALLOON, true)

  fun showInstallationMessage(project: Project) = NOTIFICATION_GROUP_INSTALL
    .createNotification(
      msg("grazie.install.title"), msg("grazie.install.body", ShowSettingsUtil.getSettingsMenuName()),
      NotificationType.INFORMATION, NotificationListener.URL_OPENING_LISTENER)
    .addAction(object : NotificationAction("Open ${ShowSettingsUtil.getSettingsMenuName()}") {
      override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, msg("grazie.name"))
        notification.expire()
      }
    }).apply {
      whenExpired { shownNotifications.remove(Group.UPDATE)?.forEach { it.expire() } }
      shownNotifications.putValue(Group.UPDATE, this)
    }
    .notify(project)

  fun showUpdateMessage(project: Project) = NOTIFICATION_GROUP_UPDATE
    .createNotification(
      msg("grazie.update.title", GraziePlugin.version), msg("grazie.update.body"),
      NotificationType.INFORMATION, NotificationListener.URL_OPENING_LISTENER)
    .apply {
      whenExpired { shownNotifications.remove(Group.UPDATE)?.forEach { it.expire() } }
      shownNotifications.putValue(Group.UPDATE, this)
    }
    .notify(project)

  fun showLanguagesMessage(project: Project) {
    val langs = GrazieConfig.get().missedLanguages
    val s = if (langs.size > 1) "s" else ""
    NOTIFICATION_GROUP_LANGUAGES
      .createNotification(msg("grazie.languages.title", s),
                          msg("grazie.languages.body", langs.toList().joinToStringWithOxfordComma()),
                          NotificationType.WARNING, null)
      .addAction(object : NotificationAction(msg("grazie.languages.action.download", s)) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          GrazieRemote.downloadMissing(project)
          notification.expire()
        }
      })
      .addAction(object : NotificationAction(msg("grazie.languages.action.disable", s)) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          GrazieConfig.update { state ->
            state.update(enabledLanguages = state.enabledLanguages - state.missedLanguages,
                         nativeLanguage = if (state.nativeLanguage.jLanguage == null) Lang.AMERICAN_ENGLISH else state.nativeLanguage)
          }
          notification.expire()
        }
      })
      .apply {
        whenExpired { shownNotifications.remove(Group.LANGUAGES)?.forEach { it.expire() } }
        shownNotifications.putValue(Group.LANGUAGES, this)
      }
      .notify(project)
  }
}
