/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.coldFusion.UI.runner;

import com.intellij.ide.browsers.WebBrowser;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CfmlRunnerParameters implements Cloneable {
  private String myUrl = "";
  private String myPort = "";
  private String myPageUrl = "";
  private WebBrowser myNonDefaultBrowser;

  public final static String WWW_ROOT = "wwwroot";

  @Attribute("web_path")
  public String getUrl() {
    return myUrl;
  }

  public void setUrl(@NotNull String url) {
    myUrl = url;
  }

  @Attribute("web_port")
  public String getPort() {
    return myPort;
  }

  public void setPort(@NotNull String port) {
    myPort = port;
  }


  @Transient
  @Nullable
  public WebBrowser getNonDefaultBrowser() {
    return myNonDefaultBrowser;
  }

  public void setNonDefaultBrowser(@Nullable WebBrowser nonDefaultBrowser) {
    myNonDefaultBrowser = nonDefaultBrowser;
  }

  @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
  @Override
  protected CfmlRunnerParameters clone() {
    try {
      return (CfmlRunnerParameters)super.clone();
    }
    catch (CloneNotSupportedException e) {
      //noinspection ConstantConditions
      return null;
    }
  }

  public void setPageUrl(@NotNull String url) {
    myPageUrl = url;
  }

  @Attribute("web_page")
  public String getPageUrl() { return myPageUrl;}
}
