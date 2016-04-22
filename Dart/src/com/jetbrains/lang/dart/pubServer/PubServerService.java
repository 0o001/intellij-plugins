package com.jetbrains.lang.dart.pubServer;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.UrlFilter;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.net.NetKt;
import com.jetbrains.lang.dart.DartBundle;
import com.jetbrains.lang.dart.ide.runner.DartConsoleFilter;
import com.jetbrains.lang.dart.ide.runner.DartRelativePathsConsoleFilter;
import com.jetbrains.lang.dart.sdk.DartSdk;
import com.jetbrains.lang.dart.sdk.DartSdkUtil;
import icons.DartIcons;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCounted;
import io.netty.util.internal.PlatformDependent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.builtInWebServer.ConsoleManager;
import org.jetbrains.builtInWebServer.NetService;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.ide.PooledThreadExecutor;
import org.jetbrains.io.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jetbrains.io.NettyUtil.nioClientBootstrap;

final class PubServerService extends NetService {
  private static final Logger LOG = Logger.getInstance(PubServerService.class.getName());

  private static final String PUB_SERVE = "Pub Serve";
  private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.toolWindowGroup(PUB_SERVE, PUB_SERVE, false);

  private volatile VirtualFile firstServedDir;

  private final Bootstrap bootstrap = nioClientBootstrap(new NioEventLoopGroup(1, PooledThreadExecutor.INSTANCE));

  private final ConcurrentMap<Channel, Channel> serverToClientChannel = ContainerUtil.newConcurrentMap();
  private final ChannelRegistrar serverChannelRegistrar = new ChannelRegistrar();

  private final ConcurrentMap<VirtualFile, ServerInfo> servedDirToSocketAddress = ContainerUtil.newConcurrentMap();

  private static class ServerInfo {
    private final InetSocketAddress address;
    private final Deque<Channel> freeServerChannels = PlatformDependent.newConcurrentDeque();

    private ServerInfo(InetSocketAddress address) {
      this.address = address;
    }
  }

  private final ChannelFutureListener serverChannelCloseListener = future -> {
    Channel channel = future.channel();
    ServerInfo serverInfo = getServerInfo(channel);
    if (serverInfo != null) {
      serverInfo.freeServerChannels.remove(channel);
    }

    Channel clientChannel = serverToClientChannel.remove(channel);
    if (clientChannel != null) {
      sendBadGateway(clientChannel);
    }
  };

  public PubServerService(@NotNull Project project, @NotNull ConsoleManager consoleManager) {
    super(project, consoleManager);

    bootstrap.handler(new ChannelInitializer() {
      @Override
      protected void initChannel(Channel channel) throws Exception {
        channel.pipeline().addLast(serverChannelRegistrar, new HttpClientCodec());
        channel.pipeline().addLast(new PubServeChannelHandler(), ChannelExceptionHandler.getInstance());
      }
    });
  }

  @Nullable
  private ServerInfo getServerInfo(@NotNull Channel channel) {
    for (ServerInfo serverInstanceInfo : servedDirToSocketAddress.values()) {
      if (channel.remoteAddress().equals(serverInstanceInfo.address)) {
        return serverInstanceInfo;
      }
    }
    return null;
  }

  @Override
  @NotNull
  protected String getConsoleToolWindowId() {
    return PUB_SERVE;
  }

  @Override
  @NotNull
  protected Icon getConsoleToolWindowIcon() {
    return DartIcons.Dart_13;
  }

  @NotNull
  @Override
  public ActionGroup getConsoleToolWindowActions() {
    return new DefaultActionGroup(ActionManager.getInstance().getAction("Dart.stop.pub.server"));
  }

  @Override
  protected void configureConsole(@NotNull final TextConsoleBuilder consoleBuilder) {
    consoleBuilder.addFilter(new DartConsoleFilter(getProject(), firstServedDir));
    consoleBuilder.addFilter(new DartRelativePathsConsoleFilter(getProject(), firstServedDir.getParent().getPath()));
    consoleBuilder.addFilter(new UrlFilter());
  }

  public boolean isPubServerProcessAlive() {
    return getProcessHandler().has() && !getProcessHandler().getResult().isProcessTerminated();
  }

  public void sendToPubServer(@NotNull final Channel clientChannel,
                              @NotNull final FullHttpRequest clientRequest,
                              @NotNull final VirtualFile servedDir,
                              @NotNull final String pathForPubServer) {
    clientRequest.retain();

    if (getProcessHandler().has()) {
      sendToServer(servedDir, clientChannel, clientRequest, pathForPubServer);
    }
    else {
      firstServedDir = servedDir;

      getProcessHandler().get()
        .done(osProcessHandler -> sendToServer(servedDir, clientChannel, clientRequest, pathForPubServer))
        .rejected(throwable -> sendBadGateway(clientChannel));
    }
  }

