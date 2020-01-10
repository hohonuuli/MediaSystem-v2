package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.BasicStream;
import hs.mediasystem.domain.work.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.Tuple.Tuple3;

import java.util.Set;

public class MediaIdentification {
  private final Set<Exceptional<Tuple3<Identifier, Identification, MediaDescriptor>>> results;
  private final BasicStream media;

  public MediaIdentification(BasicStream media, Set<Exceptional<Tuple3<Identifier, Identification, MediaDescriptor>>> set) {
    this.media = media;
    this.results = set;
  }

  public Set<Exceptional<Tuple3<Identifier, Identification, MediaDescriptor>>> getResults() {
    return results;
  }

  public BasicStream getStream() {
    return media;
  }
}
