package hs.mediasystem.plugin.home;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Recommendation;
import hs.mediasystem.util.ImageURI;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Supplier;

public class RecommendationMenuOptionAdapter implements MenuOption {
  private final Recommendation recommendation;
  private final Supplier<? extends Presentation> presentationSupplier;

  public RecommendationMenuOptionAdapter(Recommendation recommendation, Supplier<? extends Presentation> presentationSupplier) {
    this.recommendation = recommendation;
    this.presentationSupplier = presentationSupplier;
  }

  private Details getDetails() {
    return recommendation.getWork().getDetails();
  }

  @Override
  public Supplier<? extends Presentation> getPresentationSupplier() {
    return presentationSupplier;
  }

  @Override
  public String getParentTitle() {
    return recommendation.getWork().getParent()
      .filter(p -> p.getType().equals(MediaType.SERIE))
      .map(Parent::getName)
      .orElse(null);
  }

  @Override
  public String getTitle() {
    return getDetails().getTitle();
  }

  @Override
  public String getSubtitle() {
    return recommendation.getWork().getParent()
      .filter(p -> p.getType().equals(MediaType.SERIE))
      .map(p -> "")
      .orElseGet(() -> recommendation.getWork().getDetails().getReleaseDate().map(LocalDate::getYear).map(Object::toString).orElse(null));
  }

  @Override
  public String getSequence() {
    return recommendation.getWork().getDetails().getSequence()
      .map(seq -> seq.getSeasonNumber().map(s -> s + "x").orElse("") + seq.getNumber())
      .orElse(null);
  }

  @Override
  public Optional<Instant> getRecommendationLastTimeWatched() {
    return Optional.of(recommendation.getLastTimeWatched());
  }

  @Override
  public Optional<ImageURI> getImage() {
    MediaType type = recommendation.getWork().getType();

    if(type.equals(MediaType.SERIE)) {
      return Optional.of(new ImageURI(
        "multi:800,450;0,0,800,450;517,50,233,350:" + getDetails().getBackdrop().get().getUri()
          + "," + getDetails().getImage().get().getUri()
      ));
    }

    if(type.equals(MediaType.MOVIE)) {
      return getDetails().getBackdrop();
    }

    return getDetails().getImage();
  }

  @Override
  public Optional<ImageURI> getBackdrop() {
    return getDetails().getBackdrop();
  }

  @Override
  public double getWatchedFraction() {
    if(recommendation.isWatched()) {
      return 1.0;
    }

    return recommendation.getLength().map(len -> recommendation.getPosition().toSeconds() / (double)len.toSeconds()).orElse(0.0);
  }
}