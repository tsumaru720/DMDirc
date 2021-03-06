/*
 * Copyright (c) 2006-2011 Chris Smith, Shane Mc Cormack, Gregory Holmes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dmdirc;

import com.dmdirc.actions.ActionManager;
import com.dmdirc.actions.CoreActionType;
import com.dmdirc.actions.wrappers.AliasWrapper;
import com.dmdirc.commandparser.CommandManager;
import com.dmdirc.commandparser.CommandType;
import com.dmdirc.commandparser.parsers.ServerCommandParser;
import com.dmdirc.config.ConfigManager;
import com.dmdirc.config.Identity;
import com.dmdirc.config.IdentityManager;
import com.dmdirc.interfaces.AwayStateListener;
import com.dmdirc.interfaces.ConfigChangeListener;
import com.dmdirc.interfaces.InviteListener;
import com.dmdirc.logger.ErrorLevel;
import com.dmdirc.logger.Logger;
import com.dmdirc.parser.common.ChannelJoinRequest;
import com.dmdirc.parser.common.DefaultStringConverter;
import com.dmdirc.parser.common.IgnoreList;
import com.dmdirc.parser.common.MyInfo;
import com.dmdirc.parser.common.ParserError;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.ClientInfo;
import com.dmdirc.parser.interfaces.EncodingParser;
import com.dmdirc.parser.interfaces.Parser;
import com.dmdirc.parser.interfaces.ProtocolDescription;
import com.dmdirc.parser.interfaces.SecureParser;
import com.dmdirc.parser.interfaces.StringConverter;
import com.dmdirc.tls.CertificateManager;
import com.dmdirc.tls.CertificateProblemListener;
import com.dmdirc.ui.StatusMessage;
import com.dmdirc.ui.WindowManager;
import com.dmdirc.ui.core.components.StatusBarManager;
import com.dmdirc.ui.core.components.WindowComponent;
import com.dmdirc.ui.input.TabCompleter;
import com.dmdirc.ui.input.TabCompletionType;
import com.dmdirc.ui.interfaces.InputWindow;
import com.dmdirc.ui.messages.Formatter;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.net.ssl.TrustManager;

/**
 * The Server class represents the client's view of a server. It maintains
 * a list of all channels, queries, etc, and handles parser callbacks pertaining
 * to the server.
 */