  @Override
  @Nullable
  protected OSProcessHandler createProcessHandler(@NotNull final Project project, final int port) throws ExecutionException {
    final DartSdk dartSdk = DartSdk.getDartSdk(project);
    if (dartSdk == null) return null;

    final GeneralCommandLine commandLine = new GeneralCommandLine().withWorkDirectory(firstServedDir.getParent().getPath());
    commandLine.setExePath(FileUtil.toSystemDependentName(DartSdkUtil.getPubPath(dartSdk)));
    commandLine.addParameter("serve");
    commandLine.addParameter(firstServedDir.getName());
    commandLine.addParameter("--port=" + String.valueOf(port));
    //commandLine.addParameter("--admin-port=" + String.valueOf(PubServerManager.findOneMoreAvailablePort(port))); // todo uncomment and use

    final OSProcessHandler processHandler = new OSProcessHandler(commandLine);
    processHandler.addProcessListener(new PubServeOutputListener(project));

    return processHandler;
  }

  @Override
  protected void connectToProcess(@NotNull final AsyncPromise<OSProcessHandler> promise,
                                  final int port,
                                  @NotNull final OSProcessHandler processHandler,
                                  @NotNull final Consumer<String> errorOutputConsumer) {
    InetSocketAddress firstPubServerAddress = NetKt.loopbackSocketAddress(port);
    ServerInfo old = servedDirToSocketAddress.put(firstServedDir, new ServerInfo(firstPubServerAddress));
    LOG.assertTrue(old == null);

    super.connectToProcess(promise, port, processHandler, errorOutputConsumer);
  }

  @SuppressWarnings({"MethodMayBeStatic", "UnusedParameters"})
  private void serveDirAndSendRequest(@NotNull final Channel clientChannel,
                                      @NotNull final FullHttpRequest clientRequest,
                                      @NotNull final VirtualFile servedDir,
                                      @NotNull final String pathForPubServer) {
    throw new UnsupportedOperationException(); // todo this code is not reachable because of commented out /*.getParent()*/ in PubServerManager.send()
  }

  static void sendBadGateway(@NotNull final Channel channel) {
    if (channel.isActive()) {
      Responses.send(HttpResponseStatus.BAD_GATEWAY, channel);
    }
  }

