// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugins.markdown.ui.preview.jcef;

import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLoadHandlerAdapter;
import org.intellij.markdown.html.HtmlGenerator;
import org.intellij.plugins.markdown.ui.preview.MarkdownAccessor;
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel;
import org.intellij.plugins.markdown.ui.preview.PreviewStaticServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MarkdownJCEFHtmlPanel extends JCEFHtmlPanel implements MarkdownHtmlPanel {

  private static final String JS_REQ_SET_SCROLL_Y = JBCefUtils.makeUniqueJSRequestID(MarkdownJCEFHtmlPanel.class, "setMyScrollY");
  private static final String JS_REQ_OPEN_IN_BROWSER = JBCefUtils.makeUniqueJSRequestID(MarkdownJCEFHtmlPanel.class, "openInExternalBrowser");

  private static final NotNullLazyValue<String> MY_SCRIPTING_LINES = new NotNullLazyValue<String>() {
    @NotNull
    @Override
    protected String compute() {
      return SCRIPTS.stream()
        .map(s -> "<script src=\"" + PreviewStaticServer.getScriptUrl(s) + "\"></script>")
        .reduce((s, s2) -> s + "\n" + s2)
        .orElseGet(String::new);
    }
  };

  @NotNull
  private String[] myCssUris = ArrayUtil.EMPTY_STRING_ARRAY;
  @NotNull
  private String myCSP = "";
  @NotNull
  private String myLastRawHtml = "";
  @NotNull
  private final ScrollPreservingListener myScrollPreservingListener = new ScrollPreservingListener();
  @NotNull
  private final BridgeSettingListener myBridgeSettingListener = new BridgeSettingListener();

  public MarkdownJCEFHtmlPanel() {
    super();
    JBCefUtils.addJSHandler(getBrowser().getClient(), JS_REQ_SET_SCROLL_Y,
      (value) -> {
        try {
          myScrollPreservingListener.myScrollY = Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}
        return true;
      });
  }

  @Override
  public void setHtml(@NotNull String html) {
    myLastRawHtml = html;
    super.setHtml(html);
  }

  @NotNull
  @Override
  protected String prepareHtml(@NotNull String html) {
    return MarkdownAccessor.getImageRefreshFixAccessor().setStamps(html
      .replace("<head>", "<head>"
               + "<meta http-equiv=\"Content-Security-Policy\" content=\"" + myCSP + "\"/>"
               + MarkdownHtmlPanel.getCssLines(null, myCssUris) + "\n" + getScriptingLines()));
  }

  @Override
  public void setCSS(@Nullable String inlineCss, @NotNull String... fileUris) {
    PreviewStaticServer.getInstance().setInlineStyle(inlineCss);
    myCssUris = inlineCss == null ? fileUris
                                  : ArrayUtil
                  .mergeArrays(fileUris, PreviewStaticServer.getStyleUrl(PreviewStaticServer.INLINE_CSS_FILENAME));
    myCSP = PreviewStaticServer.createCSP(ContainerUtil.map(SCRIPTS, s -> PreviewStaticServer.getScriptUrl(s)),
                                          ContainerUtil.concat(
                                            ContainerUtil.map(STYLES, s -> PreviewStaticServer.getStyleUrl(s)),
                                            ContainerUtil.filter(fileUris, s -> s.startsWith("http://") || s.startsWith("https://"))
                                          ));
    setHtml(myLastRawHtml);
  }

  @Override
  public void scrollToMarkdownSrcOffset(final int offset) {
    getBrowser().executeJavaScript(
      "if ('__IntelliJTools' in window) " +
      "__IntelliJTools.scrollToOffset(" + offset + ", '" + HtmlGenerator.Companion.getSRC_ATTRIBUTE_NAME() + "');",
      getBrowser().getURL(), 0);

    getBrowser().executeJavaScript(
      "var value = document.documentElement.scrollTop || (document.body && document.body.scrollTop);" +
      JBCefUtils.makeJSRequestCode(JS_REQ_SET_SCROLL_Y, "value"),
      null, 0);
  }

  @Override
  public void dispose() {
    JBCefUtils.removeJSHandler(getBrowser().getClient(), JS_REQ_SET_SCROLL_Y);
    JBCefUtils.removeJSHandler(getBrowser().getClient(), JS_REQ_OPEN_IN_BROWSER);
  }

  @NotNull
  private static String getScriptingLines() {
    return MY_SCRIPTING_LINES.getValue();
  }

  @Override
  protected void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
    myScrollPreservingListener.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
    myBridgeSettingListener.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
  }

  private class BridgeSettingListener extends CefLoadHandlerAdapter  {
    {
      JBCefUtils.addJSHandler(getBrowser().getClient(), JS_REQ_OPEN_IN_BROWSER,
        (link) -> {
          MarkdownAccessor.getSafeOpenerAccessor().openLink(link);
          return true;
        });
    }

    @Override
    public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
      getBrowser().executeJavaScript(
        "window.JavaPanelBridge = {" +
          "openInExternalBrowser : function(link) {" +
            JBCefUtils.makeJSRequestCode(JS_REQ_OPEN_IN_BROWSER, "link") +
          "}" +
        "};",
        getBrowser().getURL(), 0);
    }
  }

  private class ScrollPreservingListener extends CefLoadHandlerAdapter {
    volatile int myScrollY = 0;

    @Override
    public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
      if (isLoading) {
        getBrowser().executeJavaScript(
          "var value = document.documentElement.scrollTop || document.body.scrollTop;" +
          JBCefUtils.makeJSRequestCode(JS_REQ_SET_SCROLL_Y, "value"),
          getBrowser().getURL(), 0);
      }
      else {
        getBrowser().executeJavaScript("document.documentElement.scrollTop = ({} || document.body).scrollTop = " + myScrollY,
                                       getBrowser().getURL(), 0);
      }
    }
  }
}