public class Server extends WritableFrameContainer
        implements ConfigChangeListener, CertificateProblemListener {

    // <editor-fold defaultstate="collapsed" desc="Properties">

    // <editor-fold defaultstate="collapsed" desc="Static">

    /** The name of the general domain. */
    private static final String DOMAIN_GENERAL = "general".intern();
    /** The name of the profile domain. */
    private static final String DOMAIN_PROFILE = "profile".intern();
    /** The name of the server domain. */
    private static final String DOMAIN_SERVER = "server".intern();

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Instance">

    /** Open channels that currently exist on the server. */
    private final Map<String, Channel> channels = new ConcurrentSkipListMap<String, Channel>();

    /** Open query windows on the server. */
    private final Map<String, Query> queries = new ConcurrentSkipListMap<String, Query>();

    /** The Parser instance handling this server. */
    private Parser parser;
    /** The Parser instance that used to be handling this server. */
    private Parser oldParser;
    /** The parser-supplied protocol description object. */
    private ProtocolDescription protocolDescription;

    /**
     * Object used to synchronoise access to parser. This object should be
     * locked by anything requiring that the parser reference remains the same
     * for a duration of time, or by anything which is updating the parser
     * reference.
     *
     * If used in conjunction with myStateLock, the parserLock must always be
     * locked INSIDE the myStateLock to prevent deadlocks.
     */
    private final ReadWriteLock parserLock = new ReentrantReadWriteLock();

    /** The Parser Thread. */
    private Thread parserThread;

    /** The raw frame used for this server instance. */
    private Raw raw;

    /** The address of the server we're connecting to. */
    private URI address;

    /** The profile we're using. */
    private Identity profile;

    /** Object used to synchronise access to myState. */
    private final Object myStateLock = new Object();

    /** The current state of this server. */
    private final ServerStatus myState = new ServerStatus(this, myStateLock);

    /** The timer we're using to delay reconnects. */
    private Timer reconnectTimer;

    /** The tabcompleter used for this server. */
    private final TabCompleter tabCompleter = new TabCompleter();

    /** Our reason for being away, if any. */
    private String awayMessage;

    /** Our event handler. */
    private final ServerEventHandler eventHandler = new ServerEventHandler(this);

    /** A list of outstanding invites. */
    private final List<Invite> invites = new ArrayList<Invite>();

    /** A set of channels we want to join without focusing. */
    private final Set<String> backgroundChannels = new HashSet<String>();

    /** Our ignore list. */
    private final IgnoreList ignoreList = new IgnoreList();

    /** Our string convertor. */
    private StringConverter converter = new DefaultStringConverter();

    /** The certificate manager in use, if any. */
    private CertificateManager certificateManager;

    // </editor-fold>

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Constructors">

    /**
     * Creates a new server which will connect to the specified URL with
     * the specified profile.
     *
     * @since 0.6.3
     * @param uri The address of the server to connect to
     * @param profile The profile to use
     */
    public Server(final URI uri, final Identity profile) {
        super("server-disconnected", getHost(uri), getHost(uri),
                InputWindow.class,
                new ConfigManager(uri.getScheme(), "", "", uri.getHost()),
                new ServerCommandParser(),
                Arrays.asList(WindowComponent.TEXTAREA.getIdentifier(),
                WindowComponent.INPUTFIELD.getIdentifier(),
                WindowComponent.CERTIFICATE_VIEWER.getIdentifier()));

        setConnectionDetails(uri, profile);

        ServerManager.getServerManager().registerServer(this);
        WindowManager.getWindowManager().addWindow(this);

        tabCompleter.addEntries(TabCompletionType.COMMAND,
                AliasWrapper.getAliasWrapper().getAliases());
        tabCompleter.addEntries(TabCompletionType.COMMAND,
                CommandManager.getCommandManager().getCommandNames(CommandType.TYPE_SERVER));
        tabCompleter.addEntries(TabCompletionType.COMMAND,
                CommandManager.getCommandManager().getCommandNames(CommandType.TYPE_GLOBAL));

        updateIcon();

        new Timer("Server Who Timer").schedule(new TimerTask() {
            @Override
            public void run() {
                for (Channel channel : channels.values()) {
                    channel.checkWho();
                }
            }
        }, 0, getConfigManager().getOptionInt(DOMAIN_GENERAL, "whotime"));

        if (getConfigManager().getOptionBool(DOMAIN_GENERAL, "showrawwindow")) {
            addRaw();
        }

        getConfigManager().addChangeListener("formatter", "serverName", this);
        getConfigManager().addChangeListener("formatter", "serverTitle", this);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Connection, disconnection & reconnection">

    /**
     * Updates the connection details for this server. If the specified URI
     * does not define a port, the default port from the protocol description
     * will be used.
     *
     * @param uri The new URI that this server should connect to
     * @param profile The profile that this server should use
     */
    private void setConnectionDetails(final URI uri, final Identity profile) {
        this.address = uri;
        this.protocolDescription = new ParserFactory().getDescription(uri);
        this.profile = profile;

        if (uri.getPort() == -1 && protocolDescription != null) {
            try {
                this.address = new URI(uri.getScheme(), uri.getUserInfo(),
                        uri.getHost(), protocolDescription.getDefaultPort(),
                        uri.getPath(), uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException ex) {
                Logger.appError(ErrorLevel.MEDIUM, "Unable to construct URI", ex);
            }
        }
    }

    /**
     * Connects to a new server with the previously supplied address and profile.
     *
     * @since 0.6.3m2
     */
    public void connect() {
        connect(address, profile);
    }

    /**
     * Connects to a new server with the specified details.
     *
     * @param address The address of the server to connect to
     * @param profile The profile to use
     * @since 0.6.3
     */
    @Precondition({
        "The current parser is null or not connected",
        "The specified profile is not null"
    })
    @SuppressWarnings("fallthrough")
    public void connect(final URI address, final Identity profile) {
        assert profile != null;

        synchronized (myStateLock) {
            switch (myState.getState()) {
                case RECONNECT_WAIT:
                    reconnectTimer.cancel();
                    break;
                case CLOSING:
                    // Ignore the connection attempt
                    return;
                case CONNECTED:
                case CONNECTING:
                    disconnect(getConfigManager().getOption(DOMAIN_GENERAL, "quitmessage"));
                case DISCONNECTING:
                    while (!myState.getState().isDisconnected()) {
                        try {
                            myStateLock.wait();
                        } catch (InterruptedException ex) {
                            return;
                        }
                    }
                    break;
                default:
                    // Do nothing
                    break;
            }

            final URI connectAddress;

            try {
                parserLock.writeLock().lock();
                if (parser != null) {
                    throw new IllegalArgumentException("Connection attempt while parser "
                            + "is still connected.\n\nMy state:" + getState());
                }

                getConfigManager().migrate(address.getScheme(), "", "", address.getHost());

                setConnectionDetails(address, profile);

                updateTitle();
                updateIcon();

                parser = buildParser();

                if (parser == null) {
                    addLine("serverUnknownProtocol", address.getScheme());
                    return;
                }

                connectAddress = parser.getURI();
            } finally {
                parserLock.writeLock().unlock();
            }

            addLine("serverConnecting", connectAddress.getHost(), connectAddress.getPort());

            myState.transition(ServerState.CONNECTING);

            doCallbacks();

            updateAwayState(null);
            removeInvites();

            parserThread = new Thread(parser, "Parser thread");
            parserThread.start();
        }

        ActionManager.getActionManager().triggerEvent(
                CoreActionType.SERVER_CONNECTING, null, this);
    }

    /**
     * Reconnects to the server with a specified reason.
     *
     * @param reason The quit reason to send
     */
    public void reconnect(final String reason) {
        synchronized (myStateLock) {
            if (myState.getState() == ServerState.CLOSING) {
                return;
            }

            disconnect(reason);

            connect(address, profile);
        }
    }

    /**
     * Reconnects to the server.
     */
    public void reconnect() {
        reconnect(getConfigManager().getOption(DOMAIN_GENERAL, "reconnectmessage"));
    }

    /**
     * Disconnects from the server with the default quit message.
     */
    public void disconnect() {
        disconnect(getConfigManager().getOption(DOMAIN_GENERAL, "quitmessage"));
    }

    /**
     * Disconnects from the server.
     *
     * @param reason disconnect reason
     */
    public void disconnect(final String reason) {
        synchronized (myStateLock) {
            switch (myState.getState()) {
            case CLOSING:
            case DISCONNECTING:
            case DISCONNECTED:
            case TRANSIENTLY_DISCONNECTED:
                return;
            case RECONNECT_WAIT:
                reconnectTimer.cancel();
                break;
            default:
                break;
            }

            clearChannels();
            backgroundChannels.clear();

            try {
                parserLock.readLock().lock();
                if (parser == null) {
                    myState.transition(ServerState.DISCONNECTED);
                } else {
                    myState.transition(ServerState.DISCONNECTING);

                    removeInvites();
                    updateIcon();

                    parserThread.interrupt();
                    parser.disconnect(reason);
                }
            } finally {
                parserLock.readLock().unlock();
            }

            if (getConfigManager().getOptionBool(DOMAIN_GENERAL,
                    "closechannelsonquit")) {
                closeChannels();
            }

            if (getConfigManager().getOptionBool(DOMAIN_GENERAL,
                    "closequeriesonquit")) {
                closeQueries();
            }
        }
    }

    /**
     * Schedules a reconnect attempt to be performed after a user-defiend delay.
     */
    @Precondition("The server state is transiently disconnected")
    private void doDelayedReconnect() {
        synchronized (myStateLock) {
            if (myState.getState() != ServerState.TRANSIENTLY_DISCONNECTED) {
                throw new IllegalStateException("doDelayedReconnect when not "
                        + "transiently disconnected\n\nState: " + myState);
            }

            final int delay = Math.max(1000,
                    getConfigManager().getOptionInt(DOMAIN_GENERAL, "reconnectdelay"));

            handleNotification("connectRetry", getAddress(), delay / 1000);

            reconnectTimer = new Timer("Server Reconnect Timer");
            reconnectTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (myStateLock) {
                        if (myState.getState() == ServerState.RECONNECT_WAIT) {
                            myState.transition(ServerState.TRANSIENTLY_DISCONNECTED);
                            reconnect();
                        }
                    }
                }
            }, delay);

            myState.transition(ServerState.RECONNECT_WAIT);
            updateIcon();
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Child windows">

    /**
     * Determines whether the server knows of the specified channel.
     *
     * @param channel The channel to be checked
     * @return True iff the channel is known, false otherwise
     */
    public boolean hasChannel(final String channel) {
        return channels.containsKey(converter.toLowerCase(channel));
    }

    /**
     * Retrieves the specified channel belonging to this server.
     *
     * @param channel The channel to be retrieved
     * @return The appropriate channel object
     */
    public Channel getChannel(final String channel) {
        return channels.get(converter.toLowerCase(channel));
    }

    /**
     * Retrieves a list of channel names belonging to this server.
     *
     * @return list of channel names belonging to this server
     */
    public List<String> getChannels() {
        return new ArrayList<String>(channels.keySet());
    }

    /**
     * Determines whether the server knows of the specified query.
     *
     * @param host The host of the query to look for
     * @return True iff the query is known, false otherwise
     */
    public boolean hasQuery(final String host) {
        return queries.containsKey(converter.toLowerCase(parseHostmask(host)[0]));
    }

    /**
     * Retrieves the specified query belonging to this server. If the query
     * does not yet exist, it is created automatically.
     *
     * @param host The host of the query to look for
     * @return The appropriate query object
     */
    public Query getQuery(final String host) {
        return getQuery(host, false);
    }

    /**
     * Retrieves the specified query belonging to this server. If the query
     * does not yet exist, it is created automatically.
     *
     * @param host The host of the query to look for
     * @param focus Should we focus the window on open?
     * @return The appropriate query object
     */
    public Query getQuery(final String host, final boolean focus) {
        synchronized (myStateLock) {
            if (myState.getState() == ServerState.CLOSING) {
                // Can't open queries while the server is closing
                return null;
            }
        }

        final String nick = parseHostmask(host)[0];
        final String lnick = converter.toLowerCase(nick);

        if (!queries.containsKey(lnick)) {
            final Query newQuery = new Query(this, host, focus);

            tabCompleter.addEntry(TabCompletionType.QUERY_NICK, nick);
            queries.put(lnick, newQuery);
        }

        return queries.get(lnick);
    }

    /**
     * Updates the state of this server following a nick change of someone
     * that the user has a query open with. Namely, this updates the
     * tabcompleter with the new name, and ensures that the <code>queries</code>
     * map uses the correct nickname.
     *
     * @param query The query object being updated
     * @param oldNick The old nickname of the user
     * @param newNick The new nickname of the user
     * @since 0.6.4
     */
    public void updateQuery(final Query query, final String oldNick, final String newNick) {
        tabCompleter.removeEntry(TabCompletionType.QUERY_NICK, oldNick);
        tabCompleter.addEntry(TabCompletionType.QUERY_NICK, newNick);

        queries.put(converter.toLowerCase(newNick), query);
        queries.remove(converter.toLowerCase(oldNick));
    }

    /**
     * Retrieves a list of queries belonging to this server.
     *
     * @return list of queries belonging to this server
     */
    public Collection<Query> getQueries() {
        return Collections.unmodifiableCollection(queries.values());
    }

    /**
     * Deletes a query from this server.
     *
     * @param query The query that should be removed.
     */
    public void delQuery(final Query query) {
        tabCompleter.removeEntry(TabCompletionType.QUERY_NICK, query.getNickname());
        queries.remove(converter.toLowerCase(query.getNickname()));
    }

    /**
     * Adds a raw window to this server.
     */
    public void addRaw() {
        if (raw == null) {
            raw = new Raw(this);

            try {
                parserLock.readLock().lock();
                if (parser != null) {
                    raw.registerCallbacks();
                }
            } finally {
                parserLock.readLock().unlock();
            }
        }
    }

    /**
     * Retrieves the raw window associated with this server.
     *
     * @return The raw window associated with this server.
     */
    public Raw getRaw() {
        return raw;
    }

    /**
     * Removes our reference to the raw object (presumably after it has been
     * closed).
     */
    public void delRaw() {
        raw = null; //NOPMD
    }

    /**
     * Removes a specific channel and window from this server.
     *
     * @param chan channel to remove
     */
    public void delChannel(final String chan) {
        tabCompleter.removeEntry(TabCompletionType.CHANNEL, chan);
        channels.remove(converter.toLowerCase(chan));
    }

    /**
     * Adds a specific channel and window to this server.
     *
     * @param chan channel to add
     * @return The channel that was added (may be null if closing)
     */
    public Channel addChannel(final ChannelInfo chan) {
        return addChannel(chan, !backgroundChannels.contains(chan.getName())
                || getConfigManager().getOptionBool(DOMAIN_GENERAL,
                    "hidechannels"));
    }

    /**
     * Adds a specific channel and window to this server.
     *
     * @param chan channel to add
     * @param focus Whether or not to focus the channel
     * @return The channel that was added (may be null if closing)
     */
    public Channel addChannel(final ChannelInfo chan, final boolean focus) {
        synchronized (myStateLock) {
            if (myState.getState() == ServerState.CLOSING) {
                // Can't join channels while the server is closing
                return null;
            }
        }

        backgroundChannels.remove(chan.getName());

        if (hasChannel(chan.getName())) {
            getChannel(chan.getName()).setChannelInfo(chan);
            getChannel(chan.getName()).selfJoin();
        } else {
            final Channel newChan = new Channel(this, chan, focus);

            tabCompleter.addEntry(TabCompletionType.CHANNEL, chan.getName());
            channels.put(converter.toLowerCase(chan.getName()), newChan);
        }

        return getChannel(chan.getName());
    }

    /**
     * Closes all open channel windows associated with this server.
     */
    private void closeChannels() {
        for (Channel channel : new ArrayList<Channel>(channels.values())) {
            channel.close();
        }
    }

    /**
     * Clears the nicklist of all open channels.
     */
    private void clearChannels() {
        for (Channel channel : channels.values()) {
            channel.resetWindow();
        }
    }

    /**
     * Closes all open query windows associated with this server.
     */
    private void closeQueries() {
        for (Query query : new ArrayList<Query>(queries.values())) {
            query.close();
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Miscellaneous methods">

    /**
     * Retrieves the host component of the specified URI, or throws a relevant
     * exception if this is not possible.
     *
     * @param uri The URI to be processed
     * @return The URI's host component, as returned by {@link URI#getHost()}.
     * @throws NullPointerException If <code>uri</code> is null
     * @throws IllegalArgumentException If the specified URI has no host
     * @since 0.6.4
     */
    private static String getHost(final URI uri) {
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("URIs must have hosts");
        }

        return uri.getHost();
    }

    /**
     * Builds an appropriately configured {@link Parser} for this server.
     *
     * @return A configured parser.
     */
    private Parser buildParser() {
        final MyInfo myInfo = buildMyInfo();
        final Parser myParser = new ParserFactory().getParser(myInfo, address);

        if (myParser instanceof SecureParser) {
            certificateManager = new CertificateManager(address.getHost(), getConfigManager());
            final SecureParser secureParser = (SecureParser) myParser;
            secureParser.setTrustManagers(new TrustManager[]{certificateManager});
            secureParser.setKeyManagers(certificateManager.getKeyManager());
            certificateManager.addCertificateProblemListener(this);
        }

        if (myParser instanceof EncodingParser) {
            final EncodingParser encodingParser = (EncodingParser) myParser;
            encodingParser.setEncoder(new MessageEncoder(this, myParser));
        }

        if (myParser != null) {
            myParser.setIgnoreList(ignoreList);
            myParser.setPingTimerInterval(getConfigManager().getOptionInt(DOMAIN_SERVER,
                    "pingtimer"));
            myParser.setPingTimerFraction((int) (getConfigManager().getOptionInt(DOMAIN_SERVER,
                    "pingfrequency") / myParser.getPingTimerInterval()));

            if (getConfigManager().hasOptionString(DOMAIN_GENERAL, "bindip")) {
                myParser.setBindIP(getConfigManager().getOption(DOMAIN_GENERAL, "bindip"));
            }
        }

        return myParser;
    }

    /**
     * Compare the given URI to the URI we are currently using to see if they
     * would both result in the server connecting to the same place, even if the
     * URIs do not match exactly.
     *
     * @param uri URI to compare with the Servers own URI.
     * @return True if the Given URI is the "same" as the one we are using.
     * @since 0.6.3
     */
    public boolean compareURI(final URI uri) {
        if (parser != null) {
            return parser.compareURI(uri);
        }

        if (oldParser != null) {
            return oldParser.compareURI(uri);
        }

        return false;
    }

    /**
     * Parses the specified hostmask in a manner prescribed by the protocol
     * currently used by this server.
     *
     * @see ProtocolDescription#parseHostmask(java.lang.String)
     * @param hostmask The hostmask to be parsed
     * @return An array containing the nickname, username and hostname
     * @since 0.6.4
     */
    public String[] parseHostmask(final String hostmask) {
        return protocolDescription.parseHostmask(hostmask);
    }

    /**
     * Retrieves the MyInfo object used for the Parser.
     *
     * @return The MyInfo object for our profile
     */
    @Precondition({
        "The current profile is not null",
        "The current profile specifies at least one nickname"
    })
    private MyInfo buildMyInfo() {
        Logger.assertTrue(profile != null);
        Logger.assertTrue(!profile.getOptionList(DOMAIN_PROFILE, "nicknames").isEmpty());

        final MyInfo myInfo = new MyInfo();
        myInfo.setNickname(profile.getOptionList(DOMAIN_PROFILE, "nicknames").get(0));
        myInfo.setRealname(profile.getOption(DOMAIN_PROFILE, "realname"));

        if (profile.hasOptionString(DOMAIN_PROFILE, "ident")) {
            myInfo.setUsername(profile.getOption(DOMAIN_PROFILE, "ident"));
        }

        return myInfo;
    }

    /**
     * Updates this server's icon.
     */
    private void updateIcon() {
        final String icon = myState.getState() == ServerState.CONNECTED
                ? protocolDescription.isSecure(address)
                ? "secure-server" : "server" : "server-disconnected";
        setIcon(icon);
    }

    /**
     * Registers callbacks.
     */
    private void doCallbacks() {
        if (raw != null) {
            raw.registerCallbacks();
        }

        eventHandler.registerCallbacks();

        for (Query query : queries.values()) {
            query.reregister();
        }
    }

    /**
     * Attempts to join the specified channels. If channels with the same name
     * already exist, they are (re)joined and their windows activated.
     *
     * @param requests The channel join requests to process
     * @since 0.6.4
     */
    public void join(final ChannelJoinRequest ... requests) {
        join(true, requests);
    }

    /**
     * Attempts to join the specified channels. If channels with the same name
     * already exist, they are (re)joined.
     *
     * @param focus Whether or not to focus any new channels
     * @param requests The channel join requests to process
     * @since 0.6.4
     */
    public void join(final boolean focus, final ChannelJoinRequest ... requests) {
        synchronized (myStateLock) {
            if (myState.getState() == ServerState.CONNECTED) {
                final List<ChannelJoinRequest> pending = new ArrayList<ChannelJoinRequest>();

                for (ChannelJoinRequest request : requests) {
                    removeInvites(request.getName());

                    final String name;
                    if (parser.isValidChannelName(request.getName())) {
                        name = request.getName();
                    } else {
                        name = parser.getChannelPrefixes().substring(0, 1)
                                + request.getName();
                    }

                    if (!hasChannel(name) || !getChannel(name).isOnChannel()) {
                        if (!focus) {
                            backgroundChannels.add(name);
                        }

                        pending.add(request);
                    }
                }

                parser.joinChannels(pending.toArray(new ChannelJoinRequest[pending.size()]));
            }
            // TODO: otherwise: address.getChannels().add(channel);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void sendLine(final String line) {
        synchronized (myStateLock) {
            try {
                parserLock.readLock().lock();
                if (parser != null && !line.isEmpty()
                        && myState.getState() == ServerState.CONNECTED) {
                    parser.sendRawMessage(line);
                }
            } finally {
                parserLock.readLock().unlock();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxLineLength() {
        try {
            parserLock.readLock().lock();
            return parser == null ? -1 : parser.getMaxLength();
        } finally {
            parserLock.readLock().unlock();
        }
    }

    /**
     * Retrieves the parser used for this connection.
     *
     * @return this connection's parser
     */
    public Parser getParser() {
        return parser;
    }

    /**
     * Retrieves the profile that's in use for this server.
     *
     * @return The profile in use by this server
     */
    public Identity getProfile() {
        return profile;
    }

    /**
     * Retrieves the possible channel prefixes in use on this server.
     *
     * @return This server's possible channel prefixes
     */
    public String getChannelPrefixes() {
        try {
            parserLock.readLock().lock();
            return parser == null ? "#&" : parser.getChannelPrefixes();
        } finally {
            parserLock.readLock().unlock();
        }
    }

    /**
     * Retrieves the address of this server.
     *
     * @return This sever's address
     */
    public String getAddress() {
        try {
            parserLock.readLock().lock();
            return parser == null ? address.getHost() : parser.getServerName();
        } finally {
            parserLock.readLock().unlock();
        }
    }

    /**
     * Retrieves the name of this server's network. The network name is
     * determined using the following rules:
     *
     *  1. If the server includes its network name in the 005 information, we
     *     use that
     *  2. If the server's name ends in biz, com, info, net or org, we use the
     *     second level domain (e.g., foo.com)
     *  3. If the server's name contains more than two dots, we drop everything
     *     up to and including the first part, and use the remainder
     *  4. In all other cases, we use the full server name
     *
     * @return The name of this server's network
     */
    public String getNetwork() {
        try {
            parserLock.readLock().lock();
            if (parser == null) {
                throw new IllegalStateException("getNetwork called when "
                        + "parser is null (state: " + getState() + ")");
            } else if (parser.getNetworkName().isEmpty()) {
                return getNetworkFromServerName(parser.getServerName());
            } else {
                return parser.getNetworkName();
            }
        } finally {
            parserLock.readLock().unlock();
        }
    }

    /**
     * Determines whether this server is currently connected to the specified
     * network.
     *
     * @param target The network to check for
     * @return True if this server is connected to the network, false otherwise
     * @since 0.6.3m1rc3
     */
    public boolean isNetwork(final String target) {
        synchronized (myStateLock) {
            try {
                parserLock.readLock().lock();
                if (parser == null) {
                    return false;
                } else {
                    return getNetwork().equalsIgnoreCase(target);
                }
            } finally {
                parserLock.readLock().unlock();
            }
        }
    }

    /**
     * Calculates a network name from the specified server name. This method
     * implements parts 2-4 of the procedure documented at getNetwork().
     *
     * @param serverName The server name to parse
     * @return A network name for the specified server
     */
    protected static String getNetworkFromServerName(final String serverName) {
        final String[] parts = serverName.split("\\.");
        final String[] tlds = {"biz", "com", "info", "net", "org"};
        boolean isTLD = false;

        for (String tld : tlds) {
            if (serverName.endsWith("." + tld)) {
                isTLD = true;
                break;
            }
        }

        if (isTLD && parts.length > 2) {
            return parts[parts.length - 2] + "." + parts[parts.length - 1];
        } else if (parts.length > 2) {
            final StringBuilder network = new StringBuilder();

            for (int i = 1; i < parts.length; i++) {
                if (network.length() > 0) {
                    network.append('.');
                }

                network.append(parts[i]);
            }

            return network.toString();
        } else {
            return serverName;
        }
    }

    /**
     * Retrieves the name of this server's IRCd.
     *
     * @return The name of this server's IRCd
     */
    public String getIrcd() {
        return parser.getServerSoftwareType();
    }

    /**
     * Retrieves the protocol used by this server.
     *
     * @return This server's protocol
     * @since 0.6.3
     */
    public String getProtocol() {
        return address.getScheme();
    }

    /**
     * Returns the current away status.
     *
     * @return True if the client is marked as away, false otherwise
     */
    public boolean isAway() {
        return awayMessage != null;
    }

    /**
     * Gets the current away message.
     *
     * @return Null if the client isn't away, or a textual away message if it is
     */
    public String getAwayMessage() {
        return awayMessage;
    }

    /** {@inheritDoc} */
    @Override
    public TabCompleter getTabCompleter() {
        return tabCompleter;
    }

    /**
     * Retrieves the current state for this server.
     *
     * @return This server's state
     */
    public ServerState getState() {
        return myState.getState();
    }

    /**
     * Retrieves the status object for this server. Effecting state transitions
     * on the object returned by this method will almost certainly cause
     * problems.
     *
     * @since 0.6.3m1
     * @return This server's status object.
     */
    public ServerStatus getStatus() {
        return myState;
    }

    /** {@inheritDoc} */
    @Override
    public void windowClosing() {
        synchronized (myStateLock) {
            // 2: Remove any callbacks or listeners
            eventHandler.unregisterCallbacks();

            // 3: Trigger any actions neccessary
            disconnect();

            myState.transition(ServerState.CLOSING);
        }

        closeChannels();
        closeQueries();
        removeInvites();

        if (raw != null) {
            raw.close();
        }

        // 4: Trigger action for the window closing
        // 5: Inform any parents that the window is closing
        ServerManager.getServerManager().unregisterServer(this);
    }

    /** {@inheritDoc} */
    @Override
    public void windowClosed() {
        // 7: Remove any references to the window and parents
        oldParser = null; //NOPMD
        parser = null; //NOPMD
    }

    /**
     * Passes the arguments to all frames for this server.
     *
     * @param messageType The type of message to send
     * @param args The arguments of the message
     */
    public void addLineToAll(final String messageType, final Object... args) {
        for (Channel channel : channels.values()) {
            channel.addLine(messageType, args);
        }

        for (Query query : queries.values()) {
            query.addLine(messageType, args);
        }

        addLine(messageType, args);
    }

    /**
     * Replies to an incoming CTCP message.
     *
     * @param source The source of the message
     * @param type The CTCP type
     * @param args The CTCP arguments
     */
    public void sendCTCPReply(final String source, final String type, final String args) {
        if (type.equalsIgnoreCase("VERSION")) {
            parser.sendCTCPReply(source, "VERSION", "DMDirc "
                    + getConfigManager().getOption("version", "version")
                    + " - http://www.dmdirc.com/");
        } else if (type.equalsIgnoreCase("PING")) {
            parser.sendCTCPReply(source, "PING", args);
        } else if (type.equalsIgnoreCase("CLIENTINFO")) {
            parser.sendCTCPReply(source, "CLIENTINFO", "VERSION PING CLIENTINFO");
        }
    }

    /**
     * Determines if the specified channel name is valid. A channel name is
     * valid if we already have an existing Channel with the same name, or
     * we have a valid parser instance and the parser says it's valid.
     *
     * @param channelName The name of the channel to test
     * @return True if the channel name is valid, false otherwise
     */
    public boolean isValidChannelName(final String channelName) {
        try {
            parserLock.readLock().lock();
            return hasChannel(channelName)
                    || (parser != null && parser.isValidChannelName(channelName));
        } finally {
            parserLock.readLock().unlock();
        }
    }

    /**
     * Returns the server instance associated with this frame.
     *
     * @return the associated server connection
     */
    @Override
    public Server getServer() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean processNotificationArg(final Object arg, final List<Object> args) {
        if (arg instanceof ClientInfo) {
            final ClientInfo clientInfo = (ClientInfo) arg;
            args.add(clientInfo.getNickname());
            args.add(clientInfo.getUsername());
            args.add(clientInfo.getHostname());
            return true;
        } else {
            return super.processNotificationArg(arg, args);
        }
    }

    /**
     * Updates the name and title of this window.
     */
    public void updateTitle() {
        synchronized (myStateLock) {
            if (myState.getState() == ServerState.CLOSING) {
                return;
            }

            try {
                parserLock.readLock().lock();
                final Object[] arguments = new Object[]{
                    address.getHost(), parser == null ? "Unknown" : parser.getServerName(),
                    address.getPort(), parser == null ? "Unknown" : getNetwork(),
                    parser == null ? "Unknown" : parser.getLocalClient().getNickname(),
                };

                setName(Formatter.formatMessage(getConfigManager(),
                        "serverName", arguments));
                setTitle(Formatter.formatMessage(getConfigManager(),
                        "serverTitle", arguments));
            } finally {
                parserLock.readLock().unlock();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void configChanged(final String domain, final String key) {
        if ("formatter".equals(domain)) {
            updateTitle();
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Parser callbacks">

    /**
     * Called when the server says that the nickname we're trying to use is
     * already in use.
     *
     * @param nickname The nickname that we were trying to use
     */
    public void onNickInUse(final String nickname) {
        final String lastNick = parser.getLocalClient().getNickname();

        // If our last nick is still valid, ignore the in use message
        if (!converter.equalsIgnoreCase(lastNick, nickname)) {
            return;
        }

        String newNick = lastNick + new Random().nextInt(10);

        final List<String> alts = profile.getOptionList(DOMAIN_PROFILE, "nicknames");
        int offset = 0;

        // Loop so we can check case sensitivity
        for (String alt : alts) {
            offset++;
            if (converter.equalsIgnoreCase(alt, lastNick)) {
                break;
            }
        }

        if (offset < alts.size() && !alts.get(offset).isEmpty()) {
            newNick = alts.get(offset);
        }

        parser.getLocalClient().setNickname(newNick);
    }

    /**
     * Called when the server sends a numeric event.
     *
     * @param numeric The numeric code for the event
     * @param tokens The (tokenised) arguments of the event
     */
    public void onNumeric(final int numeric, final String[] tokens) {
        String snumeric = String.valueOf(numeric);

        if (numeric < 10) {
            snumeric = "00" + snumeric;
        } else if (numeric < 100) {
            snumeric = "0" + snumeric;
        }

        final String sansIrcd = "numeric_" + snumeric;
        StringBuffer target = new StringBuffer("");

        if (getConfigManager().hasOptionString("formatter", sansIrcd)) {
            target = new StringBuffer(sansIrcd);
        } else if (getConfigManager().hasOptionString("formatter", "numeric_unknown")) {
            target = new StringBuffer("numeric_unknown");
        }

        ActionManager.getActionManager().triggerEvent(
                CoreActionType.SERVER_NUMERIC, target, this,
                Integer.valueOf(numeric), tokens);

        handleNotification(target.toString(), (Object[]) tokens);
    }

    /**
     * Called when the socket has been closed.
     */
    public void onSocketClosed() {
        if (Thread.holdsLock(myStateLock)) {
            new Thread(new Runnable() {
                /** {@inheritDoc} */
                @Override
                public void run() {
                    onSocketClosed();
                }
            }, "Socket closed deferred thread").start();
            return;
        }

        handleNotification("socketClosed", getAddress());

        ActionManager.getActionManager().triggerEvent(
                CoreActionType.SERVER_DISCONNECTED, null, this);

        eventHandler.unregisterCallbacks();

        synchronized (myStateLock) {
            if (myState.getState() == ServerState.CLOSING
                    || myState.getState() == ServerState.DISCONNECTED) {
                // This has been triggered via .disconnect()
                return;
            }

            if (myState.getState() == ServerState.DISCONNECTING) {
                myState.transition(ServerState.DISCONNECTED);
            } else {
                myState.transition(ServerState.TRANSIENTLY_DISCONNECTED);
            }

            clearChannels();

            try {
                parserLock.writeLock().lock();
                oldParser = parser;
                parser = null;
            } finally {
                parserLock.writeLock().unlock();
            }

            updateIcon();

            if (getConfigManager().getOptionBool(DOMAIN_GENERAL,
                    "closechannelsondisconnect")) {
                closeChannels();
            }

            if (getConfigManager().getOptionBool(DOMAIN_GENERAL,
                    "closequeriesondisconnect")) {
                closeQueries();
            }

            removeInvites();
            updateAwayState(null);

            if (getConfigManager().getOptionBool(DOMAIN_GENERAL,
                    "reconnectondisconnect")
                    && myState.getState() == ServerState.TRANSIENTLY_DISCONNECTED) {
                doDelayedReconnect();
            }
        }
    }

    /**
     * Called when an error was encountered while connecting.
     *
     * @param errorInfo The parser's error information
     */
    @Precondition("The current server state is CONNECTING")
    public void onConnectError(final ParserError errorInfo) {
        synchronized (myStateLock) {
            if (myState.getState() == ServerState.CLOSING
                    || myState.getState() == ServerState.DISCONNECTING) {
                // Do nothing
                return;
            } else if (myState.getState() != ServerState.CONNECTING) {
                // Shouldn't happen
                throw new IllegalStateException("Connect error when not "
                        + "connecting\n\n" + getStatus().getTransitionHistory());
            }

            myState.transition(ServerState.TRANSIENTLY_DISCONNECTED);

            try {
                parserLock.writeLock().lock();
                oldParser = parser;
                parser = null;
            } finally {
                parserLock.writeLock().unlock();
            }

            updateIcon();

            String description;

            if (errorInfo.getException() == null) {
                description = errorInfo.getData();
            } else {
                final Exception exception = errorInfo.getException();

                if (exception instanceof java.net.UnknownHostException) {
                    description = "Unknown host (unable to resolve)";
                } else if (exception instanceof java.net.NoRouteToHostException) {
                    description = "No route to host";
                } else if (exception instanceof java.net.SocketTimeoutException) {
                    description = "Connection attempt timed out";
                } else if (exception instanceof java.net.SocketException
                        || exception instanceof javax.net.ssl.SSLException) {
                    description = exception.getMessage();
                } else {
                    Logger.appError(ErrorLevel.LOW, "Unknown socket error: "
                            + exception.getClass().getCanonicalName(),
                            new IllegalArgumentException(exception));
                    description = "Unknown error: " + exception.getMessage();
                }
            }

            ActionManager.getActionManager().triggerEvent(
                    CoreActionType.SERVER_CONNECTERROR, null, this,
                    description);

            handleNotification("connectError", getAddress(), description);

            if (getConfigManager().getOptionBool(DOMAIN_GENERAL,
                    "reconnectonconnectfailure")) {
                doDelayedReconnect();
            }
        }
    }

    /**
     * Called when we fail to receive a ping reply within a set period of time.
     */
    public void onPingFailed() {
        StatusBarManager.getStatusBarManager().setMessage(new StatusMessage(
                "No ping reply from " + getName() + " for over "
                + ((int) (Math.floor(parser.getPingTime() / 1000.0)))
                + " seconds.", getConfigManager()));

        ActionManager.getActionManager().triggerEvent(
                CoreActionType.SERVER_NOPING, null, this,
                Long.valueOf(parser.getPingTime()));

        if (parser.getPingTime()
                 >= getConfigManager().getOptionInt(DOMAIN_SERVER, "pingtimeout")) {
            handleNotification("stonedServer", getAddress());
            reconnect();
        }
    }

    /**
     * Called after the parser receives the 005 headers from the server.
     */
    @Precondition("State is CONNECTING")
    public void onPost005() {
        synchronized (myStateLock) {
            if (myState.getState() != ServerState.CONNECTING) {
                // Shouldn't happen
                throw new IllegalStateException("Received onPost005 while not "
                        + "connecting\n\n" + myState.getTransitionHistory());
            }

            if (myState.getState() != ServerState.CONNECTING) {
                // We've transitioned while waiting for the lock. Just abort.
                return;
            }

            myState.transition(ServerState.CONNECTED);

            getConfigManager().migrate(address.getScheme(),
                    parser.getServerSoftwareType(), getNetwork(), parser.getServerName());

            updateIcon();
            updateTitle();
            updateIgnoreList();

            converter = parser.getStringConverter();

            final List<ChannelJoinRequest> requests = new ArrayList<ChannelJoinRequest>();
            if (getConfigManager().getOptionBool(DOMAIN_GENERAL, "rejoinchannels")) {
                for (Channel chan : channels.values()) {
                    requests.add(new ChannelJoinRequest(chan.getName()));
                }
            }
            join(requests.toArray(new ChannelJoinRequest[0]));

            checkModeAliases();
        }

        ActionManager.getActionManager().triggerEvent(
                CoreActionType.SERVER_CONNECTED, null, this);
    }

    /**
     * Checks that we have the necessary mode aliases for this server.
     */
    private void checkModeAliases() {
        // Check we have mode aliases
        final String modes = parser.getBooleanChannelModes() + parser.getListChannelModes()
                + parser.getParameterChannelModes() + parser.getDoubleParameterChannelModes();
        final String umodes = parser.getUserModes();

        final StringBuffer missingModes = new StringBuffer();
        final StringBuffer missingUmodes = new StringBuffer();

        for (char mode : modes.toCharArray()) {
            if (!getConfigManager().hasOptionString(DOMAIN_SERVER, "mode" + mode)) {
                missingModes.append(mode);
            }
        }

        for (char mode : umodes.toCharArray()) {
            if (!getConfigManager().hasOptionString(DOMAIN_SERVER, "umode" + mode)) {
                missingUmodes.append(mode);
            }
        }

        if (missingModes.length() + missingUmodes.length() > 0) {
            final StringBuffer missing = new StringBuffer("Missing mode aliases: ");

            if (missingModes.length() > 0) {
                missing.append("channel: +");
                missing.append(missingModes);
            }

            if (missingUmodes.length() > 0) {
                if (missingModes.length() > 0) {
                    missing.append(' ');
                }

                missing.append("user: +");
                missing.append(missingUmodes);
            }

            Logger.appError(ErrorLevel.LOW, missing.toString() + " ["
                    + parser.getServerSoftwareType() + "]",
                    new MissingModeAliasException(getNetwork(), parser,
                    getConfigManager().getOption("identity",
                    "modealiasversion"), missing.toString()));
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Ignore lists">

    /**
     * Retrieves this server's ignore list.
     *
     * @return This server's ignore list
     */
    public IgnoreList getIgnoreList() {
        return ignoreList;
    }

    /**
     * Updates this server's ignore list to use the entries stored in the
     * config manager.
     */
    public void updateIgnoreList() {
        ignoreList.clear();
        ignoreList.addAll(getConfigManager().getOptionList("network", "ignorelist"));
    }

    /**
     * Saves the contents of our ignore list to the network identity.
     */
    public void saveIgnoreList() {
        getNetworkIdentity().setOption("network", "ignorelist", ignoreList.getRegexList());
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Identity handling">

    /**
     * Retrieves the identity for this server.
     *
     * @return This server's identity
     */
    public Identity getServerIdentity() {
        return IdentityManager.getServerConfig(parser.getServerName());
    }

    /**
     * Retrieves the identity for this server's network.
     *
     * @return This server's network identity
     */
    public Identity getNetworkIdentity() {
        return IdentityManager.getNetworkConfig(getNetwork());
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Invite handling">

    /**
     * Adds an invite listener to this server.
     *
     * @param listener The listener to be added
     */
    public void addInviteListener(final InviteListener listener) {
        synchronized (listeners) {
            listeners.add(InviteListener.class, listener);
        }
    }

    /**
     * Removes an invite listener from this server.
     *
     * @param listener The listener to be removed
     */
    public void removeInviteListener(final InviteListener listener) {
        synchronized (listeners) {
            listeners.remove(InviteListener.class, listener);
        }
    }

    /**
     * Adds an invite to this server, and fires the appropriate listeners.
     *
     * @param invite The invite to be added
     */
    public void addInvite(final Invite invite) {
        synchronized (invites) {
            for (Invite oldInvite : new ArrayList<Invite>(invites)) {
                if (oldInvite.getChannel().equals(invite.getChannel())) {
                    removeInvite(oldInvite);
                }
            }

            invites.add(invite);

            synchronized (listeners) {
                for (InviteListener listener : listeners.get(InviteListener.class)) {
                    listener.inviteReceived(this, invite);
                }
            }
        }
    }

    /**
     * Attempts to accept the specified invites, and join the corresponding
     * channels.
     *
     * @param invites The invites to process
     * @since 0.6.4
     */
    public void acceptInvites(final Invite ... invites) {
        final ChannelJoinRequest[] requests = new ChannelJoinRequest[invites.length];

        for (int i = 0; i < invites.length; i++) {
            requests[i] = new ChannelJoinRequest(invites[i].getChannel());
        }

        join(requests);
    }

    /**
     * Attempts to accept all active invites for this server, and join the
     * corresponding channels.
     *
     * @since 0.6.4
     */
    public void acceptInvites() {
        synchronized (invites) {
            acceptInvites(invites.toArray(new Invite[invites.size()]));
        }
    }

    /**
     * Removes all invites for the specified channel.
     *
     * @param channel The channel to remove invites for
     */
    public void removeInvites(final String channel) {
        for (Invite invite : new ArrayList<Invite>(invites)) {
            if (invite.getChannel().equals(channel)) {
                removeInvite(invite);
            }
        }
    }

    /**
     * Removes all invites for all channels.
     */
    public void removeInvites() {
        for (Invite invite : new ArrayList<Invite>(invites)) {
            removeInvite(invite);
        }
    }

    /**
     * Removes an invite from this server, and fires the appropriate listeners.
     *
     * @param invite The invite to be removed
     */
    public void removeInvite(final Invite invite) {
        synchronized (invites) {
            invites.remove(invite);

            synchronized (listeners) {
                for (InviteListener listener : listeners.get(InviteListener.class)) {
                    listener.inviteExpired(this, invite);
                }
            }
        }
    }

    /**
     * Retusnt the list of invites for this server.
     *
     * @return Invite list
     */
    public List<Invite> getInvites() {
        return invites;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Away state handling">

    /**
     * Adds an away state lisener to this server.
     *
     * @param listener The listener to be added
     */
    public void addAwayStateListener(final AwayStateListener listener) {
        synchronized (listeners) {
            listeners.add(AwayStateListener.class, listener);
        }
    }

    /**
     * Removes an away state lisener from this server.
     *
     * @param listener The listener to be removed
     */
    public void removeAwayStateListener(final AwayStateListener listener) {
        synchronized (listeners) {
            listeners.remove(AwayStateListener.class, listener);
        }
    }

    /**
     * Updates our away state and fires the relevant listeners.
     *
     * @param message The away message to use, or null if we're not away.
     */
    public void updateAwayState(final String message) {
        if ((awayMessage != null && awayMessage.equals(message))
                || (awayMessage == null && message == null)) {
            return;
        }

        awayMessage = message;

        new Thread(new Runnable() {

            /** {@inheritDoc} */
            @Override
            public void run() {
                synchronized (listeners) {
                    if (message == null) {
                        for (AwayStateListener listener : listeners.get(AwayStateListener.class)) {
                            listener.onBack();
                        }
                    } else {
                        for (AwayStateListener listener : listeners.get(AwayStateListener.class)) {
                            listener.onAway(message);
                        }
                    }
                }
            }
        }, "Away state listener runner").start();
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="TLS listener handling">

    /**
     * Adds a new certificate problem listener to this server. If there is
     * currently an on-going problem with a certificate, the listener will
     * be called immediately before this method returns.
     *
     * @param listener The listener to be added
     */
    public void addCertificateProblemListener(final CertificateProblemListener listener) {
        listeners.add(CertificateProblemListener.class, listener);

        if (certificateManager != null && !certificateManager.getProblems().isEmpty()) {
            listener.certificateProblemEncountered(certificateManager.getChain(),
                    certificateManager.getProblems(), certificateManager);
        }
    }

    /**
     * Removes the specified listener from this server.
     *
     * @param listener The listener to be removed
     */
    public void removeCertificateProblemListener(final CertificateProblemListener listener) {
        listeners.remove(CertificateProblemListener.class, listener);
    }

    /** {@inheritDoc} */
    @Override
    public void certificateProblemEncountered(final X509Certificate[] chain,
            final Collection<CertificateException> problems,
            final CertificateManager certificateManager) {
        for (CertificateProblemListener listener : listeners.get(CertificateProblemListener.class)) {
            listener.certificateProblemEncountered(chain, problems, certificateManager);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void certificateProblemResolved(final CertificateManager manager) {
        for (CertificateProblemListener listener : listeners.get(CertificateProblemListener.class)) {
            listener.certificateProblemResolved(manager);
        }
    }

    // </editor-fold>

}
