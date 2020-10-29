// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package training.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.impl.ActionShortcutRestrictions
import com.intellij.openapi.keymap.impl.ui.KeymapPanel
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.FeaturesTrainerIcons
import training.keymap.KeymapUtil
import training.learn.LearnBundle
import training.util.invokeActionForFocusContext
import training.util.useNewLearningUi
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

class LessonMessagePane : JTextPane() {
  enum class MessageState { NORMAL, PASSED, INACTIVE, RESTORE }

  private data class LessonMessage(
    val messageParts: List<MessagePart>,
    var start: Int,
    var end: Int,
    var state: MessageState = MessageState.NORMAL
  )
  private data class RangeData(var range: IntRange, val action: (Point) -> Unit)

  private val lessonMessages get() = activeMessages + restoreMessages + inactiveMessages
  private val activeMessages = mutableListOf<LessonMessage>()
  private val restoreMessages = mutableListOf<LessonMessage>()
  private val inactiveMessages = mutableListOf<LessonMessage>()

  private val fontFamily = Font(UISettings.instance.fontFace, Font.PLAIN, UISettings.instance.fontSize).family

  private val ranges = mutableSetOf<RangeData>()

  private var insertOffset: Int = 0

  //, fontFace, check_width + check_right_indent
  init {
    UIUtil.doNotScrollToCaret(this)
    initStyleConstants()
    isEditable = false
    val listener = object : MouseAdapter() {
      override fun mouseClicked(me: MouseEvent) {
        val rangeData = getRangeDataForMouse(me) ?: return
        val middle = (rangeData.range.first + rangeData.range.last) / 2
        val rectangle = modelToView(middle)
        rangeData.action(Point(rectangle.x, (rectangle.y + rectangle.height)))
      }

      override fun mouseMoved(me: MouseEvent) {
        val rangeData = getRangeDataForMouse(me)
        cursor = if (rangeData == null) {
          Cursor.getDefaultCursor()
        }
        else {
          Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }
      }
    }
    addMouseListener(listener)
    addMouseMotionListener(listener)
  }

  private fun getRangeDataForMouse(me: MouseEvent) : RangeData? {
    val point = Point(me.x, me.y)
    val offset = viewToModel(point)
    val result = ranges.find { offset in it.range } ?: return null
    if (offset < 0 || offset >= document.length) return null
    for (i in result.range) {
      val rectangle = modelToView(i)
      if (me.x >= rectangle.x && me.y >= rectangle.y && me.y <= rectangle.y + rectangle.height) {
        return result
      }
    }
    return null
  }

  private fun initStyleConstants() {
    font = Font(UISettings.instance.fontFace, Font.PLAIN, UISettings.instance.fontSize)

    StyleConstants.setForeground(INACTIVE, UISettings.instance.passedColor)

    StyleConstants.setFontFamily(REGULAR, fontFamily)
    StyleConstants.setFontSize(REGULAR, UISettings.instance.fontSize)
    StyleConstants.setForeground(REGULAR, JBColor.BLACK)

    StyleConstants.setFontFamily(BOLD, fontFamily)
    StyleConstants.setFontSize(BOLD, UISettings.instance.fontSize)
    StyleConstants.setBold(BOLD, true)
    StyleConstants.setForeground(BOLD, JBColor.BLACK)

    StyleConstants.setFontFamily(SHORTCUT, fontFamily)
    StyleConstants.setFontSize(SHORTCUT, UISettings.instance.fontSize)
    StyleConstants.setBold(SHORTCUT, true)
    StyleConstants.setForeground(SHORTCUT, JBColor.BLACK)

    StyleConstants.setForeground(CODE, JBColor.BLUE)
    EditorColorsManager.getInstance().globalScheme.editorFontName
    StyleConstants.setFontFamily(CODE, EditorColorsManager.getInstance().globalScheme.editorFontName)
    StyleConstants.setFontSize(CODE, UISettings.instance.fontSize)

    StyleConstants.setForeground(LINK, JBColor.BLUE)
    StyleConstants.setFontFamily(LINK, fontFamily)
    StyleConstants.setUnderline(LINK, true)
    StyleConstants.setFontSize(LINK, UISettings.instance.fontSize)

    StyleConstants.setLeftIndent(PARAGRAPH_STYLE, UISettings.instance.checkIndent.toFloat())
    StyleConstants.setRightIndent(PARAGRAPH_STYLE, 0f)
    StyleConstants.setSpaceAbove(PARAGRAPH_STYLE, 16.0f)
    StyleConstants.setSpaceBelow(PARAGRAPH_STYLE, 0.0f)
    StyleConstants.setLineSpacing(PARAGRAPH_STYLE, 0.2f)

    StyleConstants.setForeground(REGULAR, UISettings.instance.defaultTextColor)
    StyleConstants.setForeground(BOLD, UISettings.instance.defaultTextColor)
    StyleConstants.setForeground(SHORTCUT, UISettings.instance.shortcutTextColor)
    StyleConstants.setForeground(LINK, UISettings.instance.lessonLinkColor)
    StyleConstants.setForeground(CODE, UISettings.instance.lessonLinkColor)

    this.setParagraphAttributes(PARAGRAPH_STYLE, true)
  }

