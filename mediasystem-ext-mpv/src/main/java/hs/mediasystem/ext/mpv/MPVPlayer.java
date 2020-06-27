package hs.mediasystem.ext.mpv;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.LongByReference;

import hs.mediasystem.ext.mpv.MPV.mpv_event;
import hs.mediasystem.ui.api.player.AudioTrack;
import hs.mediasystem.ui.api.player.PlayerEvent;
import hs.mediasystem.ui.api.player.PlayerEvent.Type;
import hs.mediasystem.ui.api.player.PlayerPresentation;
import hs.mediasystem.ui.api.player.PlayerWindowIdSupplier;
import hs.mediasystem.ui.api.player.StatOverlay;
import hs.mediasystem.ui.api.player.Subtitle;
import hs.mediasystem.util.javafx.Events;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;

import org.reactfx.Change;
import org.reactfx.EventStreams;
import org.reactfx.SuspendableEventStream;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

public class MPVPlayer implements PlayerPresentation {
  private static final Logger LOGGER = Logger.getLogger(MPVPlayer.class.getName());
  private static final ObservableList<StatOverlay> STAT_OVERLAYS = FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(
    StatOverlay.DISABLED,
    new StatOverlay(1, "Main")
  ));

  private final MPV mpv = MPV.INSTANCE;
  private final long handle;
  private final long wid;

  private final Var<Long> position = Var.newSimpleVar(0L);
  private final Var<Long> duration = Var.newSimpleVar(0L);
  private final Var<Double> rate = Var.newSimpleVar(1.0);
  private final Var<Long> subtitleDelay = Var.newSimpleVar(0L);
  private final Var<Long> audioDelay = Var.newSimpleVar(0L);
  private final Var<Long> volume = Var.newSimpleVar(100L);
  private final Var<Double> brightness = Var.newSimpleVar(1.0);
  private final Var<AudioTrack> audioTrack = Var.newSimpleVar(null);
  private final Var<Subtitle> subtitle = Var.newSimpleVar(null);
  private final Var<StatOverlay> statOverlay = Var.newSimpleVar(StatOverlay.DISABLED);
  private final BooleanProperty paused = new SimpleBooleanProperty(false);
  private final BooleanProperty muted = new SimpleBooleanProperty(false);

  private final ObjectProperty<EventHandler<PlayerEvent>> onPlayerEvent = new SimpleObjectProperty<>();
  private final ObservableList<Subtitle> subtitles = FXCollections.observableArrayList(Subtitle.DISABLED);
  private final ObservableList<AudioTrack> audioTracks = FXCollections.observableArrayList(AudioTrack.NO_AUDIO_TRACK);

  private final SuspendableEventStream<Change<Long>> positionChanges = EventStreams.changesOf(position).suppressible();

  private final AtomicBoolean isPlaying = new AtomicBoolean(false);

  private final ChangeListener<? super AudioTrack> audioTrackListener = (obs, p, v) -> setProperty("aid", v.getId() == -1 ? "no" : "" + v.getId());
  private final ChangeListener<? super Subtitle> subtitleTrackListener = (obs, p, v) -> setProperty("sid", v.getId() == -1 ? "no" : "" + v.getId());

  private volatile boolean quit;

  public MPVPlayer(PlayerWindowIdSupplier supplier) {
    this.handle = mpv.mpv_create();

    if(this.handle == 0) {
      throw new IllegalStateException("MPV could not be created (out of memory?)");
    }

    try {
      this.wid = supplier.getWindowId();
      int errorCode = mpv.mpv_set_option(handle, "wid", 4, new LongByReference(wid).getPointer());

      if(errorCode != 0) {
        throw new IllegalStateException("Error setting window id: " + errorCode);
      }

      setProperty("video-sync", "display-resample");  // default uses audio for video-sync, this is a newer better option that is vsync aware
      setProperty("load-stats-overlay", "yes");

      errorCode = mpv.mpv_initialize(handle);

      if(errorCode != 0) {
        throw new IllegalStateException("Error initializing MPV: " + errorCode);
      }

      errorCode = mpv.mpv_observe_property(handle, 0, "time-pos", 0);

      if(errorCode != 0) {
        throw new IllegalStateException("Error initializing property: " + errorCode);
      }
    }
    catch(Exception e) {
      mpv.mpv_destroy(handle);

      throw e;
    }

    Thread thread = new Thread(this::eventLoop, "MPV-EventLoop");

    thread.setDaemon(true);
    thread.start();

    initListeners();
  }

  private void initListeners() {
    positionChanges.observe(c -> setProperty("time-pos", "" + ((double)c.getNewValue()) / 1000));

    rate.addListener((obs, p, v) -> setProperty("speed", "" + v));
    subtitleDelay.addListener((obs, p, v) -> setProperty("sub-delay", "" + v / 1000.0));
    volume.addListener((obs, p, v) -> setProperty("volume", "" + v));
    audioDelay.addListener((obs, p, v) -> setProperty("audio-delay", "" + v / 1000.0));
    brightness.addListener((obs, p, v) -> setProperty("brightness", "" + (int)((v - 1) * 100)));

    paused.addListener((obs, old, current) -> setProperty("pause", current ? "yes" : "no"));
    muted.addListener((obs, old, current) -> setProperty("mute", current ? "yes" : "no"));

    statOverlay.addListener(new ChangeListener<StatOverlay>() {
      private boolean visible;

      @Override
      public void changed(ObservableValue<? extends StatOverlay> obs, StatOverlay p, StatOverlay v) {
        if(v.getId() >= 1) {
          if(!visible) {
            mpv.mpv_command(handle, new String[] {"script-binding", "stats/display-stats-toggle"});
            visible = true;
          }
        }
        else {
          if(visible) {
            mpv.mpv_command(handle, new String[] {"script-binding", "stats/display-stats-toggle"});
            visible = false;
          }
        }
      }
    });

    addTrackListeners();
  }

  private void addTrackListeners() {
    audioTrack.addListener(audioTrackListener);
    subtitle.addListener(subtitleTrackListener);
  }

  private void removeTrackListeners() {
    audioTrack.removeListener(audioTrackListener);
    subtitle.removeListener(subtitleTrackListener);
  }

  @Override
  public void play(String uriString, long positionInMillis) {
    ensureAlive();

    // Reset some properties
    removeTrackListeners();

    audioTracks.setAll(AudioTrack.NO_AUDIO_TRACK);
    subtitles.setAll(Subtitle.DISABLED);
    audioTrack.setValue(AudioTrack.NO_AUDIO_TRACK);
    subtitle.setValue(Subtitle.DISABLED);

    addTrackListeners();

    URI uri = URI.create(uriString);
    String decodedUri = uri.getScheme().equalsIgnoreCase("file") ? Paths.get(uri).toString() : uri.toString();

    LOGGER.fine("Playing \"" + decodedUri + "\" @ " + positionInMillis + " ms");

    setProperty("start", "" + positionInMillis / 1000);

    int errorCode = mpv.mpv_command(handle, new String[] {"loadfile", decodedUri});

    if(errorCode != 0) {
      throw new IllegalStateException("Error playing: \"" + uri + "\", error code: " + errorCode);
    }
  }

  @Override
  public void stop() {
    ensureAlive();

    isPlaying.set(false);
    mpv.mpv_command(handle, new String[] {"stop"});
  }

  @Override
  public void dispose() {
    mpv.mpv_command(handle, new String[] {"quit"});
    quit = true;
  }

  void ensureAlive() {
    if(quit) {
      throw new IllegalStateException("player has been disposed");
    }
  }

  @Override
  public void showSubtitle(Path path) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Val<Long> lengthProperty() {
    return duration;
  }

  @Override
  public Property<Long> positionProperty() {
    return position;
  }

  @Override
  public Property<Long> volumeProperty() {
    return volume;
  }

  @Override
  public BooleanProperty mutedProperty() {
    return muted;
  }

  @Override
  public BooleanProperty pausedProperty() {
    return paused;
  }

  @Override
  public Property<Long> subtitleDelayProperty() {
    return subtitleDelay;
  }

  @Override
  public Property<Subtitle> subtitleProperty() {
    return subtitle;
  }

  @Override
  public ObservableList<Subtitle> subtitles() {
    return FXCollections.unmodifiableObservableList(subtitles);
  }

  @Override
  public Property<Double> rateProperty() {
    return rate;
  }

  @Override
  public Property<Long> audioDelayProperty() {
    return audioDelay;
  }

  @Override
  public Property<AudioTrack> audioTrackProperty() {
    return audioTrack;
  }

  @Override
  public ObservableList<AudioTrack> audioTracks() {
    return FXCollections.unmodifiableObservableList(audioTracks);
  }

  @Override
  public Property<StatOverlay> statOverlayProperty() {
    return statOverlay;
  }

  @Override
  public ObservableList<StatOverlay> statOverlays() {
    return STAT_OVERLAYS;
  }

  @Override
  public Property<Double> brightnessProperty() {
    return brightness;
  }

  @Override
  public Node getDisplayComponent() {
    return null;
  }

  @Override
  public ObjectProperty<EventHandler<PlayerEvent>> onPlayerEvent() {
    return onPlayerEvent;
  }

  private void eventLoop() {
    for(;;) {
      mpv_event event = mpv.mpv_wait_event(handle, 1);  // wait max one second

      if(quit) {
        LOGGER.fine("Stopping MPV Event Loop thread");

        mpv.mpv_destroy(handle);

        return;
      }

      if(event.event_id == MPV.MPV_EVENT_PROPERTY_CHANGE) {
        String timePos = getProperty("time-pos");

        if(timePos != null) {
          Platform.runLater(() -> positionChanges.suspendWhile(() -> position.setValue((long)(Double.valueOf(timePos) * 1000))));
        }
      }
      else if(event.event_id == MPV.MPV_EVENT_START_FILE) {
        LOGGER.fine("Event MPV_EVENT_START_FILE");

        isPlaying.set(true);
      }
      else if(event.event_id == MPV.MPV_EVENT_FILE_LOADED) {
        LOGGER.fine("Event MPV_EVENT_FILE_LOADED");

        String durationText = getProperty("duration");

        if(durationText != null) {
          Platform.runLater(() -> duration.setValue((long)(Double.valueOf(durationText) * 1000)));
        }

        Platform.runLater(() -> {
          int trackCount = Integer.parseInt(getProperty("track-list/count"));

          for(int i = 0; i < trackCount; i++) {
            String type = getProperty("track-list/" + i + "/type");

            if(!type.equals("video")) {
              int id = Integer.parseInt(getProperty("track-list/" + i + "/id"));
              String lang = getProperty("track-list/" + i + "/lang");
              String title = getProperty("track-list/" + i + "/title");
              String selected = getProperty("track-list/" + i + "/selected");
              String description = createDescription(title, lang, id);

              LOGGER.fine("Found Track of type \"" + type + "\", title=" + title + ", lang=" + lang + ", selected=" + selected);

              if(type.equals("audio")) {
                AudioTrack track = new AudioTrack(id, description);

                audioTracks.add(track);

                if(selected.equals("yes")) {
                  audioTrack.setValue(track);
                }
              }
              else if(type.equals("sub")) {
                Subtitle track = new Subtitle(id, description);

                subtitles.add(track);

                if(selected.equals("yes")) {
                  subtitle.setValue(track);
                }
              }
            }
          }
        });
      }
      else if(event.event_id == MPV.MPV_EVENT_END_FILE) {
        LOGGER.fine("Event MPV_EVENT_END_FILE");

        if(isPlaying.getAndSet(false)) {  // set isPlaying to false, and if it previously was true, also dispatch event
          Events.dispatchEvent(onPlayerEvent, new PlayerEvent(Type.FINISHED), null);
        }
      }
      else if(event.event_id != MPV.MPV_EVENT_NONE) {
        LOGGER.finest("Unused Event Received: " + event.event_id);
      }
    }
  }

  private static String createDescription(String title, String language, int id) {
    return title != null ? title + (language != null ? " (" + language + ")" : "")
      : language != null ? language
                         : "Track " + id;
  }

  private String getProperty(String propertyName) {
    ensureAlive();

    Pointer ptr = mpv.mpv_get_property_string(handle, propertyName);

    if(ptr != null) {
      try {
        return ptr.getString(0, "UTF-8");
      }
      finally {
        mpv.mpv_free(ptr);
      }
    }

    return null;
  }

  private void setProperty(String propertyName, String value) {
    ensureAlive();

    int errorCode = mpv.mpv_set_property_string(handle, propertyName, value);

    LOGGER.finest("Setting \"" + propertyName + "\" to: " + value);

    if(errorCode != 0) {
      if(errorCode == MPV.MPV_ERROR_PROPERTY_NOT_FOUND) {
        throw new IllegalStateException("Error while setting property \"" + propertyName + "\" to \"" + value + "\": property not found");
      }

      throw new IllegalStateException("Error while setting property \"" + propertyName + "\" to \"" + value + "\": " + errorCode);
    }
  }
}
