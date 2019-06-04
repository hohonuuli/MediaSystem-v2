package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.util.ImageURI;

import java.time.LocalDate;

public class Release implements DetailedMediaDescriptor {
  private final ProductionIdentifier identifier;
  private final Details details;
  private final Reception reception;

  public Release(ProductionIdentifier identifier, Details details, Reception reception) {
    if(identifier == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    if(details == null) {
      throw new IllegalArgumentException("details cannot be null");
    }

    this.identifier = identifier;
    this.details = details;
    this.reception = reception;
  }

  @Override
  public ProductionIdentifier getIdentifier() {
    return identifier;
  }

  @Override
  public Details getDetails() {
    return details;
  }

  public String getName() {
    return details.getName();
  }

  public String getDescription() {
    return details.getDescription();
  }

  public LocalDate getDate() {
    return details.getDate();
  }

  public ImageURI getImage() {
    return details.getImage();
  }

  public ImageURI getBackdrop() {
    return details.getBackdrop();
  }

  public Reception getReception() {
    return reception;
  }

  @Override
  public String toString() {
    return "Release[" + identifier + ": " + details + "]";
  }
}