  fun messagesNumber(): Int = activeMessages.size

  private fun removeMessagesRange(startIdx: Int, endIdx: Int, list: MutableList<LessonMessage>) {
    if (startIdx == endIdx) return

    val lastIdx = endIdx - 1
    val startOffset = list[startIdx].start
    val endOffset = list[lastIdx].end
    val removeLength = endOffset - startOffset
    list.subList(startIdx, endIdx).clear()
    ranges.removeIf { startOffset <= it.range.first && it.range.last < endOffset }
    fixOffsets(endOffset, -removeLength)

    if (insertOffset in startOffset..endOffset) insertOffset = startOffset
    else if (insertOffset > endOffset) insertOffset -= removeLength
    document.remove(startOffset, endOffset - startOffset)
  }

  private fun fixOffsets(fromOffset: Int, lengthChange: Int) {
    ranges.filter { it.range.first >= fromOffset }.forEach {
      it.range = (it.range.first + lengthChange)..(it.range.last + lengthChange)
    }
    lessonMessages.forEach { message ->
      if (message.start >= fromOffset) {
        message.start += lengthChange
        message.end += lengthChange
        for (part in message.messageParts) {
          part.startOffset += lengthChange
          part.endOffset += lengthChange
        }
      }
    }
  }

  fun clearRestoreMessages() {
    removeMessagesRange(0, restoreMessages.size, restoreMessages)
  }

  fun removeInactiveMessages(number: Int) {
    removeMessagesRange(0, number, inactiveMessages)
  }

  fun resetMessagesNumber(number: Int) {
    clearRestoreMessages()

    if (useNewLearningUi) {
      val move = activeMessages.subList(number, activeMessages.size - number)
      move.forEach {
        setInactiveStyle(it)
        it.state = MessageState.INACTIVE
      }
      inactiveMessages.addAll(0, move)
      move.clear()
    }
    else {
      removeMessagesRange(number, activeMessages.size - number, activeMessages)
    }
  }

  private fun insertText(text: String, attributeSet: AttributeSet) {
    document.insertString(insertOffset, text, attributeSet)
    insertOffset += text.length
  }

  fun addMessage(messageParts: List<MessagePart>, state: MessageState = MessageState.NORMAL): Rectangle? {
    val lastActiveOffset = activeMessages.takeIf { it.isNotEmpty() }?.last()?.end ?: 0
    insertOffset = when (state) {
      MessageState.INACTIVE -> document.length
      MessageState.RESTORE -> restoreMessages.takeIf { it.isNotEmpty() }?.last()?.end ?: lastActiveOffset
      else -> lastActiveOffset
    }
    val start = insertOffset
    if (insertOffset != 0)
      insertText("\n", REGULAR)
    val newRanges = mutableListOf<RangeData>()
    for (message in messageParts) {
      val startOffset = insertOffset
      message.startOffset = startOffset
      when (message.type) {
        MessagePart.MessageType.TEXT_REGULAR -> insertText(message.text, REGULAR)
        MessagePart.MessageType.TEXT_BOLD -> insertText(message.text, BOLD)
        MessagePart.MessageType.SHORTCUT -> appendShortcut(message).let { newRanges.add(it) }
        MessagePart.MessageType.CODE -> insertText(message.text, CODE)
        MessagePart.MessageType.CHECK -> insertText(message.text, ROBOTO)
        MessagePart.MessageType.LINK -> appendLink(message)?.let { newRanges.add(it) }
        MessagePart.MessageType.ICON_IDX -> LearningUiManager.iconMap[message.text]?.let { addPlaceholderForIcon(it) }
        MessagePart.MessageType.PROPOSE_RESTORE -> insertText(message.text, BOLD)
      }
      message.endOffset = insertOffset
    }
    val end = insertOffset
    val lessonMessage = LessonMessage(messageParts, start, end)
    if (state == MessageState.INACTIVE) {
      setInactiveStyle(lessonMessage)
    }
    lessonMessage.state = state

    fixOffsets(start, end - start)
    ranges.addAll(newRanges)
    when (state) {
      MessageState.INACTIVE -> inactiveMessages
      MessageState.RESTORE -> restoreMessages
      else -> activeMessages
    }.add(lessonMessage)

    val startRect = modelToView(start) ?: return null
    val endRect = modelToView(end - 1) ?: return null
    return Rectangle(startRect.x, startRect.y, endRect.x + endRect.width - startRect.x, endRect.y + endRect.height - startRect.y)
    //learnToolWindow?.scrollToTheEnd()
  }

