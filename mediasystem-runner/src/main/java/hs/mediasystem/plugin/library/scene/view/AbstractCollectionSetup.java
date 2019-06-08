package hs.mediasystem.plugin.library.scene.view;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.CollectionDetails;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.javafx.ItemSelectedEvent;

import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;

import javax.inject.Inject;

public abstract class AbstractCollectionSetup<T extends MediaDescriptor, P extends GridViewPresentation<T>> extends AbstractSetup<T, P> {
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private ProductionPresentation.Factory productionPresentationFactory;

  @Override
  protected void configureCellFactory(MediaGridViewCellFactory<T> cellFactory) {
    cellFactory.setTitleBindProvider(item -> item.productionTitle);
    cellFactory.setSideBarTopLeftBindProvider(item -> item.productionYearRange);
    cellFactory.setSideBarCenterBindProvider(item -> new SimpleStringProperty(Optional.ofNullable(item.getData()).filter(Movie.class::isInstance).map(Movie.class::cast).map(Movie::getCollectionDetails).map(CollectionDetails::getDetails).map(Details::getName).orElse("")));
    cellFactory.setImageExtractor(item -> Optional.ofNullable(item.getProduction()).map(Production::getImage).map(imageHandleFactory::fromURI).orElse(null));
    cellFactory.setMediaStatusBindProvider(item -> item.mediaStatus);
  }

  @Override
  protected void onItemSelected(ItemSelectedEvent<MediaItem<T>> event, P presentation) {
    PresentationLoader.navigate(event, () -> productionPresentationFactory.create(event.getItem()));
  }
}
