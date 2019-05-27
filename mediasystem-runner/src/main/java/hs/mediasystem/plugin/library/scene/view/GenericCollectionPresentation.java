package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.db.SettingsStore;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.util.NaturalLanguage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

public class GenericCollectionPresentation extends GridViewPresentation<Production> {
  public final ObservableList<MediaItem<Production>> items;

  private static final List<SortOrder<Production>> SORT_ORDERS = List.of(
    new SortOrder<Production>("release-date", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())),
    new SortOrder<Production>("alpha", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getName, NaturalLanguage.ALPHABETICAL)))
  );

  private static final List<Filter<Production>> FILTERS = List.of(
    new Filter<Production>("none", mi -> true),
    new Filter<Production>("released-recently", mi -> Optional.ofNullable(mi.getProduction().getDate()).filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  @Singleton
  public static class Factory {
    @Inject private MediaService mediaService;
    @Inject private SettingsStore settingsStore;

    public GenericCollectionPresentation create(ObservableList<MediaItem<Production>> items) {
      return new GenericCollectionPresentation(settingsStore, mediaService, items);
    }
  }

  protected GenericCollectionPresentation(SettingsStore settingsStore, MediaService mediaService, ObservableList<MediaItem<Production>> items) {
    super(settingsStore, mediaService, SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.AVAILABLE, StateFilter.UNWATCHED));

    this.items = items;
  }
}