  private fun addPlaceholderForIcon(icon: Icon) {
    var placeholder = " "
    while (this.getFontMetrics(this.font).stringWidth(placeholder) <= icon.iconWidth) {
      placeholder += " "
    }
    placeholder += " "
    insertText(placeholder, REGULAR)
  }

  /**
   * inserts a checkmark icon to the end of the LessonMessagePane document as a styled label.
   */
  @Throws(BadLocationException::class)
  fun passPreviousMessages() {
    if (!useNewLearningUi) { //Repaint text with passed style
      val lessonMessage = lessonMessages.lastOrNull() ?: return
      lessonMessage.state = MessageState.PASSED
      setPassedStyle(lessonMessage)
    }
    else { //Repaint text with passed style
      val lessonMessage = lessonMessages.lastOrNull { it.state == MessageState.NORMAL } ?: return
      lessonMessage.state = MessageState.PASSED
    }
  }

  private fun setPassedStyle(lessonMessage: LessonMessage) {
    val passedStyle = this.addStyle(null, null)
    StyleConstants.setForeground(passedStyle, UISettings.instance.passedColor)
    styledDocument.setCharacterAttributes(0, lessonMessage.end, passedStyle, false)
  }

  private fun setInactiveStyle(lessonMessage: LessonMessage) {
    styledDocument.setCharacterAttributes(lessonMessage.start, lessonMessage.end, INACTIVE, false)
  }


  fun redrawMessages() {
    val copy = lessonMessages.toList()
    clear()
    for (lessonMessage in copy) {
      addMessage(lessonMessage.messageParts, lessonMessage.state)
    }
    for ((index, it) in lessonMessages.withIndex()) {
      it.state = copy[index].state
      if (it.state == MessageState.PASSED && !useNewLearningUi) setPassedStyle(it)
    }
  }

  fun clear() {
    text = ""
    activeMessages.clear()
    restoreMessages.clear()
    inactiveMessages.clear()
    ranges.clear()
  }

  /**
   * Appends link inside JTextPane to Run another lesson

   * @param messagePart - should have LINK type. message.runnable starts when the message has been clicked.
   */
  @Throws(BadLocationException::class)
  private fun appendLink(messagePart: MessagePart): RangeData? {
    val clickRange = appendClickableRange(messagePart.text, LINK)
    val runnable = messagePart.runnable ?: return null
    return RangeData(clickRange) { runnable.run() }
  }

  private fun appendShortcut(messagePart: MessagePart): RangeData {
    val range = appendClickableRange(" ${messagePart.text} ", SHORTCUT)
    val clickRange = IntRange(range.first + 1, range.last - 1) // exclude around spaces
    return RangeData(clickRange) { showShortcutBalloon(it, messagePart.link, messagePart.text) }
  }

  private fun showShortcutBalloon(it: Point, actionName: String?, shortcut: String) {
    lateinit var balloon: Balloon
    val jPanel = JPanel()
    jPanel.layout = BoxLayout(jPanel, BoxLayout.Y_AXIS)
    if (SystemInfo.isMac) {
      jPanel.add(JLabel(KeymapUtil.decryptMacShortcut(shortcut)))
    }
    val action = actionName?.let { ActionManager.getInstance().getAction(it) }
    if (action != null) {
      jPanel.add(JLabel(action.templatePresentation.text))
      jPanel.add(LinkLabel<Any>(LearnBundle.message("shortcut.balloon.apply.this.action"), null) { _, _ ->
        invokeActionForFocusContext(action)
        balloon.hide()
      })
      jPanel.add(LinkLabel<Any>(LearnBundle.message("shortcut.balloon.add.shortcut"), null) { _, _ ->
        KeymapPanel.addKeyboardShortcut(actionName, ActionShortcutRestrictions.getInstance().getForActionId(actionName),
                                        KeymapManager.getInstance().activeKeymap, this)
        balloon.hide()
        repaint()
      })
    }
    val builder = JBPopupFactory.getInstance()
      .createDialogBalloonBuilder(jPanel, null)
      //.setRequestFocus(true)
      .setHideOnClickOutside(true)
      .setCloseButtonEnabled(true)
      .setAnimationCycle(0)
      .setBlockClicksThroughBalloon(true)
      //.setContentInsets(Insets(0, 0, 0, 0))
    builder.setBorderColor(JBColor(Color.BLACK, Color.WHITE))
    balloon = builder.createBalloon()
    balloon.show(RelativePoint(this, it), Balloon.Position.below)
  }

