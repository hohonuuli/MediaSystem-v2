package hs.mediasystem.ext.local;

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

import javax.inject.Singleton;

@Singleton
public class FileIdentificationService extends AbstractIdentificationService {
  private static final DataSource FILE = DataSource.instance(MediaType.of("FILE"), "LOCAL");

  public FileIdentificationService() {
    super(FILE);
  }

  @Override
  public Optional<Identification> identify(Streamable streamable, MediaDescriptor parent) {
    return Optional.of(new Identification(List.of(new ProductionIdentifier(FILE, streamable.getId().asString())), new Match(Type.MANUAL, 1.0f, Instant.now())));
  }
}