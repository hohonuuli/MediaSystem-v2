package hs.mediasystem.ext.local;

import hs.ddif.annotations.PluginScoped;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.services.AbstractIdentificationService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@PluginScoped
public class LocalIdentificationService extends AbstractIdentificationService {
  private static final DataSource SERIE = DataSource.instance(MediaType.SERIE, "LOCAL");

  public LocalIdentificationService() {
    super(SERIE);
  }

  @Override
  public Optional<Identification> identify(Streamable streamable, MediaDescriptor parent) {
    return Optional.of(new Identification(List.of(new ProductionIdentifier(SERIE, streamable.getId().asString())), new Match(Type.MANUAL, 1.0f, Instant.now())));
  }
}