  private fun appendClickableRange(clickable: String, attributeSet: SimpleAttributeSet): IntRange {
    val startLink = insertOffset
    insertText(clickable, attributeSet)
    val endLink = insertOffset
    return startLink..endLink
  }

  override fun paintComponent(g: Graphics) {
    try {
      paintMessages(g)
    }
    catch (e: BadLocationException) {
      LOG.warn(e)
    }

    super.paintComponent(g)
    paintLessonCheckmarks(g)
  }

  private fun paintLessonCheckmarks(g: Graphics) {
    for (lessonMessage in lessonMessages) {
      if (lessonMessage.state == MessageState.PASSED) {
        var startOffset = lessonMessage.start
        if (startOffset != 0) startOffset++
        try {
          val rectangle = modelToView(startOffset)
          val checkmark = if (useNewLearningUi) FeaturesTrainerIcons.GreenCheckmark else FeaturesTrainerIcons.Checkmark
          if (SystemInfo.isMac) {
            checkmark.paintIcon(this, g, rectangle.x - UISettings.instance.checkIndent, rectangle.y + JBUI.scale(1))
          }
          else {
            checkmark.paintIcon(this, g, rectangle.x - UISettings.instance.checkIndent, rectangle.y + JBUI.scale(1))
          }
        }
        catch (e: BadLocationException) {
          LOG.warn(e)
        }

      }
    }
  }

  @Throws(BadLocationException::class)
  private fun paintMessages(g: Graphics) {
    val g2d = g as Graphics2D
    for (lessonMessage in lessonMessages) {
      val myMessages = lessonMessage.messageParts
      for (myMessage in myMessages) {
        if (myMessage.type == MessagePart.MessageType.SHORTCUT) {
          val startOffset = myMessage.startOffset
          val endOffset = myMessage.endOffset
          val rectangleStart = modelToView(startOffset + 1)
          val rectangleEnd = modelToView(endOffset - 1)
          val color = g2d.color
          val fontSize = UISettings.instance.fontSize

          g2d.color = UISettings.instance.shortcutBackgroundColor
          val r2d: RoundRectangle2D = if (!SystemInfo.isMac)
            RoundRectangle2D.Double(rectangleStart.getX() - 2 * indent, rectangleStart.getY() - indent + 1,
                                    rectangleEnd.getX() - rectangleStart.getX() + 4 * indent, (fontSize + 3 * indent).toDouble(),
                                    arc.toDouble(), arc.toDouble())
          else
            RoundRectangle2D.Double(rectangleStart.getX() - 2 * indent, rectangleStart.getY() - indent,
                                    rectangleEnd.getX() - rectangleStart.getX() + 4 * indent, (fontSize + 3 * indent).toDouble(),
                                    arc.toDouble(), arc.toDouble())
          g2d.fill(r2d)
          g2d.color = color
        }
        else if (myMessage.type == MessagePart.MessageType.ICON_IDX) {
          val rect = modelToView(myMessage.startOffset + 1)
          val icon = LearningUiManager.iconMap[myMessage.text]
          icon?.paintIcon(this, g2d, rect.x, rect.y)
        }
      }
    }
  }

  companion object {
    private val LOG = Logger.getInstance(LessonMessagePane::class.java)

    //Style Attributes for LessonMessagePane(JTextPane)
    private val INACTIVE = SimpleAttributeSet()
    private val REGULAR = SimpleAttributeSet()
    private val BOLD = SimpleAttributeSet()
    private val SHORTCUT = SimpleAttributeSet()
    private val ROBOTO = SimpleAttributeSet()
    private val CODE = SimpleAttributeSet()
    private val LINK = SimpleAttributeSet()
    private val PARAGRAPH_STYLE = SimpleAttributeSet()

    //arc & indent for shortcut back plate
    private val arc by lazy { JBUI.scale(4) }
    private val indent by lazy { JBUI.scale(2) }
  }
}
