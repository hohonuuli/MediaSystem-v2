package hs.mediasystem.db.services;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.State;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Recommendation;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.mediamanager.DescriptorStore;
import hs.mediasystem.mediamanager.StreamableStore;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RecommendationService {
  @Inject private WorksService worksService;
  @Inject private WorkService workService;
  @Inject private StreamableStore streamStore;
  @Inject private DescriptorStore descriptorStore;

  /**
   * Finds recommendations on what to continue watching or what to watch next.
   *
   * @param maximum the maximum amount of items to return
   * @return a {@link List} of {@link Recommendation}s, never null but can be empty
   */
  public List<Recommendation> findRecommendations(int maximum) {
    return worksService.findLastWatched(maximum * 2, null).stream()    // Find twice as much matched last watched Works, as Works that are consecutive will get filtered and only last one is added
      .filter(r -> r.getType().isPlayable())
      .map(this::toPartiallyWatchedOrNextUnwatchedRecommendation)
      .flatMap(Optional::stream)
      .limit(maximum)
      .collect(Collectors.toList());
  }

  /**
   * Finds new items added to the system.
   *
   * @param filter a filter, cannot be null
   * @return a {@link List} of {@link Recommendation}s, never null but can be empty
   */
  public List<Recommendation> findNew(Predicate<MediaType> filter) {
    return worksService.findNewest(100, filter).stream()
      .map(this::toNewRecommendation)
      .flatMap(Optional::stream)
      .collect(Collectors.toList());
  }

  private Optional<Recommendation> toNewRecommendation(Work work) {
    MediaStream stream = work.getPrimaryStream().orElseThrow();
    StreamID id = stream.getId();
    State state = work.getState();
    Duration position = state.getResumePosition();
    Duration length = stream.getMetaData().map(StreamMetaData::getLength).orElse(null);
    boolean watched = state.isConsumed();

    return getIdentifier(id)
      .flatMap(descriptorStore::find)
      .map(descriptor -> new Recommendation(stream.getAttributes().getCreationTime(), work, null, descriptor, id, length, position, watched));
  }

  private Optional<Recommendation> toPartiallyWatchedOrNextUnwatchedRecommendation(Work work) {
    MediaStream stream = work.getPrimaryStream().orElseThrow();
    StreamID parentId = stream.getParentId().orElse(null);

    if(parentId == null) {
      return toProductionRecommendation(work);
    }

    return toEpisodeRecommendation(work);
  }

  private Optional<Recommendation> toProductionRecommendation(Work work) {
    MediaStream stream = work.getPrimaryStream().orElseThrow();
    StreamID id = stream.getId();
    State state = work.getState();
    Duration position = state.getResumePosition();
    Duration length = stream.getMetaData().map(StreamMetaData::getLength).orElse(null);
    Instant lastWatchedTime = state.getLastConsumptionTime().orElseThrow();
    boolean watched = state.isConsumed();

    return getIdentifier(id).flatMap(descriptorStore::find).map(descriptor -> {
      if(watched) {
        // TODO Must be a movie, find collection for "next"
      }
      else if(!position.isZero()) {  // Partially watched movie
        return new Recommendation(lastWatchedTime, work, null, descriptor, id, length, position, watched);
      }

      return null;
    });
  }

  private Optional<Recommendation> toEpisodeRecommendation(Work work) {
    MediaStream stream = work.getPrimaryStream().orElseThrow();
    StreamID parentId = stream.getParentId().orElseThrow(() -> new IllegalArgumentException("work must represent an episode: " + work));
    StreamID id = stream.getId();
    State state = work.getState();

    boolean watched = state.isConsumed();
    Duration position = state.getResumePosition();
    Duration length = stream.getMetaData().map(StreamMetaData::getLength).orElse(null);
    Instant lastWatchedTime = state.getLastConsumptionTime().orElseThrow();

    return getIdentifier(parentId).flatMap(
      identifier -> descriptorStore.find(identifier).filter(Serie.class::isInstance).map(Serie.class::cast).map(serie -> {
        Episode episode = (Episode)descriptorStore.find(work.getDescriptor().getIdentifier()).orElse(null);

        if(episode != null) {
          if(watched) {
            return serie.findNextEpisode((Episode)descriptorStore.find(work.getDescriptor().getIdentifier()).orElse(null))
              .flatMap(nextEpisode -> streamStore.findStreams(nextEpisode.getIdentifier()).stream().findFirst()  // Episode to Stream
                .map(Streamable::getId)
                .flatMap(workService::find)
                .map(r -> {
                  if(!r.getState().isConsumed() && r.getState().getResumePosition().isZero()) {
                    return new Recommendation(lastWatchedTime, r, serie, nextEpisode, r.getPrimaryStream().orElseThrow().getId(), null, r.getState().getResumePosition(), false);
                  }

                  return null;
                })
              )
              .orElse(null);
          }
          else if(!position.isZero()) {
            return new Recommendation(lastWatchedTime, work, serie, episode, id, length, position, false);
          }
        }

        return null;
      })
    );
  }

  private Optional<Identifier> getIdentifier(StreamID id) {
    return streamStore.findIdentification(id).map(Identification::getPrimaryIdentifier);
  }
}