  @Override
  protected void closeProcessConnections() {
    servedDirToSocketAddress.clear();

    Channel[] clientContexts;
    try {
      Collection<Channel> clientChannels = serverToClientChannel.values();
      clientContexts = clientChannels.toArray(new Channel[clientChannels.size()]);
      for (ServerInfo serverInstanceInfo : servedDirToSocketAddress.values()) {
        serverInstanceInfo.freeServerChannels.clear();
      }
      serverToClientChannel.clear();
    }
    finally {
      serverChannelRegistrar.close();
    }

    for (Channel channel : clientContexts) {
      try {
        sendBadGateway(channel);
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }
  }

  private static void connect(@NotNull final Bootstrap bootstrap,
                              @NotNull final SocketAddress remoteAddress,
                              final @NotNull Consumer<Channel> channelConsumer) {
    final AtomicInteger attemptCounter = new AtomicInteger(1);
    bootstrap.connect(remoteAddress).addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
          channelConsumer.consume(future.channel());
        }
        else {
          int attemptCount = attemptCounter.incrementAndGet();
          if (attemptCount > NettyUtil.DEFAULT_CONNECT_ATTEMPT_COUNT) {
            channelConsumer.consume(null);
          }
          else {
            Thread.sleep(attemptCount * NettyUtil.MIN_START_TIME);
            bootstrap.connect(remoteAddress).addListener(this);
          }
        }
      }
    });
  }

  void sendToServer(@NotNull final VirtualFile servedDir,
                    @NotNull final Channel clientChannel,
                    @NotNull final FullHttpRequest clientRequest,
                    @NotNull final String pathToPubServe) {
    ServerInfo serverInstanceInfo = servedDirToSocketAddress.get(servedDir);
    if (serverInstanceInfo == null) {
      serveDirAndSendRequest(clientChannel, clientRequest, servedDir, pathToPubServe);
    }

    Channel serverChannel = findFreeServerChannel(serverInstanceInfo.freeServerChannels);
    if (serverChannel == null) {
      connect(bootstrap, serverInstanceInfo.address, serverChannel1 -> {
        if (serverChannel1 == null) {
          sendBadGateway(clientChannel);
        }
        else {
          serverChannel1.closeFuture().addListener(serverChannelCloseListener);
          sendToServer(clientChannel, clientRequest, pathToPubServe, serverChannel1);
        }
      });
    }
    else {
      sendToServer(clientChannel, clientRequest, pathToPubServe, serverChannel);
    }
  }

  @Nullable
  private static Channel findFreeServerChannel(@NotNull Deque<Channel> freeServerChannels) {
    while (true) {
      Channel channel = freeServerChannels.pollLast();
      if (channel == null) {
        break;
      }

      if (channel.isActive()) {
        return channel;
      }
    }
    return null;
  }

  private void sendToServer(@NotNull final Channel clientChannel, @NotNull FullHttpRequest clientRequest, @NotNull String pathToPubServe, @NotNull Channel serverChannel) {
    Channel oldClientChannel = serverToClientChannel.put(serverChannel, clientChannel);
    LOG.assertTrue(oldClientChannel == null);

    // duplicate - content will be shared (opposite to copy), so, we use duplicate. see ByteBuf javadoc.
    FullHttpRequest request = clientRequest.duplicate().setUri(pathToPubServe);

    // regardless of client, we always keep connection to server
    request.setProtocolVersion(HttpVersion.HTTP_1_1);
    HttpUtil.setKeepAlive(request, true);

    InetSocketAddress serverAddress = (InetSocketAddress)serverChannel.remoteAddress();
    request.headers().set(HttpHeaderNames.HOST, serverAddress.getAddress().getHostAddress() + ':' + serverAddress.getPort());
    serverChannel.writeAndFlush(request);
  }

  @ChannelHandler.Sharable
  private class PubServeChannelHandler extends SimpleChannelInboundHandlerAdapter<HttpObject> {
    public PubServeChannelHandler() {
      super(false);
    }

    @Override
    protected void messageReceived(@NotNull ChannelHandlerContext context, @NotNull HttpObject message) throws Exception {
      Channel serverChannel = context.channel();
      Channel clientChannel = serverToClientChannel.get(serverChannel);
      if (clientChannel == null || !clientChannel.isActive()) {
        // client abort request, so, just close server channel as well and don't try to reuse it
        serverToClientChannel.remove(serverChannel);
        serverChannel.close();

        if (message instanceof ReferenceCounted) {
          ((ReferenceCounted)message).release();
        }
      }
      else {
        if (message instanceof HttpMessage) {
          HttpUtil.setKeepAlive(((HttpMessage)message), true);
        }
        if (message instanceof LastHttpContent) {
          serverToClientChannel.remove(serverChannel);
          ServerInfo serverInfo = getServerInfo(serverChannel);
          if (serverInfo != null) {
            // todo sometimes dart pub server stops to respond, so, we don't reuse it for now
            //serverInfo.freeServerChannels.add(serverChannel);
            serverChannel.close();
          }
        }

        clientChannel.writeAndFlush(message);
      }
    }
  }

  private static class PubServeOutputListener extends ProcessAdapter {
    private final Project myProject;
    private boolean myNotificationAboutErrors;
    private Notification myNotification;

    public PubServeOutputListener(final Project project) {
      myProject = project;
    }

    @Override
    public void onTextAvailable(final ProcessEvent event, final Key outputType) {
      if (outputType == ProcessOutputTypes.STDERR) {
        final boolean error = event.getText().toLowerCase(Locale.US).contains("error");

        ApplicationManager.getApplication().invokeLater(() -> showNotificationIfNeeded(error));
      }
    }

    private void showNotificationIfNeeded(final boolean isError) {
      if (ToolWindowManager.getInstance(myProject).getToolWindow(PUB_SERVE).isVisible()) {
        return;
      }

      if (myNotification != null && !myNotification.isExpired()) {
        final Balloon balloon1 = myNotification.getBalloon();
        final Balloon balloon2 = ToolWindowManager.getInstance(myProject).getToolWindowBalloon(PUB_SERVE);
        if ((balloon1 != null || balloon2 != null) && (myNotificationAboutErrors || !isError)) {
          return; // already showing correct balloon
        }
        myNotification.expire();
      }

      myNotificationAboutErrors = isError; // previous errors are already reported, so reset our flag

      final String message =
        DartBundle.message(myNotificationAboutErrors ? "pub.serve.output.contains.errors" : "pub.serve.output.contains.warnings");

      myNotification = NOTIFICATION_GROUP.createNotification("", message, NotificationType.WARNING, new NotificationListener.Adapter() {
        @Override
        protected void hyperlinkActivated(@NotNull final Notification notification, @NotNull final HyperlinkEvent e) {
          notification.expire();
          ToolWindowManager.getInstance(myProject).getToolWindow(PUB_SERVE).activate(null);
        }
      });

      myNotification.notify(myProject);
    }
  }
}
